package com.academy.mudogroupware.timetable.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;

@WebMvcTest(TimetableSlotController.class)
class TimetableSlotControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "MEMBER");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateTimetableSlotUseCase createTimetableSlotUseCase;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void createSlotReturns201WithGeneratedId() throws Exception {
        when(createTimetableSlotUseCase.createSlot(any(CreateTimetableSlotCommand.class))).thenReturn(100L);
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00",
                  "grade": "고3",
                  "teacherName": "정T",
                  "subjectName": "미적분"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TIMETABLE_201_2"))
                .andExpect(jsonPath("$.data.timetableSlotId").value(100));
    }

    @Test
    void createSlotReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/timetables/1/slots").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
