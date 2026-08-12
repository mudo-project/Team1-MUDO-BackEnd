package com.academy.mudogroupware.revenuereport.application.port;

import java.util.List;

public record ExpenseSummary(Long totalAmount, List<ExpenseCategoryAmount> byCategory) {
}
