package com.academy.mudogroupware.corporatecard.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.academy.mudogroupware.corporatecard.application.command.SubmitCardExpenseCommand;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;
import com.academy.mudogroupware.corporatecard.application.port.CardExpensePort;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;
import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;
import com.academy.mudogroupware.corporatecard.application.query.CardExpensePage;
import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CorporateCardExpenseService {
    private static final Long CORPORATE_CARD_APPROVAL_TEMPLATE_ID = 1L;

    private final CorporateCardTransactionPort transactionPort;
    private final CardExpensePort expensePort;
    private final ApprovalSubmissionPort approvalSubmissionPort;

    @Transactional(readOnly = true)
    public CardExpensePage getTransactions(int page, int size) {
        log.info("event=corporate_card_transaction_list_read_시작 page={}, size={}", page, size);
        try {
        var transactionPage = transactionPort.findPage(page, size);
        var transactionIds = transactionPage.content().stream().map(CorporateCardTransactionPort.TransactionView::id).toList();
        var expenses = expensePort.findByTransactionIds(transactionIds);
        var statuses = approvalSubmissionPort.findStatuses(expenses.values().stream()
                .map(CardExpensePort.ExpenseView::approvalDocumentId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        CardExpensePage result = new CardExpensePage(
                transactionPage.content().stream().map(t -> toView(t, expenses.get(t.id()), statuses)).toList(),
                transactionPage.page(), transactionPage.size(), transactionPage.hasNext());
        log.info("event=corporate_card_transaction_list_read_완료 page={}, count={}", page, result.content().size());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_transaction_list_read_실패 page={}, reason={}", page, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public CardExpenseView getTransaction(Long transactionId) {
        log.info("event=corporate_card_transaction_detail_read_시작 transactionId={}", transactionId);
        try {
        var transaction = transactionPort.find(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        var expense = expensePort.findByTransactionId(transactionId).orElse(null);
        var statuses = approvalSubmissionPort.findStatuses(expense == null || expense.approvalDocumentId() == null
                ? java.util.Set.of() : java.util.Set.of(expense.approvalDocumentId()));
        CardExpenseView result = toView(transaction, expense, statuses);
        log.info("event=corporate_card_transaction_detail_read_완료 transactionId={}, status={}", transactionId, result.status());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_transaction_detail_read_실패 transactionId={}, reason={}", transactionId, e.getMessage());
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CardExpenseView submit(SubmitCardExpenseCommand command) {
        log.info("event=corporate_card_expense_submit_시작 userId={}, transactionId={}", command.userId(), command.transactionId());
        try {
        var transaction = transactionPort.findForUpdate(command.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
        var expense = expensePort.findForUpdate(command.transactionId()).orElse(null);
        if (expense != null && expense.approvalDocumentId() != null) {
            var status = approvalSubmissionPort.findStatus(expense.approvalDocumentId());
            if (status != null && !"REJECTED".equals(status.code())) {
                throw new IllegalStateException("진행 중이거나 승인된 정산 건은 다시 상신할 수 없습니다.");
            }
        }
        List<Long> requestedApproverIds = normalizeApproverIds(command.approverIds());
        List<Long> defaultApproverIds = approvalSubmissionPort.findDefaultApproverIds(
                CORPORATE_CARD_APPROVAL_TEMPLATE_ID);
        if (defaultApproverIds.isEmpty() && requestedApproverIds == null) {
            throw new IllegalStateException("법인카드 결재선을 지정해야 합니다.");
        }
        if (defaultApproverIds.isEmpty()) {
            approvalSubmissionPort.saveDefaultApproverIdsIfEmpty(
                    CORPORATE_CARD_APPROVAL_TEMPLATE_ID, requestedApproverIds);
        }
        String title = "법인카드 사용내역 정산 - " + transaction.merchantName();
        String content = "사용 분류: " + command.expenseCategory().displayName() + "\n사용 내용: " + command.purpose();
        Long documentId = approvalSubmissionPort.submit(
                CORPORATE_CARD_APPROVAL_TEMPLATE_ID, command.userId(), title, content, command.approverIds());
        LocalDateTime now = LocalDateTime.now();
        CardExpensePort.ExpenseView saved = expense == null
                ? expensePort.create(transaction.id(), command.userId(), command.expenseCategory(), command.purpose(), documentId, now)
                : expensePort.update(transaction.id(), command.expenseCategory(), command.purpose(), documentId, now);
        CardExpenseView result = toView(transaction, saved,
                Map.of(documentId, new ApprovalSubmissionPort.ApprovalStatusView("IN_PROGRESS", "IN_PROGRESS")));
        log.info("event=corporate_card_expense_submit_완료 userId={}, transactionId={}, expenseId={}, approvalDocumentId={}",
                command.userId(), command.transactionId(), result.expenseId(), result.approvalDocumentId());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_expense_submit_실패 userId={}, transactionId={}, reason={}",
                    command.userId(), command.transactionId(), e.getMessage());
            throw e;
        }
    }

    private List<Long> normalizeApproverIds(List<Long> approverIds) {
        if (approverIds == null || approverIds.isEmpty()) {
            return null;
        }
        if (approverIds.stream().anyMatch(Objects::isNull)
                || approverIds.stream().distinct().count() != approverIds.size()) {
            throw new IllegalArgumentException("중복되거나 올바르지 않은 결재자가 포함되어 있습니다.");
        }
        return List.copyOf(approverIds);
    }

    @Transactional
    public CardExpenseView saveExpense(Long transactionId, Long userId, ExpenseCategory category, String purpose) {
        log.info("event=corporate_card_expense_save_start userId={}, transactionId={}", userId, transactionId);
        try {
            var transaction = transactionPort.findForUpdate(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다."));
            var expense = expensePort.findForUpdate(transactionId).orElse(null);
            if (expense != null && expense.approvalDocumentId() != null) {
                var status = approvalSubmissionPort.findStatus(expense.approvalDocumentId());
                if (status != null && !"REJECTED".equals(status.code())) {
                    throw new IllegalStateException("진행 중이거나 승인된 정산 정보는 수정할 수 없습니다.");
                }
            }

            LocalDateTime now = LocalDateTime.now();
            CardExpensePort.ExpenseView saved = expense == null
                    ? expensePort.create(transactionId, userId, category, purpose, null, now)
                    : expensePort.update(transactionId, category, purpose, expense.approvalDocumentId(), now);
            var statuses = saved.approvalDocumentId() == null
                    ? Map.<Long, ApprovalSubmissionPort.ApprovalStatusView>of()
                    : approvalSubmissionPort.findStatuses(java.util.Set.of(saved.approvalDocumentId()));
            CardExpenseView result = toView(transaction, saved, statuses);
            log.info("event=corporate_card_expense_save_success userId={}, transactionId={}, expenseId={}",
                    userId, transactionId, result.expenseId());
            return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_expense_save_failure userId={}, transactionId={}, reason={}",
                    userId, transactionId, e.getMessage());
            throw e;
        }
    }

    private CardExpenseView toView(CorporateCardTransactionPort.TransactionView transaction,
                                   CardExpensePort.ExpenseView expense,
                                   Map<Long, ApprovalSubmissionPort.ApprovalStatusView> statuses) {
        String status = "UNWRITTEN";
        if (expense != null) {
            status = expense.approvalDocumentId() == null ? "UNWRITTEN" : "IN_PROGRESS";
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
