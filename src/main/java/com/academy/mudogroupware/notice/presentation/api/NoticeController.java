package com.academy.mudogroupware.notice.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.notice.application.query.NoticeDetailView;
import com.academy.mudogroupware.notice.application.usecase.CreateNoticeUseCase;
import com.academy.mudogroupware.notice.application.usecase.DeleteNoticeUseCase;
import com.academy.mudogroupware.notice.application.usecase.NoticeQueryUseCase;
import com.academy.mudogroupware.notice.application.usecase.PinNoticeUseCase;
import com.academy.mudogroupware.notice.application.usecase.UpdateNoticeUseCase;
import com.academy.mudogroupware.notice.presentation.api.common.NoticeResponseCode;
import com.academy.mudogroupware.notice.presentation.api.request.CreateNoticeRequest;
import com.academy.mudogroupware.notice.presentation.api.request.UpdateNoticeRequest;
import com.academy.mudogroupware.notice.presentation.api.response.NoticeCreateResponse;
import com.academy.mudogroupware.notice.presentation.api.response.NoticeDetailResponse;
import com.academy.mudogroupware.notice.presentation.api.response.NoticeReaderResponse;
import com.academy.mudogroupware.notice.presentation.api.response.NoticeSummaryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: 작성 권한(원장/대표, 상황에 따라 직원)은 users.role 값 체계 확정되면 Application/Domain Policy에 반영
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final CreateNoticeUseCase createNoticeUseCase;
    private final UpdateNoticeUseCase updateNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;
    private final PinNoticeUseCase pinNoticeUseCase;
    private final NoticeQueryUseCase noticeQueryUseCase;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<NoticeCreateResponse>> createNotice(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateNoticeRequest request) {
        Long noticeId = createNoticeUseCase.createNotice(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(NoticeResponseCode.NOTICE_CREATED, NoticeCreateResponse.from(noticeId)));
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<NoticeSummaryResponse>>> getNotices(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String keyword) {
        List<NoticeSummaryResponse> responses = noticeQueryUseCase.getNotices(authUser.userId(), keyword).stream()
                .map(NoticeSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_LIST_RETRIEVED, responses));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<GlobalApiResponse<NoticeDetailResponse>> getNoticeDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long noticeId) {
        NoticeDetailView view = noticeQueryUseCase.getNoticeDetail(noticeId, authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_DETAIL_RETRIEVED,
                NoticeDetailResponse.from(view)));
    }

    @GetMapping("/{noticeId}/readers")
    public ResponseEntity<GlobalApiResponse<List<NoticeReaderResponse>>> getReaders(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long noticeId) {
        List<NoticeReaderResponse> responses = noticeQueryUseCase.getReaders(noticeId, authUser.userId()).stream()
                .map(NoticeReaderResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_READERS_RETRIEVED, responses));
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(@AuthenticationPrincipal AuthUser authUser,
                                              @PathVariable Long noticeId,
                                              @Valid @RequestBody UpdateNoticeRequest request) {
        updateNoticeUseCase.updateNotice(request.toCommand(noticeId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@AuthenticationPrincipal AuthUser authUser,
                                              @PathVariable Long noticeId) {
        deleteNoticeUseCase.deleteNotice(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{noticeId}/pin")
    public ResponseEntity<Void> pinNotice(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long noticeId) {
        pinNoticeUseCase.pin(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}/pin")
    public ResponseEntity<Void> unpinNotice(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long noticeId) {
        pinNoticeUseCase.unpin(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }
}
