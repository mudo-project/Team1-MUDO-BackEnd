package com.academy.mudogroupware.planquota.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.planquota.domain.model.Plan;

class PlanLimitExceededExceptionTest {

    @Test
    void buildsMessageWithPlanLabelAndResourceMessage() {
        PlanLimitExceededException exception = new PlanLimitExceededException(
                PlanLimitErrorCode.EMPLOYEE_LIMIT_EXCEEDED, Plan.FREE, 20, 20);

        assertThat(exception.getMessage()).isEqualTo("무료 플랜의 직원 수 한도를 초과하였습니다.");
        assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("PLANLIMIT_429_1");
        assertThat(exception.getContext())
                .containsEntry("plan", Plan.FREE)
                .containsEntry("limit", 20L)
                .containsEntry("current", 20L);
    }
}
