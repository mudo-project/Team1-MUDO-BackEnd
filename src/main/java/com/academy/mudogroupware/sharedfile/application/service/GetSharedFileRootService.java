package com.academy.mudogroupware.sharedfile.application.service;

import java.util.Optional;

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
        Optional<SharedFileRoot> root = sharedFileRootRepository.find();
        boolean ready = root.map(SharedFileRoot::isReady).orElse(false);
        String rootId = root.map(SharedFileRoot::getGoogleRootFolderId).orElse(null);
        return new SharedFileRootView(ready, rootId);
    }
}
