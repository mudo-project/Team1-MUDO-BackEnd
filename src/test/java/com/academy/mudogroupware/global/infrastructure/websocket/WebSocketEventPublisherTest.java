package com.academy.mudogroupware.global.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketEventPublisherTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate);

    @Test
    void publishesPayloadToTopicDestination() {
        TestPayload payload = new TestPayload("hello");

        publisher.publish("/topic/test", payload);

        verify(messagingTemplate).convertAndSend("/topic/test", payload);
    }

    @Test
    void publishesPayloadToQueueDestination() {
        TestPayload payload = new TestPayload("hello");

        publisher.publish("/queue/test", payload);

        verify(messagingTemplate).convertAndSend("/queue/test", payload);
    }

    @Test
    void rejectsBlankDestination() {
        TestPayload payload = new TestPayload("hello");

        assertThatThrownBy(() -> publisher.publish(" ", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("destination must not be blank");
    }

    @Test
    void rejectsApplicationDestination() {
        TestPayload payload = new TestPayload("hello");

        assertThatThrownBy(() -> publisher.publish("/app/test", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("destination must start with /topic/ or /queue/");
    }

    @Test
    void rejectsNullPayload() {
        assertThatThrownBy(() -> publisher.publish("/topic/test", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }

    private record TestPayload(String message) {
    }
}
