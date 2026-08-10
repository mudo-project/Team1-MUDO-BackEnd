package com.academy.mudogroupware.approval.application.command;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;

public record CreateApprovalDocumentCommand(
        Long templateId,
        String title,
        ApprovalContentType contentType,
        String text,
        List<Long> fileIds,
        Long creatorId,
        List<Long> approverIds,
        LocalDate leaveStartDate,
        LocalDate leaveEndDate,
        ApprovalDocumentSourceType sourceType,
        // attendance 모듈이 아직 academyId로 연차 정책/잔여일수를 스코프하므로(단일 학원 전환 범위 밖)
        // 휴가 연동 결재를 attendance에 전달할 때만 사용한다. approval 자체 도메인은 이 값을 저장하지 않는다.
        Long academyId
) {
    public CreateApprovalDocumentCommand(Long templateId, String title, ApprovalContentType contentType,
                                         String text, List<Long> fileIds, Long creatorId,
                                         List<Long> approverIds, LocalDate leaveStartDate,
                                         LocalDate leaveEndDate, Long academyId) {
        this(templateId, title, contentType, text, fileIds, creatorId, approverIds,
                leaveStartDate, leaveEndDate, ApprovalDocumentSourceType.GENERAL, academyId);
    }
}
