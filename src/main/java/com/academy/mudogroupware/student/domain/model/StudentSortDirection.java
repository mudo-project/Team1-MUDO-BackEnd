package com.academy.mudogroupware.student.domain.model;

import java.util.Locale;

public enum StudentSortDirection {

    ASC,
    DESC;

    public static StudentSortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return StudentSortDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isDescending() {
        return this == DESC;
    }
}
