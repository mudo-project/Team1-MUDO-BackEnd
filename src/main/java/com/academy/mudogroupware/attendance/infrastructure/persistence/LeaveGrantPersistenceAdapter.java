package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LeaveGrantPersistenceAdapter implements LeaveGrantRepository {

    private final LeaveGrantJpaRepository leaveGrantJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public LeaveGrant save(LeaveGrant leaveGrant) {
        return toDomain(leaveGrantJpaRepository.save(toEntity(leaveGrant)));
    }

    @Override
    public boolean saveIfAbsent(LeaveGrant leaveGrant) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO leave_grant
                        (user_id, grant_date, expiration_date, granted_days, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    leaveGrant.getUserId(), leaveGrant.getGrantDate(),
                    leaveGrant.getExpirationDate(), leaveGrant.getGrantedDays(),
                    leaveGrant.getCreatedAt());
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
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
