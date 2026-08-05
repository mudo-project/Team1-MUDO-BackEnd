package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

class ChatMessageQueryServiceTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMemberDirectoryPort chatMemberDirectoryPort = mock(ChatMemberDirectoryPort.class);
    private final ChatMessageQueryService service =
            new ChatMessageQueryService(chatRoomRepository, chatMessageRepository, chatMemberDirectoryPort);

    @Test
    void rejectsOversizedMessagePageSizeBeforeQuerying() {
        assertThatThrownBy(() -> service.getMessages(1L, 1L, null, null, 101))
                .isInstanceOf(MessengerException.class);

        verifyNoInteractions(chatRoomRepository, chatMessageRepository, chatMemberDirectoryPort);
    }
}
