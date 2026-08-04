package com.academy.mudogroupware.global.infrastructure.executor;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> callerContext = MDC.getCopyOfContextMap();

    return () -> {
      Map<String, String> executorContext = MDC.getCopyOfContextMap();
      try {
        replaceContext(callerContext);
        runnable.run();
      } finally {
        replaceContext(executorContext);
      }
    };
  }

  private void replaceContext(Map<String, String> context) {
    MDC.clear();
    if (context != null) {
      MDC.setContextMap(context);
    }
  }
}
