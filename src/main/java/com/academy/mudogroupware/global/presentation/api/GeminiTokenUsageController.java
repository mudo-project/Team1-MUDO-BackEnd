package com.academy.mudogroupware.global.presentation.api;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.response.GeminiTokenUsageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 로컬에서 Gemini 기능(결재 요약, 필드 추출, 데이터 동기화 등)의 토큰 사용량을 기능별로
 * 확인하기 위한 디버그용 엔드포인트다. GeminiTokenUsageTracker가 프로세스 메모리에 쌓아둔
 * 값을 그대로 보여주며, 앱을 재시작하면 초기화된다. local 프로필에서만 뜬다.
 */
@Tag(name = "[로컬 디버그] Gemini 토큰 사용량", description = "Gemini 호출 기능별 누적 토큰 사용량 조회 (local 전용)")
@Profile("local")
@RestController
@RequestMapping("/api/debug/gemini-usage")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GeminiTokenUsageController {

    private final GeminiTokenUsageTracker tokenUsageTracker;

    @Operation(summary = "Gemini 기능별 누적 토큰 사용량 조회",
            description = "이 인스턴스가 뜬 이후 기능별 호출 횟수, 프롬프트/응답/총 토큰 합계와 호출당 평균을 반환한다.")
    @GetMapping
    public GlobalApiResponse<List<GeminiTokenUsageResponse>> getUsage() {
        List<GeminiTokenUsageResponse> usage = tokenUsageTracker.snapshot().stream()
                .map(GeminiTokenUsageResponse::from)
                .toList();
        return GlobalApiResponse.ok("GEMINI_USAGE_RETRIEVED", "Gemini 토큰 사용량 조회 성공", usage);
    }
}
