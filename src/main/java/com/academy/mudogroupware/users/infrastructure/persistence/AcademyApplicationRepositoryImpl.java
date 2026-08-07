package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyApplicationRepositoryImpl implements AcademyApplicationRepository {

    private final AcademyApplicationJpaRepository academyApplicationJpaRepository;

    @Override
    public List<AcademyApplication> findAll() {
        return academyApplicationJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AcademyApplication> findById(Long id) {
        return academyApplicationJpaRepository.findById(id).map(this::toDomain);
    }

    private AcademyApplication toDomain(AcademyApplicationEntity entity) {
        return AcademyApplication.restore(entity.getId(), entity.getRequestedLoginId(), entity.getAcademyName(),
                entity.getBusinessNo(), entity.getRepresentativeName(), entity.getRepresentativeEmail(),
                entity.getRepresentativePhone(), entity.getBusinessLicenseFileId(), entity.getStatus(),
                entity.getRejectReason(), entity.getReviewedByUserId(), entity.getReviewedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
