package com.academy.mudogroupware.calendar.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.calendar.application.command.CreateCalendarEventCommand;
import com.academy.mudogroupware.calendar.application.usecase.CreateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.application.usecase.GetCalendarEventsUseCase;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;

@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "MEMBER");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateCalendarEventUseCase createCalendarEventUseCase;
    @MockitoBean private GetCalendarEventsUseCase getCalendarEventsUseCase;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void createEventReturns201WithGeneratedId() throws Exception {
        when(createCalendarEventUseCase.createEvent(any(CreateCalendarEventCommand.class)))
                .thenReturn(101L);

        String body = """
                {
                  "title": "2학기 수업 준비 회의",
                  "content": "교재 배분 논의",
                  "eventStartAt": "2026-08-03T10:00:00",
                  "eventEndAt": "2026-08-03T11:30:00",
                  "allDay": false,
                  "color": "green"
                }
                """;

        mockMvc
                .perform(post("/api/calendars")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("CALENDAR_201_1"))
                .andExpect(jsonPath("$.message").value("일정 생성에 성공했습니다."))
                .andExpect(jsonPath("$.data.eventId").value(101));

        verify(createCalendarEventUseCase).createEvent(any(CreateCalendarEventCommand.class));
    }

    @Test
    void createEventReturns400WhenTitleIsBlank() throws Exception {
        String body = """
                {
                  "title": "  ",
                  "eventStartAt": "2026-08-03T10:00:00"
                }
                """;

        mockMvc
                .perform(post("/api/calendars")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));

        verifyNoInteractions(createCalendarEventUseCase);
    }

    @Test
    void createEventReturns401WhenUnauthenticated() throws Exception {
        String body = """
                {
                  "title": "제목",
                  "eventStartAt": "2026-08-03T10:00:00"
                }
                """;

        mockMvc
                .perform(post("/api/calendars")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createCalendarEventUseCase);
    }

    @Test
    void getEventsReturns200WithEventList() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 3, 11, 30);
        CalendarEvent event = CalendarEvent.restore(
                101L, 1L, "2학기 수업 준비 회의", "교재 배분 논의", start, end, false, "green", 7L, start, start);
        when(getCalendarEventsUseCase.getEvents(1L,
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 31, 23, 59, 59)))
                .thenReturn(List.of(event));

        mockMvc
                .perform(get("/api/calendars")
                        .with(authentication(authenticatedUser()))
                        .param("from", "2026-08-01T00:00:00")
                        .param("to", "2026-08-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("CALENDAR_200_1"))
                .andExpect(jsonPath("$.data[0].eventId").value(101))
                .andExpect(jsonPath("$.data[0].title").value("2학기 수업 준비 회의"));
    }

    @Test
    void getEventsReturns400WhenToIsBeforeFrom() throws Exception {
        when(getCalendarEventsUseCase.getEvents(any(), any(), any()))
                .thenThrow(new InvalidCalendarPeriodException());

        mockMvc
                .perform(get("/api/calendars")
                        .with(authentication(authenticatedUser()))
                        .param("from", "2026-08-31T00:00:00")
                        .param("to", "2026-08-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CALENDAR_400_2"));
    }

    @Test
    void getEventsReturns401WhenUnauthenticated() throws Exception {
        mockMvc
                .perform(get("/api/calendars")
                        .param("from", "2026-08-01T00:00:00")
                        .param("to", "2026-08-31T23:59:59"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getCalendarEventsUseCase);
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
