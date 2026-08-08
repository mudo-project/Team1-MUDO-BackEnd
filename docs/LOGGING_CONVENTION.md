# LOGGING_CONVENTION.md

## 목적

Service 메서드 단위로 비즈니스 이벤트 로그를 남겨 운영 중 장애 추적과 모니터링을
쉽게 한다. 실행시간 측정용 `PerformanceLogAspect`([%d ... executionTimeMs=...])와는
목적이 다른, 별개의 로깅 관심사다.

## 적용 대상

- Service 구현체(UseCase를 구현하는 클래스)의 public 메서드
- 상태를 변경하는 메서드뿐 아니라 조회(list/get) 메서드도 포함한다
- Controller, Domain, Repository/Adapter 계층에는 붙이지 않는다

## 로그 형식

```java
log.info("event=<도메인>_<행위>_시작 key1={}, key2={}", value1, value2);

// ... 메서드 로직 ...

log.info(
    "event=<도메인>_<행위>_완료 key1={}, key2={}, resultKey={}",
    value1,
    value2,
    result);
```

`GlobalExceptionHandler`가 모든 `ApplicationException`을
`event=exception_handled reason=... code=... traceId=...`로 이미 로깅한다. 따라서
**`ApplicationException`을 던지는 일반적인 Service 메서드(권한 검증, 존재 확인 등)에는
`_실패` try/catch를 추가하지 않는다** — 추가하면 같은 실패가 두 번 로깅된다.

`_실패`는 `GlobalExceptionHandler`를 거치지 않는 흐름, 즉 컨트롤러 응답과 무관하게
자체적으로 처리 결과를 로깅해야 하는 배치·스케줄러 로직에만 쓴다:

```java
try {
  // ... 배치 로직 ...
} catch (RuntimeException e) {
  log.warn("event=<도메인>_<행위>_실패 key1={}, key2={}, reason={}", value1, value2, e.getMessage());
  throw e;
}
```

## 이벤트명 규칙

- `<도메인>_<행위>_<시작|완료|실패>` 형태의 스네이크케이스
- 도메인·행위는 영문, 접미사(`시작`/`완료`/`실패`)만 한글로 표기한다
- 예: `recurring_template_create_시작`, `recurring_template_create_완료`,
  `recurring_task_generate_실패`

## 파라미터 규칙

- 키는 camelCase, 값은 `{}` 플레이스홀더로 남긴다
- `traceId`는 `logback-spring.xml`의 로그 패턴(`[traceId=%X{traceId:-}]`)에
  MDC로 이미 포함되므로 메시지 본문에 중복해서 넣지 않는다
- 완료 로그에는 처리 결과를 알 수 있는 값(생성된 ID, 처리 건수, 변경된 상태 등)을
  최소 하나 이상 포함한다

## 예시

```java
@Override
public RecurringTaskTemplate create(CreateRecurringTaskTemplateCommand command) {
  log.info(
      "event=recurring_template_create_시작 workspaceId={}, title={}",
      command.workspaceId(),
      command.title());

  RecurringTaskTemplate template = ...;
  RecurringTaskTemplate saved = recurringTaskTemplateRepository.save(template);

  log.info(
      "event=recurring_template_create_완료 workspaceId={}, templateId={}",
      command.workspaceId(),
      saved.getId());
  return saved;
}
```
