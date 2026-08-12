package com.academy.mudogroupware.revenuereport.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.config.SecurityConfig;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationFilter;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAccessDeniedHandler;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAuthenticationEntryPoint;
import com.academy.mudogroupware.revenuereport.application.usecase.CountUnreadRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.application.usecase.GetRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.application.usecase.ListRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

@WebMvcTest(RevenueReportController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class RevenueReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;
    @MockitoBean
    private ListRevenueReportsUseCase listRevenueReportsUseCase;
    @MockitoBean
    private GetRevenueReportUseCase getRevenueReportUseCase;
    @MockitoBean
    private CountUnreadRevenueReportsUseCase countUnreadRevenueReportsUseCase;

    @Test
    void listIsForbiddenWithoutAcademyOwnerAuthority() throws Exception {
        TestingAuthenticationToken token = new TestingAuthenticationToken("member", null, "ACCOUNT:MANAGE");

        mockMvc.perform(get("/api/revenue-reports").with(authentication(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReturnsReportsForAcademyOwner() throws Exception {
        TestingAuthenticationToken token = new TestingAuthenticationToken("owner", null, "ACADEMY:OWNER");
        RevenueReport report = RevenueReport.create(
                LocalDate.of(2026, 8, 1), "8월 리포트", "{}", LocalDateTime.now());
        when(listRevenueReportsUseCase.listReports()).thenReturn(java.util.List.of(report));

        mockMvc.perform(get("/api/revenue-reports").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REVENUE_REPORT_200_1"));
    }

    @Test
    void unreadCountReturnsCountForAcademyOwner() throws Exception {
        TestingAuthenticationToken token = new TestingAuthenticationToken("owner", null, "ACADEMY:OWNER");
        when(countUnreadRevenueReportsUseCase.countUnread()).thenReturn(2L);

        mockMvc.perform(get("/api/revenue-reports/unread-count").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }
}
