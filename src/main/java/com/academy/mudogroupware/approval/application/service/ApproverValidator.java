package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

/**
 * 결재선에 지정하려는 approverId들이 실제 존재하고 같은 학원 소속인지 검증한다.
 * 존재하지 않거나 다른 학원 소속이면 결재선/문서 생성·수정 시점에 차단해, 없는 사용자나
 * 다른 학원 사용자가 결재선에 들어가는 것을 막는다.
 */
@Component
@RequiredArgsConstructor
class ApproverValidator {

    private final ApproverDirectoryPort approverDirectoryPort;

    void validate(List<Long> approverIds, Long academyId) {
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);
        for (Long approverId : approverIds) {
            ApproverInfo approver = approvers.get(approverId);
            if (approver == null) {
                throw new ApprovalException(ApprovalErrorCode.APPROVER_NOT_FOUND);
            }
            if (!approver.academyId().equals(academyId)) {
                throw new ApprovalException(ApprovalErrorCode.CROSS_ACADEMY_APPROVER);
            }
        }
    }
}
