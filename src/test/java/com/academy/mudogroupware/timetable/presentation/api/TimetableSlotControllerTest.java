package com.academy.mudogroupware.timetable.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

@WebMvcTest(TimetableSlotController.class)
class TimetableSlotControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "MEMBER");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateTimetableSlotUseCase createTimetableSlotUseCase;
    @MockitoBean private GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    @MockitoBean private GetTimetableSlotUseCase getTimetableSlotUseCase;
    @MockitoBean private UpdateTimetableSlotUseCase updateTimetableSlotUseCase;
    @MockitoBean private DeleteTimetableSlotUseCase deleteTimetableSlotUseCase;
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
                  "grade": "HIGH_3",
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

    @Test
    void getSlotsReturns200WithList() throws Exception {
        when(getTimetableSlotsUseCase.getSlots(1L, 1L)).thenReturn(List.of(
                new TimetableSlotView(100L, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                        LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분")));

        mockMvc.perform(get("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_3"))
                .andExpect(jsonPath("$.data[0].classroomCode").value("601"));
    }

    @Test
    void getSlotReturns200WithDetail() throws Exception {
        when(getTimetableSlotUseCase.getSlot(1L, 1L, 100L)).thenReturn(
                new TimetableSlotView(100L, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                        LocalTime.of(9, 0), LocalTime.of(11, 0), Grade.HIGH_3, "정T", "미적분"));

        mockMvc.perform(get("/api/timetables/1/slots/100")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_4"))
                .andExpect(jsonPath("$.data.teacherName").value("정T"));
    }

    @Test
    void getSlotReturns404WhenNotFound() throws Exception {
        when(getTimetableSlotUseCase.getSlot(1L, 1L, 999L)).thenThrow(new TimetableSlotNotFoundException());

        mockMvc.perform(get("/api/timetables/1/slots/999")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIMETABLE_404_2"));
    }

    @Test
    void updateSlotReturns204() throws Exception {
        String body = """
                {
                  "scope": "ALL",
                  "classType": "SPECIAL",
                  "dayOfWeek": "TUESDAY",
                  "classroomCode": "602",
                  "startTime": "13:00:00",
                  "endTime": "15:00:00",
                  "grade": "HIGH_2",
                  "teacherName": "오T",
                  "subjectName": "물리"
                }
                """;

        mockMvc.perform(patch("/api/timetables/1/slots/100")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSlotReturns204() throws Exception {
        mockMvc.perform(delete("/api/timetables/1/slots/100?scope=ALL")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
