package com.academy.mudogroupware.approval.infrastructure.corporatecard;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.infrastructure.persistence.ApprovalDocumentJpaRepository;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: corporatecard
 * Purpose: 법인카드 정산 상신과 결재 상태 및 기본 결재선 조회
 */
@Component
@RequiredArgsConstructor
public class CorporateCardApprovalSubmissionAdapter implements ApprovalSubmissionPort {
    private final CreateApprovalDocumentUseCase createApprovalDocumentUseCase;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalDocumentJpaRepository approvalDocumentJpaRepository;

    /** Consumer: corporatecard / Purpose: 법인카드 정산용 독립 결재 문서 생성 */
    @Override
    public Long submit(Long templateId, Long creatorId, String title, String content, List<Long> approverIds) {
        return createApprovalDocumentUseCase.createDocument(new CreateApprovalDocumentCommand(
                templateId, title, ApprovalContentType.TEXT, content, null, creatorId, approverIds, null, null,
                ApprovalDocumentSourceType.CORPORATE_CARD_EXPENSE));
    }

    /** Consumer: corporatecard / Purpose: 단건 법인카드 정산 결재 상태 조회 */
    @Override
    public ApprovalStatusView findStatus(Long documentId) {
        return approvalDocumentRepository.findById(documentId)
                .map(document -> new ApprovalStatusView(document.getStatus().name(), document.getStatus().name()))
                .orElse(null);
    }

    /** Consumer: corporatecard / Purpose: 법인카드 시스템 템플릿의 기본 결재선 조회 */
    @Override
    public List<Long> findDefaultApproverIds(Long templateId) {
        return approvalTemplateRepository.findById(templateId)
                .map(template -> template.approverIdsInOrder())
                .orElse(List.of());
    }

    /** Consumer: corporatecard / Purpose: 최초 법인카드 결재선을 시스템 템플릿에 한 번만 저장 */
    @Override
    @Transactional
    public void saveDefaultApproverIdsIfEmpty(Long templateId, List<Long> approverIds) {
        var template = approvalTemplateRepository.findByIdForUpdate(templateId)
                .orElseThrow(() -> new IllegalStateException("법인카드 결재 템플릿을 찾을 수 없습니다."));
        if (template.approverIdsInOrder().isEmpty()) {
            template.update(template.getName(), approverIds, LocalDateTime.now());
            approvalTemplateRepository.save(template);
        }
    }

    /** Consumer: corporatecard / Purpose: 법인카드 정산 목록의 결재 상태 일괄 조회 */
    @Override
    public Map<Long, ApprovalStatusView> findStatuses(Set<Long> documentIds) {
        return approvalDocumentJpaRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(
                        document -> document.getId(),
                        document -> new ApprovalStatusView(document.getStatus().name(), document.getStatus().name())));
    }

    /** Consumer: corporatecard / Purpose: 법인카드 상세의 실제 문서 결재선 조회 */
    @Override
    public List<ApprovalLineInfo> findApprovalLines(Long documentId) {
        return approvalDocumentRepository.findById(documentId)
                .map(document -> document.getLines().stream()
                        .sorted(Comparator.comparingInt(line -> line.getStepOrder()))
                        .map(line -> new ApprovalLineInfo(line.getApproverId(), line.getStepOrder()))
                        .toList())
                .orElse(List.of());
    }
}
