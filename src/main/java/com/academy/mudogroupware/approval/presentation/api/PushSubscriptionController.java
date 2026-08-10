package com.academy.mudogroupware.approval.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.approval.application.usecase.RegisterPushSubscriptionUseCase;
import com.academy.mudogroupware.approval.application.usecase.UnregisterPushSubscriptionUseCase;
import com.academy.mudogroupware.approval.presentation.api.common.ApprovalResponseCode;
import com.academy.mudogroupware.approval.presentation.api.request.RegisterPushSubscriptionRequest;
import com.academy.mudogroupware.approval.presentation.api.response.PushSubscriptionCreateResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 사용 중지 예정 API. 결재 실시간 알림은 WebSocket/STOMP로 고정한다.
 * 기존 구독 저장 API는 호환성 때문에 남겨두지만 Web Push 발송 로직은 추가하지 않는다.
 */
@Deprecated(since = "2026-08-06", forRemoval = false)
@Tag(name = "Web Push 구독", description = "사용 중지 예정 API. 결재 실시간 알림은 WebSocket/STOMP를 사용한다.")
@RestController
@RequestMapping("/api/approvals/push-subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final RegisterPushSubscriptionUseCase registerPushSubscriptionUseCase;
    private final UnregisterPushSubscriptionUseCase unregisterPushSubscriptionUseCase;

    @Operation(summary = "푸시 구독 등록(사용 중지 예정)",
            description = "호환성 때문에 구독 정보 저장만 유지한다. 실제 Web Push 발송은 구현하지 않는다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<PushSubscriptionCreateResponse>> register(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterPushSubscriptionRequest request) {
        Long subscriptionId = registerPushSubscriptionUseCase.register(request.toCommand(authUser.userId()));
        PushSubscriptionCreateResponse data = PushSubscriptionCreateResponse.from(subscriptionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.PUSH_SUBSCRIPTION_REGISTERED, data));
    }

    @Operation(summary = "푸시 구독 해지(사용 중지 예정)", description = "본인 구독만 해지할 수 있다.")
    @DeleteMapping
    public ResponseEntity<Void> unregister(@AuthenticationPrincipal AuthUser authUser,
                                            @RequestParam String endpoint) {
        unregisterPushSubscriptionUseCase.unregister(authUser.userId(), endpoint);
        return ResponseEntity.noContent().build();
    }
}
