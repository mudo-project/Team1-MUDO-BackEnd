# 📌 매출 리포트(revenuereport) API

> 기준일: 2026-08-17
> 공통 응답 형식: `status`, `code`, `message`, `data`
> 조회 3종(`ACADEMY:OWNER`, 합성 권한 — `accountType=ADMIN && adminScope=ACADEMY` 계정에 로그인 시 자동 부여) + 수동 생성 1종(`PLATFORM:SUPER_ADMIN`)

## 1. 매출 리포트 목록 조회

`GET /api/revenue-reports`
권한: `ACADEMY:OWNER`

대상 월 최신순으로 반환하며, 항목마다 읽음 여부(`read`)를 포함한다.

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "REVENUE_REPORT_200_1",
  "message": "매출 리포트 목록 조회에 성공했습니다.",
  "data": [
    {
      "reportId": 1,
      "targetMonth": "2026-07-01",
      "read": false
    }
  ]
}
```

리포트가 하나도 없으면 `data: []`(빈 배열)를 반환한다. 로컬 e2e에서 실제 확인:

```json
{
  "status": 200,
  "code": "REVENUE_REPORT_200_1",
  "message": "매출 리포트 목록 조회에 성공했습니다.",
  "data": []
}
```

---

## 2. 매출 리포트 상세 조회

`GET /api/revenue-reports/{reportId}`
권한: `ACADEMY:OWNER`

조회 시 해당 리포트를 읽음 처리한다(이미 읽었으면 최초 읽은 시각을 유지 — `read_at`은 한 번만 채워진다).

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "REVENUE_REPORT_200_2",
  "message": "매출 리포트 상세 조회에 성공했습니다.",
  "data": {
    "reportId": 1,
    "targetMonth": "2026-08-01",
    "report": "8월 매출은 420만 원으로 저번 달보다 늘었어요...",
    "dataSnapshot": "{\"targetMonth\":\"2026-08-01\",\"revenue\":{...},...}"
  }
}
```

`dataSnapshot`은 집계 당시 숫자 스냅샷을 JSON 문자열 원문 그대로 내려준다(프론트 차트 렌더링용). 구조는 Spring 쪽 `RevenueSnapshot`과 동일하며, `previousMonth.available`이 `true`일 때만 전월 대비 필드가 채워진다.

### 검증 및 정책

- 존재하지 않는 `reportId`는 `404 REVENUE_REPORT_404_1`을 반환한다. 로컬 e2e에서 실제 확인:

```json
{
  "timestamp": "2026-08-12T22:20:32",
  "status": 404,
  "code": "REVENUE_REPORT_404_1",
  "message": "리포트를 찾을 수 없습니다.",
  "traceId": "3383f9f5",
  "details": {}
}
```

---

## 3. 안읽은 매출 리포트 수 조회

`GET /api/revenue-reports/unread-count`
권한: `ACADEMY:OWNER`

탭 배지 등 가벼운 표시용. 목록 전체를 조회하지 않고 카운트만 반환한다.

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "REVENUE_REPORT_200_3",
  "message": "안읽은 매출 리포트 수 조회에 성공했습니다.",
  "data": {
    "unreadCount": 0
  }
}
```

---

## 4. 매출 리포트 수동 생성

`POST /api/revenue-reports/generate`
권한: `PLATFORM:SUPER_ADMIN`(위 3개 엔드포인트와 다른 권한 — 이 엔드포인트만 슈퍼어드민 전용)

슈퍼어드민이 지정한 월의 매출 리포트를 즉시 생성한다. 매달 1일 자동 실행되는 배치가 실패했거나, 아직 리포트가 하나도 없을 때(신규 배포 직후 등) 수동으로 즉시 트리거하는 용도다. 내부적으로 배치와 동일한 생성 로직(집계 → 전월 스냅샷 재사용 → AI 서술 → 저장 → 원장 알림)을 그대로 재사용한다.

### Request

```json
{
  "targetMonth": "2026-07"
}
```

`targetMonth`는 `yyyy-MM` 형식만 허용한다. 배치와 달리 항상 직전월로 고정되지 않고 임의의 월을 지정할 수 있다. **당월/미래월도 형식만 맞으면 검증 없이 생성된다** — 슈퍼어드민에게 유연성을 우선 부여한 의도적 설계로, 미완성 데이터로 리포트가 만들어질 수 있음에 유의해야 한다.

### Response · `204 No Content`

바디 없음. 생성된 내용은 기존 상세조회(`GET /api/revenue-reports/{reportId}`)로 확인한다.

### 검증 및 정책

- 잘못된 월 형식 → `400`. 로컬 e2e에서 실제 확인:

```json
{
  "timestamp": "2026-08-17T04:56:21",
  "status": 400,
  "code": "COMMON_400_1",
  "message": "입력값이 올바르지 않습니다.",
  "traceId": "7337eb6b",
  "details": { "errors": [{ "field": "targetMonth", "reason": "yyyy-MM 형식이어야 합니다" }] }
}
```

- 미인증 호출 → `401`(권한 부족이 아니라 인증 자체가 없다는 뜻). 로컬 e2e에서 실제 확인:

```json
{
  "timestamp": "2026-08-17T04:56:21",
  "status": 401,
  "code": "COMMON_401_1",
  "message": "인증이 필요합니다.",
  "traceId": null,
  "details": {}
}
```

- `PLATFORM:SUPER_ADMIN`이 아닌 계정(예: `ACADEMY:OWNER`) → `403`.
- 이미 그 달 리포트가 존재 → `409 REVENUE_REPORT_409_1`(배치의 "조용히 스킵"과 다르게, 사람이 직접 호출하는 API라 명시적으로 알린다).
- AI 서버 호출 자체가 실패 → `502 REVENUE_REPORT_502_1`(fallback 없음, 배치와 동일 정책). 로컬 e2e에서 실제 확인(mudo-ai-server는 정상 응답했으나 Gemini 쪽 크레딧 소진으로 502를 반환한 상황 — 요청/인증/로직은 정상, AI 단계에서만 실패):

```json
{
  "timestamp": "2026-08-17T04:41:21",
  "status": 502,
  "code": "REVENUE_REPORT_502_1",
  "message": "매출 리포트 AI 서버 호출에 실패했습니다.",
  "traceId": "6f5f3380",
  "details": {}
}
```

---

## 인증 실패 공통 동작

토큰 없이 호출하면 `401`(인증 자체가 안 됨), 토큰은 있지만 `ACADEMY:OWNER` 권한이 없으면 `403`을 반환한다. 로컬 e2e에서 실제 확인(토큰 없이 목록 조회):

```json
{
  "timestamp": "2026-08-12T22:20:33",
  "status": 401,
  "code": "COMMON_401_1",
  "message": "인증이 필요합니다.",
  "traceId": null,
  "details": {}
}
```
