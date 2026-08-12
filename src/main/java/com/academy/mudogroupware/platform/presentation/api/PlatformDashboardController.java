package com.academy.mudogroupware.platform.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.platform.application.service.PlatformDashboardQueryService;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.presentation.api.common.PlatformResponseCode;
import com.academy.mudogroupware.platform.presentation.api.response.AcademyResponse;
import com.academy.mudogroupware.platform.presentation.api.response.MemberCountResponse;
import com.academy.mudogroupware.platform.presentation.api.response.OperationalMetricsResponse;
import com.academy.mudogroupware.platform.presentation.api.response.StorageUsageResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM:SUPER_ADMIN')")
@Tag(name = "플랫폼 대시보드", description = "슈퍼어드민 운영 성능·자원, 회원 수, 데이터 보유량 조회 API")
public class PlatformDashboardController {
  private final PlatformDashboardQueryService queryService;

  @GetMapping("/academies")
  @Operation(summary = "플랫폼 학원 목록 조회", description = "플랫폼 대시보드에서 선택할 학원 코드를 조회한다.")
  @ApiResponses(@ApiResponse(responseCode = "200", description = "학원 목록 조회 성공"))
  public ResponseEntity<GlobalApiResponse<List<AcademyResponse>>> academies() {
    return ResponseEntity.ok(GlobalApiResponse.ok(PlatformResponseCode.ACADEMIES_READ,
        queryService.academies().stream().map(AcademyResponse::from).toList()));
  }

  @GetMapping("/operational-metrics")
  @Operation(summary = "운영 성능·자원 지표 조회", description = "전체 또는 선택 학원의 운영 성능·자원 지표를 조회한다.")
  @ApiResponses(@ApiResponse(responseCode = "200", description = "운영 지표 조회 성공"))
  public ResponseEntity<GlobalApiResponse<OperationalMetricsResponse>> operationalMetrics(
      @RequestParam(defaultValue = "ALL") DashboardScope scope,
      @RequestParam(required = false) String academyCode,
      @RequestParam(defaultValue = "LAST_HOUR") DashboardPeriod period) {
    return ResponseEntity.ok(GlobalApiResponse.ok(PlatformResponseCode.OPERATIONAL_METRICS_READ,
        OperationalMetricsResponse.from(queryService.operationalMetrics(scope, academyCode, period))));
  }

  @GetMapping("/academies/{academyCode}/member-count")
  @Operation(summary = "학원 회원 수 조회", description = "선택 학원의 현재 활성 회원 수를 조회한다.")
  @ApiResponses(@ApiResponse(responseCode = "200", description = "회원 수 조회 성공"))
  public ResponseEntity<GlobalApiResponse<MemberCountResponse>> memberCount(@PathVariable String academyCode) {
    return ResponseEntity.ok(GlobalApiResponse.ok(PlatformResponseCode.MEMBER_COUNT_READ,
        new MemberCountResponse(academyCode, queryService.activeMemberCount(academyCode), Instant.now())));
  }

  @GetMapping("/academies/{academyCode}/storage-usage")
  @Operation(summary = "학원 데이터 보유량 조회", description = "선택 학원의 DB와 S3 파일 사용량을 조회한다.")
  @ApiResponses(@ApiResponse(responseCode = "200", description = "데이터 보유량 조회 성공"))
  public ResponseEntity<GlobalApiResponse<StorageUsageResponse>> storageUsage(@PathVariable String academyCode) {
    return ResponseEntity.ok(GlobalApiResponse.ok(PlatformResponseCode.STORAGE_USAGE_READ,
        StorageUsageResponse.from(queryService.storageUsage(academyCode))));
  }
}
