package com.academy.mudogroupware.timetable.domain.model;

public enum TimetableExportDensity {
    COMPACT(24, 11f),
    NORMAL(32, 13f),
    SPACIOUS(44, 15f);

    private final int rowHeightPx;
    private final float fontSize;

    TimetableExportDensity(int rowHeightPx, float fontSize) {
        this.rowHeightPx = rowHeightPx;
        this.fontSize = fontSize;
    }

    public int rowHeightPx() {
        return rowHeightPx;
    }

    public float fontSize() {
        return fontSize;
    }
}
