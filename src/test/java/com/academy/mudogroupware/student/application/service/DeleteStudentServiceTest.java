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

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.application.command.DeleteStudentCommand;
import com.academy.mudogroupware.student.domain.exception.StudentException;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentGrade;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

class DeleteStudentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 10, 0);

    private final FakeStudentRepository studentRepository = new FakeStudentRepository();
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final DeleteStudentService service = new DeleteStudentService(studentRepository, clock);

    @Test
    void softDeletesStudent() {
        studentRepository.add(Student.restore(1L, "김민수", StudentGrade.HIGH_1, "무도고",
                "010-0000-0001", "010-0000-0002", null, NOW, NOW));

        service.deleteStudent(new DeleteStudentCommand(1L));

        assertThat(studentRepository.findById(1L)).isEmpty();
        assertThat(studentRepository.deletedIds).containsExactly(1L);
    }

    @Test
    void rejectsDeleteForMissingStudent() {
        assertThatThrownBy(() -> service.deleteStudent(new DeleteStudentCommand(999L)))
                .isInstanceOf(StudentException.class);
    }

    private static final class FakeStudentRepository implements StudentRepository {
        private final List<Student> students = new ArrayList<>();
        private final List<Long> deletedIds = new ArrayList<>();

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
            return PageResult.of(List.of(), page, size, false);
        }

        @Override
        public void markDeleted(Long id, LocalDateTime deletedAt) {
            deletedIds.add(id);
            students.removeIf(student -> student.getId().equals(id));
        }
    }
}
