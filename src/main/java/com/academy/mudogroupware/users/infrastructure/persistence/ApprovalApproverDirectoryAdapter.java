package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalApproverDirectoryAdapter implements ApproverDirectoryPort {

    private final UserRepository userRepository;

    /**
     * Consumer: approval
     * Purpose: Resolve approver identity and academy scope without approval mapping the users table.
     */
    @Override
    public ApproverInfo getApprover(Long userId) {
        return userRepository.findById(userId)
                .map(this::toApproverInfo)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.APPROVER_NOT_FOUND));
    }

    /**
     * Consumer: approval
     * Purpose: Batch resolve approval line users without exposing users entities.
     */
    @Override
    public Map<Long, ApproverInfo> getApprovers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(new LinkedHashSet<>(userIds)).stream()
                .map(this::toApproverInfo)
                .collect(Collectors.toMap(ApproverInfo::userId, Function.identity(), (left, right) -> left));
    }

    private ApproverInfo toApproverInfo(User user) {
        return new ApproverInfo(user.getId(), user.getName());
    }
}
