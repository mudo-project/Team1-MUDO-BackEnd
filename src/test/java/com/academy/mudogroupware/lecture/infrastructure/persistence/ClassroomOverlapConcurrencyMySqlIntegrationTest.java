package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.service.CreateLectureService;
import com.academy.mudogroupware.lecture.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.lecture.domain.model.ClassType;

// 강의실 시간 충돌 검사(existsOverlap)와 강의 저장 사이의 race condition을 검증한다.
// findByNameForUpdate가 강의실 행을 잠그지 않으면, 동시에 들어온 두 요청이 모두 existsOverlap에서
// false를 보고 겹치는 시간대를 둘 다 저장할 수 있다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@Import({TimeConfig.class, ClassroomRepositoryImpl.class, LectureRepositoryImpl.class,
        TermRepositoryImpl.class, SubjectRepositoryImpl.class, CreateLectureService.class})
class ClassroomOverlapConcurrencyMySqlIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Autowired
    private CreateLectureService createLectureService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void onlyOneOfTwoConcurrentOverlappingRegistrationsSucceeds() throws Exception {
        insertClassroom("601호");
        runConcurrentOverlappingRegistrations("601호");
    }

    // 강의실이 아직 없는 상태(첫 등록)에서 동시에 겹치는 시간대로 등록하면, findByNameForUpdate가
    // 잠글 행이 없어 두 트랜잭션 모두 save를 시도할 수 있다. uk_classroom_name 유니크 제약에 걸려
    // 진 쪽이 DataIntegrityViolationException 대신 정상적으로 ClassroomTimeConflictException을
    // 받는지 검증한다(findOrCreateClassroom의 재조회 재시도 경로).
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void onlyOneOfTwoConcurrentOverlappingRegistrationsForBrandNewClassroomSucceeds() throws Exception {
        runConcurrentOverlappingRegistrations("701호");
    }

    private void runConcurrentOverlappingRegistrations(String classroomCode) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            Future<?> first = submitCreate(executor, ready, start, classroomCode,
                    LocalTime.of(19, 0), LocalTime.of(21, 0), successCount, conflictCount);
            Future<?> second = submitCreate(executor, ready, start, classroomCode,
                    LocalTime.of(20, 0), LocalTime.of(22, 0), successCount, conflictCount);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        Integer savedLectures = jdbcTemplate.queryForObject(
                "select count(*) from lecture where classroom_code = ?", Integer.class, classroomCode);
        assertThat(savedLectures).isEqualTo(1);
    }

    private Future<?> submitCreate(ExecutorService executor, CountDownLatch ready, CountDownLatch start,
                                    String classroomCode, LocalTime startTime, LocalTime endTime,
                                    AtomicInteger successCount, AtomicInteger conflictCount) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                createLectureService.createLecture(command(classroomCode, startTime, endTime));
                successCount.incrementAndGet();
            } catch (ClassroomTimeConflictException exception) {
                conflictCount.incrementAndGet();
            }
            return null;
        });
    }

    private CreateLectureCommand command(String classroomCode, LocalTime startTime, LocalTime endTime) {
        return new CreateLectureCommand("동시 등록 테스트", ClassType.CLASS, classroomCode, null, null, null, null, null,
                null, List.of(new ScheduleInput(DayOfWeek.MONDAY, startTime, endTime)), 99L, null);
    }

    private void insertClassroom(String name) {
        LocalDateTime createdAt = LocalDateTime.now(Clock.system(ZoneId.of("Asia/Seoul")));
        jdbcTemplate.update(
                "insert into classroom (name, created_at) values (?, ?)", name, createdAt);
    }
}
