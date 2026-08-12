package com.academy.mudogroupware.revenuereport.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RevenueReportResponseCode implements ResponseCode {
    REVENUE_REPORT_LIST_FETCHED("REVENUE_REPORT_200_1", "매출 리포트 목록 조회에 성공했습니다."),
    REVENUE_REPORT_DETAIL_FETCHED("REVENUE_REPORT_200_2", "매출 리포트 상세 조회에 성공했습니다."),
    REVENUE_REPORT_UNREAD_COUNT_FETCHED("REVENUE_REPORT_200_3", "안읽은 매출 리포트 수 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
