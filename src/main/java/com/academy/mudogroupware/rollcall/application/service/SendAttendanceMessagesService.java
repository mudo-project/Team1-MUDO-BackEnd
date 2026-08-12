package com.academy.mudogroupware.rollcall.application.service;

import java.time.LocalDate;
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
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendAttendanceMessagesService implements SendAttendanceMessagesUseCase {

    private final GetMessageSendCandidatesUseCase getMessageSendCandidatesUseCase;
    private final MessageTemplateRepository messageTemplateRepository;
    private final SmsSenderPort smsSenderPort;

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
        List<MessageSendResultView> results = requestedStudentIds.stream()
                .map(studentId -> sendToStudent(studentId, candidatesByStudentId.get(studentId), templatesById,
                        command.date()))
                .toList();

        long sentCount = results.stream().filter(MessageSendResultView::sent).count();
        log.info("event=attendance_message_send_완료 lectureId={}, sentCount={}, failedCount={}",
                command.lectureId(), sentCount, results.size() - sentCount);
        return results;
    }

    private MessageSendResultView sendToStudent(Long studentId, MessageSendCandidateView candidate,
                                                 Map<Long, MessageTemplate> templatesById, LocalDate date) {
        if (candidate == null || !candidate.eligible()) {
            return new MessageSendResultView(studentId, candidate != null ? candidate.studentName() : null,
                    false, "발송 대상이 아닙니다(출결 미입력 또는 매칭되는 템플릿 없음).");
        }
        MessageTemplate template = templatesById.get(candidate.matchedTemplateId());
        if (template == null) {
            return new MessageSendResultView(studentId, candidate.studentName(), false,
                    "문자 템플릿을 찾을 수 없습니다.");
        }

        SmsSendResult result;
        String message = renderMessage(template.getContent(), candidate, date);
        try {
            result = smsSenderPort.send(candidate.parentPhone(), message);
        } catch (RuntimeException e) {
            log.warn("event=attendance_message_send_student_실패 studentId={}, reason={}",
                    studentId, e.getMessage(), e);
            return new MessageSendResultView(studentId, candidate.studentName(), false,
                    "SMS 발송 처리 중 오류가 발생했습니다.");
        }
        return new MessageSendResultView(studentId, candidate.studentName(), result.success(),
                result.success() ? null : result.failureReason());
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
}
