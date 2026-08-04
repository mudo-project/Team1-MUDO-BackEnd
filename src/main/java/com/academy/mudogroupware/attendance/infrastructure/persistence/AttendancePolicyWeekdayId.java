package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AttendancePolicyWeekdayId implements Serializable {

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "day_of_week")
    private int dayOfWeek;
}
