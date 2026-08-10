package com.academy.mudogroupware.corporatecard.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.corporatecard.application.command.SubmitCardExpenseCommand;
import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubmitCardExpenseRequest(
        @NotBlank @Schema(example = "식대") String expenseCategory,
        @NotBlank String purpose,
        List<Long> approverIds) {
    public SubmitCardExpenseCommand toCommand(Long transactionId, Long userId) {
        return new SubmitCardExpenseCommand(transactionId, userId,
                ExpenseCategory.fromDisplayName(expenseCategory), purpose, approverIds);
    }
}
