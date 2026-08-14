package com.academy.mudogroupware.payroll.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailDispatchService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailPolicy;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class PayrollServiceLogAspectTest {
  @Mock ProceedingJoinPoint joinPoint;
  @Mock MethodSignature signature;

  @Test
  void 발송_대상이_없는_폴링은_DEBUG로_기록한다() throws Throwable {
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(PayrollStatementEmailDispatchService.class);
    when(signature.getName()).thenReturn("dispatch");
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn(0);

    List<ILoggingEvent> events = captureLogs(() -> new PayrollServiceLogAspect().logServiceEvent(joinPoint));

    assertThat(events)
        .extracting(ILoggingEvent::getLevel)
        .containsOnly(Level.DEBUG);
  }

  @Test
  void 발송_배치_크기_조회는_DEBUG로_기록한다() throws Throwable {
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(PayrollStatementEmailPolicy.class);
    when(signature.getName()).thenReturn("dispatchBatchSize");
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn(20);

    List<ILoggingEvent> events = captureLogs(() -> new PayrollServiceLogAspect().logServiceEvent(joinPoint));

    assertThat(events)
        .extracting(ILoggingEvent::getLevel)
        .containsOnly(Level.DEBUG);
  }

  private List<ILoggingEvent> captureLogs(ThrowingRunnable action) throws Throwable {
    Logger logger = (Logger) LoggerFactory.getLogger(PayrollServiceLogAspect.class);
    Level originalLevel = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.DEBUG);
    logger.addAppender(appender);
    try {
      action.run();
      return appender.list;
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(originalLevel);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Throwable;
  }
}
