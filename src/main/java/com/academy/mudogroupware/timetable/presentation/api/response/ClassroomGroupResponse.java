package com.academy.mudogroupware.timetable.presentation.api.response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClassroomGroupResponse(
        @Schema(description = "층 이름", example = "6층") String floor,
        @Schema(description = "이 층의 강의실 코드 목록") List<String> codes
) {

    public static List<ClassroomGroupResponse> fromGroupedByFloor(List<TimetableClassroom> classrooms) {
        Map<String, List<String>> byFloor = classrooms.stream()
                .collect(Collectors.groupingBy(TimetableClassroom::floor, LinkedHashMap::new,
                        Collectors.mapping(TimetableClassroom::code, Collectors.toList())));
        return byFloor.entrySet().stream()
                .map(entry -> new ClassroomGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
