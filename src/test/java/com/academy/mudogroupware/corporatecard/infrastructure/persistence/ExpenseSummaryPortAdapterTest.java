package com.academy.mudogroupware.corporatecard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.corporatecard.domain.model.ExpenseCategory;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;

class ExpenseSummaryPortAdapterTest {

    @Test
    void summarizesTotalAndByCategory() {
        CardExpenseJpaRepository jpaRepository = mock(CardExpenseJpaRepository.class);
        CardExpenseJpaRepository.CategoryAmountProjection mealRow = mock(
                CardExpenseJpaRepository.CategoryAmountProjection.class);
        when(mealRow.getCategory()).thenReturn(ExpenseCategory.MEAL);
        when(mealRow.getAmount()).thenReturn(300000L);
        when(jpaRepository.sumAmountByCategoryAndApprovedAtBetween(
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0)))
                .thenReturn(List.of(mealRow));
        ExpenseSummaryPortAdapter adapter = new ExpenseSummaryPortAdapter(jpaRepository);

        ExpenseSummary result = adapter.summarize(
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0));

        assertThat(result.totalAmount()).isEqualTo(300000L);
        assertThat(result.byCategory()).containsExactly(
                new com.academy.mudogroupware.revenuereport.application.port.ExpenseCategoryAmount("MEAL", 300000L));
    }
}
