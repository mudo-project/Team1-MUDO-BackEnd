package com.academy.mudogroupware.corporatecard.application.query;

import java.util.List;

public record BatchSubmitCardExpensesResult(
        int successCount,
        int failureCount,
        List<ItemResult> results
) {
    public record ItemResult(
            Long transactionId,
            boolean success,
            Long approvalDocumentId,
            String message
    ) { }
}
