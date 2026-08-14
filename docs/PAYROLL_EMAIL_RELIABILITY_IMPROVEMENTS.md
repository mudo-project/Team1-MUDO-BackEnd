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

## 3. 설계 의사결정과 Trade-off

### 3.1 영속 폴러와 기존 Delivery 테이블 재사용

**개선 전 한계**

커밋 후 발행되는 비동기 이벤트는 메모리에만 존재하므로, DB 커밋과 비동기 실행 사이에
서버가 종료되면 발송 요청을 다시 찾을 근거가 없었다. 이벤트 수신 실패를 확인하는 별도의
운영 수단도 없었다.

**선택한 방식**

발송 요청을 먼저 `payroll_statement_delivery`에 `PENDING`으로 저장하고, AFTER_COMMIT
이벤트는 빠른 실행 경로로만 사용한다. 별도의 폴러가 DB에 남은 `PENDING`과 실행 시각이 된
`RETRY_WAIT`을 주기적으로 조회한다. 실제 발송 전에는 조건부 UPDATE로 `SENDING` 상태를
선점한다.

**이 방식을 선택한 이유**

- DB 트랜잭션 안에서 발송 요청과 Delivery를 함께 저장할 수 있어 별도의 이중 쓰기가 없다.
- 현재 요구사항은 급여명세서 이메일 한 종류이므로 범용 Outbox 테이블과 라우터를 도입하면
  필요한 범위보다 구조가 커진다.
- 메시지 브로커나 분산 락을 새로 운영하지 않고 기존 MySQL만으로 장애 복구 근거를 남길 수 있다.
- 조건부 UPDATE는 여러 서버가 같은 작업을 읽어도 실제 발송 실행자를 하나로 제한한다.

**얻은 것**

- 서버 재기동 후 미처리 작업 복구
- 이벤트 유실에 의존하지 않는 최소 한 번 실행 기회
- 다중 인스턴스 환경에서 중복 선점 방지
- 별도의 메시지 브로커 없이 구현 가능한 운영 단순성

**감수한 Trade-off**

- 폴링 주기만큼 발송이 늦어질 수 있다. 기본 최악 지연은 약 10초다.
- 주기적인 DB 조회가 추가되므로 작업량이 커지면 인덱스와 배치 크기 조정이 필요하다.
- Delivery 테이블이 발송 이력과 작업 큐 역할을 함께 담당한다.
- 범용 Outbox가 아니므로 다른 도메인의 이벤트 전달에는 그대로 재사용하기 어렵다.
- 현재 구조는 높은 처리량보다 급여명세서 중복 방지와 복구 가능성을 우선한다.

### 3.2 보수적인 재시도 정책과 `UNKNOWN`

**개선 전 한계**

외부 호출 실패를 모두 동일하게 처리하면 두 가지 문제가 생긴다. 재시도하지 않으면 일시
장애에서 자동 복구되지 않고, 반대로 모든 실패를 재시도하면 Mailgun이 이미 접수한 요청을
다시 보내 동일 급여명세서가 중복 발송될 수 있다.

**선택한 방식**

오류를 `RETRYABLE`, `PERMANENT`, `UNKNOWN`으로 구분한다. Mailgun 미접수가 명확한 429와
외부 호출 전 준비 실패만 자동 재시도한다. 타임아웃, 연결 단절, 408, 5xx처럼 접수 여부가
불명확한 결과는 `UNKNOWN`으로 전환하고 자동 재발송하지 않는다.

**이 방식을 선택한 이유**

급여명세서는 민감한 문서이므로 일시적인 발송 지연보다 동일 문서의 중복 전달을 더 큰 위험으로
판단했다. HTTP 오류 코드만으로 외부 시스템의 처리 여부를 완전히 증명할 수 없기 때문에,
불명확한 결과는 재시도 정책이 아니라 대사 정책으로 해결하는 편이 안전하다.

**얻은 것**

- 명확한 일시 장애는 지수 백오프로 자동 복구
- 불명확한 결과의 무분별한 재발송 방지
- 시도 횟수와 다음 시도 시각을 통한 재시도 이력 추적
- 외부 접수 후 내부 상태 저장 실패까지 중복 방지 범위에 포함

**감수한 Trade-off**

- 실제로 Mailgun이 접수하지 않았더라도 `UNKNOWN`이면 자동 재발송되지 않는다.
- 대사 결과가 올 때까지 발송 결과 확정이 늦어진다.
- Logs에서도 결과를 찾지 못한 `UNKNOWN`은 운영자가 판단해야 할 수 있다.
- 오류 분류 정책을 Mailgun 응답 계약 변화에 맞춰 지속적으로 관리해야 한다.

### 3.3 SMTP 대신 Mailgun HTTP API 사용

**개선 전 한계**

SMTP 전송은 애플리케이션이 Mailgun의 메시지 ID와 구조화된 응답을 직접 확보하기 어렵고,
발송 요청에 대사용 식별자를 일관되게 포함시키는 것도 제한적이었다. 그 결과 내부 Delivery와
Mailgun 이벤트를 안정적으로 연결하기 어려웠다.

**선택한 방식**

미국 리전 Mailgun REST API로 전환하고 응답의 메시지 ID를 저장한다. 동일한 무작위
`deliveryToken`을 `v:deliveryToken`과 `o:tag`로 전달하여 각각 Webhook 연결과 Logs 대사에
사용한다.

**이 방식을 선택한 이유**

- 구조화된 HTTP 상태와 응답 본문으로 오류를 분류할 수 있다.
- Mailgun 메시지 ID를 내부 발송 이력에 즉시 저장할 수 있다.
- 사용자 변수와 태그를 통해 개인정보 없이 내부·외부 이벤트를 연결할 수 있다.
- 발송과 대사를 하나의 Mailgun API 인증 체계로 운영할 수 있다.

**얻은 것**

- Mailgun 메시지 ID 추적
- Webhook과 Logs 조회에 사용할 안정적인 상관관계 키
- HTTP 상태별 재시도·영구 실패·불명확 결과 분리
- SMTP 설정 제거와 발송 방식 단일화

**감수한 Trade-off**

- Mailgun의 API 경로, 응답 스키마, 태그 및 Logs 계약에 대한 결합도가 높아졌다.
- API 키 관리와 HTTP 타임아웃 설정이 필요하다.
- 5xx나 연결 단절은 HTTP API에서도 접수 여부를 확정할 수 없으므로 대사가 여전히 필요하다.
- 다른 메일 공급자로 교체할 때 Sender뿐 아니라 대사 Adapter도 새로 구현해야 한다.

### 3.4 Webhook과 Logs 대사의 병행

**개선 전 한계**

Webhook만 사용하면 네트워크 장애, 설정 오류 또는 수신 서버 장애로 이벤트가 유실됐을 때
Mailgun의 실제 결과가 내부 상태에 반영되지 않는다. `SENT`가 장기간 남아 있어도 정상 지연과
Webhook 유실을 구분하기 어려웠다.

**선택한 방식**

Webhook은 빠른 상태 반영 경로로 유지하고, 스케줄러가 오래된 `SENT`와 `UNKNOWN`을 Mailgun
Logs API에서 다시 조회한다. 결과가 없으면 미접수로 단정하지 않고 기존 상태를 유지한다.

**이 방식을 선택한 이유**

Webhook과 조회 API는 장애 형태가 다르다. Push 방식의 빠른 반영과 Pull 방식의 누락 복구를
함께 사용하면 하나의 전달 경로에 의존하지 않고 최종 상태를 점진적으로 맞출 수 있다.

**얻은 것**

- Webhook 유실 후 `DELIVERED`, `FAILED`, `SENT` 상태 복구
- `UNKNOWN`을 재발송하지 않고 외부 사실로 해소할 수 있는 경로
- `last_reconciled_at`을 통한 조회 주기 제어와 운영 추적

**감수한 Trade-off**

- 내부 상태는 즉시 강한 일관성이 아니라 최종적 일관성을 가진다.
- Mailgun Logs 보존 기간, 조회 지연, 사용량 제한의 영향을 받는다.
- 대사 스케줄러와 오류 대응 절차를 추가로 운영해야 한다.
- `NOT_FOUND`는 미접수를 의미하지 않으므로 일부 `UNKNOWN`이 자동으로 종결되지 않을 수 있다.

### 3.5 DB 기반 운영 지표와 알림

**개선 전 한계**

발송 API의 성공·실패 로그만으로는 현재 대기 작업 수, 가장 오래된 작업, 반복 재시도 또는
대사 장애를 정량적으로 판단하기 어려웠다. 문제가 사용자 문의 이후에 발견될 가능성이 있었다.

**선택한 방식**

DB의 현재 상태를 주기적으로 집계하여 Micrometer Gauge로 노출하고, 대사 오류는 Counter로
기록한다. Prometheus가 정체와 반복 실패 조건을 평가하도록 Alert Rule을 추가한다.

**이 방식을 선택한 이유**

프로세스 메모리의 이벤트 카운터만 사용하면 재기동 시 현재 적체 상태를 잃는다. DB 기반 Gauge는
애플리케이션 재기동 이후에도 영속 상태를 다시 계산할 수 있고, 기존 Prometheus 운영 구조를
그대로 사용할 수 있다.

**얻은 것**

- 장애가 사용자 문의로 나타나기 전 적체 감지
- 재기동 후에도 복원되는 현재 상태 지표
- PENDING, RETRY_WAIT, UNKNOWN, 재시도, 대사 실패의 분리 관찰
- 알림에서 Runbook으로 이어지는 운영 대응 경로

**감수한 Trade-off**

- 메트릭 갱신 주기만큼 관측이 늦을 수 있다.
- 상태 집계 쿼리가 DB 부하를 추가한다.
- 임계값이 실제 발송량과 맞지 않으면 과도한 알림 또는 탐지 지연이 발생할 수 있다.
- 장기적으로 데이터가 많아지면 보존 정책이나 별도 집계 방식이 필요할 수 있다.

### 3.6 동일 명세서 DB Unique Constraint

**개선 전 한계**

애플리케이션에서 기존 발송 이력을 먼저 조회하는 방식만으로는 두 트랜잭션이 동시에 조회한 뒤
각각 새 Delivery를 생성하는 경쟁 조건을 막을 수 없다.

**선택한 방식**

활성 상태일 때만 `statement_id`를 반환하는 생성 컬럼에 Unique Constraint를 적용한다.
애플리케이션은 충돌한 요청에서 이미 생성된 발송 이력을 다시 조회하여 멱등 응답으로 반환한다.

**이 방식을 선택한 이유**

동시성의 최종 방어선은 모든 애플리케이션 인스턴스가 공유하는 DB에 있어야 한다. 사전 조회는
정상 흐름 최적화로 사용하고, Unique Constraint가 실제 불변식을 보장하도록 구성했다.

**얻은 것**

- 동시 요청에서도 활성 발송 이력 하나만 생성
- 서버 인스턴스 수와 무관한 멱등성 보장
- 별도 분산 락 없이 DB 제약으로 불변식 보호

**감수한 Trade-off**

- `DELIVERED`도 활성 상태이므로 정상 발송 완료 후 단순 반복 요청은 기존 이력을 반환한다.
- 정책상 재발송이 필요하면 기존 실패 처리 또는 별도의 명시적 재발송 정책이 필요하다.
- 생성 컬럼과 Unique Constraint가 MySQL 구현에 의존한다.
- 기존 중복 데이터가 있다면 마이그레이션 전에 정리 정책이 필요할 수 있다.

## 4. 전체 처리 흐름

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

## 5. 상태 모델

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

## 6. 재시도와 중복 발송 방지

### 6.1 자동 재시도 대상

- Mailgun이 요청을 접수하지 않았음이 명확한 HTTP `429`
- Mailgun 호출 전에 발생한 PDF 조회·발송 준비 실패

재시도는 기본 최대 3회이며 지수 백오프를 적용한다.

```text
1차 실패 → 1분 후
2차 실패 → 2분 후
3차 실패 → 최종 FAILED
```

최대 지연 시간은 기본 30분으로 제한한다.

### 6.2 자동 재시도하지 않는 대상

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

## 7. Mailgun HTTP API 전환

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

## 8. Webhook 유실 대비 대사

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

## 9. 운영 지표와 알림

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

## 10. 기본 멱등 발송

동일한 급여명세서에 발송 요청이 동시에 들어오면 DB Unique Constraint로 활성 발송 이력을
하나만 유지한다. 최초 요청만 새 Delivery를 생성하고 나머지 요청은 기존 발송 이력을
멱등 응답으로 반환한다.

활성 상태는 다음과 같다.

```text
PENDING, SENDING, RETRY_WAIT, UNKNOWN, SENT, DELIVERED
```

`FAILED`, `SKIPPED` 이후에는 새로운 발송 요청을 만들 수 있다.

## 11. 검증 결과

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

## 12. 별도 환경 검증이 필요한 항목

다음 항목은 구현 및 단위·회귀 테스트와 별개로 실제 인프라에서 확인해야 한다.

1. 실제 MySQL에서 Flyway `V2.3.2` 적용 검증
2. 실제 MySQL 다중 트랜잭션 조건부 선점·Unique Constraint 동시성 검증
3. 실제 Mailgun 계정으로 메시지 발송과 메시지 ID 저장 검증
4. 실제 Mailgun Logs API에서 태그 기반 대사 검증
5. 운영 환경의 `MAILGUN_API_KEY`와 Webhook 서명 키 주입 확인
6. Prometheus·Alertmanager에서 신규 지표 수집과 알림 전달 확인

이 항목들은 현재 구현 완료 여부와 분리하여 운영 배포 전에 검증해야 한다.

## 13. 주요 구현 파일

- `PayrollStatementEmailProcessor`: 발송 준비, 오류 분류, 상태 전이
- `PayrollStatementEmailDispatchService`: 영속 작업 조회와 비동기 실행 요청
- `PayrollStatementEmailDispatchScheduler`: 주기적 PENDING 폴링
- `PayrollStatementEmailReconciliationService`: Mailgun 결과 대사
- `MailgunPayrollStatementEmailSender`: Mailgun HTTP 발송
- `MailgunPayrollStatementEmailStatusAdapter`: Mailgun Logs 조회
- `PayrollStatementEmailOperationalMetrics`: 운영 지표 등록과 갱신
- `PayrollStatementDeliveryPersistenceAdapter`: 조건부 선점과 상태 영속화
- `V2.3.2__ensure_single_active_payroll_statement_delivery.sql`: 상태·재시도·멱등 제약
