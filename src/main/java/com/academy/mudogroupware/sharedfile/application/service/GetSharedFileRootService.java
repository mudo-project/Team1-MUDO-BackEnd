package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSharedFileRootService implements GetSharedFileRootUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;

    @Override
    public SharedFileRootView getRoot() {
        boolean ready = sharedFileRootRepository.find()
                .map(SharedFileRoot::isReady)
                .orElse(false);
        return new SharedFileRootView(ready);
    }
}
