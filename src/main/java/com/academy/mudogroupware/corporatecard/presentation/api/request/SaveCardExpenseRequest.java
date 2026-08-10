package com.academy.mudogroupware.corporatecard.presentation.api.request;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SaveCardExpenseRequest(
        @NotBlank
        @Schema(description = "사용 분류", example = "식비")
        String expenseCategory,
        @NotBlank
        @Schema(description = "사용 목적 및 내용", example = "8월 정기 강사회의 점심 식대")
        String purpose) {

    public ExpenseCategory category() {
        return ExpenseCategory.fromDisplayName(expenseCategory);
    }
}
