package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

class GetSharedFileRootServiceTest {

    private final SharedFileRootRepository repository = mock(SharedFileRootRepository.class);
    private final GetSharedFileRootService service = new GetSharedFileRootService(repository);

    @Test
    void returnsNotReadyWhenNoRootRowExists() {
        when(repository.find()).thenReturn(Optional.empty());

        SharedFileRootView view = service.getRoot();

        assertThat(view.ready()).isFalse();
    }

    @Test
    void returnsReadyWhenRootIsReady() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.ready("folder-id")));

        SharedFileRootView view = service.getRoot();

        assertThat(view.ready()).isTrue();
    }

    @Test
    void returnsNotReadyWhenRootIsFailed() {
        when(repository.find()).thenReturn(Optional.of(SharedFileRoot.failed()));

        SharedFileRootView view = service.getRoot();

        assertThat(view.ready()).isFalse();
    }
}
