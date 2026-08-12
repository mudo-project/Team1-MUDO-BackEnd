package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.ExpenseCategoryAmount;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummaryPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: revenuereport
 * Purpose: 기간별 법인카드 지출 합계와 카테고리별 breakdown을 제공한다(비용, 매출 아님).
 */
@Component
@RequiredArgsConstructor
public class ExpenseSummaryPortAdapter implements ExpenseSummaryPort {

    private final CardExpenseJpaRepository cardExpenseJpaRepository;

    @Override
    public ExpenseSummary summarize(LocalDateTime from, LocalDateTime to) {
        List<CardExpenseJpaRepository.CategoryAmountProjection> rows =
                cardExpenseJpaRepository.sumAmountByCategoryAndApprovedAtBetween(from, to);

        List<ExpenseCategoryAmount> byCategory = rows.stream()
                .map(row -> new ExpenseCategoryAmount(row.getCategory().name(), row.getAmount()))
                .toList();
        long total = byCategory.stream().mapToLong(ExpenseCategoryAmount::amount).sum();

        return new ExpenseSummary(total, byCategory);
    }
}
