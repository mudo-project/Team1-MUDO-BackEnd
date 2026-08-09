package com.academy.mudogroupware.corporatecard.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

public interface CardExpensePort {
    Optional<ExpenseView> findByTransactionId(Long transactionId);
    Optional<ExpenseView> findForUpdate(Long transactionId, Long academyId);
    Map<Long, ExpenseView> findByTransactionIds(List<Long> transactionIds);
    ExpenseView create(Long transactionId, Long userId, ExpenseCategory category, String purpose,
                       Long approvalDocumentId, LocalDateTime now);
    ExpenseView update(Long transactionId, ExpenseCategory category, String purpose,
                       Long approvalDocumentId, LocalDateTime now);

    record ExpenseView(Long id, Long transactionId, Long userId, ExpenseCategory category,
                       String purpose, Long approvalDocumentId) { }
}
