package com.academy.mudogroupware.revenuereport.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.RevenueSnapshot;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentStatus;

/** 순수 계산만 담당한다(DB/외부 호출 없음) — AI는 이 결과를 서술만 한다. */
@Component
public class RevenueSnapshotCalculator {

    public RevenueSnapshot calculate(LocalDate targetMonth, List<LectureRevenueInfo> lectures,
                                     Map<Long, Long> activeEnrollmentCounts, List<Payment> payments,
                                     Map<Long, Long> enrollmentIdToLectureId, ExpenseSummary expenseSummary,
                                     Optional<RevenueSnapshot> previousSnapshot) {
        long expectedRevenue = lectures.stream()
                .filter(l -> l.feeAmount() != null)
                .mapToLong(l -> (long) l.feeAmount() * activeEnrollmentCounts.getOrDefault(l.lectureId(), 0L))
                .sum();

        Map<Long, Long> actualRevenueByLecture = actualRevenueByLecture(payments, enrollmentIdToLectureId);
        long actualRevenue = payments.stream().mapToLong(this::signedAmount).sum();

        long actualExpense = expenseSummary.totalAmount();
        long actualProfit = actualRevenue - actualExpense;
        long expectedProfit = expectedRevenue - actualExpense;

        List<RevenueSnapshot.LectureBreakdown> byLecture = lectures.stream()
                .filter(l -> actualRevenueByLecture.containsKey(l.lectureId()))
                .map(l -> new RevenueSnapshot.LectureBreakdown(l.lectureName(), l.teacherName(),
                        activeEnrollmentCounts.getOrDefault(l.lectureId(), 0L),
                        actualRevenueByLecture.get(l.lectureId())))
                .toList();

        List<RevenueSnapshot.TeacherBreakdown> byTeacher = byLecture.stream()
                .collect(Collectors.groupingBy(RevenueSnapshot.LectureBreakdown::teacherName))
                .entrySet().stream()
                .map(entry -> new RevenueSnapshot.TeacherBreakdown(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().mapToLong(RevenueSnapshot.LectureBreakdown::studentCount).sum(),
                        entry.getValue().stream().mapToLong(RevenueSnapshot.LectureBreakdown::actualRevenue).sum()))
                .toList();

        RevenueSnapshot.PreviousMonth previousMonth = previousSnapshot
                .map(previous -> new RevenueSnapshot.PreviousMonth(
                        true,
                        previous.targetMonth(),
                        previous.revenue().actual(),
                        previous.expense().actual(),
                        previous.profit().actual(),
                        actualRevenue - previous.revenue().actual(),
                        actualExpense - previous.expense().actual(),
                        actualProfit - previous.profit().actual()))
                .orElseGet(RevenueSnapshot.PreviousMonth::unavailable);

        return new RevenueSnapshot(targetMonth,
                new RevenueSnapshot.Revenue(expectedRevenue, actualRevenue),
                new RevenueSnapshot.Expense(actualExpense, expenseSummary.byCategory()),
                new RevenueSnapshot.Profit(actualProfit, expectedProfit),
                previousMonth,
                byLecture, byTeacher);
    }

    private Map<Long, Long> actualRevenueByLecture(List<Payment> payments, Map<Long, Long> enrollmentIdToLectureId) {
        Map<Long, Long> result = new java.util.HashMap<>();
        for (Payment payment : payments) {
            Long lectureId = enrollmentIdToLectureId.get(payment.getEnrollmentId());
            if (lectureId == null) {
                continue;
            }
            result.merge(lectureId, signedAmount(payment), Long::sum);
        }
        return result;
    }

    private long signedAmount(Payment payment) {
        return payment.getStatus() == PaymentStatus.REFUNDED
                ? -payment.getAmount()
                : payment.getAmount();
    }
}
