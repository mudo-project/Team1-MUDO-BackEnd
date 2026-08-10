package com.academy.mudogroupware.corporatecard.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CorporateCardTransactionPort {
    TransactionPage findPage(int page, int size);
    Optional<TransactionView> find(Long transactionId);
    Optional<TransactionView> findForUpdate(Long transactionId);

    record TransactionPage(List<TransactionView> content, int page, int size, boolean hasNext) { }
    record TransactionView(Long id, LocalDateTime approvedAt, String approvalNumber, String merchantName,
                           String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
                           Long approvalTemplateId) { }
}
