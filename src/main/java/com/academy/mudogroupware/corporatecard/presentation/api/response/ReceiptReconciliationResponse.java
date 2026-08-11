package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.corporatecard.application.query.ReceiptReconciliationView;

public record ReceiptReconciliationResponse(
        Long transactionId,
        Long actualAmount,
        Long extractedAmount,
        String amountMatch,
        String actualMerchant,
        String extractedMerchant,
        String merchantMatch,
        LocalDate actualDate,
        LocalDate extractedDate,
        String dateMatch,
        String overallStatus) {

    public static ReceiptReconciliationResponse from(ReceiptReconciliationView view) {
        return new ReceiptReconciliationResponse(view.transactionId(), view.actualAmount(), view.extractedAmount(),
                view.amountMatch().name(), view.actualMerchant(), view.extractedMerchant(),
                view.merchantMatch().name(), view.actualDate(), view.extractedDate(), view.dateMatch().name(),
                view.overallStatus().name());
    }
}
