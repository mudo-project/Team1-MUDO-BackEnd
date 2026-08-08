package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.domain.model.User;

public record UserSearchResponse(Long userId, String name, String username) {

    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(user.getId(), user.getName(), user.getUsername());
    }
}
