package com.academy.mudogroupware.planquota.infrastructure.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
 */
@Aspect
@Component
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
        if (current > limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.RDS_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, current);
        }
    }
}
