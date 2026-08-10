package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyApplicationRepositoryImpl implements AcademyApplicationRepository {

    private final AcademyApplicationJpaRepository academyApplicationJpaRepository;

    @Override
    public AcademyApplication save(AcademyApplication application) {
        AcademyApplicationEntity entity = AcademyApplicationEntity.builder()
                .requestedLoginId(application.getRequestedLoginId())
                .academyName(application.getAcademyName())
                .plan(application.getPlan())
                .representativeName(application.getRepresentativeName())
                .representativeEmail(application.getRepresentativeEmail())
                .representativePhone(application.getRepresentativePhone())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
        return toDomain(academyApplicationJpaRepository.save(entity));
    }

    @Override
    public List<AcademyApplication> findAll() {
        return academyApplicationJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AcademyApplication> findById(Long id) {
        return academyApplicationJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsActiveRequestedLoginId(String requestedLoginId) {
        return academyApplicationJpaRepository.existsByRequestedLoginIdAndStatusIn(
                requestedLoginId, List.of(AcademyApplicationStatus.PENDING, AcademyApplicationStatus.APPROVED));
    }

    @Override
    public void markApproved(Long id, Long reviewerId, LocalDateTime reviewedAt) {
        AcademyApplicationEntity entity = academyApplicationJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("방금 조회한 신청서를 찾을 수 없습니다: " + id));
        entity.markApproved(reviewerId, reviewedAt);
    }

    @Override
    public void markRejected(Long id, Long reviewerId, LocalDateTime reviewedAt, String reason) {
        AcademyApplicationEntity entity = academyApplicationJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("방금 조회한 신청서를 찾을 수 없습니다: " + id));
        entity.markRejected(reviewerId, reviewedAt, reason);
    }

    private AcademyApplication toDomain(AcademyApplicationEntity entity) {
        return AcademyApplication.restore(entity.getId(), entity.getRequestedLoginId(), entity.getAcademyName(),
                entity.getBusinessNo(), entity.getRepresentativeName(), entity.getRepresentativeEmail(),
                entity.getRepresentativePhone(), entity.getPlan(), entity.getBusinessLicenseFileId(),
                entity.getStatus(), entity.getRejectReason(), entity.getReviewedByUserId(), entity.getReviewedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
