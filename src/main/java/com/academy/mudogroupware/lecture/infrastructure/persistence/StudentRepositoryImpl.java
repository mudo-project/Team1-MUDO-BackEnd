package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.lecture.domain.model.Student;
import com.academy.mudogroupware.lecture.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {

    private final StudentJpaRepository studentJpaRepository;

    @Override
    public Student save(Student student) {
        StudentEntity entity = StudentEntity.builder()
                .academyId(student.getAcademyId())
                .name(student.getName())
                .grade(student.getGrade())
                .school(student.getSchool())
                .phone(student.getPhone())
                .parentPhone(student.getParentPhone())
                .note(student.getNote())
                .build();
        return toDomain(studentJpaRepository.save(entity));
    }

    @Override
    public Optional<Student> findById(Long id) {
        return studentJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Student> findAllById(List<Long> ids) {
        return studentJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private Student toDomain(StudentEntity entity) {
        return Student.restore(entity.getId(), entity.getAcademyId(), entity.getName(), entity.getGrade(),
                entity.getSchool(), entity.getPhone(), entity.getParentPhone(), entity.getNote(),
                entity.getCreatedAt());
    }
}
