package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.corporatecard.application.query.BatchSubmitCardExpensesResult;

public record BatchSubmitCardExpensesResponse(
        int successCount,
        int failureCount,
        List<Result> results
) {
    public static BatchSubmitCardExpensesResponse from(BatchSubmitCardExpensesResult result) {
        return new BatchSubmitCardExpensesResponse(
                result.successCount(),
                result.failureCount(),
                result.results().stream().map(item -> new Result(
                        item.transactionId(), item.success(), item.approvalDocumentId(), item.message())).toList());
    }

    public record Result(
            Long transactionId,
            boolean success,
            Long approvalDocumentId,
            String message
    ) { }
}
