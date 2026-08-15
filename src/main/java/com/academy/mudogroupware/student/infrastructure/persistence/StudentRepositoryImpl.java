package com.academy.mudogroupware.student.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.student.domain.model.Student;
import com.academy.mudogroupware.student.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {

    private final StudentJpaRepository studentJpaRepository;

    @Override
    public Student save(Student student) {
        StudentEntity entity = student.getId() == null ? toNewEntity(student) : updateExisting(student);
        return toDomain(studentJpaRepository.save(entity));
    }

    @Override
    public Optional<Student> findById(Long id) {
        return studentJpaRepository.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public List<Student> findAllById(List<Long> ids) {
        return studentJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<Student> findAll(String keyword, int page, int size) {
        Slice<StudentEntity> slice = studentJpaRepository.findAllByKeyword(
                keyword, PageRequest.of(page, size));
        List<Student> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public void markDeleted(Long id, LocalDateTime deletedAt) {
        studentJpaRepository.markDeleted(id, deletedAt);
    }

    @Override
    public long countAll() {
        return studentJpaRepository.countByDeletedAtIsNull();
    }

    private StudentEntity toNewEntity(Student student) {
        return StudentEntity.builder()
                .name(student.getName())
                .grade(student.getGrade())
                .school(student.getSchool())
                .phone(student.getPhone())
                .parentPhone(student.getParentPhone())
                .note(student.getNote())
                .build();
    }

    private StudentEntity updateExisting(Student student) {
        StudentEntity entity = studentJpaRepository.getReferenceById(student.getId());
        entity.setName(student.getName());
        entity.setGrade(student.getGrade());
        entity.setSchool(student.getSchool());
        entity.setPhone(student.getPhone());
        entity.setParentPhone(student.getParentPhone());
        entity.setNote(student.getNote());
        return entity;
    }

    private Student toDomain(StudentEntity entity) {
        return Student.restore(entity.getId(), entity.getName(), entity.getGrade(),
                entity.getSchool(), entity.getPhone(), entity.getParentPhone(), entity.getNote(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
