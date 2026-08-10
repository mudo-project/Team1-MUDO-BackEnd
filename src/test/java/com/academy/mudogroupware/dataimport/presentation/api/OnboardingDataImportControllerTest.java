package com.academy.mudogroupware.dataimport.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;
import com.academy.mudogroupware.dataimport.application.usecase.ConfirmOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.CreateOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.GetImportDraftUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.GetImportResultUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.UpdateImportDraftUseCase;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;

@WebMvcTest(OnboardingDataImportController.class)
class OnboardingDataImportControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "OWNER");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOnboardingImportUseCase createOnboardingImportUseCase;

    @MockitoBean
    private GetImportDraftUseCase getImportDraftUseCase;

    @MockitoBean
    private UpdateImportDraftUseCase updateImportDraftUseCase;

    @MockitoBean
    private ConfirmOnboardingImportUseCase confirmOnboardingImportUseCase;

    @MockitoBean
    private GetImportResultUseCase getImportResultUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void uploadFilesReturnsImportId() throws Exception {
        MockMultipartFile studentFile = new MockMultipartFile("studentFile", "students.csv",
                "text/csv", "이름,학년\n김민수,고1".getBytes(StandardCharsets.UTF_8));
        when(createOnboardingImportUseCase.create(any(CreateOnboardingImportCommand.class))).thenReturn(1L);

        mockMvc.perform(multipart("/api/data-imports/onboarding/files")
                        .file(studentFile)
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DATA_IMPORT_201_1"))
                .andExpect(jsonPath("$.data.importId").value(1));

        verify(createOnboardingImportUseCase).create(any(CreateOnboardingImportCommand.class));
    }

    @Test
    void getDraftReturnsDraftTabs() throws Exception {
        when(getImportDraftUseCase.getDraft(1L, 1L)).thenReturn(ImportDraft.empty());

        mockMvc.perform(get("/api/data-imports/onboarding/1/draft")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DATA_IMPORT_200_1"))
                .andExpect(jsonPath("$.data.students").isArray())
                .andExpect(jsonPath("$.data.lectures").isArray())
                .andExpect(jsonPath("$.data.enrollments").isArray());
    }

    @Test
    void confirmReturnsResult() throws Exception {
        when(confirmOnboardingImportUseCase.confirm(1L, 1L, 7L))
                .thenReturn(new ImportResult(1, 2, 3, 4, 0));

        mockMvc.perform(post("/api/data-imports/onboarding/1/confirm")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DATA_IMPORT_200_3"))
                .andExpect(jsonPath("$.data.createdStudents").value(1))
                .andExpect(jsonPath("$.data.createdLectures").value(2))
                .andExpect(jsonPath("$.data.createdEnrollments").value(3));
    }

    @Test
    void getResultReturnsStoredResult() throws Exception {
        when(getImportResultUseCase.getResult(1L, 1L)).thenReturn(new ImportResult(1, 2, 3, 4, 0));

        mockMvc.perform(get("/api/data-imports/onboarding/1/result")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DATA_IMPORT_200_4"))
                .andExpect(jsonPath("$.data.skippedRows").value(4));
    }

    @Test
    void uploadFilesRejectsUserWithoutImportPermission() throws Exception {
        MockMultipartFile studentFile = new MockMultipartFile("studentFile", "students.csv",
                "text/csv", "이름,학년\n김민수,고1".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/data-imports/onboarding/files")
                        .file(studentFile)
                        .with(authentication(userWithoutImportPermission()))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(createOnboardingImportUseCase);
    }

    @Test
    void getDraftRejectsUserWithoutImportPermission() throws Exception {
        mockMvc.perform(get("/api/data-imports/onboarding/1/draft")
                        .with(authentication(userWithoutImportPermission())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getImportDraftUseCase);
    }

    @Test
    void updateDraftRejectsUserWithoutImportPermission() throws Exception {
        mockMvc.perform(patch("/api/data-imports/onboarding/1/draft")
                        .contentType("application/json")
                        .content("{\"students\":[],\"lectures\":[],\"enrollments\":[]}")
                        .with(authentication(userWithoutImportPermission()))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(updateImportDraftUseCase);
    }

    @Test
    void confirmRejectsUserWithoutImportPermission() throws Exception {
        mockMvc.perform(post("/api/data-imports/onboarding/1/confirm")
                        .with(authentication(userWithoutImportPermission()))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(confirmOnboardingImportUseCase);
    }

    @Test
    void getResultRejectsUserWithoutImportPermission() throws Exception {
        mockMvc.perform(get("/api/data-imports/onboarding/1/result")
                        .with(authentication(userWithoutImportPermission())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getImportResultUseCase);
    }

    private Authentication authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of("STUDENT:MANAGE", "LECTURE:MANAGE").stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList());
    }

    private Authentication userWithoutImportPermission() {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(new SimpleGrantedAuthority("STUDENT:MANAGE")));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
