package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.command.CreateMessageTemplateCommand;
import com.academy.mudogroupware.rollcall.domain.exception.MessageTemplateStatusConflictException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

class CreateMessageTemplateServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final MessageTemplateRepository messageTemplateRepository = mock(MessageTemplateRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private CreateMessageTemplateService service;

    @BeforeEach
    void setUp() {
        service = new CreateMessageTemplateService(messageTemplateRepository, clock);
    }

    @Test
    void throwsWhenTemplateAlreadyExistsForStatus() {
        when(messageTemplateRepository.findByStatus(AttendanceStatus.ABSENT))
                .thenReturn(Optional.of(MessageTemplate.create("결석 안내", AttendanceStatus.ABSENT,
                        "내용", 1L, NOW)));

        assertThatThrownBy(() -> service.createTemplate(
                new CreateMessageTemplateCommand("결석 안내2", AttendanceStatus.ABSENT, "내용2", 1L)))
                .isInstanceOf(MessageTemplateStatusConflictException.class);
    }

    @Test
    void createsTemplateWhenNoneExistsForStatus() {
        when(messageTemplateRepository.findByStatus(AttendanceStatus.ABSENT))
                .thenReturn(Optional.empty());
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenAnswer(invocation -> {
            MessageTemplate saved = invocation.getArgument(0);
            return MessageTemplate.restore(1L, saved.getName(), saved.getStatus(),
                    saved.getContent(), saved.getCreatedBy(), saved.getCreatedAt(), saved.getUpdatedAt());
        });

        service.createTemplate(new CreateMessageTemplateCommand("결석 안내", AttendanceStatus.ABSENT,
                "내용", 1L));

        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }
}
