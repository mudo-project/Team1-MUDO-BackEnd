package com.academy.mudogroupware.corporatecard.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.global.domain.common.page.PagedResult;

public interface CorporateCardTransactionPort {
    PagedResult<TransactionView> findPage(int page, int size);
    Optional<TransactionView> find(Long transactionId);
    Optional<TransactionView> findForUpdate(Long transactionId);

    record TransactionView(Long id, LocalDateTime approvedAt, String approvalNumber, String merchantName,
                           String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
                           Long approvalTemplateId) { }
}
