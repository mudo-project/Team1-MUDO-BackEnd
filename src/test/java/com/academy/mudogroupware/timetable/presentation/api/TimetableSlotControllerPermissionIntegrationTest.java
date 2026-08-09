package com.academy.mudogroupware.timetable.presentation.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.presentation.security.AuthUser;

@SpringBootTest
@AutoConfigureMockMvc
class TimetableSlotControllerPermissionIntegrationTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createSlotReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00"
                }
                """;

        mockMvc.perform(post("/api/timetables/1/slots")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSlotReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "scope": "ALL",
                  "classType": "CLASS",
                  "dayOfWeek": "MONDAY",
                  "classroomCode": "601",
                  "startTime": "09:00:00",
                  "endTime": "11:00:00"
                }
                """;

        mockMvc.perform(patch("/api/timetables/1/slots/100")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSlotReturns403WhenMissingManageAuthority() throws Exception {
        mockMvc.perform(delete("/api/timetables/1/slots/100?scope=ALL")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
