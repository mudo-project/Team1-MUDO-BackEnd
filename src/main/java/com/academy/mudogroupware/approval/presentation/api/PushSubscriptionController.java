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
 * 결재 차례 도래 등을 Web Push로 알리기 위한 구독 정보 저장 API.
 * 실제 푸시 발송(VAPID, web-push 라이브러리 연동)은 아직 구현하지 않았다 — 프론트 서비스워커 준비 후 별도 작업으로 연동한다.
 */
@Tag(name = "Web Push 구독", description = "브라우저 푸시 구독 정보 저장/해지 API (실제 발송은 아직 미구현)")
@RestController
@RequestMapping("/api/approvals/push-subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final RegisterPushSubscriptionUseCase registerPushSubscriptionUseCase;
    private final UnregisterPushSubscriptionUseCase unregisterPushSubscriptionUseCase;

    @Operation(summary = "푸시 구독 등록", description = "같은 사용자·endpoint 조합이 이미 있으면 새로 만들지 않고 p256dh/auth 키만 갱신한다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<PushSubscriptionCreateResponse>> register(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterPushSubscriptionRequest request) {
        Long subscriptionId = registerPushSubscriptionUseCase.register(request.toCommand(authUser.userId()));
        PushSubscriptionCreateResponse data = PushSubscriptionCreateResponse.from(subscriptionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.PUSH_SUBSCRIPTION_REGISTERED, data));
    }

    @Operation(summary = "푸시 구독 해지", description = "본인 구독만 해지할 수 있다.")
    @DeleteMapping
    public ResponseEntity<Void> unregister(@AuthenticationPrincipal AuthUser authUser,
                                            @RequestParam String endpoint) {
        unregisterPushSubscriptionUseCase.unregister(authUser.userId(), endpoint);
        return ResponseEntity.noContent().build();
    }
}
