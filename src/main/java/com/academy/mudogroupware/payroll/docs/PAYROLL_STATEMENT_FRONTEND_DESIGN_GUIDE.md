# 급여명세서 프론트엔드 디자인 가이드

## 1. 문서 목적

이 문서는 급여명세서 관리 화면을 디자인하고 프론트엔드를 구현할 때 필요한 페이지 구성,
화면별 API, 응답 필드, 상태값, 버튼 노출 조건을 정리한다.

현재 `PayrollController`, 응답 DTO, Payroll 상태 enum과
`PAYROLL_API_SPEC.md`를 기준으로 작성했다. 이 문서의 화면 구성은 디자인 제안이며, API 경로와
응답값은 현재 백엔드 계약이다.

## 2. 현재 API로 구성 가능한 화면

| 화면 | 주요 목적 | 사용하는 API |
| --- | --- | --- |
| 월별 급여명세서 관리 | 귀속월별 직원 급여 준비 현황과 금액 조회 | `GET /api/payrolls` |
| 급여명세서 미리보기 | 직원·급여·지급·공제·계산 기준 확인 | `GET /api/payrolls/{payrollId}/preview` |
| 명세서 작업 | PDF 다운로드, 생성 재시도, 개별 이메일 발송 | 다운로드·재시도·개별 발송 API |
| 이메일 일괄 발송 확인 | 일괄 발송 시작 후 상태 집계와 직원별 결과 조회 | 일괄 발송 생성·결과 조회 API |

### 화면 흐름 제안

```text
월별 급여명세서 관리
├─ 직원 행 선택 ───────────────→ 급여명세서 미리보기
│                                  ├─ PDF 다운로드
│                                  ├─ 명세서 생성 재시도
│                                  └─ 개별 이메일 발송
└─ 이 달 명세서 일괄 발송 ────→ 일괄 발송 결과
                                   └─ 직원별 요청·접수·전달·실패 시각 확인
```

프론트엔드 URL은 백엔드에서 정하지 않는다. 위 화면 이름과 흐름은 정보 구조를 위한 제안이다.

## 3. 공통 계약

### 인증과 권한

- 모든 급여·급여명세서 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다.
- 요청자는 `PAYROLL:MANAGE` 권한을 가져야 한다.
- 직원 본인의 명세서여도 해당 권한이 없으면 조회하거나 다운로드할 수 없다.

### 공통 성공 응답

```json
{
  "status": 200,
  "code": "PAYROLL_200_1",
  "message": "급여 목록을 조회했습니다.",
  "data": {}
}
```

| 필드 | 타입 | 용도 |
| --- | --- | --- |
| `status` | `Integer` | HTTP 상태 코드 |
| `code` | `String` | 클라이언트 분기용 성공 코드 |
| `message` | `String` | 사용자에게 표시 가능한 성공 메시지 |
| `data` | `Object` | API별 실제 응답 데이터 |

### 공통 오류 처리

| HTTP 상태 | 의미 | 기본 UI 처리 제안 |
| --- | --- | --- |
| `400` | Query 또는 요청값 오류 | 잘못 입력한 필드 안내 |
| `401` | 인증 토큰 없음·만료·검증 실패 | 로그인 화면 이동 |
| `403` | `PAYROLL:MANAGE` 권한 없음 | 접근 불가 화면 또는 안내 |
| `404` | 급여 또는 발송 배치 없음 | 목록으로 이동하거나 새로고침 안내 |
| `409` | 현재 급여·명세서 상태와 작업 충돌 | 응답 메시지 표시 후 상세 재조회 |
| `422` | 이메일 또는 급여 계산 기준 데이터 부족 | 누락 정보 안내 |

오류 UI는 `message`만으로 분기하지 말고 `code`를 사용한다.

## 4. 페이지 A — 월별 급여명세서 관리

### 권장 화면 구성

```text
[귀속 연월] [고용 형태] [준비 상태] [직원명 검색]          [조회]

[대상 직원] [미작성] [계산 완료] [확정] [지급 합계] [공제 합계] [실수령 합계]

직원명 | 고용 형태 | 준비 상태 | 지급 합계 | 공제 합계 | 실수령액 | 차수 | 작업
------+----------+----------+----------+----------+----------+------+------
이민준 | 정규직    | 확정      | ...      | ...      | ...      | 1    | 미리보기

[이 달 명세서 일괄 발송]                                  [페이지네이션]
```

### 최초·필터·페이지 조회 API

`GET /api/payrolls`

#### Query Parameter

| 이름 | 타입 | 필수 | 허용값·규칙 |
| --- | --- | --- | --- |
| `year` | `Integer` | 필수 | 귀속 연도 |
| `month` | `Integer` | 필수 | `1`~`12` |
| `employmentType` | `String` | 선택 | `REGULAR`, `FIXED_TERM`, `PART_TIME` |
| `status` | `String` | 선택 | `NOT_CREATED`, `DRAFT`, `CALCULATED`, `CONFIRMED` |
| `employeeName` | `String` | 선택 | 직원명 검색어 |
| `page` | `Integer` | 선택 | 0부터 시작, 기본값 `0` |
| `size` | `Integer` | 선택 | `1`~`100`, 기본값 `20` |

#### 응답 예시

```json
{
  "status": 200,
  "code": "PAYROLL_200_1",
  "message": "급여 목록을 조회했습니다.",
  "data": {
    "content": [
      {
        "employeeId": 10,
        "employeeName": "이민준",
        "employmentType": "REGULAR",
        "payrollId": 100,
        "preparationStatus": "CONFIRMED",
        "totalEarnings": 3380000,
        "totalDeductions": 408580,
        "netPay": 2971420,
        "revisionNo": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false,
    "summary": {
      "targetEmployeeCount": 1,
      "notCreatedCount": 0,
      "draftCount": 0,
      "calculatedCount": 0,
      "confirmedCount": 1,
      "totalEarnings": 3380000,
      "totalDeductions": 408580,
      "totalNetPay": 2971420
    }
  }
}
```

#### 목록 응답 필드

| 필드 | 타입 | null 가능 | 화면 용도 |
| --- | --- | --- | --- |
| `content[].employeeId` | `Long` | 아니요 | 직원 식별자 |
| `content[].employeeName` | `String` | 아니요 | 직원명 |
| `content[].employmentType` | `String` | 예 | 고용 형태 배지. 급여 설정이 없으면 `null` 가능 |
| `content[].payrollId` | `Long` | 예 | 상세·명세서 API 호출 키. `NOT_CREATED`이면 `null` |
| `content[].preparationStatus` | `String` | 아니요 | 급여 준비 상태 배지 |
| `content[].totalEarnings` | `Number` | 예 | 지급 합계. `NOT_CREATED`이면 `null` |
| `content[].totalDeductions` | `Number` | 예 | 공제 합계. `NOT_CREATED`이면 `null` |
| `content[].netPay` | `Number` | 예 | 차인지급액. `NOT_CREATED`이면 `null` |
| `content[].revisionNo` | `Integer` | 아니요 | 정정 차수. `NOT_CREATED`이면 `0` |
| `page` | `Integer` | 아니요 | 현재 페이지 |
| `size` | `Integer` | 아니요 | 페이지 크기 |
| `totalElements` | `Long` | 아니요 | 필터 적용 후 전체 직원 수 |
| `totalPages` | `Integer` | 아니요 | 전체 페이지 수 |
| `first`, `last` | `Boolean` | 아니요 | 첫·마지막 페이지 여부 |
| `hasNext`, `hasPrevious` | `Boolean` | 아니요 | 다음·이전 페이지 존재 여부 |
| `summary.targetEmployeeCount` | `Long` | 아니요 | 필터 적용 후 대상 직원 수 |
| `summary.notCreatedCount` | `Long` | 아니요 | 미작성 수 |
| `summary.draftCount` | `Long` | 아니요 | 작성 중 수 |
| `summary.calculatedCount` | `Long` | 아니요 | 계산 완료 수 |
| `summary.confirmedCount` | `Long` | 아니요 | 확정 수 |
| `summary.totalEarnings` | `Number` | 아니요 | 필터 대상 전체 지급 합계 |
| `summary.totalDeductions` | `Number` | 아니요 | 필터 대상 전체 공제 합계 |
| `summary.totalNetPay` | `Number` | 아니요 | 필터 대상 전체 차인지급 합계 |

### 행 상태와 이동 규칙

플랫폼 SUPER_ADMIN(`accountType=ADMIN`, `adminScope=PLATFORM`)은 목록 대상에서 제외된다.

| `preparationStatus` | 표시명 제안 | 가능한 다음 화면·작업 |
| --- | --- | --- |
| `NOT_CREATED` | 미작성 | `payrollId`가 없으므로 명세서 미리보기 불가 |
| `DRAFT` | 작성 중 | 급여 상세는 가능하지만 명세서 미리보기 불가 |
| `CALCULATED` | 계산 완료 | JSON 명세서 미리보기 가능, PDF 다운로드·이메일 발송 불가 |
| `CONFIRMED` | 확정 | 미리보기 가능. 명세서 상태에 따라 다운로드·재시도·발송 가능 |

### 이 목록에서 제공하지 않는 값

다음 정보는 `GET /api/payrolls` 응답에 없다.

- 급여명세서 생성 상태 `PENDING`, `READY`, `FAILED`
- 명세서 생성 일시와 실패 사유
- 이메일 발송 요청·접수·전달·실패 일시
- 개별 이메일 발송 상태
- 직원 이메일 주소

따라서 목록만으로 PDF 다운로드 또는 이메일 발송 버튼의 활성 여부를 완전히 결정할 수 없다.
확정 행을 선택한 후 미리보기나 상세 API에서 `statement`를 확인해야 한다.

## 5. 페이지 B — 급여명세서 미리보기

### 조회 API

`GET /api/payrolls/{payrollId}/preview`

- `CALCULATED`, `CONFIRMED` 급여만 조회할 수 있다.
- `DRAFT`는 `409 INVALID_PAYROLL_STATE`를 반환한다.
- JSON 미리보기이며 PDF를 생성하지 않는다.

### 주요 응답 구조

```json
{
  "status": 200,
  "code": "PAYROLL_200_8",
  "message": "급여명세서 미리보기를 조회했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CONFIRMED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 0,
        "holidayHours": 0,
        "paidLeaveHours": 8
      },
      "compensations": [
        {
          "appliedFrom": "2026-08-01",
          "appliedTo": "2026-08-31",
          "employmentType": "REGULAR",
          "salaryType": "MONTHLY",
          "baseSalary": 3200000,
          "hourlyWage": null,
          "ordinaryHourlyWage": 20000,
          "weeklyContractHours": 40
        }
      ],
      "rule": {
        "laborScopeId": 1,
        "fiveOrMore": true,
        "overtimeMultiplier": 1.5,
        "nightMultiplier": 0.5,
        "holidayUnder8Multiplier": 1.5,
        "holidayOver8Multiplier": 2
      }
    },
    "earnings": [
      {
        "itemId": 1,
        "type": "BASE_SALARY",
        "name": "기본급",
        "sourceType": "CONTRACT",
        "amount": 3200000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "deductions": [],
    "totalEarnings": 3200000,
    "totalDeductions": 0,
    "netPay": 3200000,
    "memo": null,
    "statement": {
      "statementId": 300,
      "status": "READY",
      "fileSize": 182340,
      "generatedAt": "2026-08-12T14:25:00",
      "failureReason": null
    },
    "version": 3
  }
}
```

### 미리보기 응답 필드

| 영역 | 필드 | 설명 |
| --- | --- | --- |
| 기본 | `payrollId` | 급여 식별자와 후속 작업 API의 Path 값 |
| 기본 | `employee.employeeId`, `name`, `employmentType` | 직원 정보 |
| 기본 | `yearMonth` | 급여 귀속월, `yyyy-MM` |
| 기본 | `scheduledPayDate` | 지급 예정일, `yyyy-MM-dd` |
| 기본 | `status` | `CALCULATED` 또는 `CONFIRMED` |
| 기본 | `revisionNo`, `originalPayrollId` | 현재 정정 차수와 원본 급여 식별자 |
| 근태 | `snapshots.attendance.*` | 근무일·근로·연장·야간·휴일·유급휴가 시간 |
| 계약 | `snapshots.compensations[]` | 적용 기간별 고용·급여 형태와 급여 기준 |
| 계산 기준 | `snapshots.rule.*` | 5인 이상 여부와 수당 배율 |
| 지급·공제 | `earnings[]`, `deductions[]` | 항목별 유형, 이름, 금액, 계산 근거 |
| 합계 | `totalEarnings`, `totalDeductions`, `netPay` | 지급·공제·차인지급 합계 |
| 메모 | `memo` | 관리자 메모. 없으면 `null` |
| 명세서 | `statement` | 아직 생성되지 않았으면 `null` |
| 동시성 | `version` | 수정 API에서 사용하는 버전. 디자인 표시값은 아님 |

#### 지급·공제 항목 필드

| 필드 | 설명 |
| --- | --- |
| `itemId` | 급여항목 식별자 |
| `type` | 급여항목 enum |
| `name` | 화면 표시용 항목명 |
| `sourceType` | 항목 생성 근거 |
| `amount` | 현재 적용 금액 |
| `originalAmount` | 조정 전 금액. 조정되지 않았으면 `null` 가능 |
| `adjusted` | 관리자 조정 여부 |
| `adjustmentReason` | 조정 사유. 없으면 `null` |
| `calculationFormula` | 계산식 설명. 없으면 `null` |
| `calculationBasis` | 계산 근거 문자열. JSON 문자열일 수 있으며 없으면 `null` |
| `editable` | 현재 급여 상태에서 수정 가능한 항목인지 여부 |

#### `statement` 필드

| 필드 | 설명 |
| --- | --- |
| `statementId` | 급여명세서 식별자 |
| `status` | `PENDING`, `READY`, `FAILED` |
| `fileSize` | PDF 크기(Byte). 준비 전에는 `null` 가능 |
| `generatedAt` | PDF 생성 완료 일시. 준비 전에는 `null` 가능 |
| `failureReason` | 생성 실패 사유. 실패 상태가 아니면 `null` |

### 명세서 상태별 작업 영역

| 급여 상태 | `statement` | UI 상태 | 가능한 작업 |
| --- | --- | --- | --- |
| `CALCULATED` | 보통 `null` | 확정 전 안내 | 미리보기만 가능 |
| `CONFIRMED` | `null` 또는 `PENDING` | PDF 생성 중 | 상태 안내, 이후 상세 재조회 |
| `CONFIRMED` | `READY` | PDF 준비 완료 | 다운로드, 개별 이메일 발송 |
| `CONFIRMED` | `FAILED` | PDF 생성 실패 | 실패 사유 표시, 생성 재시도 |

## 6. 명세서 작업 API

### PDF 다운로드

`GET /api/payrolls/{payrollId}/statement/download-url`

성공 조건은 급여 `CONFIRMED`와 명세서 `READY`다.

```json
{
  "status": 200,
  "code": "PAYROLL_200_9",
  "message": "급여명세서 다운로드 URL을 발급했습니다.",
  "data": {
    "statementId": 300,
    "payrollId": 100,
    "fileName": "2026년 8월 급여명세서.pdf",
    "downloadUrl": "https://finance-bucket.s3.ap-northeast-2.amazonaws.com/...",
    "expiresInSeconds": 300
  }
}
```

- `downloadUrl`은 현재 300초 동안 유효하다.
- 사용자가 다운로드 버튼을 누른 시점에 URL을 발급받아 사용한다.
- 준비되지 않았으면 `409 PAYROLL_STATEMENT_NOT_READY`다.

### PDF 생성 재시도

`PATCH /api/payrolls/{payrollId}/statement/retry`

`CONFIRMED` 급여의 `FAILED` 명세서만 재시도할 수 있다.

```json
{
  "status": 200,
  "code": "PAYROLL_200_10",
  "message": "급여명세서 생성을 재시도합니다.",
  "data": {
    "statementId": 300,
    "payrollId": 100,
    "status": "PENDING",
    "failureReason": null
  }
}
```

재시도 성공 응답은 생성 완료가 아니라 비동기 재생성 시작을 의미한다.

### 개별 이메일 발송

`POST /api/payrolls/{payrollId}/statement/email-deliveries`

성공 조건은 최신 정정본, 급여 `CONFIRMED`, 명세서 `READY`, 직원 이메일 존재다.

```json
{
  "status": 201,
  "code": "PAYROLL_201_4",
  "message": "급여명세서 이메일 발송을 시작했습니다.",
  "data": {
    "deliveryId": 501,
    "payrollId": 100,
    "status": "PENDING",
    "requestedAt": "2026-08-12T14:30:00",
    "reused": false
  }
}
```

| 필드 | 설명 |
| --- | --- |
| `deliveryId` | 발송 이력 식별자 |
| `payrollId` | 급여 식별자 |
| `status` | 새 요청은 `PENDING`, 멱등 응답은 기존 상태 |
| `requestedAt` | 발송 요청 일시 |
| `reused` | 기존 활성 이력을 반환했으면 `true` |

- 새 이력이 만들어지면 `201 PAYROLL_201_4`다.
- 동일 명세서가 이미 전달됐거나 처리 중이면 새 이력을 만들지 않고
  `200 PAYROLL_200_17`, `reused=true`로 기존 이력을 반환한다.
- 성공 응답은 수신 완료가 아니라 발송 작업 등록 성공을 의미한다.
- 이 응답에는 `sentAt`, `deliveredAt`, `failedAt`이 없다.
- 현재 `deliveryId`로 개별 발송 결과를 다시 조회하는 API도 없다.

## 7. 페이지 C — 이메일 일괄 발송 결과

### 일괄 발송 시작

`POST /api/payrolls/statement/email-delivery-batches`

```json
{
  "year": 2026,
  "month": 8
}
```

```json
{
  "status": 201,
  "code": "PAYROLL_201_5",
  "message": "급여명세서 이메일 일괄 발송을 시작했습니다.",
  "data": {
    "batchId": 701,
    "payrollYearMonth": "2026-08-01",
    "targetCount": 25,
    "status": "PENDING"
  }
}
```

- `targetCount`에는 실제 발송 대기 건과 `SKIPPED` 건이 모두 포함된다.
- 대상이 없으면 `targetCount=0`, `status=COMPLETED`다.
- 응답의 `batchId`를 결과 화면 이동에 보관해야 한다.

### 일괄 발송 결과 조회

`GET /api/payrolls/statement/email-delivery-batches/{batchId}?page=0&size=20`

#### 응답 예시

```json
{
  "status": 200,
  "code": "PAYROLL_200_15",
  "message": "급여명세서 이메일 일괄 발송 결과를 조회했습니다.",
  "data": {
    "batchId": 701,
    "payrollYearMonth": "2026-08-01",
    "status": "AWAITING_DELIVERY",
    "summary": {
      "totalCount": 25,
      "pendingCount": 0,
      "sendingCount": 0,
      "sentCount": 20,
      "retryWaitCount": 0,
      "unknownCount": 0,
      "deliveredCount": 2,
      "failedCount": 1,
      "skippedCount": 2
    },
    "deliveries": {
      "content": [
        {
          "deliveryId": 501,
          "payrollId": 100,
          "employeeId": 10,
          "employeeName": "홍길동",
          "recipientEmail": "ho***@example.com",
          "status": "SENT",
          "failureCode": null,
          "failureReason": null,
          "requestedAt": "2026-08-12T14:30:00",
          "sentAt": "2026-08-12T14:30:02",
          "deliveredAt": null,
          "failedAt": null
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 25,
      "totalPages": 2,
      "first": true,
      "last": false,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

#### 배치와 집계 필드

| 필드 | 설명 |
| --- | --- |
| `batchId` | 일괄 발송 배치 식별자 |
| `payrollYearMonth` | 귀속월의 1일. `yyyy-MM-dd` 형식 |
| `status` | 배치 진행 상태 |
| `summary.totalCount` | 전체 발송 이력 수 |
| `summary.pendingCount` | 대기 수 |
| `summary.sendingCount` | 발송 처리 중 수 |
| `summary.retryWaitCount` | 재시도 대기 수 |
| `summary.unknownCount` | 외부 발송 결과 불명 수 |
| `summary.sentCount` | Mailgun 접수 수 |
| `summary.deliveredCount` | 수신 서버 전달 완료 수 |
| `summary.failedCount` | 최종 실패 수 |
| `summary.skippedCount` | 발송 대상 제외 수 |

#### 직원별 발송 결과 필드

| 필드 | null 가능 | 설명 |
| --- | --- | --- |
| `deliveryId` | 아니요 | 발송 이력 식별자 |
| `payrollId` | 아니요 | 급여 식별자 |
| `employeeId` | 아니요 | 직원 식별자 |
| `employeeName` | 아니요 | 직원명 |
| `recipientEmail` | 예 | 일부 마스킹된 수신 이메일 |
| `status` | 아니요 | 개별 발송 상태 |
| `failureCode` | 예 | 실패·제외 사유 코드 |
| `failureReason` | 예 | 실패·제외 사유 설명 |
| `requestedAt` | 아니요 | 발송 요청 일시 |
| `sentAt` | 예 | Mailgun 접수 일시 |
| `deliveredAt` | 예 | 수신 서버 전달 완료 일시 |
| `failedAt` | 예 | 영구 실패 일시 |

`deliveries`의 페이지 필드는 목록 API와 동일하게 `page`, `size`, `totalElements`,
`totalPages`, `first`, `last`, `hasNext`, `hasPrevious`를 제공한다.

### 결과 화면 구성 제안

- 상단: 귀속월, 전체 진행 상태
- 집계 카드: 전체, 대기, 처리 중, 접수, 전달 완료, 실패, 제외
- 목록: 직원명, 마스킹 이메일, 상태, 발송 요청·접수·전달·실패 일시, 실패 사유
- 진행 상태에서는 새로고침 기능을 제공할 수 있다.
- 자동 갱신 주기는 API 계약에 정의되어 있지 않으므로 프론트엔드 정책으로 확정해야 한다.

### 현재 제공되지 않는 기능

- 발송 배치 전체 이력 목록 API가 없다.
- 결과 조회에는 상태 검색이나 직원명 검색 Query가 없다.
- 개별 발송 결과를 `deliveryId`로 조회하는 API가 없다.

따라서 현재 디자인에서는 일괄 발송 생성 직후 받은 `batchId`로 결과 화면에 이동하는 흐름이
가장 안전하다. 배치 이력 메뉴나 직원별 발송 이력 화면은 현재 API만으로 구현할 수 없다.

## 8. 상태값과 표시명

### 고용 형태

| 값 | 표시명 제안 |
| --- | --- |
| `REGULAR` | 정규직 |
| `FIXED_TERM` | 기간제 |
| `PART_TIME` | 파트타임 |

### 급여 준비 상태

| 값 | 의미 |
| --- | --- |
| `NOT_CREATED` | 해당 직원·귀속월의 급여 데이터가 없음 |
| `DRAFT` | 급여 생성 후 계산 전 |
| `CALCULATED` | 급여 계산 완료 |
| `CONFIRMED` | 관리자 최종 확정 |

`NOT_CREATED`는 DB에 저장되는 `PayrollStatus`가 아니라 목록 조회용 문자열이다.

### 급여명세서 생성 상태

| 값 | 표시명 제안 | 디자인 방향 제안 |
| --- | --- | --- |
| `PENDING` | 생성 중 | 진행 표시 |
| `READY` | 준비 완료 | 다운로드·발송 작업 강조 |
| `FAILED` | 생성 실패 | 실패 사유와 재시도 버튼 표시 |

### 개별 이메일 발송 상태

| 값 | 의미 |
| --- | --- |
| `PENDING` | 발송 대기 |
| `SENDING` | 발송 처리 중 |
| `RETRY_WAIT` | 일시 실패 후 재시도 대기 |
| `UNKNOWN` | 외부 접수 여부 확인 필요 |
| `SENT` | Mailgun 접수 완료, 수신 서버 결과 대기 |
| `DELIVERED` | 수신 이메일 서버 전달 완료 |
| `FAILED` | 최종 발송 실패 |
| `SKIPPED` | 발송 대상 제외 |

`SENT`는 사용자가 이메일을 읽었다는 의미가 아니다. `DELIVERED`도 수신 서버까지 전달됐다는
의미이며 열람 완료를 뜻하지 않는다.

### 일괄 발송 배치 상태

| 값 | 의미 |
| --- | --- |
| `PENDING` | 전체 건이 아직 대기 중 |
| `PROCESSING` | 대기·발송 중·재시도 대기 건이 존재 |
| `AWAITING_DELIVERY` | 발송 처리는 끝났지만 외부 전달 결과 대기 건이 존재 |
| `COMPLETED` | 모든 건이 전달 완료·실패·제외로 종결됐거나 대상 없음 |

### 일괄 발송 제외 사유

| `failureCode` | 의미 |
| --- | --- |
| `PAYROLL_NOT_CONFIRMED` | 급여 미확정 |
| `STATEMENT_NOT_READY` | PDF 명세서 준비 안 됨 |
| `NO_EMAIL` | 직원 이메일 없음 |
| `ALREADY_DELIVERED_OR_IN_PROGRESS` | 이미 전달됐거나 발송 처리 중 |

발송 처리 과정에서는 공급자 오류 코드, `PLAN_MAIL_LIMIT_EXCEEDED`,
`EMAIL_PREPARATION_FAILED`, `MAILGUN_RESULT_UNKNOWN`, `RETRY_EXHAUSTED` 등 다른 실패 코드도
반환될 수 있다. 알려지지 않은 코드는 `failureReason`을 그대로 표시할 수 있는 UI가 필요하다.

### 지급·공제 항목 유형

| 구분 | 값 |
| --- | --- |
| 지급 | `BASE_SALARY`, `HOURLY_PAY`, `MEAL_ALLOWANCE`, `POSITION_ALLOWANCE`, `DUTY_ALLOWANCE`, `TRANSPORTATION_ALLOWANCE`, `OVERTIME_PAY`, `NIGHT_PAY`, `HOLIDAY_PAY`, `WEEKLY_HOLIDAY_PAY`, `BONUS`, `OTHER_ALLOWANCE` |
| 공제 | `NATIONAL_PENSION`, `HEALTH_INSURANCE`, `LONG_TERM_CARE`, `EMPLOYMENT_INSURANCE`, `INCOME_TAX`, `LOCAL_INCOME_TAX` |

항목명은 가능하면 enum을 직접 번역하지 말고 응답의 `name`을 우선 표시한다.

### 항목 생성 근거

| 값 | 의미 |
| --- | --- |
| `CONTRACT` | 급여 계약 |
| `ATTENDANCE` | 근태 계산 |
| `MOCK_INSURANCE` | 입력된 보험 기준 데이터 |
| `MOCK_TAX` | 입력된 세금 기준 데이터 |
| `MANUAL` | 관리자 수기 입력 |

## 9. 날짜·금액·빈 상태 디자인 기준

### 날짜와 시각

- `yearMonth`: `2026-08` 또는 `2026-08-01`로 응답되므로 화면에서는 `2026년 8월`처럼 통일할 수 있다.
- `scheduledPayDate`: `yyyy-MM-dd`다.
- `requestedAt`, `sentAt`, `deliveredAt`, `failedAt`, `generatedAt`은 현재 DTO가
  `LocalDateTime`이고 기존 명세 예시는 UTC offset 없는 `yyyy-MM-ddTHH:mm:ss`다.
- 전역 `API_CONTRACT.md`는 `Asia/Seoul`의 offset 포함 형식을 요구하므로 현재 코드·세부 명세와
  불일치한다. 프론트 구현 전에 실제 직렬화 형식과 시간대 계약을 확정해야 한다.

### 금액

- 금액 필드는 JSON Number다.
- 화면에서는 원 단위 천 단위 구분을 적용할 수 있다.
- `null` 금액을 `0원`으로 오인하지 말고 `-` 또는 미작성 상태로 표시한다.

### 빈 상태

| 상황 | 구분 방법 | 표시 제안 |
| --- | --- | --- |
| 조회 직원 없음 | `content=[]`, `totalElements=0` | 조건에 맞는 직원 없음 |
| 급여 미작성 | `preparationStatus=NOT_CREATED` | 미작성 배지와 금액 `-` |
| 명세서 미생성 | `statement=null` | 확정 전 또는 생성 대기 안내 |
| 일괄 발송 대상 없음 | `targetCount=0`, 배치 `COMPLETED` | 발송 대상 없음 |
| 발송 실패·제외 | `FAILED` 또는 `SKIPPED` | `failureReason` 함께 표시 |

## 10. 구현 전 확인이 필요한 API 공백

아래 UI를 디자인 범위에 포함하려면 백엔드 계약 추가 또는 변경 논의가 필요하다.

1. 월별 목록에서 명세서 생성 상태·최근 발송 상태·발송 일시를 바로 표시하는 기능
2. 과거 일괄 발송 배치 이력 목록
3. 개별 이메일 발송 이후 상태 추적 화면
4. 발송 결과의 서버 검색·상태 필터·정렬
5. 발송 일시의 명확한 timezone/offset 계약

특히 월별 목록에 단일 `발송 일시`를 추가할 경우 재발송 이력이 여러 개일 수 있으므로
`최근 요청 일시`, `최근 Mailgun 접수 일시`, `최근 수신 서버 전달 완료 일시` 중 무엇을
표시할지 먼저 정해야 한다.

## 11. 소스 기준

- `presentation/api/PayrollController.java`
- `application/result/PayrollListResult.java`
- `application/result/PayrollDetailResult.java`
- `application/service/PayrollStatementEmailService.java`
- `domain/model/PayrollTypes.java`
- `domain/exception/PayrollErrorCode.java`
- `presentation/api/common/PayrollResponseCode.java`
- `docs/PAYROLL_API_SPEC.md`
- 프로젝트 루트 `docs/API_CONTRACT.md`
