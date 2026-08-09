package com.academy.mudogroupware.corporatecard.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.corporatecard.application.query.CardExpensePage;

public record CorporateCardTransactionPageResponse(
        List<CorporateCardTransactionResponse> content,
        int page,
        int size,
        boolean hasNext) {
    public static CorporateCardTransactionPageResponse from(CardExpensePage page) {
        return new CorporateCardTransactionPageResponse(
                page.content().stream().map(CorporateCardTransactionResponse::from).toList(),
                page.page(), page.size(), page.hasNext());
    }
}
