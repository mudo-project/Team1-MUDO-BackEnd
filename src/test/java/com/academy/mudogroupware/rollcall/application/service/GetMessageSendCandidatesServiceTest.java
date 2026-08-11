package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterSummaryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;
import com.academy.mudogroupware.rollcall.domain.model.MessageTemplate;
import com.academy.mudogroupware.rollcall.domain.repository.MessageTemplateRepository;

class GetMessageSendCandidatesServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private final GetLectureRosterUseCase getLectureRosterUseCase = mock(GetLectureRosterUseCase.class);
    private final MessageTemplateRepository messageTemplateRepository = mock(MessageTemplateRepository.class);

    private GetMessageSendCandidatesService service;

    @BeforeEach
    void setUp() {
        service = new GetMessageSendCandidatesService(getLectureRosterUseCase, messageTemplateRepository);
    }

    @Test
    void excludesStudentsWithoutAttendanceStatusAndMarksMissingTemplatesIneligible() {
        List<RosterEntryView> entries = List.of(
                new RosterEntryView(10L, "이준호", "MIDDLE_3", "010-1111-1111", AttendanceStatus.ABSENT, null),
                new RosterEntryView(20L, "김서윤", "HIGH_1", "010-2222-2222", AttendanceStatus.ONLINE, null),
                new RosterEntryView(30L, "최예린", "HIGH_2", "010-3333-3333", null, null));
        when(getLectureRosterUseCase.getRoster(LECTURE_ID, DATE)).thenReturn(
                new RosterView(LECTURE_ID, "수학 기초반", DATE, entries, new RosterSummaryView(3, 0, 1, 0, 1, 0)));
        when(messageTemplateRepository.findAll()).thenReturn(List.of(
                MessageTemplate.create("결석 안내", AttendanceStatus.ABSENT, "내용", 1L, NOW)));

        var candidates = service.getCandidates(LECTURE_ID, DATE);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).eligible()).isTrue();
        assertThat(candidates.get(0).matchedTemplateName()).isEqualTo("결석 안내");
        assertThat(candidates.get(1).eligible()).isFalse();
        assertThat(candidates.get(1).matchedTemplateName()).isNull();
        verify(messageTemplateRepository).findAll();
        verify(messageTemplateRepository, never()).findByStatus(any());
    }
}
