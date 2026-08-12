package com.academy.mudogroupware.resourceusage.presentation.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;
import com.academy.mudogroupware.resourceusage.application.query.ResourceUsageResourceSummaryView;
import com.academy.mudogroupware.resourceusage.application.usecase.GetMonthlyResourceUsageUseCase;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

@WebMvcTest(ResourceUsageController.class)
class ResourceUsageControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 3L, "OWNER");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMonthlyResourceUsageUseCase getMonthlyResourceUsageUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void getMonthlyUsageReturnsAiAndSmsUsageForAcademyAdmin() throws Exception {
        YearMonth month = YearMonth.of(2026, 8);
        when(getMonthlyResourceUsageUseCase.getMonthlyUsage(month)).thenReturn(new MonthlyResourceUsageView(month,
                List.of(new ResourceUsageResourceSummaryView(ResourceUsageType.AI_TOKEN, "tokens", 97478,
                        List.of(new ResourceUsageFeatureSummary(ResourceUsageType.AI_TOKEN,
                                "approval-attachment-summary", 30, 70350, 33510, 3060, 70350))),
                        new ResourceUsageResourceSummaryView(ResourceUsageType.SMS, "messages", 150,
                                List.of(new ResourceUsageFeatureSummary(ResourceUsageType.SMS,
                                        "rollcall-attendance-sms", 12, 150, 0, 0, 0))))));

        mockMvc.perform(get("/api/resource-usage/monthly")
                        .param("month", "2026-08")
                        .with(authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RESOURCE_USAGE_200_1"))
                .andExpect(jsonPath("$.data.month").value("2026-08"))
                .andExpect(jsonPath("$.data.resources[0].resourceType").value("AI_TOKEN"))
                .andExpect(jsonPath("$.data.resources[0].totalAmount").value(97478))
                .andExpect(jsonPath("$.data.resources[1].resourceType").value("SMS"))
                .andExpect(jsonPath("$.data.resources[1].totalAmount").value(150));
    }

    @Test
    void getMonthlyUsageRejectsUserWithoutAccountManagePermission() throws Exception {
        mockMvc.perform(get("/api/resource-usage/monthly")
                        .param("month", "2026-08")
                        .with(authentication(userWithoutAccountManagePermission())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getMonthlyResourceUsageUseCase);
    }

    private Authentication adminUser() {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(new SimpleGrantedAuthority("ACCOUNT:MANAGE")));
    }

    private Authentication userWithoutAccountManagePermission() {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(new SimpleGrantedAuthority("ROLLCALL:MANAGE")));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
