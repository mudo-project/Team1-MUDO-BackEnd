package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance_policy_weekdays")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendancePolicyWeekdayJpaEntity {

    @EmbeddedId
    private AttendancePolicyWeekdayId id;

    @Column(name = "is_workday", nullable = false)
    private boolean workday;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Builder
    private AttendancePolicyWeekdayJpaEntity(AttendancePolicyWeekdayId id, boolean workday,
                                             LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.workday = workday;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
