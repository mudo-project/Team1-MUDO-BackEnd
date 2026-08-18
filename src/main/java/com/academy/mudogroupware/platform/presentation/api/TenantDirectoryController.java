package com.academy.mudogroupware.platform.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.platform.application.service.TenantDirectoryQueryService;
import com.academy.mudogroupware.platform.presentation.api.common.PlatformResponseCode;
import com.academy.mudogroupware.platform.presentation.api.response.TenantDirectoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tenants")
@RequiredArgsConstructor
@Tag(name = "테넌트 진입점", description = "프론트가 학원 코드로 실제 백엔드 API 호스트를 조회하는 로그인 전 공개 API")
public class TenantDirectoryController {
  private final TenantDirectoryQueryService queryService;

  @Operation(summary = "학원 코드로 백엔드 API 호스트 조회", description = "프론트가 서브도메인에서 추출한 학원 코드를 실제 백엔드 API 호스트로 변환한다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "등록되지 않은 학원 코드")
  })
  @GetMapping("/{code}")
  public ResponseEntity<GlobalApiResponse<TenantDirectoryResponse>> resolve(@PathVariable String code) {
    return ResponseEntity.ok(GlobalApiResponse.ok(PlatformResponseCode.TENANT_DIRECTORY_READ,
        TenantDirectoryResponse.from(queryService.resolve(code))));
  }
}
