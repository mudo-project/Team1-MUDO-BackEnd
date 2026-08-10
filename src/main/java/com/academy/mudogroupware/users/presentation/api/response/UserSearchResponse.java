package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.domain.model.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserSearchResponse(
        @Schema(description = "사용자 ID", example = "12") Long userId,
        @Schema(description = "이름", example = "김강사") String name,
        @Schema(description = "로그인 아이디(동명이인 구분용)", example = "kim_teacher01") String username) {

    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(user.getId(), user.getName(), user.getUsername());
    }
}
