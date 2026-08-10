package com.academy.mudogroupware.users.application.command;

import com.academy.mudogroupware.users.domain.model.Plan;

public record SubmitAcademyApplicationCommand(
        String requestedLoginId,
        String academyName,
        String representativeName,
        String representativeEmail,
        String representativePhone,
        Plan plan) {
}
