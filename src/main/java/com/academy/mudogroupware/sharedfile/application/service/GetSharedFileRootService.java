package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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
