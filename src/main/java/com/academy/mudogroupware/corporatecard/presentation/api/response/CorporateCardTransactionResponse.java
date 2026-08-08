package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;

public record CorporateCardTransactionResponse(
        Long transactionId,
        LocalDateTime approvedAt,
        String merchantName,
        String cardName,
        Long amount,
        String expenseCategory,
        String status) {
    public static CorporateCardTransactionResponse from(CardExpenseView view) {
        return new CorporateCardTransactionResponse(
                view.transactionId(), view.approvedAt(), view.merchantName(), view.cardName(),
                view.amount(), view.expenseCategory() == null ? null : view.expenseCategory().displayName(),
                view.status());
    }
}
