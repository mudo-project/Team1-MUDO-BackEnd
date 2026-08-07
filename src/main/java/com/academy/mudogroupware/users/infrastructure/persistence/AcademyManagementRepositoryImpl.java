package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.Academy;
import com.academy.mudogroupware.users.domain.repository.AcademyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyManagementRepositoryImpl implements AcademyRepository {

    private final AcademyManagementJpaRepository academyManagementJpaRepository;

    @Override
    public Academy save(Academy academy) {
        AcademyEntity entity = AcademyEntity.builder()
                .name(academy.getName())
                .businessNo(academy.getBusinessNo())
                .userId(academy.getUserId())
                .applicationId(academy.getApplicationId())
                .status(academy.getStatus())
                .createdAt(academy.getCreatedAt())
                .updatedAt(academy.getCreatedAt())
                .build();
        return toDomain(academyManagementJpaRepository.save(entity));
    }

    @Override
    public void assignUser(Long academyId, Long userId, LocalDateTime updatedAt) {
        AcademyEntity entity = academyManagementJpaRepository.findById(academyId)
                .orElseThrow(() -> new IllegalStateException("승인 트랜잭션 안에서 방금 만든 academy를 찾을 수 없습니다: " + academyId));
        entity.assignUser(userId, updatedAt);
    }

    private Academy toDomain(AcademyEntity entity) {
        return Academy.restore(entity.getId(), entity.getName(), entity.getBusinessNo(), entity.getUserId(),
                entity.getApplicationId(), entity.getStatus(), entity.getCreatedAt());
    }
}
