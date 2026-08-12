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
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of())))
                .isInstanceOf(NoStudentsSelectedException.class);

        verifyNoInteractions(getMessageSendCandidatesUseCase, messageTemplateRepository, smsSenderPort);
    }

    @Test
    void sendsMessageToEligibleStudentAndReportsSuccess() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).studentId()).isEqualTo(10L);
        assertThat(results.get(0).sent()).isTrue();
        assertThat(results.get(0).failureReason()).isNull();
    }

    @Test
    void replacesTemplateVariablesBeforeSendingSms() {
        MessageTemplate template = MessageTemplate.restore(
                7L,
                "결석 안내",
                AttendanceStatus.ABSENT,
                "[학원명] 오늘 {학생명} 학생이 {강의명} 강의에 결석했습니다. 기준일 {날짜}.",
                1L,
                NOW,
                NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true,
                "수학 기초반");
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111",
                "[학원명] 오늘 이준호 학생이 수학 기초반 강의에 결석했습니다. 기준일 2026-08-05."))
                .thenReturn(SmsSendResult.succeeded());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isTrue();
        verify(smsSenderPort).send("010-1111-1111",
                "[학원명] 오늘 이준호 학생이 수학 기초반 강의에 결석했습니다. 기준일 2026-08-05.");
    }

    @Test
    void doesNotCallSmsPortForIneligibleStudent() {
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", null, null, false);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).isNotBlank();
        verify(smsSenderPort, never()).send(any(), any());
    }

    @Test
    void reportsFailureReasonWhenSmsPortFails() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다"))
                .thenReturn(SmsSendResult.failed("인증오류입니다"));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).isEqualTo("인증오류입니다");
    }

    @Test
    void continuesToNextStudentAfterOneSmsFailure() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView firstCandidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        MessageSendCandidateView secondCandidate = new MessageSendCandidateView(
                11L, "김서윤", AttendanceStatus.ABSENT, "010-2222-2222", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(firstCandidate, secondCandidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다"))
                .thenReturn(SmsSendResult.failed("인증오류입니다"));
        when(smsSenderPort.send("010-2222-2222", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L, 11L)));

        assertThat(results).hasSize(2);
        assertThat(results).filteredOn(result -> result.studentId().equals(10L))
                .allSatisfy(result -> {
                    assertThat(result.sent()).isFalse();
                    assertThat(result.failureReason()).isEqualTo("인증오류입니다");
                });
        assertThat(results).filteredOn(result -> result.studentId().equals(11L))
                .allSatisfy(result -> {
                    assertThat(result.sent()).isTrue();
                    assertThat(result.failureReason()).isNull();
                });
        verify(smsSenderPort).send("010-1111-1111", "결석했습니다");
        verify(smsSenderPort).send("010-2222-2222", "결석했습니다");
    }

    @Test
    void continuesToNextStudentWhenSmsPortThrows() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView firstCandidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        MessageSendCandidateView secondCandidate = new MessageSendCandidateView(
                11L, "김서윤", AttendanceStatus.ABSENT, "010-2222-2222", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(firstCandidate, secondCandidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다"))
                .thenThrow(new IllegalStateException("HMAC 서명 생성에 실패했습니다."));
        when(smsSenderPort.send("010-2222-2222", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L, 11L)));

        assertThat(results).hasSize(2);
        assertThat(results).filteredOn(result -> result.studentId().equals(10L))
                .allSatisfy(result -> {
                    assertThat(result.sent()).isFalse();
                    assertThat(result.failureReason()).contains("SMS 발송 처리 중 오류가 발생했습니다.");
                });
        assertThat(results).filteredOn(result -> result.studentId().equals(11L))
                .allSatisfy(result -> {
                    assertThat(result.sent()).isTrue();
                    assertThat(result.failureReason()).isNull();
                });
    }

    @Test
    void marksRequestedStudentNotInCandidateListAsNotEligible() {
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of());

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(999L)));

        assertThat(results.get(0).sent()).isFalse();
        verify(smsSenderPort, never()).send(any(), any());
    }
}
