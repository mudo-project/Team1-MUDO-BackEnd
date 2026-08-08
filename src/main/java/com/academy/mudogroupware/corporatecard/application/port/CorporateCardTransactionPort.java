package com.academy.mudogroupware.corporatecard.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CorporateCardTransactionPort {
    TransactionPage findPage(Long academyId, int page, int size);
    Optional<TransactionView> find(Long academyId, Long transactionId);
    Optional<TransactionView> findForUpdate(Long academyId, Long transactionId);

    record TransactionPage(List<TransactionView> content, int page, int size, boolean hasNext) { }
    record TransactionView(Long id, LocalDateTime approvedAt, String approvalNumber, String merchantName,
                           String cardName, String cardNumberMasked, Integer installmentMonths, Long amount,
                           Long approvalTemplateId) { }
}
