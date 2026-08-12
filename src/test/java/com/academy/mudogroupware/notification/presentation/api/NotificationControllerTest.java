package com.academy.mudogroupware.notification.presentation.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.notification.application.usecase.CountUnreadNotificationsUseCase;
import com.academy.mudogroupware.notification.application.usecase.ListNotificationsUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.model.NotificationType;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListNotificationsUseCase listNotificationsUseCase;
    @MockitoBean
    private CountUnreadNotificationsUseCase countUnreadNotificationsUseCase;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private Authentication auth() {
        return new UsernamePasswordAuthenticationToken(AUTH_USER, null, List.of());
    }

    @Test
    void getNotificationsReturnsPagedList() throws Exception {
        Notification notification = Notification.restore(1L, 10L,
                NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                null, LocalDateTime.of(2026, 8, 13, 9, 0));
        when(listNotificationsUseCase.getNotifications(10L, 0, 20))
                .thenReturn(PageResult.of(List.of(notification), 0, 20, false));

        mockMvc.perform(get("/api/notifications").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].notificationId").value(1))
                .andExpect(jsonPath("$.data.content[0].read").value(false));
    }

    @Test
    void getUnreadCountReturnsCount() throws Exception {
        when(countUnreadNotificationsUseCase.countUnread(10L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void getNotificationsPassesNonDefaultPageAndSizeToUseCase() throws Exception {
        when(listNotificationsUseCase.getNotifications(10L, 2, 50))
                .thenReturn(PageResult.of(List.of(), 2, 50, false));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "2")
                        .param("size", "50")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(50));
    }

    @Test
    void getNotificationsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("page", "-1")
                        .with(authentication(auth())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listNotificationsUseCase);
    }

    @Test
    void getNotificationsRejectsSizeBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("size", "0")
                        .with(authentication(auth())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listNotificationsUseCase);
    }

    @Test
    void getNotificationsRejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .param("size", "101")
                        .with(authentication(auth())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listNotificationsUseCase);
    }

    @Test
    void getNotificationsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(listNotificationsUseCase);
    }

    @Test
    void getUnreadCountReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(countUnreadNotificationsUseCase);
    }
}
