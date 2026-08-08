package com.academy.mudogroupware.corporatecard.application.command;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

public record SubmitCardExpenseCommand(Long transactionId, Long userId,
                                       ExpenseCategory expenseCategory, String purpose) { }
