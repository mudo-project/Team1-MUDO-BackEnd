package com.academy.mudogroupware.users.presentation.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.users.application.usecase.ChangeMyPasswordUseCase;
import com.academy.mudogroupware.users.application.usecase.ChangeUserStatusUseCase;
import com.academy.mudogroupware.users.application.usecase.CreateAccountUseCase;
import com.academy.mudogroupware.users.application.usecase.GetMemberDetailUseCase;
import com.academy.mudogroupware.users.application.usecase.GetMyProfileUseCase;
import com.academy.mudogroupware.users.application.usecase.ListMembersUseCase;
import com.academy.mudogroupware.users.application.usecase.PasswordSetupUseCase;
import com.academy.mudogroupware.users.application.usecase.SearchUsersUseCase;
import com.academy.mudogroupware.users.application.usecase.UpdateMemberProfileUseCase;
import com.academy.mudogroupware.users.application.usecase.UpdateMyProfileUseCase;

// @WebMvcTest 슬라이스는 실제 SecurityConfig(@EnableMethodSecurity)를 로드하지 않아 @PreAuthorize가
// 동작하지 않는다. ACCOUNT:MANAGE 권한 없이 403이 반환되는지는 전체 컨텍스트 통합 테스트에서 검증한다.
@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(1L, "admin", 7L, "원장");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchUsersUseCase searchUsersUseCase;
    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;
    @MockitoBean
    private PasswordSetupUseCase passwordSetupUseCase;
    @MockitoBean
    private ListMembersUseCase listMembersUseCase;
    @MockitoBean
    private GetMyProfileUseCase getMyProfileUseCase;
    @MockitoBean
    private UpdateMyProfileUseCase updateMyProfileUseCase;
    @MockitoBean
    private ChangeMyPasswordUseCase changeMyPasswordUseCase;
    @MockitoBean
    private GetMemberDetailUseCase getMemberDetailUseCase;
    @MockitoBean
    private UpdateMemberProfileUseCase updateMemberProfileUseCase;
    @MockitoBean
    private ChangeUserStatusUseCase changeUserStatusUseCase;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void listMembersReturns400WhenStatusIsInvalidAndNeverInvokesUseCase() throws Exception {
        mockMvc
                .perform(get("/api/users/members")
                        .with(authentication(authenticatedUser("ACCOUNT:MANAGE")))
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));

        verifyNoInteractions(listMembersUseCase);
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
