package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LeaveGrantPersistenceAdapter implements LeaveGrantRepository {

    private final LeaveGrantJpaRepository leaveGrantJpaRepository;

    @Override
    public LeaveGrant save(LeaveGrant leaveGrant) {
        return toDomain(leaveGrantJpaRepository.save(toEntity(leaveGrant)));
    }

    @Override
    public boolean existsByUserIdAndGrantDate(Long userId, LocalDate grantDate) {
        return leaveGrantJpaRepository.existsByUserIdAndGrantDate(userId, grantDate);
    }

    @Override
    public Optional<LeaveGrant> findActiveForUpdate(Long userId, LocalDate date) {
        return leaveGrantJpaRepository.findActiveForUpdate(userId, date).map(this::toDomain);
    }

    @Override
    public Optional<LeaveGrant> findActive(Long userId, LocalDate date) {
        return leaveGrantJpaRepository.findActive(userId, date).map(this::toDomain);
    }

    private LeaveGrantJpaEntity toEntity(LeaveGrant grant) {
        return LeaveGrantJpaEntity.builder()
                .id(grant.getId())
                .userId(grant.getUserId())
                .grantDate(grant.getGrantDate())
                .expirationDate(grant.getExpirationDate())
                .grantedDays(grant.getGrantedDays())
                .createdAt(grant.getCreatedAt())
                .build();
    }

    private LeaveGrant toDomain(LeaveGrantJpaEntity entity) {
        return LeaveGrant.restore(entity.getId(), entity.getUserId(), entity.getGrantDate(),
                entity.getExpirationDate(), entity.getGrantedDays(), entity.getCreatedAt());
    }
}
