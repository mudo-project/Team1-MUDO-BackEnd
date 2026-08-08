package com.academy.mudogroupware.timetable.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TimetableClassroomEmbeddable {

    @Column(name = "floor", nullable = false, length = 50)
    private String floor;

    @Column(name = "code", nullable = false, length = 50)
    private String code;
}
