package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CorporateCardTransactionPersistenceAdapter implements CorporateCardTransactionPort {
    private final CorporateCardTransactionJpaRepository repository;

    @Override
    public TransactionPage findPage(Long academyId, int page, int size) {
        var slice = repository.findAllByCard_AcademyId(academyId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "approvedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new TransactionPage(slice.getContent().stream().map(this::toView).toList(), page, size, slice.hasNext());
    }

    @Override
    public Optional<TransactionView> find(Long academyId, Long transactionId) {
        return repository.findByIdAndCard_AcademyId(transactionId, academyId).map(this::toView);
    }

    @Override
    public Optional<TransactionView> findForUpdate(Long academyId, Long transactionId) {
        return repository.findForUpdate(transactionId, academyId).map(this::toView);
    }

    private TransactionView toView(CorporateCardTransactionJpaEntity entity) {
        return new TransactionView(entity.getId(), entity.getApprovedAt(), entity.getApprovalNumber(),
                entity.getMerchantName(), entity.getCard().getCardName(), entity.getCard().getCardNumberMasked(),
                entity.getInstallmentMonths(), entity.getAmount(), entity.getCard().getApprovalTemplateId());
    }
}
