package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;

public record CardExpenseResponse(
        Long transactionId, LocalDateTime approvedAt, String approvalNumber,
        String merchantName, String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
        Long expenseId, Long userId, String expenseCategory,
        String purpose, Long approvalDocumentId, String status) {
    public static CardExpenseResponse from(CardExpenseView view) {
        return new CardExpenseResponse(view.transactionId(), view.approvedAt(), view.approvalNumber(),
                view.merchantName(), view.cardName(), view.cardNumberMasked(), view.installmentMonths(), view.amount(), view.expenseId(),
                view.userId(), view.expenseCategory() == null ? null : view.expenseCategory().displayName(), view.purpose(),
                view.approvalDocumentId(), view.status());
    }
}
