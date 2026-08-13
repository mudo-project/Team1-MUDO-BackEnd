package com.academy.mudogroupware.corporatecard.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.corporatecard.application.port.ApprovalAttachmentFieldsPort;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;
import com.academy.mudogroupware.corporatecard.application.port.CardExpensePort;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;
import com.academy.mudogroupware.corporatecard.application.query.ReceiptReconciliationView;
import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;
import com.academy.mudogroupware.global.domain.common.page.PagedResult;

class CorporateCardExpenseServiceTest {

    private static final Long TRANSACTION_ID = 1L;

    private final CorporateCardTransactionPort transactionPort = mock(CorporateCardTransactionPort.class);
    private final CardExpensePort expensePort = mock(CardExpensePort.class);
    private final ApprovalSubmissionPort approvalSubmissionPort = mock(ApprovalSubmissionPort.class);
    private final ApprovalAttachmentFieldsPort approvalAttachmentFieldsPort = mock(ApprovalAttachmentFieldsPort.class);

    private final CorporateCardExpenseService service = new CorporateCardExpenseService(
            transactionPort, expensePort, approvalSubmissionPort, approvalAttachmentFieldsPort);

    private CorporateCardTransactionPort.TransactionView transaction() {
        return new CorporateCardTransactionPort.TransactionView(TRANSACTION_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0), "APPROVAL-1", "스타벅스 강남점", "카드", "1234-**-**-5678", 1,
                45000L, 1L);
    }

    @Test
    void throwsWhenTransactionNotFound() {
        when(transactionPort.find(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reconcileReceipt(TRANSACTION_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenExpenseNotYetSubmitted() {
        when(transactionPort.find(TRANSACTION_ID)).thenReturn(Optional.of(transaction()));
        when(expensePort.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reconcileReceipt(TRANSACTION_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void throwsWhenExpenseHasNoApprovalDocumentYet() {
        when(transactionPort.find(TRANSACTION_ID)).thenReturn(Optional.of(transaction()));
        when(expensePort.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.of(
                new CardExpensePort.ExpenseView(1L, TRANSACTION_ID, 5L, ExpenseCategory.MEAL, "점심", null)));

        assertThatThrownBy(() -> service.reconcileReceipt(TRANSACTION_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnsReconciliationResultOnSuccess() {
        when(transactionPort.find(TRANSACTION_ID)).thenReturn(Optional.of(transaction()));
        when(expensePort.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.of(
                new CardExpensePort.ExpenseView(1L, TRANSACTION_ID, 5L, ExpenseCategory.MEAL, "점심", 99L)));
        when(approvalAttachmentFieldsPort.extractFields(99L)).thenReturn(
                new ApprovalAttachmentFieldsPort.ExtractedReceiptFields(45000L, LocalDate.of(2026, 8, 5), "스타벅스 강남점"));

        ReceiptReconciliationView result = service.reconcileReceipt(TRANSACTION_ID);

        assertThat(result.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.overallStatus()).isEqualTo(ReceiptReconciliationView.OverallStatus.MATCH);
    }

    @Test
    void returnsTransactionPageMetadata() {
        when(transactionPort.findPage(0, 20))
                .thenReturn(PagedResult.of(List.of(transaction()), 0, 20, 42));
        when(expensePort.findByTransactionIds(List.of(TRANSACTION_ID))).thenReturn(Map.of());
        when(approvalSubmissionPort.findStatuses(java.util.Set.of())).thenReturn(Map.of());

        var result = service.getTransactions(0, 20);

        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }
}
