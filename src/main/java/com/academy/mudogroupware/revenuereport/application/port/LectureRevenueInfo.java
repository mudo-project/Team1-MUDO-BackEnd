package com.academy.mudogroupware.revenuereport.application.port;

public record LectureRevenueInfo(
        Long lectureId,
        String lectureName,
        String teacherName,
        Integer feeAmount
) {
}
