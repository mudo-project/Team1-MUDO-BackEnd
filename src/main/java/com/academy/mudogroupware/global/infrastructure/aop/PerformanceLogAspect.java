package com.academy.mudogroupware.global.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceLogAspect {
  @Around(
      "execution(* com.academy.mudogroupware..service..get*(..)) || execution(*"
          + " com.academy.mudogroupware..service..find*(..))")
  public Object measure(ProceedingJoinPoint p) throws Throwable {
    long s = System.nanoTime();
    try {
      return p.proceed();
    } finally {
      long ms = (System.nanoTime() - s) / 1_000_000;
      MethodSignature m = (MethodSignature) p.getSignature();
      log.info(
          "[PERFORMANCE] traceId={}, method={}.{}, executionTimeMs={}ms",
          MDC.get("traceId"),
          m.getDeclaringType().getSimpleName(),
          m.getMethod().getName(),
          ms);
    }
  }
}
