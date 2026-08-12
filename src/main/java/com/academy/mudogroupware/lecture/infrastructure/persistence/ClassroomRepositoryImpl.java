package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.lecture.domain.model.Classroom;
import com.academy.mudogroupware.lecture.domain.repository.ClassroomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassroomRepositoryImpl implements ClassroomRepository {

    private final ClassroomJpaRepository classroomJpaRepository;

    @Override
    public Optional<Classroom> findByName(String name) {
        return classroomJpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public Optional<Classroom> findByNameForUpdate(String name) {
        return classroomJpaRepository.findByNameForUpdate(name).map(this::toDomain);
    }

    @Override
    public List<Classroom> findAllById(List<Long> ids) {
        return classroomJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Classroom save(Classroom classroom) {
        ClassroomEntity entity = ClassroomEntity.builder()
                .name(classroom.getName())
                .build();
        return toDomain(classroomJpaRepository.save(entity));
    }

    private Classroom toDomain(ClassroomEntity entity) {
        return Classroom.restore(entity.getId(), entity.getName(), entity.getCreatedAt());
    }
}
