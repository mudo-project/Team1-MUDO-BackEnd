package com.academy.mudogroupware.student.application.port;

public record LectureCatalogInfo(
        Long lectureId,
        String lectureName,
        String teacherName,
        String scheduleText,
        String priceType,
        Integer priceAmount
) {
}
