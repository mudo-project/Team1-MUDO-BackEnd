package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.corporatecard.application.port.CardExpensePort;
import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CardExpensePersistenceAdapter implements CardExpensePort {
    private final CardExpenseJpaRepository repository;
    private final CorporateCardTransactionJpaRepository transactionRepository;

    @Override public Optional<ExpenseView> findByTransactionId(Long transactionId) {
        return repository.findByTransaction_Id(transactionId).map(this::toView);
    }
    @Override public Optional<ExpenseView> findForUpdate(Long transactionId, Long academyId) {
        return repository.findForUpdate(transactionId, academyId).map(this::toView);
    }
    @Override public Map<Long, ExpenseView> findByTransactionIds(List<Long> transactionIds) {
        return repository.findAllByTransaction_IdIn(transactionIds).stream().map(this::toView)
                .collect(Collectors.toMap(ExpenseView::transactionId, Function.identity()));
    }
    @Override public ExpenseView create(Long transactionId, Long userId, ExpenseCategory category, String purpose,
                                        Long approvalDocumentId, LocalDateTime now) {
        var transaction = transactionRepository.getReferenceById(transactionId);
        var saved = repository.save(new CardExpenseJpaEntity(transaction, userId, category, purpose, now));
        saved.assignApprovalDocumentId(approvalDocumentId, now);
        return toView(saved);
    }
    @Override public ExpenseView update(Long transactionId, ExpenseCategory category, String purpose,
                                        Long approvalDocumentId, LocalDateTime now) {
        var expense = repository.findByTransaction_Id(transactionId).orElseThrow();
        expense.update(category, purpose, approvalDocumentId, now);
        return toView(expense);
    }
    private ExpenseView toView(CardExpenseJpaEntity e) {
        return new ExpenseView(e.getId(), e.getTransaction().getId(), e.getUserId(), e.getExpenseCategory(), e.getPurpose(), e.getApprovalDocumentId());
    }
}
