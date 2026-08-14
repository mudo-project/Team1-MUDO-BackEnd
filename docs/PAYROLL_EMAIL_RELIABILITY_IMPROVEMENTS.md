# 급여명세서 이메일 발송 신뢰성 개선

## 1. 개선 배경

기존 급여명세서 이메일 발송은 트랜잭션 커밋 후 비동기 이벤트에 의존했다.
따라서 다음과 같은 운영 위험이 있었다.

- 커밋 직후 서버가 종료되면 메모리의 발송 이벤트가 유실될 수 있다.
- 외부 메일 서버의 응답이 불명확할 때 무조건 재시도하면 중복 발송될 수 있다.
- Mailgun Webhook이 유실되면 실제 전달 결과와 내부 상태가 달라질 수 있다.
- 장기 대기, 반복 재시도, 대사 실패를 조기에 파악할 운영 지표가 부족했다.

이번 개선에서는 기존 `payroll_statement_delivery`를 영속 작업 큐로 활용하여 별도의
범용 Outbox 테이블을 추가하지 않고 필요한 신뢰성만 보강했다.

## 2. 개선 범위

다음 항목을 구현했다.

1. `PENDING` 영속 폴러
2. 재시도 상태와 시도 횟수
3. Mailgun HTTP API 발송
4. `SENT`, `UNKNOWN` 대사 스케줄러
5. 운영 지표와 Prometheus 알림

법인카드 등 다른 도메인의 로직은 이번 이메일 신뢰성 개선 범위에 포함하지 않았다.

## 3. 전체 처리 흐름

```text
발송 요청
  → Delivery PENDING 저장
  → 트랜잭션 커밋
  → AFTER_COMMIT 비동기 발송 시도
       또는
     영속 폴러가 PENDING 재조회
  → 조건부 선점: PENDING/RETRY_WAIT → SENDING
  → PDF 준비
  → Mailgun HTTP API 호출
  → 결과에 따라 SENT / RETRY_WAIT / UNKNOWN / FAILED / SKIPPED
  → Webhook 또는 대사 스케줄러
  → DELIVERED / FAILED / SENT 보정
```

커밋 직후 이벤트가 유실되더라도 Delivery가 DB에 `PENDING`으로 남기 때문에 재기동 후
폴러가 다시 조회할 수 있다. 여러 서버가 동일 작업을 조회하더라도 조건부 UPDATE에
성공한 하나의 실행자만 `SENDING` 상태를 획득한다.

## 4. 상태 모델

| 상태 | 의미 |
|---|---|
| `PENDING` | 발송 요청이 저장됐지만 아직 선점되지 않음 |
| `SENDING` | 실행자가 작업을 선점하여 처리 중 |
| `RETRY_WAIT` | 안전하게 재시도할 수 있는 오류로 다음 시각까지 대기 |
| `UNKNOWN` | Mailgun 접수 여부가 불명확하여 자동 재발송 금지 |
| `SENT` | Mailgun이 발송 요청을 접수함 |
| `DELIVERED` | 수신 서버 전달이 확인됨 |
| `FAILED` | 영구 실패 또는 최대 시도 횟수 초과 |
| `SKIPPED` | 메일 발송이 비활성화된 환경에서 건너뜀 |

추가된 영속 필드는 다음과 같다.

- `attempt_count`: 실제 선점·시도 횟수
- `next_attempt_at`: 다음 재시도 가능 시각
- `last_attempt_at`: 마지막 발송 시도 시각
- `last_reconciled_at`: 마지막 Mailgun 대사 시각
- `active_statement_id`: 동일 명세서의 활성 발송을 하나로 제한하는 생성 컬럼

## 5. 재시도와 중복 발송 방지

### 5.1 자동 재시도 대상

- Mailgun이 요청을 접수하지 않았음이 명확한 HTTP `429`
- Mailgun 호출 전에 발생한 PDF 조회·발송 준비 실패

재시도는 기본 최대 3회이며 지수 백오프를 적용한다.

```text
1차 실패 → 1분 후
2차 실패 → 2분 후
3차 실패 → 최종 FAILED
```

최대 지연 시간은 기본 30분으로 제한한다.

### 5.2 자동 재시도하지 않는 대상

- 연결 단절 또는 읽기 타임아웃
- HTTP `408`
- Mailgun HTTP `5xx`
- 정상 응답을 받았지만 Mailgun 메시지 ID가 없는 경우
- Mailgun 접수 성공 후 내부 `SENT` 기록만 실패한 경우

이 경우 외부 접수 여부를 단정할 수 없으므로 `UNKNOWN` 또는 만료 전 `SENDING`으로
유지한다. 같은 작업을 자동 재발송하지 않고 Mailgun 대사로 최종 결과를 확인한다.

특히 Mailgun 호출 성공 뒤 DB 상태 저장이 실패한 경우 일반 재시도 경로로 보내지 않는다.
시간이 지난 `SENDING`은 복구 스케줄러가 `UNKNOWN`으로 전환하므로, 내부 상태 저장 장애가
중복 이메일 발송으로 이어지지 않는다.

## 6. Mailgun HTTP API 전환

SMTP 대신 Mailgun REST API를 사용한다.

```text
Base URL: https://api.mailgun.net
Domain: mg.market-app.org
Method: POST /v3/{domain}/messages
Content-Type: multipart/form-data
Authentication: Basic api:{MAILGUN_API_KEY}
```

발송 요청에는 다음 식별자를 포함한다.

- `v:deliveryToken`: Webhook과 내부 Delivery 연결용 사용자 변수
- `o:tag`: Logs API 대사용 태그
- Mailgun 응답 `id`: 공급자 메시지 식별자

`deliveryToken`은 개인정보를 포함하지 않는 무작위 값이다. API 키는 코드나 설정 파일에
저장하지 않고 `MAILGUN_API_KEY` 환경변수로만 주입한다.

## 7. Webhook 유실 대비 대사

대사 스케줄러는 일정 시간이 지난 `SENT`와 `UNKNOWN` 작업을 조회하고 Mailgun의 다음 API를
호출한다.

```text
POST https://api.mailgun.net/v1/analytics/logs
```

조회 시 발송 도메인과 `deliveryToken` 태그를 함께 사용한다. 응답 태그가 정확히 일치하는
이벤트만 내부 Delivery에 반영한다.

| Mailgun 결과 | 내부 처리 |
|---|---|
| `delivered` | `DELIVERED`로 보정 |
| 영구 `failed`, `rejected` | `FAILED`로 보정 |
| `accepted` | `SENT`로 보정 |
| 일시적 `failed` | Mailgun 자체 재시도를 기다리며 `SENT` 유지 |
| 조회 결과 없음 | 상태 유지, 자동 재발송하지 않음 |

조회 결과가 없다는 사실만으로 Mailgun 미접수를 증명할 수 없으므로 `UNKNOWN`을 자동으로
재발송하지 않는다.

## 8. 운영 지표와 알림

추가한 Micrometer 지표는 다음과 같다.

```text
mudo.payroll.email.pending
mudo.payroll.email.retry.wait
mudo.payroll.email.unknown
mudo.payroll.email.oldest.waiting.age.seconds
mudo.payroll.email.retry.attempts
mudo.payroll.email.reconciliation.failures
```

Prometheus Alert Rule은 다음 상황을 감시한다.

- `PENDING` 작업 장기 정체
- `RETRY_WAIT` 상태 장기 지속
- 짧은 시간 안에 반복되는 재시도
- `UNKNOWN` 상태 지속
- Mailgun 대사 실패

운영 대응 절차는 `infra/runbooks/payroll-email-delivery.md`에 정리했다.

## 9. 기본 멱등 발송

동일한 급여명세서에 발송 요청이 동시에 들어오면 DB Unique Constraint로 활성 발송 이력을
하나만 유지한다. 최초 요청만 새 Delivery를 생성하고 나머지 요청은 기존 발송 이력을
멱등 응답으로 반환한다.

활성 상태는 다음과 같다.

```text
PENDING, SENDING, RETRY_WAIT, UNKNOWN, SENT, DELIVERED
```

`FAILED`, `SKIPPED` 이후에는 새로운 발송 요청을 만들 수 있다.

## 10. 검증 결과

최종 코드 기준 검증 결과는 다음과 같다.

- 전체 Gradle 테스트: 1,578건 실행, 실패 0, 오류 0, 스킵 7
- 급여 도메인 테스트: 37건 통과
- 배포 스크립트 테스트: 6건 통과
- Prometheus Rule YAML 파싱 통과
- `git diff --check` 통과
- Spring ApplicationContext 기동 테스트 통과

테스트에는 다음 시나리오가 포함된다.

- 이벤트 없이 DB에 남은 `PENDING` 작업을 폴러가 처리
- 조건부 선점에 실패한 중복 실행은 발송하지 않음
- 안전한 일시 오류는 `RETRY_WAIT`으로 전환
- 불명확한 결과는 `UNKNOWN`으로 전환하고 재시도하지 않음
- 최대 시도 횟수 초과 시 `FAILED`로 전환
- Mailgun 접수 후 `SENT` 기록 실패 시 재시도하지 않음
- Webhook 없이 Logs 대사로 `DELIVERED` 복구
- Mailgun 조회 결과가 없어도 `UNKNOWN`을 재발송하지 않음

## 11. 별도 환경 검증이 필요한 항목

다음 항목은 구현 및 단위·회귀 테스트와 별개로 실제 인프라에서 확인해야 한다.

1. 실제 MySQL에서 Flyway `V2.3.2` 적용 검증
2. 실제 MySQL 다중 트랜잭션 조건부 선점·Unique Constraint 동시성 검증
3. 실제 Mailgun 계정으로 메시지 발송과 메시지 ID 저장 검증
4. 실제 Mailgun Logs API에서 태그 기반 대사 검증
5. 운영 환경의 `MAILGUN_API_KEY`와 Webhook 서명 키 주입 확인
6. Prometheus·Alertmanager에서 신규 지표 수집과 알림 전달 확인

이 항목들은 현재 구현 완료 여부와 분리하여 운영 배포 전에 검증해야 한다.

## 12. 주요 구현 파일

- `PayrollStatementEmailProcessor`: 발송 준비, 오류 분류, 상태 전이
- `PayrollStatementEmailDispatchService`: 영속 작업 조회와 비동기 실행 요청
- `PayrollStatementEmailDispatchScheduler`: 주기적 PENDING 폴링
- `PayrollStatementEmailReconciliationService`: Mailgun 결과 대사
- `MailgunPayrollStatementEmailSender`: Mailgun HTTP 발송
- `MailgunPayrollStatementEmailStatusAdapter`: Mailgun Logs 조회
- `PayrollStatementEmailOperationalMetrics`: 운영 지표 등록과 갱신
- `PayrollStatementDeliveryPersistenceAdapter`: 조건부 선점과 상태 영속화
- `V2.3.2__ensure_single_active_payroll_statement_delivery.sql`: 상태·재시도·멱등 제약

