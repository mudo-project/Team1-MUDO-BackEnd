package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.SearchUsersUseCase;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUsersService implements SearchUsersUseCase {

    private final UserRepository userRepository;

    @Override
    public List<User> search(Long academyId, String keyword) {
        return userRepository.searchByAcademyId(academyId, keyword);
    }
}
