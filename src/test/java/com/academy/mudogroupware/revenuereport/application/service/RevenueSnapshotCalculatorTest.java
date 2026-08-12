package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.port.ExpenseCategoryAmount;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentMethod;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentStatus;

class RevenueSnapshotCalculatorTest {

    private final RevenueSnapshotCalculator calculator = new RevenueSnapshotCalculator();

    @Test
    void calculatesAllMetrics() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        List<LectureRevenueInfo> lectures = List.of(
                new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000),
                new LectureRevenueInfo(2L, "고등 영어 독해", "이강사", null)); // feeAmount null -> 예상매출 스킵
        Map<Long, Long> activeEnrollmentCounts = Map.of(1L, 10L, 2L, 5L);
        List<Payment> payments = List.of(
                Payment.restore(1L, 100L, 300000, java.time.LocalDateTime.of(2026, 8, 5, 10, 0),
                        PaymentMethod.CARD, PaymentStatus.PAID, null, null),
                Payment.restore(2L, 101L, 50000, java.time.LocalDateTime.of(2026, 8, 6, 10, 0),
                        PaymentMethod.CARD, PaymentStatus.REFUNDED, null, null));
        // enrollmentId -> lectureId 매핑(테스트용 단순화: 100=강의1, 101=강의1)
        Map<Long, Long> enrollmentToLecture = Map.of(100L, 1L, 101L, 1L);
        ExpenseSummary expense = new ExpenseSummary(200000L, List.of(new ExpenseCategoryAmount("MEAL", 200000L)));

        RevenueSnapshot snapshot = calculator.calculate(
                targetMonth, lectures, activeEnrollmentCounts, payments, enrollmentToLecture, expense,
                java.util.Optional.empty());

        assertThat(snapshot.revenue().expected()).isEqualTo(3000000); // 300000 * 10, 강의2는 feeAmount null이라 제외
        assertThat(snapshot.revenue().actual()).isEqualTo(250000); // 300000 - 50000
        assertThat(snapshot.expense().actual()).isEqualTo(200000);
        assertThat(snapshot.profit().actual()).isEqualTo(50000); // 250000 - 200000
        assertThat(snapshot.profit().expected()).isEqualTo(2800000); // 3000000 - 200000
        assertThat(snapshot.previousMonth().available()).isFalse(); // 이전 리포트 없이 호출한 케이스
        assertThat(snapshot.byLecture()).hasSize(1); // 강의1만 실 매출 있음
        assertThat(snapshot.byLecture().get(0).actualRevenue()).isEqualTo(250000);
        assertThat(snapshot.byTeacher()).extracting("teacherName").containsExactly("김강사");
    }

    @Test
    void includesUnmappedPaymentsInActualRevenueTotalButNotInBreakdown() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        List<LectureRevenueInfo> lectures = List.of(
                new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000));
        Map<Long, Long> activeEnrollmentCounts = Map.of(1L, 10L);
        List<Payment> payments = List.of(
                Payment.restore(1L, 100L, 300000, java.time.LocalDateTime.of(2026, 8, 5, 10, 0),
                        PaymentMethod.CARD, PaymentStatus.PAID, null, null),
                // enrollmentId=999는 매핑 테이블에 없음(삭제된 수강 등) — 그래도 총액에는 포함되어야 한다
                Payment.restore(2L, 999L, 100000, java.time.LocalDateTime.of(2026, 8, 6, 10, 0),
                        PaymentMethod.CARD, PaymentStatus.PAID, null, null));
        Map<Long, Long> enrollmentToLecture = Map.of(100L, 1L);
        ExpenseSummary expense = new ExpenseSummary(0L, List.of());

        RevenueSnapshot snapshot = calculator.calculate(
                targetMonth, lectures, activeEnrollmentCounts, payments, enrollmentToLecture, expense,
                java.util.Optional.empty());

        assertThat(snapshot.revenue().actual()).isEqualTo(400000); // 300000 + 100000, 매핑 안 된 결제도 총액엔 포함
        assertThat(snapshot.byLecture()).hasSize(1); // breakdown에는 매핑된 강의만
        assertThat(snapshot.byLecture().get(0).actualRevenue()).isEqualTo(300000);
    }

    @Test
    void calculatesPreviousMonthDeltaWhenPreviousReportAvailable() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        List<LectureRevenueInfo> lectures = List.of(
                new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000));
        Map<Long, Long> activeEnrollmentCounts = Map.of(1L, 10L);
        List<Payment> payments = List.of(Payment.restore(1L, 100L, 300000,
                java.time.LocalDateTime.of(2026, 8, 5, 10, 0), PaymentMethod.CARD, PaymentStatus.PAID, null, null));
        Map<Long, Long> enrollmentToLecture = Map.of(100L, 1L);
        ExpenseSummary expense = new ExpenseSummary(200000L, List.of());
        RevenueSnapshot previous = new RevenueSnapshot(
                LocalDate.of(2026, 7, 1),
                new RevenueSnapshot.Revenue(2800000, 150000),
                new RevenueSnapshot.Expense(250000, List.of()),
                new RevenueSnapshot.Profit(-100000, 2550000),
                RevenueSnapshot.PreviousMonth.unavailable(),
                List.of(), List.of());

        RevenueSnapshot snapshot = calculator.calculate(targetMonth, lectures, activeEnrollmentCounts, payments,
                enrollmentToLecture, expense, java.util.Optional.of(previous));

        assertThat(snapshot.previousMonth().available()).isTrue();
        assertThat(snapshot.previousMonth().targetMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(snapshot.previousMonth().revenueActual()).isEqualTo(150000L);
        assertThat(snapshot.previousMonth().revenueActualDelta()).isEqualTo(150000L); // 300000 - 150000
        assertThat(snapshot.previousMonth().expenseActualDelta()).isEqualTo(-50000L); // 200000 - 250000
        assertThat(snapshot.previousMonth().profitActualDelta()).isEqualTo(200000L); // 100000 - (-100000)
    }
}
