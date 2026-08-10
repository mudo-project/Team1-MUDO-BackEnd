package com.academy.mudogroupware.global.infrastructure.websocket;

import java.util.Objects;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private static final String TOPIC_PREFIX = "/topic/";
    private static final String QUEUE_PREFIX = "/queue/";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(String destination, Object payload) {
        validateDestination(destination);
        messagingTemplate.convertAndSend(destination, Objects.requireNonNull(payload, "payload must not be null"));
    }

    private void validateDestination(String destination) {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("destination must not be blank");
        }
        if (!destination.startsWith(TOPIC_PREFIX) && !destination.startsWith(QUEUE_PREFIX)) {
            throw new IllegalArgumentException("destination must start with /topic/ or /queue/");
        }
    }
}
