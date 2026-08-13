package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.corporatecard.application.query.CardExpensePage;

public record CorporateCardTransactionPageResponse(
        List<CorporateCardTransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious) {
    public static CorporateCardTransactionPageResponse from(CardExpensePage page) {
        return new CorporateCardTransactionPageResponse(
                page.content().stream().map(CorporateCardTransactionResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.first(), page.last(), page.hasNext(), page.hasPrevious());
    }
}
