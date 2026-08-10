package com.academy.mudogroupware.corporatecard.application.command;

import java.util.List;

public record SubmitBatchCardExpensesCommand(
        List<Item> items,
        List<Long> approverIds
) {
    public record Item(Long transactionId) { }
}
