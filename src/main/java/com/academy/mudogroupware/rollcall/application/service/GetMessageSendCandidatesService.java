package com.academy.mudogroupware.rollcall.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
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
    public List<MessageSendCandidateView> getCandidates(Long lectureId, Long academyId, LocalDate date) {
        RosterView roster = getLectureRosterUseCase.getRoster(lectureId, academyId, date);

        return roster.entries().stream()
                .filter(entry -> entry.status() != null)
                .map(entry -> toCandidate(academyId, entry))
                .toList();
    }

    private MessageSendCandidateView toCandidate(Long academyId, RosterEntryView entry) {
        Optional<MessageTemplate> template = messageTemplateRepository.findByAcademyIdAndStatus(academyId,
                entry.status());
        return new MessageSendCandidateView(entry.studentId(), entry.studentName(), entry.status(),
                entry.parentPhone(), template.map(MessageTemplate::getId).orElse(null),
                template.map(MessageTemplate::getName).orElse(null), template.isPresent());
    }
}
