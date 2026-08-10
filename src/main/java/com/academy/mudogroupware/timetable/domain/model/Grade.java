package com.academy.mudogroupware.timetable.domain.model;

public enum Grade {
    ELEMENTARY_1("초1"),
    ELEMENTARY_2("초2"),
    ELEMENTARY_3("초3"),
    ELEMENTARY_4("초4"),
    ELEMENTARY_5("초5"),
    ELEMENTARY_6("초6"),
    MIDDLE_1("중1"),
    MIDDLE_2("중2"),
    MIDDLE_3("중3"),
    HIGH_1("고1"),
    HIGH_2("고2"),
    HIGH_3("고3");

    private final String label;

    Grade(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
