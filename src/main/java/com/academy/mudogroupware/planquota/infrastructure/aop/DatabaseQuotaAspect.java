package com.academy.mudogroupware.planquota.infrastructure.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitErrorCode;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.platform.application.port.CurrentTenantDatabaseUsagePort;

import lombok.RequiredArgsConstructor;

/**
 * 클래스 레벨 @Transactional에도 걸려야 해서 @annotation() 포인트컷 대신
 * 메서드→선언 클래스 순으로 @Transactional을 직접 찾는다(Spring이 트랜잭션
 * 속성을 해석하는 순서와 동일해, 메서드 레벨 오버라이드도 올바르게 반영된다).
 *
 * <p>{@code @Order}로 트랜잭션 어드바이저(기본값 {@code LOWEST_PRECEDENCE})보다
 * 반드시 먼저(더 바깥쪽에서) 실행되도록 고정한다. 순서를 명시하지 않으면 이 한도
 * 체크 쿼리가 대상 메서드의 트랜잭션 "안쪽"에서 실행될 수 있는데, 그 경우 같은
 * 트랜잭션 안에서 비관적 락({@code SELECT ... FOR UPDATE})을 거는 다른 서비스
 * (예: 시간표 슬롯 생성의 교실 중복 예약 방지)의 락 타이밍에 영향을 줘 동시성
 * 방어가 깨지는 게 실제로 재현됐다 — 이 클래스가 앱 전체 서비스 계층
 * (`..service..`)에 광범위하게 걸리기 때문이다.
 *
 * <p>{@code HIGHEST_PRECEDENCE} 그 자체는 쓰지 않는다 — Spring의
 * {@code ExposeInvocationInterceptor}가 이미 그 값을 쓰고 있어서, 동률이면 이
 * 어드바이스가 그보다 먼저 실행돼 {@code JoinPoint} 매칭에 필요한
 * {@code MethodInvocation} 컨텍스트가 아직 없는 상태로 실행될 수 있다
 * (실행 시 {@code IllegalStateException: No MethodInvocation found}로 즉시
 * 재현됨). 그래서 한 칸 양보한 {@code HIGHEST_PRECEDENCE + 1}을 쓴다 —
 * 여전히 트랜잭션 어드바이저(LOWEST_PRECEDENCE)보다는 압도적으로 먼저다.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class DatabaseQuotaAspect {

    private final CurrentTenantDatabaseUsagePort databaseUsagePort;
    private final CurrentPlanProvider currentPlanProvider;

    @Before("execution(* com.academy.mudogroupware..service..*(..))")
    public void checkBeforeWrite(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(
                signature.getMethod(), Transactional.class);
        if (transactional == null) {
            transactional = AnnotatedElementUtils.findMergedAnnotation(
                    joinPoint.getTarget().getClass(), Transactional.class);
        }
        if (transactional == null || transactional.readOnly()) {
            return;
        }

        long limit = currentPlanProvider.currentLimits().rdsBytesLimit();
        long current = databaseUsagePort.databaseBytes();
        if (current >= limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.RDS_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, current);
        }
    }
}
