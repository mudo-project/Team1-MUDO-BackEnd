package com.academy.mudogroupware.corporatecard.application.query;

import java.util.List;

public record CardExpensePage(
        List<CardExpenseView> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious) { }
