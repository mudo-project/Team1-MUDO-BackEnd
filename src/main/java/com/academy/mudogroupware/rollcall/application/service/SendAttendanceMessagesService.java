package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;
import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;
import com.academy.mudogroupware.rollcall.application.port.SmsSenderPort;
import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.application.query.MessageSendResultView;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.SendAttendanceMessagesUseCase;
import com.academy.mudogroupware.rollcall.domain.exception.NoStudentsSelectedException;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.AttendanceMessageSendRecordRepository;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendAttendanceMessagesService implements SendAttendanceMessagesUseCase {

    private static final String SMS_USAGE_FEATURE = "rollcall-attendance-sms";

    private final GetMessageSendCandidatesUseCase getMessageSendCandidatesUseCase;
    private final MessageTemplateRepository messageTemplateRepository;
    private final SmsSenderPort smsSenderPort;
    private final ResourceUsageRecorder resourceUsageRecorder;
    private final AttendanceMessageSendRecordRepository attendanceMessageSendRecordRepository;
    private final Clock clock;

    @Override
    public List<MessageSendResultView> send(SendAttendanceMessagesCommand command) {
        log.info("event=attendance_message_send_시작 lectureId={}, studentCount={}",
                command.lectureId(),
                command.studentIds() == null ? 0 : command.studentIds().size());
        if (command.studentIds() == null || command.studentIds().isEmpty()) {
            throw new NoStudentsSelectedException();
        }

        List<MessageSendCandidateView> candidates = getMessageSendCandidatesUseCase
                .getCandidates(command.lectureId(), command.date());
        Map<Long, MessageSendCandidateView> candidatesByStudentId = candidates.stream()
                .collect(Collectors.toMap(MessageSendCandidateView::studentId, Function.identity()));
        Map<Long, MessageTemplate> templatesById = candidates.stream()
                .map(MessageSendCandidateView::matchedTemplateId)
                .filter(id -> id != null)
                .distinct()
                .map(id -> messageTemplateRepository.findById(id).orElse(null))
                .filter(template -> template != null)
                .collect(Collectors.toMap(MessageTemplate::getId, Function.identity()));

        Set<Long> requestedStudentIds = Set.copyOf(command.studentIds());
        List<SendOutcome> outcomes = requestedStudentIds.stream()
                .map(studentId -> sendToStudent(command.lectureId(), studentId, candidatesByStudentId.get(studentId),
                        templatesById, command.date()))
                .toList();
        List<MessageSendResultView> results = outcomes.stream().map(SendOutcome::view).toList();

        long sentCount = outcomes.stream().filter(SendOutcome::newlySent).count();
        if (sentCount > 0) {
            try {
                resourceUsageRecorder.recordSmsMessages(new RecordSmsUsageCommand(SMS_USAGE_FEATURE, sentCount));
            } catch (RuntimeException e) {
                log.warn("event=attendance_message_usage_persist_failed lectureId={}, sentCount={}, reason={}",
                        command.lectureId(), sentCount, e.getMessage(), e);
            }
        }
        long failedCount = results.stream().filter(result -> !result.sent()).count();
        log.info("event=attendance_message_send_완료 lectureId={}, sentCount={}, failedCount={}",
                command.lectureId(), sentCount, failedCount);
        return results;
    }

    private SendOutcome sendToStudent(Long lectureId, Long studentId, MessageSendCandidateView candidate,
                                       Map<Long, MessageTemplate> templatesById, LocalDate date) {
        if (candidate == null || !candidate.eligible()) {
            return new SendOutcome(new MessageSendResultView(studentId,
                    candidate != null ? candidate.studentName() : null, false,
                    "발송 대상이 아닙니다(출결 미입력 또는 매칭되는 템플릿 없음)."), false);
        }
        MessageTemplate template = templatesById.get(candidate.matchedTemplateId());
        if (template == null) {
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "문자 템플릿을 찾을 수 없습니다."), false);
        }

        AttendanceMessageSendRecord record = attendanceMessageSendRecordRepository
                .createOrGetExisting(lectureId, studentId, date, candidate.status());
        if (record.isAlreadySent()) {
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), true, null), false);
        }
        if (record.isIndeterminate()) {
            // 이전 시도의 실제 발송 여부를 모르는 상태라 자동 재시도는 막는다 — 중복 발송 위험을
            // 감수하고서라도 재발송하려면 별도의 관리자 확인 절차가 필요하다(이번 범위 밖).
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "이전 발송 결과를 확인할 수 없어 자동 재발송을 차단했습니다. 관리자 확인이 필요합니다."), false);
        }
        if (!attendanceMessageSendRecordRepository.claimForSending(record.getId())) {
            // 동시에 들어온 다른 요청이 먼저 발송 권한(SENDING)을 가져갔거나, 그 사이 이미 SENT로 바뀌었다.
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "다른 요청이 처리 중이거나 이미 처리되었습니다."), false);
        }

        SmsSendResult result;
        String message = renderMessage(template.getContent(), candidate, date);
        try {
            result = smsSenderPort.send(candidate.parentPhone(), message);
        } catch (RuntimeException e) {
            log.warn("event=attendance_message_send_student_실패 studentId={}, reason={}",
                    studentId, e.getMessage(), e);
            record.markResult(AttendanceMessageSendStatus.FAILED, e.getMessage(), LocalDateTime.now(clock));
            attendanceMessageSendRecordRepository.save(record);
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "SMS 발송 처리 중 오류가 발생했습니다."), false);
        }

        AttendanceMessageSendStatus status = classifyStatus(result);
        record.markResult(status, result.failureReason(), LocalDateTime.now(clock));
        attendanceMessageSendRecordRepository.save(record);
        MessageSendResultView view = new MessageSendResultView(studentId, candidate.studentName(), result.success(),
                result.success() ? null : result.failureReason());
        return new SendOutcome(view, status == AttendanceMessageSendStatus.SENT);
    }

    private AttendanceMessageSendStatus classifyStatus(SmsSendResult result) {
        if (result.success()) {
            return AttendanceMessageSendStatus.SENT;
        }
        return result.indeterminate() ? AttendanceMessageSendStatus.INDETERMINATE : AttendanceMessageSendStatus.FAILED;
    }

    private String renderMessage(String content, MessageSendCandidateView candidate, LocalDate date) {
        return content
                .replace("{학생명}", valueOrBlank(candidate.studentName()))
                .replace("{강의명}", valueOrBlank(candidate.lectureName()))
                .replace("{날짜}", date != null ? date.toString() : "");
    }

    private String valueOrBlank(String value) {
        return value != null ? value : "";
    }

    /**
     * view.sent()는 클라이언트에 보여줄 성공 여부(이미 보낸 건도 true)이고, newlySent는 이번 호출에서
     * 실제로 SmsSenderPort를 호출해 새로 발송에 성공했는지를 나타낸다 — 사용량 집계는 후자를 기준으로 한다.
     */
    private record SendOutcome(MessageSendResultView view, boolean newlySent) {
    }
}
