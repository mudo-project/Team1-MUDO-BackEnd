package com.academy.mudogroupware.planquota.domain.model;

public record PlanLimits(
        long employeeLimit,
        long studentLimit,
        long s3BytesLimit,
        long smsMonthlyLimit,
        long rdsBytesLimit,
        long aiTokenMonthlyLimit,
        long mailMonthlyLimit
) {
    private static final PlanLimits FREE_LIMITS = new PlanLimits(
            20, 50, 500L * 1024 * 1024, 150, 300L * 1024 * 1024, 100_000, 100);

    private static final PlanLimits PAID_LIMITS = new PlanLimits(
            500, Long.MAX_VALUE, 5L * 1024 * 1024 * 1024, 10_000,
            2L * 1024 * 1024 * 1024, 1_000_000, 10_000);

    public static PlanLimits of(Plan plan) {
        return switch (plan) {
            case FREE -> FREE_LIMITS;
            case PAID -> PAID_LIMITS;
        };
    }
}
