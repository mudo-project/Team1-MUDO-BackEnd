package com.academy.mudogroupware.corporatecard.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.corporatecard.application.port.ApprovalAttachmentFieldsPort.ExtractedReceiptFields;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort.TransactionView;
import com.academy.mudogroupware.corporatecard.application.query.ReceiptReconciliationView.FieldMatch;
import com.academy.mudogroupware.corporatecard.application.query.ReceiptReconciliationView.OverallStatus;

class ReceiptReconciliationViewTest {

    private TransactionView transaction(Long amount, String merchantName, LocalDateTime approvedAt) {
        return new TransactionView(1L, approvedAt, "APPROVAL-1", merchantName, "카드", "1234-**-**-5678", 1,
                amount, 1L);
    }

    @Test
    void matchesWhenAllFieldsAgree() {
        TransactionView transaction = transaction(45000L, "스타벅스 강남점", LocalDateTime.of(2026, 8, 5, 10, 0));
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(45000L, LocalDate.of(2026, 8, 5), "스타벅스 강남점");

        ReceiptReconciliationView result = ReceiptReconciliationView.of(transaction, extracted);

        assertThat(result.amountMatch()).isEqualTo(FieldMatch.MATCH);
        assertThat(result.merchantMatch()).isEqualTo(FieldMatch.MATCH);
        assertThat(result.dateMatch()).isEqualTo(FieldMatch.MATCH);
        assertThat(result.overallStatus()).isEqualTo(OverallStatus.MATCH);
    }

    @Test
    void flagsMismatchWhenAmountDiffers() {
        TransactionView transaction = transaction(45000L, "스타벅스 강남점", LocalDateTime.of(2026, 8, 5, 10, 0));
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(50000L, LocalDate.of(2026, 8, 5), "스타벅스 강남점");

        ReceiptReconciliationView result = ReceiptReconciliationView.of(transaction, extracted);

        assertThat(result.amountMatch()).isEqualTo(FieldMatch.MISMATCH);
        assertThat(result.overallStatus()).isEqualTo(OverallStatus.MISMATCH);
    }

    @Test
    void treatsPartialMerchantNameAsMatch() {
        TransactionView transaction = transaction(45000L, "스타벅스 강남점", LocalDateTime.of(2026, 8, 5, 10, 0));
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(45000L, LocalDate.of(2026, 8, 5), "스타벅스");

        ReceiptReconciliationView result = ReceiptReconciliationView.of(transaction, extracted);

        assertThat(result.merchantMatch()).isEqualTo(FieldMatch.MATCH);
        assertThat(result.overallStatus()).isEqualTo(OverallStatus.MATCH);
    }

    @Test
    void flagsMismatchWhenMerchantUnrelated() {
        TransactionView transaction = transaction(45000L, "스타벅스 강남점", LocalDateTime.of(2026, 8, 5, 10, 0));
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(45000L, LocalDate.of(2026, 8, 5), "이디야커피");

        ReceiptReconciliationView result = ReceiptReconciliationView.of(transaction, extracted);

        assertThat(result.merchantMatch()).isEqualTo(FieldMatch.MISMATCH);
        assertThat(result.overallStatus()).isEqualTo(OverallStatus.MISMATCH);
    }

    @Test
    void treatsMissingExtractedFieldsAsUnknownNotMismatch() {
        TransactionView transaction = transaction(45000L, "스타벅스 강남점", LocalDateTime.of(2026, 8, 5, 10, 0));
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(null, null, null);

        ReceiptReconciliationView result = ReceiptReconciliationView.of(transaction, extracted);

        assertThat(result.amountMatch()).isEqualTo(FieldMatch.UNKNOWN);
        assertThat(result.merchantMatch()).isEqualTo(FieldMatch.UNKNOWN);
        assertThat(result.dateMatch()).isEqualTo(FieldMatch.UNKNOWN);
        assertThat(result.overallStatus()).isEqualTo(OverallStatus.UNKNOWN);
    }
}
