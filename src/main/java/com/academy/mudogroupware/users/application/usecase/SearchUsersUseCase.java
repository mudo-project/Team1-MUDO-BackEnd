package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.domain.model.User;

public interface SearchUsersUseCase {

    List<User> search(Long academyId, String keyword);
}
