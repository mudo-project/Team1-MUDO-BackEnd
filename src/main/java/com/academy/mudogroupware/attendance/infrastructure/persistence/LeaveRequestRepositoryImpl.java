package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LeaveRequestRepositoryImpl implements LeaveRequestRepository {

    private final LeaveRequestJpaRepository leaveRequestJpaRepository;

    @Override
    public LeaveRequest save(LeaveRequest leaveRequest) {
        LeaveRequestJpaEntity entity = LeaveRequestJpaEntity.builder()
                .id(leaveRequest.getId())
                .academyId(leaveRequest.getAcademyId())
                .userId(leaveRequest.getUserId())
                .documentId(leaveRequest.getDocumentId())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .usedDays(leaveRequest.getUsedDays())
                .status(leaveRequest.getStatus())
                .createdAt(leaveRequest.getCreatedAt())
                .updatedAt(leaveRequest.getUpdatedAt())
                .build();
        return toDomain(leaveRequestJpaRepository.save(entity));
    }

    @Override
    public Optional<LeaveRequest> findByDocumentId(Long documentId) {
        return leaveRequestJpaRepository.findByDocumentId(documentId).map(this::toDomain);
    }

    @Override
    public Set<Long> findApprovedUserIds(Long academyId, LocalDate date) {
        return new HashSet<>(leaveRequestJpaRepository
                .findUserIdsByAcademyIdAndStatusAndDateBetween(academyId, LeaveRequestStatus.APPROVED, date));
    }

    @Override
    public boolean existsOverlapping(Long academyId, Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestJpaRepository.existsOverlapping(academyId, userId,
                Set.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED), startDate, endDate);
    }

    @Override
    public int sumReservedDays(Long academyId, Long userId, LocalDate periodStart, LocalDate periodEnd) {
        return leaveRequestJpaRepository.sumUsedDays(academyId, userId,
                Set.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED), periodStart, periodEnd);
    }

    private LeaveRequest toDomain(LeaveRequestJpaEntity entity) {
        return LeaveRequest.restore(entity.getId(), entity.getAcademyId(), entity.getUserId(),
                entity.getDocumentId(), entity.getStartDate(), entity.getEndDate(), entity.getUsedDays(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
