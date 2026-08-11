package com.academy.mudogroupware.student.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;
import com.academy.mudogroupware.student.application.port.LectureCatalogPort;
import com.academy.mudogroupware.student.application.query.StudentDetail;
import com.academy.mudogroupware.student.application.query.StudentSummary;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentGrade;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

class StudentQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    private final FakeStudentRepository studentRepository = new FakeStudentRepository();
    private final FakeEnrollmentRepository enrollmentRepository = new FakeEnrollmentRepository();
    private final LectureCatalogPort lectureCatalogPort = lectureIds -> Map.of(
            100L, new LectureCatalogInfo(100L, "고1 수학", "박선생", "월 19:00", "MONTHLY", 300000)
    );
    private final StudentQueryService service =
            new StudentQueryService(studentRepository, enrollmentRepository, lectureCatalogPort);

    @Test
    void searchesStudentsByKeywordInKoreanNameOrder() {
        studentRepository.add(Student.restore(1L, "최지훈", StudentGrade.HIGH_1, "무도고",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));
        studentRepository.add(Student.restore(2L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0003", "010-0000-0004", null, NOW, NOW));
        studentRepository.add(Student.restore(3L, "박민서", StudentGrade.MIDDLE_3, "무도중",
                "010-0000-0005", "010-0000-0006", null, NOW, NOW));

        PageResult<StudentSummary> result = service.getStudents("민", 0, 20);

        assertThat(result.content()).extracting(StudentSummary::name)
                .containsExactly("김민수", "박민서");
    }

    @Test
    void getsStudentDetailWithActiveEnrollmentsAndLectureInfo() {
        studentRepository.add(Student.restore(1L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0003", "010-0000-0004", null, NOW, NOW));
        enrollmentRepository.add(Enrollment.restore(1L, 1L, 100L, EnrollmentStatus.ACTIVE, NOW, null));

        StudentDetail detail = service.getStudentDetail(1L);

        assertThat(detail.name()).isEqualTo("김민수");
        assertThat(detail.enrollments()).hasSize(1);
        assertThat(detail.enrollments().get(0).lectureName()).isEqualTo("고1 수학");
    }

    @Test
    void getsStudentsWithActiveEnrollmentCountsInOneBatch() {
        studentRepository.add(Student.restore(1L, "A", StudentGrade.HIGH_1, "School",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));
        studentRepository.add(Student.restore(2L, "B", StudentGrade.HIGH_1, "School",
                "010-0000-0003", "010-0000-0004", null, NOW, NOW));
        studentRepository.add(Student.restore(3L, "C", StudentGrade.HIGH_1, "School",
                "010-0000-0005", "010-0000-0006", null, NOW, NOW));
        enrollmentRepository.add(Enrollment.restore(1L, 1L, 100L, EnrollmentStatus.ACTIVE, NOW, null));
        enrollmentRepository.add(Enrollment.restore(2L, 1L, 101L, EnrollmentStatus.ACTIVE, NOW, null));
        enrollmentRepository.add(Enrollment.restore(3L, 2L, 102L, EnrollmentStatus.ACTIVE, NOW, null));

        PageResult<StudentSummary> result = service.getStudents(null, 0, 20);

        assertThat(result.content()).extracting(StudentSummary::activeEnrollmentCount)
                .containsExactly(2, 1, 0);
        assertThat(enrollmentRepository.countActiveByStudentIdsCalls).isEqualTo(1);
        assertThat(enrollmentRepository.findActiveByStudentIdCalls).isZero();
    }

    private static final class FakeStudentRepository implements StudentRepository {
        private final List<Student> students = new ArrayList<>();

        void add(Student student) {
            students.add(student);
        }

        @Override
        public Student save(Student student) {
            students.add(student);
            return student;
        }

        @Override
        public Optional<Student> findById(Long id) {
            return students.stream().filter(student -> student.getId().equals(id)).findFirst();
        }

        @Override
        public List<Student> findAllById(List<Long> ids) {
            return students.stream().filter(student -> ids.contains(student.getId())).toList();
        }

        @Override
        public PageResult<Student> findAll(String keyword, int page, int size) {
            List<Student> content = students.stream()
                    .filter(student -> keyword == null || keyword.isBlank() || student.getName().contains(keyword))
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .skip((long) page * size)
                    .limit(size + 1L)
                    .toList();
            boolean hasNext = content.size() > size;
            List<Student> pageContent = hasNext ? content.subList(0, size) : content;
            return PageResult.of(pageContent, page, size, hasNext);
        }

        @Override
        public void markDeleted(Long id, LocalDateTime deletedAt) {
            students.removeIf(student -> student.getId().equals(id));
        }
    }

    private static final class FakeEnrollmentRepository implements EnrollmentRepository {
        private final List<Enrollment> enrollments = new ArrayList<>();
        private int countActiveByStudentIdsCalls;
        private int findActiveByStudentIdCalls;

        void add(Enrollment enrollment) {
            enrollments.add(enrollment);
        }

        @Override
        public Enrollment save(Enrollment enrollment) {
            enrollments.add(enrollment);
            return enrollment;
        }

        @Override
        public Optional<Enrollment> findByStudentIdAndLectureId(Long studentId, Long lectureId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(enrollment -> enrollment.getLectureId().equals(lectureId))
                    .findFirst();
        }

        @Override
        public Optional<Enrollment> findById(Long studentId, Long enrollmentId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(enrollment -> enrollment.getId().equals(enrollmentId))
                    .findFirst();
        }

        @Override
        public List<Enrollment> findActiveByStudentId(Long studentId) {
            findActiveByStudentIdCalls++;
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(Enrollment::isActive)
                    .toList();
        }

        @Override
        public Map<Long, Long> countActiveByStudentIds(List<Long> studentIds) {
            countActiveByStudentIdsCalls++;
            return enrollments.stream()
                    .filter(enrollment -> studentIds.contains(enrollment.getStudentId()))
                    .filter(Enrollment::isActive)
                    .collect(Collectors.groupingBy(Enrollment::getStudentId, Collectors.counting()));
        }

        @Override
        public List<Enrollment> findActiveByLectureId(Long lectureId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getLectureId().equals(lectureId))
                    .filter(Enrollment::isActive)
                    .toList();
        }

        @Override
        public Map<Long, Long> countActiveByLectureIds(List<Long> lectureIds) {
            return enrollments.stream()
                    .filter(enrollment -> lectureIds.contains(enrollment.getLectureId()))
                    .filter(Enrollment::isActive)
                    .collect(Collectors.groupingBy(Enrollment::getLectureId, Collectors.counting()));
        }
    }
}
