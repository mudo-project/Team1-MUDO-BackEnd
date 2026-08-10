package com.academy.mudogroupware.corporatecard.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

public record CardExpenseView(
        Long transactionId, LocalDateTime approvedAt, String approvalNumber,
        String merchantName, String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
        Long expenseId, Long userId, ExpenseCategory expenseCategory, String purpose,
        Long approvalDocumentId, String status) { }
