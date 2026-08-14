package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.corporatecard.application.query.CardExpenseDetailView;
import com.academy.mudogroupware.corporatecard.application.query.CardExpenseView;

public record CardExpenseDetailResponse(
        Long transactionId, LocalDateTime approvedAt, String approvalNumber,
        String merchantName, String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
        Long expenseId, Long userId, String expenseCategory,
        String purpose, Long approvalDocumentId, String status, List<ApprovalLineResponse> approvalLines) {

    public static CardExpenseDetailResponse from(CardExpenseDetailView detail) {
        CardExpenseView view = detail.expense();
        return new CardExpenseDetailResponse(
                view.transactionId(), view.approvedAt(), view.approvalNumber(), view.merchantName(),
                view.cardName(), view.cardNumberMasked(), view.installmentMonths(), view.amount(),
                view.expenseId(), view.userId(),
                view.expenseCategory() == null ? null : view.expenseCategory().displayName(), view.purpose(),
                view.approvalDocumentId(), view.status(), detail.approvalLines() == null ? null
                        : detail.approvalLines().stream().map(ApprovalLineResponse::from).toList());
    }
}
