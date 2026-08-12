package com.academy.mudogroupware.revenuereport.application.port;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record RevenueSnapshot(
        LocalDate targetMonth,
        Revenue revenue,
        Expense expense,
        Profit profit,
        PreviousMonth previousMonth,
        List<LectureBreakdown> byLecture,
        List<TeacherBreakdown> byTeacher
) {
    public record Revenue(long expected, long actual) {
    }

    public record Expense(long actual, List<ExpenseCategoryAmount> byCategory) {
    }

    public record Profit(long actual, long expected) {
    }

    /**
     * 전월 대비 top-line 비교(실 매출/실 지출/실 순이익 3개만). available=false면
     * 첫 리포트라 비교 대상이 없다는 뜻 — 나머지 필드는 응답 JSON에서 생략된다(0이 아니라 없음).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PreviousMonth(boolean available, LocalDate targetMonth, Long revenueActual, Long expenseActual,
                                Long profitActual, Long revenueActualDelta, Long expenseActualDelta,
                                Long profitActualDelta) {
        public static PreviousMonth unavailable() {
            return new PreviousMonth(false, null, null, null, null, null, null, null);
        }
    }

    public record LectureBreakdown(String lectureName, String teacherName, long studentCount, long actualRevenue) {
    }

    public record TeacherBreakdown(String teacherName, long lectureCount, long studentCount, long actualRevenue) {
    }
}
