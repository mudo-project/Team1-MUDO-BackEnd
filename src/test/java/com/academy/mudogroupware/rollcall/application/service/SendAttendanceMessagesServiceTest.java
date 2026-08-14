package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;
import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;
import com.academy.mudogroupware.rollcall.application.port.SmsSenderPort;
import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.application.query.MessageSendResultView;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.NoStudentsSelectedException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceMessageSendRecordRepository;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

class SendAttendanceMessagesServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final GetMessageSendCandidatesUseCase getMessageSendCandidatesUseCase =
            mock(GetMessageSendCandidatesUseCase.class);
    private final MessageTemplateRepository messageTemplateRepository = mock(MessageTemplateRepository.class);
    private final SmsSenderPort smsSenderPort = mock(SmsSenderPort.class);
    private final ResourceUsageRecorder resourceUsageRecorder = mock(ResourceUsageRecorder.class);
    private final AttendanceMessageSendRecordRepository attendanceMessageSendRecordRepository =
            mock(AttendanceMessageSendRecordRepository.class);
    private final ResourceUsageQueryPort resourceUsageQueryPort = mock(ResourceUsageQueryPort.class);
    private final CurrentPlanProvider currentPlanProvider = mock(CurrentPlanProvider.class);
    private final Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private SendAttendanceMessagesService service;

    @BeforeEach
    void setUp() {
        service = new SendAttendanceMessagesService(
                getMessageSendCandidatesUseCase, messageTemplateRepository, smsSenderPort, resourceUsageRecorder,
                attendanceMessageSendRecordRepository, clock, resourceUsageQueryPort, currentPlanProvider);
        when(attendanceMessageSendRecordRepository.createOrGetExisting(any(), any(), any(), any()))
                .thenAnswer(invocation -> AttendanceMessageSendRecord.createPending(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                        invocation.getArgument(3), NOW));
        when(attendanceMessageSendRecordRepository.claimForSending(any())).thenReturn(true);
        when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.PAID));
        when(resourceUsageQueryPort.sumByTypeAndPeriod(eq(ResourceUsageType.SMS), any(), any())).thenReturn(0L);
    }

    @Test
    void throwsWhenMonthlySmsLimitReached() {
        when(resourceUsageQueryPort.sumByTypeAndPeriod(eq(ResourceUsageType.SMS), any(), any()))
                .thenReturn(150L);
        when(currentPlanProvider.currentPlan()).thenReturn(Plan.FREE);
        when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));

        assertThatThrownBy(() -> service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L))))
                .isInstanceOf(PlanLimitExceededException.class);

        verifyNoInteractions(smsSenderPort);
        verifyNoInteractions(getMessageSendCandidatesUseCase);
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

    @Test
    void recordsOnlySuccessfullySentSmsCount() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "absence", AttendanceStatus.ABSENT, "absence message", 1L, NOW, NOW);
        MessageSendCandidateView firstCandidate = new MessageSendCandidateView(
                10L, "student-a", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "absence", true);
        MessageSendCandidateView secondCandidate = new MessageSendCandidateView(
                11L, "student-b", AttendanceStatus.ABSENT, "010-2222-2222", 7L, "absence", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(firstCandidate, secondCandidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "absence message"))
                .thenReturn(SmsSendResult.failed("provider failure"));
        when(smsSenderPort.send("010-2222-2222", "absence message")).thenReturn(SmsSendResult.succeeded());

        service.send(new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L, 11L)));

        verify(resourceUsageRecorder).recordSmsMessages(new RecordSmsUsageCommand("rollcall-attendance-sms", 1));
    }

    @Test
    void returnsSendResultsWhenSmsUsageRecordingFails() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "absence", AttendanceStatus.ABSENT, "absence message", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "student-a", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "absence", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "absence message")).thenReturn(SmsSendResult.succeeded());
        doThrow(new IllegalStateException("usage store unavailable"))
                .when(resourceUsageRecorder)
                .recordSmsMessages(new RecordSmsUsageCommand("rollcall-attendance-sms", 1));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results).singleElement()
                .satisfies(result -> assertThat(result.sent()).isTrue());
        verify(resourceUsageRecorder).recordSmsMessages(new RecordSmsUsageCommand("rollcall-attendance-sms", 1));
    }

    @Test
    void skipsCallingSmsPortWhenAlreadyMarkedAsSent() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        AttendanceMessageSendRecord alreadySent = AttendanceMessageSendRecord.createPending(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT, NOW);
        alreadySent.markResult(AttendanceMessageSendStatus.SENT, null, NOW);
        when(attendanceMessageSendRecordRepository.createOrGetExisting(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT))
                .thenReturn(alreadySent);

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isTrue();
        verify(smsSenderPort, never()).send(any(), any());
        verify(attendanceMessageSendRecordRepository, never()).save(any());
        verify(attendanceMessageSendRecordRepository, never()).claimForSending(any());
        verify(resourceUsageRecorder, never()).recordSmsMessages(any());
    }

    @Test
    void blocksAutomaticRetryWhenRecordIsIndeterminate() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        AttendanceMessageSendRecord indeterminate = AttendanceMessageSendRecord.createPending(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT, NOW);
        indeterminate.markResult(AttendanceMessageSendStatus.INDETERMINATE, "타임아웃", NOW);
        when(attendanceMessageSendRecordRepository.createOrGetExisting(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT))
                .thenReturn(indeterminate);

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).contains("자동 재발송을 차단");
        verify(smsSenderPort, never()).send(any(), any());
        verify(attendanceMessageSendRecordRepository, never()).claimForSending(any());
        verify(resourceUsageRecorder, never()).recordSmsMessages(any());
    }

    @Test
    void doesNotCallSmsPortWhenAnotherRequestAlreadyClaimedSending() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(attendanceMessageSendRecordRepository.claimForSending(any())).thenReturn(false);

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        verify(smsSenderPort, never()).send(any(), any());
        verify(resourceUsageRecorder, never()).recordSmsMessages(any());
    }

    @Test
    void doesNotCountAlreadySentSkipTowardUsage() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView alreadySentCandidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        MessageSendCandidateView newCandidate = new MessageSendCandidateView(
                11L, "김서윤", AttendanceStatus.ABSENT, "010-2222-2222", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(alreadySentCandidate, newCandidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        AttendanceMessageSendRecord alreadySent = AttendanceMessageSendRecord.createPending(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT, NOW);
        alreadySent.markResult(AttendanceMessageSendStatus.SENT, null, NOW);
        when(attendanceMessageSendRecordRepository.createOrGetExisting(LECTURE_ID, 10L, DATE, AttendanceStatus.ABSENT))
                .thenReturn(alreadySent);
        when(smsSenderPort.send("010-2222-2222", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        service.send(new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L, 11L)));

        verify(smsSenderPort, never()).send("010-1111-1111", "결석했습니다");
        verify(resourceUsageRecorder).recordSmsMessages(new RecordSmsUsageCommand("rollcall-attendance-sms", 1));
    }

    @Test
    void looksUpTheSendRecordUsingTheCandidatesCurrentAttendanceStatus() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "지각 안내", AttendanceStatus.LATE, "지각했습니다", 1L, NOW, NOW);
        MessageSendCandidateView correctedCandidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.LATE, "010-1111-1111", 7L, "지각 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(correctedCandidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "지각했습니다")).thenReturn(SmsSendResult.succeeded());

        service.send(new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        // 출결이 결석에서 지각으로 정정된 뒤에도(같은 강의·학생·날짜) LATE 기준으로 조회해서 새 발송으로
        // 취급해야 한다 — ABSENT로 조회했다면 이전에 결석 안내가 이미 SENT였을 때 잘못 스킵될 수 있다.
        verify(attendanceMessageSendRecordRepository)
                .createOrGetExisting(LECTURE_ID, 10L, DATE, AttendanceStatus.LATE);
    }

    @Test
    void marksSendRecordAsSentAfterSuccessfulSend() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다")).thenReturn(SmsSendResult.succeeded());

        service.send(new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        ArgumentCaptor<AttendanceMessageSendRecord> captor = ArgumentCaptor.forClass(AttendanceMessageSendRecord.class);
        verify(attendanceMessageSendRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttendanceMessageSendStatus.SENT);
    }

    @Test
    void marksSendRecordAsIndeterminateWhenProviderResultIsIndeterminate() {
        MessageTemplate template = MessageTemplate.restore(
                7L, "결석 안내", AttendanceStatus.ABSENT, "결석했습니다", 1L, NOW, NOW);
        MessageSendCandidateView candidate = new MessageSendCandidateView(
                10L, "이준호", AttendanceStatus.ABSENT, "010-1111-1111", 7L, "결석 안내", true);
        when(getMessageSendCandidatesUseCase.getCandidates(LECTURE_ID, DATE))
                .thenReturn(List.of(candidate));
        when(messageTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(smsSenderPort.send("010-1111-1111", "결석했습니다"))
                .thenReturn(SmsSendResult.indeterminate("응답을 받지 못했습니다"));

        List<MessageSendResultView> results = service.send(
                new SendAttendanceMessagesCommand(LECTURE_ID, DATE, List.of(10L)));

        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).failureReason()).isEqualTo("응답을 받지 못했습니다");
        ArgumentCaptor<AttendanceMessageSendRecord> captor = ArgumentCaptor.forClass(AttendanceMessageSendRecord.class);
        verify(attendanceMessageSendRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttendanceMessageSendStatus.INDETERMINATE);
    }
}
