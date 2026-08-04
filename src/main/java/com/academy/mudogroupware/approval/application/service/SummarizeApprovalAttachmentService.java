package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.SummarizeApprovalAttachmentCommand;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizerPort;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;
import com.academy.mudogroupware.approval.application.usecase.SummarizeApprovalAttachmentUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SummarizeApprovalAttachmentService implements SummarizeApprovalAttachmentUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final AttachmentSummarizerPort attachmentSummarizerPort;
    private final Clock clock;

    @Override
    public ApprovalAttachmentSummaryView summarize(SummarizeApprovalAttachmentCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.isApprover(command.requesterId())
                && !approvalDocument.getCreatorId().equals(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        ApprovalAttachment attachment = approvalDocument.findAttachmentByFileId(command.fileId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.ATTACHMENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);

        // TODO: file 모듈이 fileId → 실제 파일 내용(objectKey 등) 조회를 제공하면 이 placeholder를
        // 실제 첨부파일 내용으로 교체한다. 지금은 fileId만 있고 실제 파일에 접근할 방법이 없다.
        String placeholderContent = "첨부파일(fileId=" + attachment.getFileId() + ")의 내용을 한국어로 3문장 이내로 "
                + "요약해줘. 참고: 아직 실제 파일 내용을 읽어오는 연동이 없어 임시로 이 안내문을 대신 보낸다.";

        try {
            String summary = attachmentSummarizerPort.summarize(placeholderContent);
            attachment.applySummary(summary, now);
        } catch (AttachmentSummarizationException e) {
            attachment.markSummaryFailed(now);
            approvalDocumentRepository.save(approvalDocument);
            throw new ApprovalException(ApprovalErrorCode.SUMMARY_GENERATION_FAILED);
        }

        approvalDocumentRepository.save(approvalDocument);

        return new ApprovalAttachmentSummaryView(attachment.getFileId(), attachment.getAiSummary(),
                attachment.getSummaryStatus(), attachment.getSummarizedAt());
    }
}
