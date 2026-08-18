package com.academy.mudogroupware.rollcall.infrastructure.executor;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rollcall.sms.executor")
public class RollcallSmsExecutorProperties {
  // SOLAPI 발송 API 기본 한도는 5초당 100회(초당 20건, SOLAPI 개발자 문서 기준)다. 응답 시간을
  // 200~300ms로 가정하면 core 4 / max 6 정도가 이 한도 안에 안전하게 들어간다 - 공용 실행기
  // (applicationTaskExecutor, core 1/max 2)를 같이 쓰면 큰 반 SMS 발송이 다른 비동기 작업을
  // 밀어내므로 전용 실행기로 분리한다.
  @Min(1) private int corePoolSize = 4;

  @Min(1) private int maxPoolSize = 6;

  @Min(0) private int queueCapacity = 50;

  @Min(0) private int keepAliveSeconds = 60;

  @Min(0) private int awaitTerminationSeconds = 30;
}
