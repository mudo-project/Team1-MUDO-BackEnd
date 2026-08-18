package com.academy.mudogroupware.payroll.infrastructure.observability;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PayrollServiceLogAspect {

  @Around(
      "execution(public * com.academy.mudogroupware.payroll.application.service..*(..)) "
          + "&& !within(com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailPolicy)")
  public Object logServiceEvent(ProceedingJoinPoint joinPoint) throws Throwable {
    if (isEmailDispatch(joinPoint)) return joinPoint.proceed();

    String action = snake(joinPoint.getSignature().getDeclaringType().getSimpleName()) + "_"
        + snake(joinPoint.getSignature().getName());
    Object key = firstSafeKey(joinPoint.getArgs());
    log.info("event=payroll_{}_시작 requestKey={}", action, key);
    try {
      Object result = joinPoint.proceed();
      log.info("event=payroll_{}_완료 requestKey={}, result=success", action, key);
      return result;
    } catch (Throwable e) {
      log.warn("event=payroll_{}_실패 requestKey={}, errorType={}",
          action, key, e.getClass().getSimpleName());
      throw e;
    }
  }

  private boolean isEmailDispatch(ProceedingJoinPoint joinPoint) {
    return "PayrollStatementEmailDispatchService".equals(
        joinPoint.getSignature().getDeclaringType().getSimpleName())
        && "dispatch".equals(joinPoint.getSignature().getName());
  }

  private Object firstSafeKey(Object[] arguments) {
    if (arguments.length == 0) return "none";
    Object first = arguments[0];
    return first instanceof Number || first instanceof java.time.temporal.Temporal
        ? first : first.getClass().getSimpleName();
  }

  private String snake(String value) {
    return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }
}
