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
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: 작성 권한(원장/대표, 상황에 따라 직원)은 users.role 값 체계 확정되면 Application/Domain Policy에 반영
@Tag(name = "공지사항", description = "공지사항 작성/조회/수정/삭제/고정 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final CreateNoticeUseCase createNoticeUseCase;
    private final UpdateNoticeUseCase updateNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;
    private final PinNoticeUseCase pinNoticeUseCase;
    private final NoticeQueryUseCase noticeQueryUseCase;

    @Operation(summary = "공지사항 작성", description = "제목·내용은 필수, 여러 개의 파일을 첨부할 수 있다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<NoticeCreateResponse>> createNotice(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateNoticeRequest request) {
        Long noticeId = createNoticeUseCase.createNotice(request.toCommand(authUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(NoticeResponseCode.NOTICE_CREATED, NoticeCreateResponse.from(noticeId)));
    }

    @Operation(summary = "공지사항 목록 조회", description = "요청자 소속 학원 공지만 페이지 단위로 조회. 고정 공지가 항상 먼저, 그다음 최신순.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<SliceResponse<NoticeSummaryResponse>>> getNotices(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<NoticeSummaryResponse> data = SliceResponse.from(
                noticeQueryUseCase.getNotices(authUser.userId(), keyword, page, size), NoticeSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_LIST_RETRIEVED, data));
    }

    @Operation(summary = "공지사항 상세 조회", description = "호출 시 조회수가 1 증가하고 읽음 처리가 자동 기록된다. 다른 학원 공지 조회 시 403.")
    @GetMapping("/{noticeId}")
    public ResponseEntity<GlobalApiResponse<NoticeDetailResponse>> getNoticeDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long noticeId) {
        NoticeDetailView view = noticeQueryUseCase.getNoticeDetail(noticeId, authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_DETAIL_RETRIEVED,
                NoticeDetailResponse.from(view)));
    }

    @Operation(summary = "읽은 사람 목록 조회", description = "상세 화면의 '읽음 N/M' 클릭 시 호출. 최근에 읽은 사람 순으로 정렬.")
    @GetMapping("/{noticeId}/readers")
    public ResponseEntity<GlobalApiResponse<List<NoticeReaderResponse>>> getReaders(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long noticeId) {
        List<NoticeReaderResponse> responses = noticeQueryUseCase.getReaders(noticeId, authUser.userId()).stream()
                .map(NoticeReaderResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(NoticeResponseCode.NOTICE_READERS_RETRIEVED, responses));
    }

    @Operation(summary = "공지사항 수정", description = "작성자 본인만 가능.")
    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(@AuthenticationPrincipal AuthUser authUser,
                                              @PathVariable Long noticeId,
                                              @Valid @RequestBody UpdateNoticeRequest request) {
        updateNoticeUseCase.updateNotice(request.toCommand(noticeId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공지사항 삭제", description = "현재는 작성자 본인만 가능 (권한자 확대는 role 체계 확정 후).")
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@AuthenticationPrincipal AuthUser authUser,
                                              @PathVariable Long noticeId) {
        deleteNoticeUseCase.deleteNotice(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공지사항 고정", description = "작성자 본인만 가능.")
    @PostMapping("/{noticeId}/pin")
    public ResponseEntity<Void> pinNotice(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long noticeId) {
        pinNoticeUseCase.pin(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공지사항 고정 해제", description = "현재는 임시로 인증 사용자 전체 허용 (권한자 제한은 role 체계 확정 후).")
    @DeleteMapping("/{noticeId}/pin")
    public ResponseEntity<Void> unpinNotice(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long noticeId) {
        pinNoticeUseCase.unpin(noticeId, authUser.userId());
        return ResponseEntity.noContent().build();
    }
}
