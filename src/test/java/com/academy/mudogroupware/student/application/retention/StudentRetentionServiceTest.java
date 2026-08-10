package com.academy.mudogroupware.student.application.retention;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.scheduler.RetentionJobResult;
import com.academy.mudogroupware.student.application.port.StudentRetentionPort;

class StudentRetentionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 3, 0);

    private final FakeStudentRetentionPort port = new FakeStudentRetentionPort();

    @Test
    void returnsEmptyResultWhenNoCandidates() {
        StudentRetentionService service = new StudentRetentionService(
                new StudentRetentionProperties(30, 500), port);

        RetentionJobResult result = service.hardDeleteExpiredStudents(NOW);

        assertThat(result).isEqualTo(RetentionJobResult.empty(StudentRetentionService.JOB_NAME));
        assertThat(port.deleteEnrollmentsCalls).isEmpty();
        assertThat(port.hardDeleteCalls).isEmpty();
    }

    @Test
    void hardDeletesChildrenBeforeParentsAndReturnsCounts() {
        port.candidateIds = List.of(1L, 2L, 3L);
        port.enrollmentDeleteCount = 5;
        port.studentDeleteCount = 3;
        StudentRetentionService service = new StudentRetentionService(
                new StudentRetentionProperties(30, 500), port);

        RetentionJobResult result = service.hardDeleteExpiredStudents(NOW);

        assertThat(result).isEqualTo(new RetentionJobResult(StudentRetentionService.JOB_NAME, 3, 5, 3));
        // 자식(수강 이력) 삭제가 부모(학생) 삭제보다 먼저 호출돼야 한다.
        assertThat(port.deleteEnrollmentsCalls).containsExactly(List.of(1L, 2L, 3L));
        assertThat(port.hardDeleteCalls).hasSize(1);
    }

    @Test
    void passesThresholdAndBatchSizeToCandidateLookup() {
        StudentRetentionProperties properties = new StudentRetentionProperties(7, 200);
        StudentRetentionService service = new StudentRetentionService(properties, port);

        service.hardDeleteExpiredStudents(NOW);

        assertThat(port.lastThreshold).isEqualTo(NOW.minusDays(7));
        assertThat(port.lastBatchSize).isEqualTo(200);
    }

    private static final class FakeStudentRetentionPort implements StudentRetentionPort {
        private List<Long> candidateIds = List.of();
        private int enrollmentDeleteCount;
        private int studentDeleteCount;
        private LocalDateTime lastThreshold;
        private int lastBatchSize;
        private final List<List<Long>> deleteEnrollmentsCalls = new ArrayList<>();
        private final List<List<Long>> hardDeleteCalls = new ArrayList<>();

        @Override
        public List<Long> findHardDeleteCandidateIds(LocalDateTime threshold, int batchSize) {
            this.lastThreshold = threshold;
            this.lastBatchSize = batchSize;
            return candidateIds;
        }

        @Override
        public int deleteEnrollmentsByStudentIds(List<Long> studentIds) {
            deleteEnrollmentsCalls.add(studentIds);
            return enrollmentDeleteCount;
        }

        @Override
        public int hardDeleteStudentsByIds(List<Long> studentIds, LocalDateTime threshold) {
            hardDeleteCalls.add(studentIds);
            return studentDeleteCount;
        }
    }
}
