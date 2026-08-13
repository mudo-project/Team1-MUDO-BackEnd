package com.academy.mudogroupware.timetable.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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
import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;
import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.application.query.TimetableSetSummaryView;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.ExportTimetableUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetsUseCase;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

// @WebMvcTest 슬라이스는 실제 SecurityConfig(@EnableMethodSecurity)를 로드하지 않아 @PreAuthorize가
// 동작하지 않는다. TIMETABLE:MANAGE 없이 403이 반환되는지는 전체 컨텍스트 통합 테스트에서 검증한다.
@WebMvcTest(TimetableController.class)
class TimetableControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 3L, "MEMBER");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateTimetableSetUseCase createTimetableSetUseCase;
    @MockitoBean private GetTimetableSetsUseCase getTimetableSetsUseCase;
    @MockitoBean private GetTimetableSetUseCase getTimetableSetUseCase;
    @MockitoBean private UpdateTimetableSetUseCase updateTimetableSetUseCase;
    @MockitoBean private DeleteTimetableSetUseCase deleteTimetableSetUseCase;
    @MockitoBean private ExportTimetableUseCase exportTimetableUseCase;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void createTimetableSetReturns201WithGeneratedId() throws Exception {
        when(createTimetableSetUseCase.createTimetableSet(any(CreateTimetableSetCommand.class))).thenReturn(101L);
        String body = """
                {
                  "name": "2026 여름특강",
                  "startDate": "2026-07-20",
                  "endDate": "2026-08-16",
                  "operatingStartTime": "08:30",
                  "operatingEndTime": "22:00",
                  "operatingDays": ["MONDAY", "WEDNESDAY"],
                  "slotUnitMinutes": 30,
                  "classrooms": [
                    {"floor": "6층", "codes": ["601", "602"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/timetables")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("TIMETABLE_201_1"))
                .andExpect(jsonPath("$.data.timetableSetId").value(101));

        verify(createTimetableSetUseCase).createTimetableSet(any(CreateTimetableSetCommand.class));
    }

    @Test
    void createTimetableSetReturns400WhenNameIsBlank() throws Exception {
        String body = """
                {
                  "name": "  ",
                  "startDate": "2026-07-20",
                  "endDate": "2026-08-16",
                  "operatingStartTime": "08:30",
                  "operatingEndTime": "22:00",
                  "operatingDays": ["MONDAY"],
                  "slotUnitMinutes": 30,
                  "classrooms": [{"floor": "6층", "codes": ["601"]}]
                }
                """;

        mockMvc.perform(post("/api/timetables")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @Test
    void createTimetableSetReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/timetables").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTimetableSetsReturns200WithList() throws Exception {
        when(getTimetableSetsUseCase.getTimetableSets()).thenReturn(List.of(
                new TimetableSetSummaryView(
                        1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                        TimetableSetStatus.ACTIVE)));

        mockMvc.perform(get("/api/timetables")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_1"))
                .andExpect(jsonPath("$.data[0].timetableSetId").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void getTimetableSetReturns200WithDetail() throws Exception {
        TimetableSetDetailView view = new TimetableSetDetailView(
                1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), TimetableSetStatus.ACTIVE);
        when(getTimetableSetUseCase.getTimetableSet(1L)).thenReturn(view);

        mockMvc.perform(get("/api/timetables/1")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TIMETABLE_200_2"))
                .andExpect(jsonPath("$.data.classrooms[0].floor").value("6층"))
                .andExpect(jsonPath("$.data.classrooms[0].codes[0]").value("601"));
    }

    @Test
    void getTimetableSetReturns404WhenNotFound() throws Exception {
        when(getTimetableSetUseCase.getTimetableSet(999L))
                .thenThrow(new TimetableSetNotFoundException());

        mockMvc.perform(get("/api/timetables/999")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIMETABLE_404_1"));
    }

    @Test
    void updateTimetableSetReturns204() throws Exception {
        String body = """
                {
                  "name": "새 이름",
                  "startDate": "2026-09-01",
                  "endDate": "2026-12-31",
                  "operatingStartTime": "09:00",
                  "operatingEndTime": "21:00",
                  "operatingDays": ["TUESDAY"],
                  "slotUnitMinutes": 10,
                  "classrooms": [{"floor": "3층", "codes": ["301"]}]
                }
                """;

        mockMvc.perform(patch("/api/timetables/1")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTimetableSetReturns204() throws Exception {
        mockMvc.perform(delete("/api/timetables/1")
                        .with(authentication(authenticatedUser("TIMETABLE:MANAGE")))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void exportTimetableReturns200WithExcelContentType() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class))).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "EXCEL")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("timetable_1.xlsx")));
    }

    @Test
    void exportTimetableAcceptsFilterAndDensityParameters() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class))).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "PNG")
                        .param("density", "SPACIOUS")
                        .param("dayOfWeek", "MONDAY")
                        .param("floor", "6층")
                        .param("classType", "CLASS")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void exportTimetableReturns404WhenNotFound() throws Exception {
        when(exportTimetableUseCase.export(any(ExportTimetableCommand.class)))
                .thenThrow(new TimetableSetNotFoundException());

        mockMvc.perform(get("/api/timetables/999/export")
                        .param("format", "PDF")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIMETABLE_404_1"));
    }

    @Test
    void exportTimetableReturns400WhenFormatIsInvalidEnumValue() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "INVALID")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportTimetableReturns400WhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @Test
    void exportTimetableReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/timetables/1/export")
                        .param("format", "EXCEL"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
