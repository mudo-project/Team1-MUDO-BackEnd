package com.academy.mudogroupware.dataimport.application.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

@Component
public class ImportValueNormalizer {

    public String text(ParsedImportRow row, String... aliases) {
        for (String alias : aliases) {
            String value = findValue(row, alias);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public Long longValue(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        String numeric = value.replaceAll("[^0-9-]", "");
        return numeric.isBlank() ? null : Long.valueOf(numeric);
    }

    public Integer integerValue(ParsedImportRow row, String... aliases) {
        Long value = longValue(row, aliases);
        return value != null ? value.intValue() : null;
    }

    public StudentGrade studentGrade(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        String normalized = normalize(value);
        try {
            return StudentGrade.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return switch (normalized) {
                case "초1", "초등1", "초등학교1" -> StudentGrade.ELEMENTARY_1;
                case "초2", "초등2", "초등학교2" -> StudentGrade.ELEMENTARY_2;
                case "초3", "초등3", "초등학교3" -> StudentGrade.ELEMENTARY_3;
                case "초4", "초등4", "초등학교4" -> StudentGrade.ELEMENTARY_4;
                case "초5", "초등5", "초등학교5" -> StudentGrade.ELEMENTARY_5;
                case "초6", "초등6", "초등학교6" -> StudentGrade.ELEMENTARY_6;
                case "중1", "중등1", "중학교1" -> StudentGrade.MIDDLE_1;
                case "중2", "중등2", "중학교2" -> StudentGrade.MIDDLE_2;
                case "중3", "중등3", "중학교3" -> StudentGrade.MIDDLE_3;
                case "고1", "고등1", "고등학교1" -> StudentGrade.HIGH_1;
                case "고2", "고등2", "고등학교2" -> StudentGrade.HIGH_2;
                case "고3", "고등3", "고등학교3" -> StudentGrade.HIGH_3;
                case "n", "없음", "기타" -> StudentGrade.N;
                default -> null;
            };
        }
    }

    public Grade lectureGrade(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        String normalized = normalize(value);
        try {
            return Grade.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            if ("재수".equals(normalized) || "retake".equals(normalized)) {
                return Grade.RETAKE;
            }
            StudentGrade studentGrade = studentGrade(row, aliases);
            if (studentGrade == null || studentGrade == StudentGrade.N) {
                return null;
            }
            return Grade.valueOf(studentGrade.name());
        }
    }

    public FeeType feeType(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        String normalized = normalize(value);
        if (normalized.contains("회") || "per_session".equals(normalized)) {
            return FeeType.PER_SESSION;
        }
        if (normalized.contains("월") || "monthly".equals(normalized) || "per_month".equals(normalized)) {
            return FeeType.PER_MONTH;
        }
        return null;
    }

    public DayOfWeek dayOfWeek(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        return dayOfWeekValue(value);
    }

    public DayOfWeek dayOfWeekValue(String value) {
        String normalized = normalize(value);
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return switch (normalized) {
                case "월", "월요일", "1" -> DayOfWeek.MONDAY;
                case "화", "화요일", "2" -> DayOfWeek.TUESDAY;
                case "수", "수요일", "3" -> DayOfWeek.WEDNESDAY;
                case "목", "목요일", "4" -> DayOfWeek.THURSDAY;
                case "금", "금요일", "5" -> DayOfWeek.FRIDAY;
                case "토", "토요일", "6" -> DayOfWeek.SATURDAY;
                case "일", "일요일", "7" -> DayOfWeek.SUNDAY;
                default -> null;
            };
        }
    }

    public LocalTime time(ParsedImportRow row, String... aliases) {
        String value = text(row, aliases);
        if (value == null) {
            return null;
        }
        return timeValue(value);
    }

    public LocalTime timeValue(String value) {
        String normalized = normalize(value);
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException ignored) {
            if (normalized.endsWith("시")) {
                return parseHour(normalized.substring(0, normalized.length() - 1));
            }
            return parseHour(normalized);
        }
    }

    private LocalTime parseHour(String value) {
        if (!value.matches("\\d{1,2}")) {
            return null;
        }
        int hour = Integer.parseInt(value);
        return hour >= 0 && hour <= 23 ? LocalTime.of(hour, 0) : null;
    }

    private String findValue(ParsedImportRow row, String alias) {
        String normalizedAlias = normalize(alias);
        return row.values().entrySet().stream()
                .filter(entry -> normalize(entry.getKey()).equals(normalizedAlias))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }
}
