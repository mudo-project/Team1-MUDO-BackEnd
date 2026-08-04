package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApproverDirectoryPortAdapter implements ApproverDirectoryPort {

    private final UserNameJpaRepository userNameJpaRepository;

    @Override
    public ApproverInfo getApprover(Long userId) {
        UserNameEntity entity = userNameJpaRepository.findById(userId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.APPROVER_NOT_FOUND));
        return new ApproverInfo(entity.getId(), entity.getName(), entity.getAcademyId());
    }

    @Override
    public Map<Long, ApproverInfo> getApprovers(List<Long> userIds) {
        return userNameJpaRepository.findAllById(userIds).stream()
                .map(entity -> new ApproverInfo(entity.getId(), entity.getName(), entity.getAcademyId()))
                .collect(Collectors.toMap(ApproverInfo::userId, Function.identity()));
    }
}
