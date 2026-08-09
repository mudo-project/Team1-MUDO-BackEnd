package com.academy.mudogroupware.corporatecard.domain.model;

import java.util.Arrays;

public enum ExpenseCategory {
    MEAL("식대"),
    BOOK("도서·교재"),
    OFFICE_SUPPLIES("사무용품"),
    TRANSPORTATION("교통비"),
    FACILITY("시설·비품"),
    EDUCATION("교육비"),
    ETC("기타");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ExpenseCategory fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(category -> category.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 사용 분류입니다."));
    }
}
