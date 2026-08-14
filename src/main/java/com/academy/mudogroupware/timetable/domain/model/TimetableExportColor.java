package com.academy.mudogroupware.timetable.domain.model;

import java.util.regex.Pattern;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableColorException;

public record TimetableExportColor(int red, int green, int blue) {

    private static final Pattern HEX_COLOR = Pattern.compile("^[0-9A-Fa-f]{6}$");

    public static TimetableExportColor fromHex(String hex) {
        if (hex == null || !HEX_COLOR.matcher(hex).matches()) {
            throw new InvalidTimetableColorException();
        }
        int rgb = Integer.parseInt(hex, 16);
        return new TimetableExportColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
