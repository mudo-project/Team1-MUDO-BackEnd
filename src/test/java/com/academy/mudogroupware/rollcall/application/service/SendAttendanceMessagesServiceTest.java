package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;
import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;
import com.academy.mudogroupware.rollcall.application.port.SmsSenderPort;
import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.application.query.MessageSendResultView;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.NoStudentsSelectedException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

class SendAttendanceMessagesServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final Long ACADEMY_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final GetMessageSendCandidatesUseCase getMessageSendCandidatesUseCase =
            mock(GetMessageSendCandidatesUseCase.class);
    private final MessageTemplateRepository messageTemplateRepository = mock(MessageTemplateRepository.class);
    private final SmsSenderPort smsSenderPort = mock(SmsSenderPort.class);

    private SendAttendanceMessagesService service;

    @BeforeEach
    void setUp() {
        service = new SendAttendanceMessagesService(
                getMessageSendCandidatesUseCase, messageTemplateRepository, smsSenderPort);
    }

    @Test
    void throwsWhenNoStudentsSelected() {
        assertThatThrownBy(() -> service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, ACADEMY_ID, DATE, List.of())))
                .isInstanceOf(NoStudentsSelectedException.class);

        verifyNoInteractions(getMessageSendCandidatesUseCase, messageTemplateRepository, smsSenderPort);
    }

    @Test
    void sendsMessageToEligibleStudentAndReportsSuccess() {
        MessageTemplate template = MessageTemplate.restore(
                7L, ACADEMY_ID, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, ACADEMY_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, ACADEMY_ID, DATE, List.of(10L)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).studentId()).isEqualTo(10L);
        assertThat(results.get(0).sent()).isTrue();
        assertThat(results.get(0).failureReason()).isNull();
    }

    @Test
    void doesNotCallSmsPortForIneligibleStudent() {
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", null, null, false);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, ACADEMY_ID, DATE))
                .thenReturn(List.of(candidate));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, ACADEMY_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).isNotBlank();
        verify(smsSenderPort, never()).send(any(), any());
    }

    @Test
    void reportsFailureReasonWhenSmsPortFails() {
        MessageTemplate template = MessageTemplate.restore(
                7L, ACADEMY_ID, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, ACADEMY_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다"))
                .thenReturn(SmsSendResult.failed("인증오류입니다"));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, ACADEMY_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).isEqualTo("인증오류입니다");
    }

    @Test
    void marksRequestedStudentNotInCandidateListAsNotEligible() {
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, ACADEMY_ID, DATE))
                .thenReturn(List.of());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, ACADEMY_ID, DATE, List.of(999L)));

        assertThat(results.get(0).sent()).isFalse();
        verify(smsSenderPort, never()).send(any(), any());
    }
}
