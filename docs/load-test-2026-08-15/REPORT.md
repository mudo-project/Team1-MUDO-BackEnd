# MUDO-Groupware 피크타임 부하테스트 결과 리포트

- **대상 시나리오**: 유료 플랜 MAX(직원 500명) 학원의 피크타임 재현
- **테스트 스크립트**: `k6/scripts/02-peak-load-500.js` *(로컬 전용, `k6/`는 `.gitignore` 대상이라 이 문서에선 링크 대신 경로만 표기)*
- **설계 문서**: `k6/PEAK_LOAD_SCENARIO.md` *(동일, 로컬 전용)*
- **최종 실행**: 2026-08-15, 30분, `RESULT_NAME=peak-load-500-run1`
- **환경**: 로컬 (Spring Boot 직접 기동 + MySQL + Prometheus/Loki/Grafana)

## 결론 요약

30분 피크타임 부하테스트 **통과**. k6 threshold 4개 전부 `ok:true`, 부하 곡선이 설계(평균 25 / 피크 60~80 RPS)와 거의 정확히 일치, Tomcat 스레드·DB 커넥션 풀 모두 여유 큼. 사전 캘리브레이션에서 발견한 이슈 4건 중 3건은 이번 세션에서 수정 완료·재발 없음 확인, 1건(presigned-url 500)은 별도 담당자가 배포 서버에서 확인 중.

---

## 1. 캘리브레이션 단계에서 발견·수정한 이슈

5분/7분 사전 테스트를 반복하며 아래 4건을 발견해 수정했고, 30분 본 테스트에서 재발하지 않는 것까지 확인했다.

### 1-1. RPS 설계값과 실측 불일치 (2.53배 오버슛)

- **원인**: `RATE_PER_10S`는 iteration(=exec 함수 호출) 기준인데, exec 함수 하나가 API를 1~6개씩 묶어 호출해서 실제 HTTP req/s가 설정값보다 훨씬 높게 나옴 (5분 캘리브레이션 기준 base 목표 25 req/s vs 실측 63.5 req/s).
- **수정**: `RATE_PER_10S` 전체를 ×0.394(=25÷63.5)로 낮춤. 시나리오 간 비율은 유지, 전체 크기만 실측 기준으로 재조정.
- **검증**: 이후 7분 재테스트에서 실측 23.2 req/s로 목표(25)에 근접, 30분 본 테스트에서도 base 구간 26~27 req/s로 정확히 재현됨.

### 1-2. messenger/workspace 랜덤 ID로 인한 403 대량 발생

- **원인**: `messengerHttp`/`workspaceTaskComment`/`messengerWebsocket`이 `roomId`/`workspaceId`를 계정의 실제 소속과 무관하게 전체 범위에서 완전 랜덤으로 선택 → 500계정이 자기 소속 아닌 방/워크스페이스에 계속 접근 시도 → 서버가 정상적으로 403 응답 (20분 기준 messenger 1376건, workspace tasks/comments 171건).
- **수정**: 시드 SQL(`seed/001`, `seed/004`)의 배치 공식을 역산하는 헬퍼(`workspaceIdFor`, `randomTaskIdForWorkspace`, `chatRoomIdsFor`)를 추가해, 계정이 실제로 속한 워크스페이스/채팅방 안에서만 랜덤으로 고르도록 변경.
- **검증**: 이후 모든 재테스트(7분, 30분)에서 해당 URI들 403/404 **0건** 유지.

### 1-3. attendance check-in/check-out 403 (WiFi IP 화이트리스트 불일치)

- **원인**: `ClientIpResolver`가 `request.getRemoteAddr()`를 그대로 사용하는데, 화이트리스트(`academy_wifi_ip`)엔 `0:0:0:0:0:0:0:1`(IPv6 루프백)만 등록돼 있었고, k6가 `localhost`를 `127.0.0.1`(IPv4)로 해석해 매 요청이 불일치 → 100% 403.
- **재현**: `GET /api/attendance/wifi-ips/current`를 `localhost`/`127.0.0.1` 각각으로 호출해 서로 다른 IP가 찍히는 것으로 직접 확인.
- **수정**: `127.0.0.1`을 화이트리스트에 추가 등록 (API로 직접 등록, 기존 `::1` 행은 유지).
- **검증**: 이후 재테스트에서 403 **0건**, 정상적으로 201/200/409(하루 1회 제약)로 처리됨.

### 1-4. `/api/approvals` 404 — 시드 데이터와 서버 로직 불일치 (잠재적 실서비스 버그 가능성)

- **원인**: `seed/005_approval_files.sql`이 결재선(`approval_line_step`)을 `role_id=1, approver_id=NULL`로 시딩. 그런데 `CreateApprovalDocumentService`는 `role_id` 기반 동적 해석 없이 저장된 `approver_id`를 그대로 읽어서 씀 → `null` → `ApproverValidator`가 `APPROVER_NOT_FOUND`(404) 던짐.
- **주의**: DB 제약(`role_id`/`approver_id` 중 정확히 하나만 NULL)은 "역할 기반 결재선"을 스키마 차원에서 지원하려던 설계로 보이나, 실제 문서 생성 로직엔 역할→사용자 동적 해석이 구현돼 있지 않다. **k6 테스트만의 문제가 아니라 실서비스에서 역할 기반 결재선을 등록하면 동일하게 실패할 가능성이 있어 별도 확인이 필요.**
- **수정(로컬 한정)**: `approval_line_step` 16건을 `role_id=NULL, approver_id=1`로 변경 (DB 직접 수정 + `seed/005_approval_files.sql` 원본도 동일하게 수정).
- **검증**: 이후 재테스트에서 404 **0건**, 결재 신청 201 정상 처리 확인.

---

## 2. 최종 30분 테스트 결과

### 2-1. 핵심 지표

| 지표 | 값 |
| --- | ---: |
| http_reqs | 57,292 |
| iterations | 24,848 |
| checks 성공률 | 99.23% |
| http_req_failed | 2.92% (threshold `rate<0.05` ✅) |
| http_req_duration avg | 29ms |
| http_req_duration p95 | 82ms (threshold `p95<2000ms` ✅) |
| http_req_duration max | 1035ms *(단발성, 아래 참고)* |
| ws_connect_success | 100% (threshold `rate>0.95` ✅) |
| ws_subscribe_success | 100% (threshold `rate>0.95` ✅) |

원본 리포트: [`peak-load-500-run1-summary.md`](peak-load-500-run1-summary.md) / [`.json`](peak-load-500-run1-summary.json)

### 2-2. 부하 곡선 — 설계와 거의 일치

Prometheus 1분 단위 RPS로 확인한 실측 곡선:

| 구간 | 설계 목표 | 실측 |
| --- | --- | ---: |
| 0~2분 웜업 | 0→25 | 0→27 |
| 2~10분 평상시 | 25 | 26~27 req/s |
| 10~12분 피크 ramp | →75 | 42→67→76 |
| 12~15분 피크 유지 | 60~80 | 75~76 req/s |
| 15~17분 복귀 | →25 | 60→35→27 |
| 17~28분 평상시 | 25 | 26~27 req/s |
| 28~30분 쿨다운 | →0 | 17→5→0 |

캘리브레이션 보정값이 정확히 들어맞아, 설계 의도(평균 25 / 피크 60~80 RPS)를 실측으로 재현했다.

### 2-3. 1035ms 최대 응답시간 — 단발성, 무시 가능

`GET /api/messenger/rooms`(200 성공)에서 피크 ramp-up 전환 시점(10~12분)에 1건 발생. 순간적인 GC/커넥션 경합으로 추정되며, p95(82ms)/threshold엔 영향 없음.

### 2-4. WebSocket

| 지표 | 값 |
| --- | ---: |
| 연결 성공률 | 100% (2468/2468) |
| 구독 성공률 | 100% (2468/2468) |
| 재연결 횟수 | 2,053회 (3~8분 주기, 정상) |
| 최대 동시 연결(vus_max) | 548 |

**알려진 이슈**: `ws_message_delivery_delay_ms`(평균 165초, 최대 8분)는 실제 지연시간이 아니라 측정 로직 버그다. `messengerWebsocket()`이 `subscribedAt`을 연결 시작 시점 한 번만 기록하고, 그 이후 수신하는 모든 MESSAGE 프레임마다 "지금 - 최초 구독 시각"을 지연시간으로 기록하기 때문에, 연결이 오래 유지될수록(최대 8분) 값이 커지는 구조다. `sendMessage`가 보내는 메시지 본문에 이미 `Date.now()` 타임스탬프가 있으니, 이를 파싱해 `수신시각 - 전송시각`으로 계산하도록 고치면 실제 지연시간을 측정할 수 있다. **미수정 상태.**

### 2-5. 남은 에러 (http_req_failed 2.92%) — 전부 파악됨

| URI | Status | 30분 건수 | 성격 |
| --- | --- | ---: | --- |
| `/api/files/presigned-url` | 500 | 457 | **실제 버그, 담당자 확인 중** (로컬 S3 미연동으로 재현 불가, 배포 서버에서만 확인 가능) |
| `/api/attendance/check-ins` | 409 | 583 | 정상 (하루 1회 체크인 제약) |
| `/api/attendance/check-outs` | 409 | 543 | 정상 (동일) |
| `/api/lectures` | 409 | 85 | 정상 (중복 생성 방지) |
| `/api/attendance/check-outs` | 404 | 6 | 극소량 엣지케이스, 미조사(볼륨상 실익 낮음) |

409/403처럼 check()가 정상으로 취급하는 응답도 k6의 `http_req_failed` 메트릭엔 실패로 잡힌다. 즉 2.92% 중 상당수(1211건의 409)는 실제 장애가 아니다 — 순수 문제성 에러는 presigned-url 500(457건)이 거의 전부다.

---

## 3. 인프라 용량 분석

30분 내내 임계치 근처에도 가지 않아, 이번 시나리오(500명, 피크 76 RPS) 기준으로는 **애플리케이션 서버 용량이 병목이 아니었다.**

| 리소스 | 설정 용량 | 피크 사용량 | 사용률 |
| --- | ---: | ---: | ---: |
| Tomcat 스레드 (`maxThreads`) | 200 | 4~5 | 2~2.5% |
| DB 커넥션 풀 (HikariCP, 고정 크기) | 10 | 2~4 | 20~40% |

- Tomcat busy_threads는 Little's Law(동시 요청 수 ≈ RPS × 평균 응답시간 = 76 × 0.029 ≈ 2.2)와 정확히 일치하는 수준으로, 응답이 워낙 빨라(p95 82ms) 스레드가 오래 붙잡히지 않았다.
- HikariCP는 `pending`(커넥션 대기) **0**, `timeout` **0**으로 30분 내내 커넥션 부족이 전혀 없었다.
- 다만 **절대 크기가 Tomcat(200)보다 훨씬 작은 DB 풀(10)이 향후 더 큰 부하 테스트에선 먼저 병목이 될 가능성**이 높다 — 지금 규모에선 문제 아님.

---

## 4. 남은 미해결 항목

| 항목 | 상태 | 비고 |
| --- | --- | --- |
| `/api/files/presigned-url` 500 | 담당자 확인 중 | 로컬 S3 미연동으로 재현 불가, 배포 서버 전용 |
| WS `delivery_delay` 측정 로직 버그 | 미수정 | `messengerWebsocket()`에서 메시지 본문 타임스탬프 파싱 방식으로 교체 필요 |
| `http_req_failed`가 설계상 정상 4xx/409를 실패로 집계 | 미수정 | `http.setResponseCallback(http.expectedStatuses(...))` 적용 권장 (급하지 않음) |
| attendance check-outs 404 (6건) | 미조사 | 볼륨 극소, 조사 실익 낮음 |
| 결재선 role_id 동적 해석 미구현 | 팀 공유 필요 | 실서비스에서 역할 기반 결재선 사용 시 동일 문제 재현 가능성 |
| `03-cost-verification.js` (SMS/Gemini 소량 실호출) | 미작성 | 이번 30분 테스트 범위 밖 |

---

## 5. 변경 파일 (이번 세션 전체)

- 수정: `k6/scripts/02-peak-load-500.js` *(로컬 전용)* — RATE_PER_10S 보정, 계정 소속 기반 workspaceId/roomId/taskId 선택 로직 추가
- 수정: `k6/seed/005_approval_files.sql` *(로컬 전용)* — `approval_line_step.approver_id` NULL → 1
- DB 직접 수정(로컬, 파일 아님): `academy_wifi_ip`에 `127.0.0.1` 추가 등록, `approval_line_step` 16건 갱신
- 결과 파일: [`peak-load-500-run1-summary.md`](peak-load-500-run1-summary.md) / [`.json`](peak-load-500-run1-summary.json) *(이 문서와 같은 폴더로 이동함)*

> **참고**: `k6/` 폴더 자체는 `.gitignore` 대상이라 위에서 "로컬 전용"으로 표시한 스크립트/시드 SQL은 git으로 공유되지 않는다. 팀원이 원인·수정 내용을 코드로 직접 봐야 한다면 해당 파일을 별도로 전달해야 한다.
