package com.academy.mudogroupware.timetable.presentation.api.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ClassroomGroupRequest(
        @Schema(description = "층 이름", example = "6층") @NotBlank String floor,
        @Schema(description = "이 층의 강의실 코드 목록", example = "[\"601\",\"602\"]") @NotEmpty List<@NotBlank String> codes
) {
}
