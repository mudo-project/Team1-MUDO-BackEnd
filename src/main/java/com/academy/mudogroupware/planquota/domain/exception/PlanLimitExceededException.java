package com.academy.mudogroupware.planquota.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ApplicationException;
import com.academy.mudogroupware.planquota.domain.model.Plan;

public class PlanLimitExceededException extends ApplicationException {

    public PlanLimitExceededException(PlanLimitErrorCode code, Plan plan, long limit, long current) {
        super(code, "%s의 %s".formatted(plan.label(), code.getMessage()));
        addContext("plan", plan);
        addContext("limit", limit);
        addContext("current", current);
    }
}
