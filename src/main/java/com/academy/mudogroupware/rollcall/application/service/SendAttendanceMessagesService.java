package com.academy.mudogroupware.rollcall.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitErrorCode;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
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
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

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
    private final ResourceUsageQueryPort resourceUsageQueryPort;
    private final CurrentPlanProvider currentPlanProvider;
    @Qualifier("rollcallSmsExecutor")
    private final Executor rollcallSmsExecutor;

    @Override
    public List<MessageSendResultView> send(SendAttendanceMessagesCommand command) {
        log.info("event=attendance_message_send_시작 lectureId={}, studentCount={}",
                command.lectureId(),
                command.studentIds() == null ? 0 : command.studentIds().size());
        if (command.studentIds() == null || command.studentIds().isEmpty()) {
            throw new NoStudentsSelectedException();
        }

        long limit = currentPlanProvider.currentLimits().smsMonthlyLimit();
        long current = monthlySmsUsage();
        if (current >= limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.SMS_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, current);
        }
        AtomicLong remainingBudget = new AtomicLong(limit - current);

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
        // SOLAPI 호출을 순차로 하면 학생 수만큼 응답 시간이 누적된다 - 전용 실행기(rollcallSmsExecutor)로
        // 동시에 던지고 모아서, 전체 소요 시간을 "가장 느린 1건" 수준으로 줄인다. 학생별 발송 기록은
        // 이미 DB 조건부 UPDATE(claimForSending)로 동시성이 보장되므로 안전하다. 월간 SMS 한도는
        // remainingBudget(AtomicLong)에 대한 원자적 예약(reserveBudgetSlot)으로 동시 요청 간에도
        // 정확히 남은 건수만큼만 통과하도록 한다.
        List<CompletableFuture<SendOutcome>> futures = requestedStudentIds.stream()
                .map(studentId -> submitSendTask(command, studentId, candidatesByStudentId, templatesById,
                        remainingBudget))
                .toList();
        List<SendOutcome> outcomes = futures.stream().map(CompletableFuture::join).toList();
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

    private long monthlySmsUsage() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        return resourceUsageQueryPort.sumByTypeAndPeriod(ResourceUsageType.SMS, from, to);
    }

    /**
     * executor/queue가 가득 차면 supplyAsync 제출 시점에 RejectedExecutionException(또는 Spring이
     * 감싼 TaskRejectedException, 둘 다 RejectedExecutionException의 하위 타입)이 던져진다. 이걸 안
     * 잡으면 이미 제출된 다른 학생의 future까지 통째로 유실되므로, 거부된 학생만 실패로 변환하고 나머지는
     * 그대로 진행한다.
     */
    private CompletableFuture<SendOutcome> submitSendTask(SendAttendanceMessagesCommand command, Long studentId,
                                                           Map<Long, MessageSendCandidateView> candidatesByStudentId,
                                                           Map<Long, MessageTemplate> templatesById,
                                                           AtomicLong remainingBudget) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> sendToStudent(command.lectureId(), studentId, candidatesByStudentId.get(studentId),
                            templatesById, command.date(), remainingBudget),
                    rollcallSmsExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("event=attendance_message_send_student_거부 studentId={}, reason={}", studentId, e.getMessage());
            MessageSendCandidateView candidate = candidatesByStudentId.get(studentId);
            return CompletableFuture.completedFuture(new SendOutcome(new MessageSendResultView(studentId,
                    candidate != null ? candidate.studentName() : null, false,
                    "발송 대기열이 가득 차 처리하지 못했습니다."), false));
        }
    }

    private SendOutcome sendToStudent(Long lectureId, Long studentId, MessageSendCandidateView candidate,
                                       Map<Long, MessageTemplate> templatesById, LocalDate date,
                                       AtomicLong remainingBudget) {
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

        if (!reserveBudgetSlot(remainingBudget)) {
            // 배치 처리 도중 이번 달 SMS 한도를 소진했다 — 나머지 학생은 보내지 않고 건너뛴다. 배치 시작
            // 시점의 1회성 한도 체크만으로는 배치 크기(학생 수)만큼 한도를 초과해 보낼 수 있어서, 실제
            // 발송 직전마다 잔여 한도를 다시 확인한다. 여러 학생을 병렬로 처리하므로 이 예약 자체가
            // 원자적(CAS)이어야 동시 요청 간에도 한도를 정확히 지킬 수 있다.
            record.markResult(AttendanceMessageSendStatus.FAILED, "PLAN_SMS_LIMIT_EXCEEDED", LocalDateTime.now(clock));
            attendanceMessageSendRecordRepository.save(record);
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "이번 달 SMS 발송 한도에 도달해 전송하지 않았습니다."), false);
        }

        SmsSendResult result;
        String message = renderMessage(template.getContent(), candidate, date);
        try {
            result = smsSenderPort.send(candidate.parentPhone(), message);
        } catch (RuntimeException e) {
            remainingBudget.incrementAndGet();
            log.warn("event=attendance_message_send_student_실패 studentId={}, reason={}",
                    studentId, e.getMessage(), e);
            record.markResult(AttendanceMessageSendStatus.FAILED, e.getMessage(), LocalDateTime.now(clock));
            attendanceMessageSendRecordRepository.save(record);
            return new SendOutcome(new MessageSendResultView(studentId, candidate.studentName(), false,
                    "SMS 발송 처리 중 오류가 발생했습니다."), false);
        }

        AttendanceMessageSendStatus status = classifyStatus(result);
        boolean newlySent = status == AttendanceMessageSendStatus.SENT;
        if (!newlySent) {
            // 실패/INDETERMINATE는 실제로 SMS 발송량을 소비하지 않았으므로 예약해둔 슬롯을 반환한다.
            remainingBudget.incrementAndGet();
        }
        record.markResult(status, result.failureReason(), LocalDateTime.now(clock));
        attendanceMessageSendRecordRepository.save(record);
        MessageSendResultView view = new MessageSendResultView(studentId, candidate.studentName(), result.success(),
                result.success() ? null : result.failureReason());
        return new SendOutcome(view, newlySent);
    }

    /**
     * remainingBudget이 남아있을 때만 원자적으로 1을 차감하고 true를 반환한다. 실제 발송이 성공으로
     * 확정되지 않으면(실패/INDETERMINATE/예외) 호출부에서 incrementAndGet으로 되돌려준다.
     */
    private boolean reserveBudgetSlot(AtomicLong remainingBudget) {
        return remainingBudget.getAndUpdate(v -> v > 0 ? v - 1 : v) > 0;
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
                .replace("{studentName}", valueOrBlank(candidate.studentName()))
                .replace("{강의명}", valueOrBlank(candidate.lectureName()))
                .replace("{lectureName}", valueOrBlank(candidate.lectureName()))
                .replace("{날짜}", date != null ? date.toString() : "")
                .replace("{date}", date != null ? date.toString() : "");
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
