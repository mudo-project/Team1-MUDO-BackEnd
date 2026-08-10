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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CorporateCardExpenseService {
    private final CorporateCardTransactionPort transactionPort;
    private final CardExpensePort expensePort;
    private final ApprovalSubmissionPort approvalSubmissionPort;

    @Transactional(readOnly = true)
    public CardExpensePage getTransactions(Long academyId, int page, int size) {
        log.info("event=corporate_card_transaction_list_read_시작 academyId={}, page={}, size={}", academyId, page, size);
        try {
        var transactionPage = transactionPort.findPage(academyId, page, size);
        var transactionIds = transactionPage.content().stream().map(CorporateCardTransactionPort.TransactionView::id).toList();
        var expenses = expensePort.findByTransactionIds(transactionIds);
        var statuses = approvalSubmissionPort.findStatuses(expenses.values().stream()
                .map(CardExpensePort.ExpenseView::approvalDocumentId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        CardExpensePage result = new CardExpensePage(
                transactionPage.content().stream().map(t -> toView(t, expenses.get(t.id()), statuses)).toList(),
                transactionPage.page(), transactionPage.size(), transactionPage.hasNext());
        log.info("event=corporate_card_transaction_list_read_완료 academyId={}, page={}, count={}",
                academyId, page, result.content().size());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_transaction_list_read_실패 academyId={}, page={}, reason={}",
                    academyId, page, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public CardExpenseView getTransaction(Long academyId, Long transactionId) {
        log.info("event=corporate_card_transaction_detail_read_시작 academyId={}, transactionId={}", academyId, transactionId);
        try {
        var transaction = transactionPort.find(academyId, transactionId)
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        var expense = expensePort.findByTransactionId(transactionId).orElse(null);
        var statuses = approvalSubmissionPort.findStatuses(expense == null || expense.approvalDocumentId() == null
                ? java.util.Set.of() : java.util.Set.of(expense.approvalDocumentId()));
        CardExpenseView result = toView(transaction, expense, statuses);
        log.info("event=corporate_card_transaction_detail_read_완료 academyId={}, transactionId={}, status={}",
                academyId, transactionId, result.status());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_transaction_detail_read_실패 academyId={}, transactionId={}, reason={}",
                    academyId, transactionId, e.getMessage());
            throw e;
        }
    }

    public CardExpenseView submit(SubmitCardExpenseCommand command, Long academyId) {
        log.info("event=corporate_card_expense_submit_시작 academyId={}, userId={}, transactionId={}",
                academyId, command.userId(), command.transactionId());
        try {
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
        Long documentId = approvalSubmissionPort.submit(
                transaction.approvalTemplateId(), command.userId(), title, content, command.approverIds());
        LocalDateTime now = LocalDateTime.now();
        CardExpensePort.ExpenseView saved = expense == null
                ? expensePort.create(transaction.id(), command.userId(), command.expenseCategory(), command.purpose(), documentId, now)
                : expensePort.update(transaction.id(), command.expenseCategory(), command.purpose(), documentId, now);
        CardExpenseView result = toView(transaction, saved,
                Map.of(documentId, new ApprovalSubmissionPort.ApprovalStatusView("IN_PROGRESS", "IN_PROGRESS")));
        log.info("event=corporate_card_expense_submit_완료 academyId={}, userId={}, transactionId={}, expenseId={}, approvalDocumentId={}",
                academyId, command.userId(), command.transactionId(), result.expenseId(), result.approvalDocumentId());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_expense_submit_실패 academyId={}, userId={}, transactionId={}, reason={}",
                    academyId, command.userId(), command.transactionId(), e.getMessage());
            throw e;
        }
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
