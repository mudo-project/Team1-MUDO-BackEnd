package com.academy.mudogroupware.planquota.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlanLimitsTest {

    @Test
    void freePlanHasExpectedLimits() {
        PlanLimits limits = PlanLimits.of(Plan.FREE);

        assertThat(limits.employeeLimit()).isEqualTo(20);
        assertThat(limits.studentLimit()).isEqualTo(50);
        assertThat(limits.s3BytesLimit()).isEqualTo(500L * 1024 * 1024);
        assertThat(limits.smsMonthlyLimit()).isEqualTo(150);
        assertThat(limits.rdsBytesLimit()).isEqualTo(300L * 1024 * 1024);
        assertThat(limits.aiTokenMonthlyLimit()).isEqualTo(100_000);
        assertThat(limits.mailMonthlyLimit()).isEqualTo(100);
    }

    @Test
    void paidPlanHasExpectedLimitsWithUnlimitedStudents() {
        PlanLimits limits = PlanLimits.of(Plan.PAID);

        assertThat(limits.employeeLimit()).isEqualTo(500);
        assertThat(limits.studentLimit()).isEqualTo(Long.MAX_VALUE);
        assertThat(limits.s3BytesLimit()).isEqualTo(5L * 1024 * 1024 * 1024);
        assertThat(limits.smsMonthlyLimit()).isEqualTo(10_000);
        assertThat(limits.rdsBytesLimit()).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(limits.aiTokenMonthlyLimit()).isEqualTo(1_000_000);
        assertThat(limits.mailMonthlyLimit()).isEqualTo(10_000);
    }

    @Test
    void planLabelsAreKorean() {
        assertThat(Plan.FREE.label()).isEqualTo("무료 플랜");
        assertThat(Plan.PAID.label()).isEqualTo("유료 플랜");
    }
}
