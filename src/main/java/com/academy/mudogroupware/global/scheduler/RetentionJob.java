package com.academy.mudogroupware.global.scheduler;

import java.time.LocalDateTime;

public interface RetentionJob {

    // 작업 식별 역할
    String name();

    // 기준 시각을 받아 도메인 정리 작업을 실행하는 역할
    RetentionJobResult run(LocalDateTime now);
}
