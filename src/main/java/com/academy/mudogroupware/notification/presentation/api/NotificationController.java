package com.academy.mudogroupware.notification.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.notification.application.usecase.CountUnreadNotificationsUseCase;
import com.academy.mudogroupware.notification.application.usecase.ListNotificationsUseCase;
import com.academy.mudogroupware.notification.application.usecase.MarkNotificationAsReadUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.presentation.api.common.NotificationResponseCode;
import com.academy.mudogroupware.notification.presentation.api.response.NotificationItemResponse;
import com.academy.mudogroupware.notification.presentation.api.response.UnreadNotificationCountResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "알림", description = "알림 목록 조회·안읽은 개수 조회·읽음 처리·삭제 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final CountUnreadNotificationsUseCase countUnreadNotificationsUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;

    @Operation(summary = "알림 목록 조회", description = "본인에게 온 알림을 생성일 최신순으로 페이지네이션 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<SliceResponse<NotificationItemResponse>>> getNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResult<Notification> notifications =
                listNotificationsUseCase.getNotifications(authUser.userId(), page, size);
        SliceResponse<NotificationItemResponse> response =
                SliceResponse.from(notifications, NotificationItemResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(NotificationResponseCode.NOTIFICATION_LIST_RETRIEVED, response));
    }

    @Operation(summary = "안읽은 알림 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<GlobalApiResponse<UnreadNotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal AuthUser authUser) {
        long unreadCount = countUnreadNotificationsUseCase.countUnread(authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(
                NotificationResponseCode.UNREAD_COUNT_RETRIEVED, new UnreadNotificationCountResponse(unreadCount)));
    }

    @Operation(summary = "알림 읽음 처리", description = "본인 소유 알림만 처리할 수 있습니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<GlobalApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long notificationId) {
        markNotificationAsReadUseCase.markAsRead(notificationId, authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(NotificationResponseCode.NOTIFICATION_READ));
    }
}
