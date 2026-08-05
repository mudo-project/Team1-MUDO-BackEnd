package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.lecture.domain.model.Subject;
import com.academy.mudogroupware.lecture.domain.repository.SubjectRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubjectRepositoryImpl implements SubjectRepository {

    private final SubjectJpaRepository subjectJpaRepository;

    @Override
    public Optional<Subject> findByAcademyIdAndName(Long academyId, String name) {
        return subjectJpaRepository.findByAcademyIdAndName(academyId, name).map(this::toDomain);
    }

    @Override
    public List<Subject> findAllById(List<Long> ids) {
        return subjectJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Subject save(Subject subject) {
        SubjectEntity entity = SubjectEntity.builder()
                .academyId(subject.getAcademyId())
                .name(subject.getName())
                .build();
        return toDomain(subjectJpaRepository.save(entity));
    }

    private Subject toDomain(SubjectEntity entity) {
        return Subject.restore(entity.getId(), entity.getAcademyId(), entity.getName(), entity.getCreatedAt());
    }
}
