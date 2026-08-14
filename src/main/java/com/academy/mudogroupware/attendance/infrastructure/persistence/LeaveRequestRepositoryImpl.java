package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
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
    public Set<Long> findApprovedUserIds(LocalDate date) {
        return new HashSet<>(leaveRequestJpaRepository
                .findUserIdsByStatusAndDateBetween(LeaveRequestStatus.APPROVED, date));
    }

    @Override
    public Map<LocalDate, Set<Long>> findApprovedUserIdsBetween(
            LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Set<Long>> result = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.put(date, new HashSet<>());
        }
        for (LeaveRequestJpaEntity leave : leaveRequestJpaRepository.findAllOverlapping(
                LeaveRequestStatus.APPROVED, startDate, endDate)) {
            LocalDate from = leave.getStartDate().isBefore(startDate)
                    ? startDate : leave.getStartDate();
            LocalDate to = leave.getEndDate().isAfter(endDate) ? endDate : leave.getEndDate();
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                result.get(date).add(leave.getUserId());
            }
        }
        return result;
    }

    @Override
    public boolean existsOverlapping(Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestJpaRepository.existsOverlapping(userId,
                Set.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED), startDate, endDate);
    }

    @Override
    public int sumReservedDays(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        return leaveRequestJpaRepository.sumUsedDays(userId,
                Set.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED), periodStart, periodEnd);
    }

    @Override
    public List<LeaveRequest> findApprovedOverlapping(
            Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestJpaRepository.findOverlapping(
                        userId, LeaveRequestStatus.APPROVED, startDate, endDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int sumUsedDaysByStatus(
            Long userId, LocalDate periodStart, LocalDate periodEnd,
            LeaveRequestStatus status) {
        return leaveRequestJpaRepository.sumUsedDays(
                userId, Set.of(status), periodStart, periodEnd);
    }

    private LeaveRequest toDomain(LeaveRequestJpaEntity entity) {
        return LeaveRequest.restore(entity.getId(), entity.getUserId(),
                entity.getDocumentId(), entity.getStartDate(), entity.getEndDate(), entity.getUsedDays(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
