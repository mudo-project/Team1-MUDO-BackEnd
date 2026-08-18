package com.academy.mudogroupware.payroll.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailDispatchServiceTest {
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock PayrollStatementEmailProcessor processor;

  @Test
  void 이벤트가_없어도_영속된_발송_대상을_다시_처리한다() {
    var policy = new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1),
        Duration.ofMinutes(30), Duration.ofMinutes(10), Duration.ofMinutes(5));
    var service = new PayrollStatementEmailDispatchService(deliveries, processor, policy);
    when(deliveries.findDispatchableIds(any(), anyInt()))
        .thenReturn(List.of(10L, 11L));

    List<ILoggingEvent> logs = captureLogs(service::dispatch);

    assertThat(logs).anySatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.INFO);
      assertThat(event.getFormattedMessage())
          .contains("event=payroll_statement_email_dispatch_완료", "processedCount=2");
    });
    verify(processor).processPending(10L);
    verify(processor).processPending(11L);
  }

  @Test
  void 빈_발송_배치는_dispatch_시작과_완료를_DEBUG로_기록한다() {
    var service = service();
    when(deliveries.findDispatchableIds(any(), anyInt())).thenReturn(List.of());

    List<ILoggingEvent> logs = captureLogs(service::dispatch);

    assertThat(logs).filteredOn(event -> event.getFormattedMessage()
            .contains("event=payroll_statement_email_dispatch_"))
        .hasSize(2)
        .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
  }

  @Test
  void dispatch_예외는_WARN으로_기록한다() {
    var service = service();
    when(deliveries.findDispatchableIds(any(), anyInt()))
        .thenThrow(new IllegalStateException("database unavailable"));
    Logger logger = (Logger) LoggerFactory.getLogger(PayrollStatementEmailDispatchService.class);
    Level previousLevel = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.DEBUG);
    logger.addAppender(appender);

    try {
      assertThatThrownBy(service::dispatch).isInstanceOf(IllegalStateException.class);
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(previousLevel);
      appender.stop();
    }

    assertThat(appender.list).anySatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.WARN);
      assertThat(event.getFormattedMessage())
          .contains("event=payroll_statement_email_dispatch_실패");
    });
  }

  private PayrollStatementEmailDispatchService service() {
    var policy = new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1),
        Duration.ofMinutes(30), Duration.ofMinutes(10), Duration.ofMinutes(5));
    return new PayrollStatementEmailDispatchService(deliveries, processor, policy);
  }

  private List<ILoggingEvent> captureLogs(Runnable action) {
    Logger logger = (Logger) LoggerFactory.getLogger(PayrollStatementEmailDispatchService.class);
    Level previousLevel = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.DEBUG);
    logger.addAppender(appender);
    try {
      action.run();
      return List.copyOf(appender.list);
    } finally {
      logger.detachAppender(appender);
      logger.setLevel(previousLevel);
      appender.stop();
    }
  }
}
