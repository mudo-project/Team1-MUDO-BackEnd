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
    public Set<Long> findConfirmedUserIds(Long academyId, LocalDate date) {
        return new HashSet<>(leaveRequestJpaRepository
                .findUserIdsByAcademyIdAndStatusAndDateBetween(academyId, LeaveRequestStatus.CONFIRMED, date));
    }

    private LeaveRequest toDomain(LeaveRequestJpaEntity entity) {
        return LeaveRequest.restore(entity.getId(), entity.getAcademyId(), entity.getUserId(),
                entity.getDocumentId(), entity.getStartDate(), entity.getEndDate(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
