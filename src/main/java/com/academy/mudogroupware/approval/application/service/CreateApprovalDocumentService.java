package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.port.LeaveRequestSubmissionPort;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateApprovalDocumentService implements CreateApprovalDocumentUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApproverValidator approverValidator;
    private final LeaveRequestSubmissionPort leaveRequestSubmissionPort;
    private final Clock clock;

    @Override
    public Long createDocument(CreateApprovalDocumentCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        List<Long> approverIds = (command.approverIds() != null && !command.approverIds().isEmpty())
                ? command.approverIds()
                : approvalTemplate.approverIdsInOrder();
        approverValidator.validate(approverIds);

        validateLeavePeriod(command.leaveStartDate(), command.leaveEndDate());

        ApprovalContent content = ApprovalContent.create(command.contentType(), command.text());
        LocalDateTime now = LocalDateTime.now(clock);
        ApprovalDocument approvalDocument = ApprovalDocument.create(
                approvalTemplate.getId(), command.sourceType(), command.title(), content,
                command.creatorId(), approverIds, command.fileIds(), now);

        Long documentId = approvalDocumentRepository.save(approvalDocument).getId();

        if (command.leaveStartDate() != null) {
            leaveRequestSubmissionPort.submit(documentId, command.academyId(), command.creatorId(),
                    command.leaveStartDate(), command.leaveEndDate(), now);
        }

        return documentId;
    }

    // 휴가 기간은 approval이 영구 저장하지 않는 선택 입력값이라, 둘 다 없거나(일반 결재) 둘 다
    // 있는(휴가 연동) 경우만 허용한다. 휴가 기간은 attendance의 동기 Port로 전달한다.
    private void validateLeavePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ApprovalException(ApprovalErrorCode.INVALID_LEAVE_PERIOD);
        }
    }
}
