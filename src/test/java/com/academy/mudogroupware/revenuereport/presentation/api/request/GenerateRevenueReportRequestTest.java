package com.academy.mudogroupware.revenuereport.presentation.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class GenerateRevenueReportRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidYearMonthFormat() {
        GenerateRevenueReportRequest request = new GenerateRevenueReportRequest("2026-07");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankTargetMonth() {
        GenerateRevenueReportRequest request = new GenerateRevenueReportRequest("");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsInvalidMonthNumber() {
        GenerateRevenueReportRequest request = new GenerateRevenueReportRequest("2026-13");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsNonYearMonthText() {
        GenerateRevenueReportRequest request = new GenerateRevenueReportRequest("july");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void convertsToFirstDayOfTargetMonth() {
        GenerateRevenueReportRequest request = new GenerateRevenueReportRequest("2026-07");

        assertThat(request.toTargetMonthDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }
}
