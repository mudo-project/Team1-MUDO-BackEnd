package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyRepositoryImpl implements AcademyRepository {

    private final AcademyJpaRepository academyJpaRepository;

    @Override
    public Optional<OwnedAcademy> findByOwnerUserId(Long userId) {
        return academyJpaRepository.findByUserId(userId)
                .map(entity -> new OwnedAcademy(entity.getId(), entity.getUserId()));
    }
}
