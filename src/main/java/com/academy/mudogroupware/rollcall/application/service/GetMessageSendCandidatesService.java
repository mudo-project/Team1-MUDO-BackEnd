package com.academy.mudogroupware.rollcall.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageSendCandidatesService implements GetMessageSendCandidatesUseCase {

    private final GetLectureRosterUseCase getLectureRosterUseCase;
    private final MessageTemplateRepository messageTemplateRepository;

    @Override
    public List<MessageSendCandidateView> getCandidates(Long lectureId, LocalDate date) {
        RosterView roster = getLectureRosterUseCase.getRoster(lectureId, date);
        Map<AttendanceStatus, MessageTemplate> templatesByStatus = messageTemplateRepository.findAll().stream()
                .collect(Collectors.toMap(MessageTemplate::getStatus, Function.identity(), (a, b) -> a));

        return roster.entries().stream()
                .filter(entry -> entry.status() != null)
                .map(entry -> toCandidate(entry, templatesByStatus))
                .toList();
    }

    private MessageSendCandidateView toCandidate(
            RosterEntryView entry,
            Map<AttendanceStatus, MessageTemplate> templatesByStatus
    ) {
        MessageTemplate template = templatesByStatus.get(entry.status());
        return new MessageSendCandidateView(entry.studentId(), entry.studentName(), entry.status(),
                entry.parentPhone(), template != null ? template.getId() : null,
                template != null ? template.getName() : null, template != null);
    }
}
