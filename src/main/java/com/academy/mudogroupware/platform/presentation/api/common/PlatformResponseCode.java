package com.academy.mudogroupware.platform.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlatformResponseCode implements ResponseCode {
  ACADEMIES_READ("PLATFORM_200_1", "플랫폼 학원 목록 조회에 성공했습니다."),
  OPERATIONAL_METRICS_READ("PLATFORM_200_2", "운영 성능·자원 지표 조회에 성공했습니다."),
  MEMBER_COUNT_READ("PLATFORM_200_3", "학원 회원 수 조회에 성공했습니다."),
  STORAGE_USAGE_READ("PLATFORM_200_4", "학원 데이터 보유량 조회에 성공했습니다."),
  API_CALL_FREQUENCY_READ("PLATFORM_200_5", "학원별 API 호출 빈도 조회에 성공했습니다."),
  TENANT_DIRECTORY_READ("PLATFORM_200_6", "테넌트 API 진입점 조회에 성공했습니다.");

  private final String code;
  private final String message;
}
