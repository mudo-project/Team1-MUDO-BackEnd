package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import({TimeConfig.class, TimetableSetPersistenceAdapter.class})
class TimetableSetPersistenceAdapterDataJpaTest {

    @Autowired
    private TimetableSetPersistenceAdapter adapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsTimetableSetWithClassrooms() {
        TimetableSet set = TimetableSet.create(
                "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY),
                30, List.of(new TimetableClassroom("6층", "601")));

        TimetableSet saved = adapter.save(set);
        Optional<TimetableSet> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("2026 여름특강");
        assertThat(found.get().getClassrooms()).containsExactly(new TimetableClassroom("6층", "601"));
    }

    @Test
    void savesTimetableSetWithAllSevenOperatingDays() {
        TimetableSet set = TimetableSet.create(
                "매일 운영", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.values()),
                30, List.of(new TimetableClassroom("6층", "601")));

        TimetableSet saved = adapter.save(set);
        entityManager.flush();
        entityManager.clear();
        Optional<TimetableSet> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOperatingDays()).containsExactlyInAnyOrder(DayOfWeek.values());
    }

    @Test
    void deletesTimetableSetById() {
        TimetableSet set = TimetableSet.create(
                "삭제될 세트", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY),
                30, List.of(new TimetableClassroom("6층", "601")));
        TimetableSet saved = adapter.save(set);

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    @Test
    void findAllDoesNotIssueOneQueryPerSetForClassrooms() {
        for (int i = 0; i < 3; i++) {
            TimetableSet set = TimetableSet.create(
                    "세트 " + i, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                    LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                    List.of(new TimetableClassroom("6층", "601-" + i), new TimetableClassroom("5층", "501-" + i)));
            adapter.save(set);
        }
        entityManager.flush();
        entityManager.clear();

        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
        org.hibernate.stat.Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        List<TimetableSet> found = adapter.findAll();

        assertThat(found).hasSize(3);
        // Statistics.getQueryExecutionCount()는 명시적 HQL/JPQL 쿼리 실행만 집계하며, EAGER 컬렉션을
        // 엔티티별로 초기화할 때 발생하는 묵시적 select는 집계하지 않는다(N+1이 있어도 항상 1로 남는다).
        // 실제 DB로 나가는 SQL 왕복 횟수를 재려면 getPrepareStatementCount()를 써야 한다:
        // 세트 목록 1번 + classrooms 컬렉션 로딩(N+1이면 세트당 1번, SUBSELECT면 총 1번)을 모두 집계한다.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
