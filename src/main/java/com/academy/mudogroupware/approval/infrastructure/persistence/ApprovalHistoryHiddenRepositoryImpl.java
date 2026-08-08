package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.repository.ApprovalHistoryHiddenRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApprovalHistoryHiddenRepositoryImpl implements ApprovalHistoryHiddenRepository {

    private final ApprovalHistoryHiddenJpaRepository approvalHistoryHiddenJpaRepository;

    @Override
    public boolean exists(Long documentId, Long userId) {
        return approvalHistoryHiddenJpaRepository.existsByApprovalDocumentIdAndUserId(documentId, userId);
    }

    @Override
    public void save(Long documentId, Long userId, LocalDateTime hiddenAt) {
        ApprovalHistoryHiddenEntity entity = ApprovalHistoryHiddenEntity.builder()
                .approvalDocumentId(documentId)
                .userId(userId)
                .hiddenAt(hiddenAt)
                .build();
        approvalHistoryHiddenJpaRepository.save(entity);
    }
}
