package com.academy.mudogroupware.student.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentGrade;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

class EnrollStudentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 11, 0);

    private final FakeStudentRepository studentRepository = new FakeStudentRepository();
    private final FakeEnrollmentRepository enrollmentRepository = new FakeEnrollmentRepository();
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final EnrollStudentService service = new EnrollStudentService(studentRepository, enrollmentRepository,
            clock);

    @Test
    void enrollsStudentInLecture() {
        studentRepository.add(Student.restore(1L, 10L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));

        Long enrollmentId = service.enroll(new EnrollStudentCommand(10L, 1L, 100L));

        Enrollment saved = enrollmentRepository.findById(10L, 1L, enrollmentId).orElseThrow();
        assertThat(saved.getAcademyId()).isEqualTo(10L);
        assertThat(saved.getStudentId()).isEqualTo(1L);
        assertThat(saved.getLectureId()).isEqualTo(100L);
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(saved.getEnrolledAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsDuplicateActiveEnrollment() {
        studentRepository.add(Student.restore(1L, 10L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));

        service.enroll(new EnrollStudentCommand(10L, 1L, 100L));

        assertThatThrownBy(() -> service.enroll(new EnrollStudentCommand(10L, 1L, 100L)))
                .isInstanceOf(StudentException.class);
    }

    @Test
    void rejectsEnrollmentForStudentOutsideAcademy() {
        studentRepository.add(Student.restore(1L, 20L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));

        assertThatThrownBy(() -> service.enroll(new EnrollStudentCommand(10L, 1L, 100L)))
                .isInstanceOf(StudentException.class);
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
        public com.academy.mudogroupware.global.domain.common.page.PageResult<Student> findAll(
                Long academyId, String keyword, int page, int size) {
            return com.academy.mudogroupware.global.domain.common.page.PageResult.of(List.of(), page, size, false);
        }
    }

    private static final class FakeEnrollmentRepository implements EnrollmentRepository {
        private final List<Enrollment> enrollments = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Enrollment save(Enrollment enrollment) {
            Enrollment saved = Enrollment.restore(sequence++, enrollment.getAcademyId(), enrollment.getStudentId(),
                    enrollment.getLectureId(), enrollment.getStatus(), enrollment.getEnrolledAt(),
                    enrollment.getEndedAt());
            enrollments.add(saved);
            return saved;
        }

        @Override
        public Optional<Enrollment> findByStudentIdAndLectureId(Long academyId, Long studentId, Long lectureId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getAcademyId().equals(academyId))
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(enrollment -> enrollment.getLectureId().equals(lectureId))
                    .findFirst();
        }

        @Override
        public Optional<Enrollment> findById(Long academyId, Long studentId, Long enrollmentId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getAcademyId().equals(academyId))
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(enrollment -> enrollment.getId().equals(enrollmentId))
                    .findFirst();
        }

        @Override
        public List<Enrollment> findActiveByStudentId(Long academyId, Long studentId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getAcademyId().equals(academyId))
                    .filter(enrollment -> enrollment.getStudentId().equals(studentId))
                    .filter(Enrollment::isActive)
                    .toList();
        }

        @Override
        public List<Enrollment> findActiveByLectureId(Long academyId, Long lectureId) {
            return enrollments.stream()
                    .filter(enrollment -> enrollment.getAcademyId().equals(academyId))
                    .filter(enrollment -> enrollment.getLectureId().equals(lectureId))
                    .filter(Enrollment::isActive)
                    .toList();
        }
    }
}
