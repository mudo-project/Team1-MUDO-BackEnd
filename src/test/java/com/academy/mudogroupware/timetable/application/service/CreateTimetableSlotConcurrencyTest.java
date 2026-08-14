package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CreateTimetableSlotConcurrencyTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private CreateTimetableSetUseCase createTimetableSetUseCase;
    @Autowired private CreateTimetableSlotUseCase createTimetableSlotUseCase;

    @Test
    void onlyOneOfTwoConcurrentCreatesForTheSameClassroomAndTimeSucceeds() throws Exception {
        Long timetableSetId = createTimetableSetUseCase.createTimetableSet(new CreateTimetableSetCommand(
                "동시성 테스트 세트", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                LocalTime.of(8, 0), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601"))));

        int attempts = 2;
        CountDownLatch readyLatch = new CountDownLatch(attempts);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            List<CompletableFuture<Void>> futures = List.of(1, 2).stream()
                    .map(i -> CompletableFuture.runAsync(() -> {
                        readyLatch.countDown();
                        awaitUninterruptibly(startLatch);
                        try {
                            createTimetableSlotUseCase.createSlot(new CreateTimetableSlotCommand(
                                    timetableSetId, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                                    LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분", "FFCC00"));
                            successCount.incrementAndGet();
                        } catch (com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException e) {
                            conflictCount.incrementAndGet();
                        }
                    }, executor))
                    .toList();

            readyLatch.await();
            startLatch.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } finally {
            executor.shutdown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
