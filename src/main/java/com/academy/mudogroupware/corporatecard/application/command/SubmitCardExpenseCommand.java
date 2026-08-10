package com.academy.mudogroupware.corporatecard.application.command;

import java.util.List;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

public record SubmitCardExpenseCommand(Long transactionId, Long userId,
                                       ExpenseCategory expenseCategory, String purpose,
                                       List<Long> approverIds) { }
