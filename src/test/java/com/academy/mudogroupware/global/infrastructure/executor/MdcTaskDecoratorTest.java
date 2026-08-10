package com.academy.mudogroupware.global.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void copiesCallerMdcAndRestoresExecutorMdc() {
    MDC.put("tenantId", "academy-a");
    AtomicReference<String> observedTenant = new AtomicReference<>();
    Runnable decorated =
        new MdcTaskDecorator().decorate(() -> observedTenant.set(MDC.get("tenantId")));

    MDC.put("tenantId", "executor-thread-value");
    decorated.run();

    assertThat(observedTenant.get()).isEqualTo("academy-a");
    assertThat(MDC.get("tenantId")).isEqualTo("executor-thread-value");
  }
}
