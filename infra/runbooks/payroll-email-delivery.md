# 급여명세서 이메일 발송 장애 대응

## PENDING 정체

1. `mudo_payroll_email_pending`과 `mudo_async_rejected_total`을 확인한다.
2. 애플리케이션과 DB 연결 상태, 비동기 실행기 큐를 확인한다.
3. 원인을 복구한 뒤 영속 디스패처가 `PENDING`을 다시 선점하는지 확인한다.
4. DB 상태를 수동 변경하거나 동일 명세서를 다시 등록하지 않는다.

## RETRY_WAIT 지속·반복 재시도

1. `mudo_payroll_email_retry_wait`과 `mudo_payroll_email_retry_attempts`를 확인한다.
2. 429 응답과 발송 전 PDF 저장소·DB 오류가 반복되는지 `errorType` 로그로 확인한다.
3. 접수 여부가 불명확한 `UNKNOWN`은 재시도 대상으로 바꾸지 않는다.
4. 원인 복구 후 예약 시간이 지난 작업이 폴러에 의해 다시 선점되는지 확인한다.

## UNKNOWN 발생

1. 해당 발송의 `delivery_id`, `delivery_token`, `mailgun_message_id`와 요청 시각을 확인한다.
2. Mailgun Logs 조회 및 대사 스케줄러 오류 로그를 확인한다.
3. Mailgun 접수가 확인되면 대사 결과가 `SENT`, `DELIVERED`, `FAILED`로 반영되는지 확인한다.
4. Mailgun에서 찾지 못했다는 이유만으로 즉시 재발송하지 않는다.

## 대사 실패

1. `MAILGUN_API_KEY`, 발송 도메인, 미국 리전 API 접근 상태를 확인한다.
2. Mailgun API 응답 코드와 `errorType` 로그를 확인한다. API 키와 수신 이메일은 로그에 남기지 않는다.
3. 장애 복구 후 다음 대사 주기에 `last_reconciled_at`이 갱신되는지 확인한다.

## reconcile/recover 로그가 지연 없이 반복 출력

1. `event=payroll_..._reconciliation_service_reconcile_시작/완료`,
   `event=payroll_..._recovery_service_recover_시작/완료`가 0.xx초 간격으로 계속 찍히는지 확인한다.
2. `payroll_statement_delivery`에서 실제 대사 대상(`SENT`/`UNKNOWN`, `last_reconciled_at` 만료분)이
   있는지 조회한다. 대상이 0건인데도 로그가 계속 찍히면 실제 처리 지연이 아니라 워커의 시각 판단
   오류다.
3. 컨테이너의 JVM 기본 타임존을 확인한다(`docker exec <container> date`). `Asia/Seoul`이 아니면
   `PayrollStatementEmailDeliveryWorker`(예약 시각 판단)와 `PayrollStatementEmailReconciliationService`/
   `PayrollStatementEmailRecoveryService`(DB 시각 비교)가 서로 다른 기준으로 "지금"을 계산해 무한
   재실행을 일으킬 수 있다(#593, PR #594에서 공용 `Clock` Bean 주입으로 수정).
4. 애플리케이션은 수정됐으므로, 배포 버전이 해당 수정을 포함하는지 먼저 확인한다. 포함하는데도
   재현되면 별도 원인이므로 에스컬레이션한다.
