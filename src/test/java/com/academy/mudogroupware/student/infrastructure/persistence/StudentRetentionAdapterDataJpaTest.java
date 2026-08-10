package com.academy.mudogroupware.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(StudentRetentionAdapter.class)
class StudentRetentionAdapterDataJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 3, 0);
    private static final LocalDateTime THRESHOLD = NOW.minusDays(30);

    @Autowired
    private StudentRetentionAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsOnlySoftDeletedStudentsOlderThanThresholdUpToBatchSize() {
        insertStudent(1L, "old-1", THRESHOLD.minusDays(1)); // 대상: threshold보다 오래됨
        insertStudent(2L, "old-2", THRESHOLD.minusDays(2)); // 대상
        insertStudent(3L, "recent", THRESHOLD.plusDays(1)); // 제외: threshold보다 최근
        insertStudent(4L, "not-deleted", null); // 제외: 소프트 삭제 안 됨

        List<Long> candidates = adapter.findHardDeleteCandidateIds(THRESHOLD, 10);

        assertThat(candidates).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void respectsBatchSizeLimit() {
        insertStudent(1L, "old-1", THRESHOLD.minusDays(3));
        insertStudent(2L, "old-2", THRESHOLD.minusDays(2));
        insertStudent(3L, "old-3", THRESHOLD.minusDays(1));

        List<Long> candidates = adapter.findHardDeleteCandidateIds(THRESHOLD, 2);

        assertThat(candidates).hasSize(2);
    }

    @Test
    void deletesEnrollmentsBeforeStudentsAndActuallyRemovesRows() {
        insertStudent(1L, "student-1", THRESHOLD.minusDays(1));
        insertEnrollment(100L, 1L);
        insertEnrollment(101L, 1L);

        int deletedEnrollments = adapter.deleteEnrollmentsByStudentIds(List.of(1L));
        int deletedStudents = adapter.hardDeleteStudentsByIds(List.of(1L), THRESHOLD);

        assertThat(deletedEnrollments).isEqualTo(2);
        assertThat(deletedStudents).isEqualTo(1);
        Integer remainingStudents = jdbcTemplate.queryForObject(
                "select count(*) from student where student_id = 1", Integer.class);
        Integer remainingEnrollments = jdbcTemplate.queryForObject(
                "select count(*) from student_enrollment where student_id = 1", Integer.class);
        assertThat(remainingStudents).isZero();
        assertThat(remainingEnrollments).isZero();
    }

    @Test
    void doesNotHardDeleteStudentWhoseDeletedAtIsNoLongerOlderThanThreshold() {
        // 후보 조회 이후 deleted_at이 threshold보다 최근으로 바뀐 경우(복구 등)를 가정 — 삭제되면 안 된다.
        insertStudent(1L, "recovered", THRESHOLD.plusDays(1));

        int deletedStudents = adapter.hardDeleteStudentsByIds(List.of(1L), THRESHOLD);

        assertThat(deletedStudents).isZero();
        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from student where student_id = 1", Integer.class);
        assertThat(remaining).isEqualTo(1);
    }

    private void insertStudent(long id, String name, LocalDateTime deletedAt) {
        jdbcTemplate.update("""
                insert into student (
                    student_id, academy_id, name, grade, created_at, updated_at, deleted_at
                ) values (?, 1, ?, 'HIGH_1', ?, ?, ?)
                """, id, name, NOW, NOW, deletedAt);
    }

    private void insertEnrollment(long enrollmentId, long studentId) {
        jdbcTemplate.update("""
                insert into student_enrollment (
                    enrollment_id, academy_id, student_id, lecture_id, status, enrolled_at, created_at, updated_at
                ) values (?, 1, ?, 999, 'ACTIVE', ?, ?, ?)
                """, enrollmentId, studentId, NOW, NOW, NOW);
    }
}
