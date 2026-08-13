# 📌 매출 리포트(revenuereport) API

> 기준일: 2026-08-12
> 공통 응답 형식: `status`, `code`, `message`, `data`
> 전체 엔드포인트 권한: `ACADEMY:OWNER`(합성 권한, `accountType=ADMIN && adminScope=ACADEMY` 계정에 로그인 시 자동 부여)

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
