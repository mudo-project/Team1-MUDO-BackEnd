package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;
import com.academy.mudogroupware.global.domain.common.page.PagedResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CorporateCardTransactionPersistenceAdapter implements CorporateCardTransactionPort {
    private final CorporateCardTransactionJpaRepository repository;

    @Override
    public PagedResult<TransactionView> findPage(int page, int size) {
        var result = repository.findAllBy(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "approvedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return PagedResult.of(result.getContent().stream().map(this::toView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public Optional<TransactionView> find(Long transactionId) {
        return repository.findById(transactionId).map(this::toView);
    }

    @Override
    public Optional<TransactionView> findForUpdate(Long transactionId) {
        return repository.findForUpdate(transactionId).map(this::toView);
    }

    private TransactionView toView(CorporateCardTransactionJpaEntity entity) {
        return new TransactionView(entity.getId(), entity.getApprovedAt(), entity.getApprovalNumber(),
                entity.getMerchantName(), entity.getCard().getCardName(), entity.getCard().getCardNumberMasked(),
                entity.getInstallmentMonths(), entity.getAmount(), entity.getCard().getApprovalTemplateId());
    }
}
