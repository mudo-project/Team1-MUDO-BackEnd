package com.academy.mudogroupware.corporatecard.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.corporatecard.application.command.SubmitBatchCardExpensesCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BatchSubmitCardExpensesRequest(
        @NotEmpty @Valid List<Item> items,
        List<Long> approverIds
) {
    public record Item(
            @NotNull Long transactionId
    ) {
        public SubmitBatchCardExpensesCommand.Item toCommand() {
            return new SubmitBatchCardExpensesCommand.Item(transactionId);
        }
    }

    public SubmitBatchCardExpensesCommand toCommand() {
        return new SubmitBatchCardExpensesCommand(items.stream().map(Item::toCommand).toList(), approverIds);
    }
}
