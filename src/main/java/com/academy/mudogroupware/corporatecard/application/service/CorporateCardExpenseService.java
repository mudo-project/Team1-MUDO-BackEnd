package com.academy.mudogroupware.corporatecard.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.corporatecard.application.command.SubmitCardExpenseCommand;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;
import com.academy.mudogroupware.corporatecard.application.port.CardExpensePort;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;
import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;
import com.academy.mudogroupware.corporatecard.application.query.CardExpensePage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CorporateCardExpenseService {
    private final CorporateCardTransactionPort transactionPort;
    private final CardExpensePort expensePort;
    private final ApprovalSubmissionPort approvalSubmissionPort;

    @Transactional(readOnly = true)
    public CardExpensePage getTransactions(Long academyId, int page, int size) {
        var transactionPage = transactionPort.findPage(academyId, page, size);
        var transactionIds = transactionPage.content().stream().map(CorporateCardTransactionPort.TransactionView::id).toList();
        var expenses = expensePort.findByTransactionIds(transactionIds);
        var statuses = approvalSubmissionPort.findStatuses(expenses.values().stream()
                .map(CardExpensePort.ExpenseView::approvalDocumentId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        return new CardExpensePage(
                transactionPage.content().stream().map(t -> toView(t, expenses.get(t.id()), statuses)).toList(),
                transactionPage.page(), transactionPage.size(), transactionPage.hasNext());
    }

    @Transactional(readOnly = true)
    public CardExpenseView getTransaction(Long academyId, Long transactionId) {
        var transaction = transactionPort.find(academyId, transactionId)
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        var expense = expensePort.findByTransactionId(transactionId).orElse(null);
        var statuses = approvalSubmissionPort.findStatuses(expense == null || expense.approvalDocumentId() == null
                ? java.util.Set.of() : java.util.Set.of(expense.approvalDocumentId()));
        return toView(transaction, expense, statuses);
    }

    public CardExpenseView submit(SubmitCardExpenseCommand command, Long academyId) {
        var transaction = transactionPort.findForUpdate(academyId, command.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        var expense = expensePort.findForUpdate(command.transactionId(), academyId).orElse(null);
        if (expense != null && expense.approvalDocumentId() != null) {
            var status = approvalSubmissionPort.findStatus(expense.approvalDocumentId());
            if (status != null && !"REJECTED".equals(status.code())) {
                throw new IllegalStateException("진행 중이거나 승인된 정산 건은 다시 상신할 수 없습니다.");
            }
        }
        if (transaction.approvalTemplateId() == null) {
            throw new IllegalStateException("법인카드에 결재 템플릿이 설정되지 않았습니다.");
        }
        String title = "법인카드 사용내역 정산 - " + transaction.merchantName();
        String content = "사용 분류: " + command.expenseCategory().displayName() + "\n사용 내용: " + command.purpose();
        Long documentId = approvalSubmissionPort.submit(transaction.approvalTemplateId(), command.userId(), title, content);
        LocalDateTime now = LocalDateTime.now();
        CardExpensePort.ExpenseView saved = expense == null
                ? expensePort.create(transaction.id(), command.userId(), command.expenseCategory(), command.purpose(), documentId, now)
                : expensePort.update(transaction.id(), command.expenseCategory(), command.purpose(), documentId, now);
        return toView(transaction, saved, Map.of(documentId, new ApprovalSubmissionPort.ApprovalStatusView("IN_PROGRESS", "IN_PROGRESS")));
    }

    private CardExpenseView toView(CorporateCardTransactionPort.TransactionView transaction,
                                   CardExpensePort.ExpenseView expense,
                                   Map<Long, ApprovalSubmissionPort.ApprovalStatusView> statuses) {
        String status = "UNWRITTEN";
        if (expense != null) {
            status = "IN_PROGRESS";
            var approval = expense.approvalDocumentId() == null ? null : statuses.get(expense.approvalDocumentId());
            if (approval != null) {
                status = switch (approval.code()) {
                    case "APPROVED" -> "APPROVED";
                    case "REJECTED" -> "REJECTED";
                    default -> "IN_PROGRESS";
                };
            }
        }
        return new CardExpenseView(transaction.id(), transaction.approvedAt(), transaction.approvalNumber(),
                transaction.merchantName(), transaction.cardName(), transaction.cardNumberMasked(),
                transaction.installmentMonths(), transaction.amount(), expense == null ? null : expense.id(),
                expense == null ? null : expense.userId(), expense == null ? null : expense.category(),
                expense == null ? null : expense.purpose(), expense == null ? null : expense.approvalDocumentId(), status);
    }
}
