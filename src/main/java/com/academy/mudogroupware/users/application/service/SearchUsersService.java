package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.SearchUsersUseCase;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUsersService implements SearchUsersUseCase {

    private final UserRepository userRepository;

    @Override
    public List<User> search(Long academyId, String keyword) {
        boolean keywordPresent = keyword != null && !keyword.isBlank();
        log.info("event=user_search_시작 academyId={}, keywordPresent={}", academyId, keywordPresent);
        List<User> result = userRepository.searchByAcademyId(academyId, keyword);
        log.info("event=user_search_완료 academyId={}, count={}", academyId, result.size());
        return result;
    }
}
