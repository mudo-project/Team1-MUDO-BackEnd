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
//
// 이미 존재하는 강의실에 동시 등록하는 시나리오만 다룬다. 완전히 새 강의실을 동시에 처음
// 만드는 경우는 findOrCreateClassroom의 주석 참고 — 알려진 한계로 남겨뒀다(갭 락 데드락과
// Hibernate 1차 캐시로 인한 잠금 우회를 둘 다 직접 재현해본 뒤 내린 결정).
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
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            Future<?> first = submitCreate(executor, ready, start,
                    LocalTime.of(19, 0), LocalTime.of(21, 0), successCount, conflictCount);
            Future<?> second = submitCreate(executor, ready, start,
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
                "select count(*) from lecture where classroom_code = ?", Integer.class, "601호");
        assertThat(savedLectures).isEqualTo(1);
    }

    private Future<?> submitCreate(ExecutorService executor, CountDownLatch ready, CountDownLatch start,
                                    LocalTime startTime, LocalTime endTime,
                                    AtomicInteger successCount, AtomicInteger conflictCount) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                createLectureService.createLecture(command(startTime, endTime));
                successCount.incrementAndGet();
            } catch (ClassroomTimeConflictException exception) {
                conflictCount.incrementAndGet();
            }
            return null;
        });
    }

    private CreateLectureCommand command(LocalTime startTime, LocalTime endTime) {
        return new CreateLectureCommand("동시 등록 테스트", ClassType.CLASS, "601호", null, null, null, null, null, null,
                List.of(new ScheduleInput(DayOfWeek.MONDAY, startTime, endTime)), 99L, null);
    }

    private void insertClassroom(String name) {
        LocalDateTime createdAt = LocalDateTime.now(Clock.system(ZoneId.of("Asia/Seoul")));
        jdbcTemplate.update(
                "insert into classroom (name, created_at) values (?, ?)", name, createdAt);
    }
}
