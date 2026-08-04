package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.Academy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyRepositoryImpl implements AcademyRepository {

    private final AcademyJpaRepository academyJpaRepository;

    @Override
    public Optional<Academy> findByOwnerUserId(Long userId) {
        return academyJpaRepository.findByUserId(userId)
                .map(entity -> new Academy(entity.getId(), entity.getUserId()));
    }
}
