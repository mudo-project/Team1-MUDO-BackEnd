package com.academy.mudogroupware.users.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.users.domain.model.User;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);
}
