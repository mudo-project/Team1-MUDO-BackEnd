package com.academy.mudogroupware.student.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.student.application.command.CreateStudentCommand;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentGrade;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

class CreateStudentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    private final FakeStudentRepository studentRepository = new FakeStudentRepository();
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final CurrentPlanProvider currentPlanProvider = unlimitedPlanProvider();
    private final CreateStudentService service = new CreateStudentService(
            studentRepository, clock, currentPlanProvider);

    private static CurrentPlanProvider unlimitedPlanProvider() {
        CurrentPlanProvider stub = mock(CurrentPlanProvider.class);
        when(stub.currentLimits()).thenReturn(PlanLimits.of(Plan.PAID));
        return stub;
    }

    @Test
    void createsStudentWithClockBasedTimestamp() {
        Long studentId = service.createStudent(new CreateStudentCommand(
                "김민수",
                StudentGrade.HIGH_1,
                "무도고",
                "010-1111-2222",
                "010-3333-4444",
                "수학 선행 중"
        ));

        Student saved = studentRepository.findById(studentId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("김민수");
        assertThat(saved.getGrade()).isEqualTo(StudentGrade.HIGH_1);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void throwsWhenStudentLimitReached() {
        for (int i = 0; i < 50; i++) {
            studentRepository.save(Student.create("학생" + i, StudentGrade.HIGH_1, "무도고",
                    "010-0000-0000", "010-0000-0001", null, NOW));
        }
        CurrentPlanProvider planProvider = mock(CurrentPlanProvider.class);
        when(planProvider.currentPlan()).thenReturn(Plan.FREE);
        when(planProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));
        CreateStudentService limitedService = new CreateStudentService(studentRepository, clock, planProvider);

        assertThatThrownBy(() -> limitedService.createStudent(new CreateStudentCommand(
                "51번째학생", StudentGrade.HIGH_1, "무도고", "010-1111-2222", "010-3333-4444", null)))
                .isInstanceOf(PlanLimitExceededException.class);
    }

    private static final class FakeStudentRepository implements StudentRepository {
        private final List<Student> students = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public Student save(Student student) {
            Student saved = Student.restore(sequence++, student.getName(),
                    student.getGrade(), student.getSchool(), student.getPhone(), student.getParentPhone(),
                    student.getNote(), student.getCreatedAt(), student.getUpdatedAt());
            students.add(saved);
            return saved;
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
        public void markDeleted(Long id, java.time.LocalDateTime deletedAt) {
            students.removeIf(student -> student.getId().equals(id));
        }

        @Override
        public long countAll() {
            return students.size();
        }
    }
}
