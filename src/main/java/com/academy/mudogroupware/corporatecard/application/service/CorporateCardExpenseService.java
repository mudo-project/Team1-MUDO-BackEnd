package com.academy.mudogroupware.corporatecard.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.corporatecard.application.command.SubmitCardExpenseCommand;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;
import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;
import com.academy.mudogroupware.corporatecard.infrastructure.persistence.CardExpenseJpaEntity;
import com.academy.mudogroupware.corporatecard.infrastructure.persistence.CardExpenseJpaRepository;
import com.academy.mudogroupware.corporatecard.infrastructure.persistence.CorporateCardTransactionJpaEntity;
import com.academy.mudogroupware.corporatecard.infrastructure.persistence.CorporateCardTransactionJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CorporateCardExpenseService {
    private final CorporateCardTransactionJpaRepository transactionRepository;
    private final CardExpenseJpaRepository expenseRepository;
    private final ApprovalSubmissionPort approvalSubmissionPort;

    @Transactional(readOnly = true)
    public List<CardExpenseView> getTransactions(Long academyId) {
        return transactionRepository.findAllByCard_AcademyIdOrderByApprovedAtDesc(academyId)
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public CardExpenseView getTransaction(Long academyId, Long transactionId) {
        return transactionRepository.findByIdAndCard_AcademyId(transactionId, academyId)
                .map(this::toView)
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
    }

    public CardExpenseView submit(SubmitCardExpenseCommand command, Long academyId) {
        CorporateCardTransactionJpaEntity transaction = transactionRepository
                .findByIdAndCard_AcademyId(command.transactionId(), academyId)
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        CardExpenseJpaEntity expense = expenseRepository.findForUpdate(command.transactionId(), academyId).orElse(null);
        if (expense != null && expense.getApprovalDocumentId() != null) {
            ApprovalSubmissionPort.ApprovalStatusView status = approvalSubmissionPort.findStatus(expense.getApprovalDocumentId());
            if (status != null && !"REJECTED".equals(status.code())) {
                throw new IllegalStateException("진행 중이거나 승인된 정산 건은 다시 상신할 수 없습니다.");
            }
        }

        Long templateId = transaction.getCard().getApprovalTemplateId();
        if (templateId == null) {
            throw new IllegalStateException("법인카드에 결재 템플릿이 설정되지 않았습니다.");
        }
        String title = "법인카드 사용내역 정산 - " + transaction.getMerchantName();
        String content = "사용 분류: " + command.expenseCategory().displayName() + "\n사용 내용: " + command.purpose();
        Long documentId = approvalSubmissionPort.submit(templateId, command.userId(), title, content);
        LocalDateTime now = LocalDateTime.now();
        if (expense == null) {
            expense = new CardExpenseJpaEntity(transaction, command.userId(), command.expenseCategory(), command.purpose(), now);
            expense.assignApprovalDocumentId(documentId, now);
            expenseRepository.save(expense);
        } else {
            expense.update(command.expenseCategory(), command.purpose(), documentId, now);
        }
        return toView(transaction, expense);
    }

    private CardExpenseView toView(CorporateCardTransactionJpaEntity transaction) {
        return toView(transaction, expenseRepository.findByTransaction_Id(transaction.getId()).orElse(null));
    }

    private CardExpenseView toView(CorporateCardTransactionJpaEntity transaction, CardExpenseJpaEntity expense) {
        String status = "UNWRITTEN";
        if (expense != null) {
            status = "IN_PROGRESS";
            if (expense.getApprovalDocumentId() != null) {
                var approval = approvalSubmissionPort.findStatus(expense.getApprovalDocumentId());
                if (approval != null) status = switch (approval.code()) {
                    case "APPROVED" -> "APPROVED";
                    case "REJECTED" -> "REJECTED";
                    default -> "IN_PROGRESS";
                };
            }
        }
        return new CardExpenseView(transaction.getId(), transaction.getApprovedAt(), transaction.getApprovalNumber(),
                transaction.getMerchantName(), transaction.getCard().getCardName(), transaction.getCard().getCardNumberMasked(),
                transaction.getInstallmentMonths(), transaction.getAmount(), expense == null ? null : expense.getId(), expense == null ? null : expense.getUserId(),
                expense == null ? null : expense.getExpenseCategory(), expense == null ? null : expense.getPurpose(),
                expense == null ? null : expense.getApprovalDocumentId(), status);
    }
}
