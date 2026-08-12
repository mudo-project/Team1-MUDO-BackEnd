package com.academy.mudogroupware.platform.presentation.api.response;

import java.time.Instant;

public record MemberCountResponse(String academyCode, long activeMemberCount, Instant collectedAt) {}
