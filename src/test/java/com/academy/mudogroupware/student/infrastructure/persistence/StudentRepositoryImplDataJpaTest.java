package com.academy.mudogroupware.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({StudentRepositoryImpl.class, StudentRepositoryImplDataJpaTest.AuditingConfig.class})
class StudentRepositoryImplDataJpaTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingConfig {
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Autowired
    private StudentRepositoryImpl studentRepository;

    @Test
    void countAllOnlyCountsNonDeletedStudents() {
        Student active = studentRepository.save(Student.create(
                "김민수", StudentGrade.HIGH_1, "무도고", "010-1111-2222", "010-3333-4444", null, NOW));
        Student toDelete = studentRepository.save(Student.create(
                "이영희", StudentGrade.HIGH_2, "무도고", "010-5555-6666", "010-7777-8888", null, NOW));
        studentRepository.markDeleted(toDelete.getId(), NOW);

        assertThat(studentRepository.countAll()).isEqualTo(1L);
    }
}
