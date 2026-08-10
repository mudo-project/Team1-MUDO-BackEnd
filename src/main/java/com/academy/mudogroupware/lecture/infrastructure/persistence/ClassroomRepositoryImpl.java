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
    public Optional<Classroom> findByAcademyIdAndName(Long academyId, String name) {
        return classroomJpaRepository.findByAcademyIdAndName(academyId, name).map(this::toDomain);
    }

    @Override
    public List<Classroom> findAllById(List<Long> ids) {
        return classroomJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Classroom save(Classroom classroom) {
        ClassroomEntity entity = ClassroomEntity.builder()
                .academyId(classroom.getAcademyId())
                .name(classroom.getName())
                .build();
        return toDomain(classroomJpaRepository.save(entity));
    }

    private Classroom toDomain(ClassroomEntity entity) {
        return Classroom.restore(entity.getId(), entity.getAcademyId(), entity.getName(), entity.getCreatedAt());
    }
}
