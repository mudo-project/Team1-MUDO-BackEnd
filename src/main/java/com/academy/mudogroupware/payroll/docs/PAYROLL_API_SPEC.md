# 급여·급여명세서 API 명세

- 기준일: 2026-08-13
- 기준: 현재 Controller, Request/Response DTO, Security, Service, ErrorCode 구현
- Notion 원본: [API 명세서 데이터베이스](https://app.notion.com/p/3b213f22e202808a8a1bee21d6a5a76d)
- 인증·인가: Mailgun Webhook을 제외한 모든 API에 `PAYROLL:MANAGE` 권한이 필요합니다. Webhook은 사용자 인증 대신 Mailgun HMAC 서명을 검증합니다.
- 이 문서는 코드와 동기화한 Notion API 명세 내용을 도메인별로 모은 문서입니다.

## API 목록

1. [월 급여 목록 조회](https://app.notion.com/p/3ba13f22e20281e18d5dc8c130f8d567?pvs=204)
2. [월 급여 초안 생성](https://app.notion.com/p/3ba13f22e2028141b04dd68347ec7194?pvs=204)
3. [급여 계산 및 재계산](https://app.notion.com/p/3ba13f22e20281dea2ddc501a6329f7c?pvs=204)
4. [급여 상세 조회](https://app.notion.com/p/3ba13f22e20281e5a347cedbdc2466a1?pvs=204)
5. [급여 지급항목 및 메모 수정](https://app.notion.com/p/3ba13f22e2028167839cd6943bee85df?pvs=204)
6. [수기 지급항목 추가](https://app.notion.com/p/3ba13f22e202814da91ae549114a40a9?pvs=204)
7. [수기 지급항목 삭제](https://app.notion.com/p/3ba13f22e20281c19f48f2dadf7af1b9?pvs=204)
8. [급여 확정 및 명세서 생성](https://app.notion.com/p/3ba13f22e20281f9a1eedbb17437be36?pvs=204)
9. [급여 정정본 생성](https://app.notion.com/p/3ba13f22e2028180b938d173b4ff8d34?pvs=204)
10. [급여 정정 이력 조회](https://app.notion.com/p/3ba13f22e20281848ea3e41b56dd1477?pvs=204)
11. [급여명세서 미리보기](https://app.notion.com/p/3ba13f22e2028184a0cec2faf745382a?pvs=204)
12. [급여명세서 다운로드 URL 발급](https://app.notion.com/p/3ba13f22e20281d38819d769b0e916bc?pvs=204)
13. [급여명세서 생성 재시도](https://app.notion.com/p/3ba13f22e202813aabb2d55aa9bd1f1a?pvs=204)
14. [급여명세서 개별 이메일 발송](https://app.notion.com/p/3ba13f22e2028183bf20e31122a483e8?pvs=204)
15. [급여명세서 이메일 일괄 발송](https://app.notion.com/p/3ba13f22e20281a9aedad79257f3de6a?pvs=204)
16. [급여명세서 이메일 일괄 발송 결과 조회](https://app.notion.com/p/3ba13f22e202811ebd81c39457d7da97?pvs=204)
17. [급여 정책 조회](https://app.notion.com/p/3ba13f22e20281e285adf75cea58918e?pvs=204)
18. [급여 정책 수정](https://app.notion.com/p/3ba13f22e2028143962ec5767ebf9821?pvs=204)
19. [직원 급여 설정 조회](https://app.notion.com/p/3ba13f22e2028110b817e8a9b12fe920?pvs=204)
20. [직원 급여 설정 저장](https://app.notion.com/p/3ba13f22e20281c7a399d360103cb29f?pvs=204)
21. [Mailgun 급여명세서 이메일 상태 Webhook](https://app.notion.com/p/3ba13f22e20281128f24cf5dd7c5767a?pvs=204)

---

## 1. 월 급여 목록 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281e18d5dc8c130f8d567?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Query: `year`, `month` 필수. `employmentType`, `status`, `employeeName` 선택. `page` 기본 0, `size` 기본 20(최대 100).
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_1`</td>
<td>급여 목록을 조회했습니다.</td>
</tr>
</table>
Response Body
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
      },
      {
        "employeeId": 11,
        "employeeName": "윤예진",
        "employmentType": "PART_TIME",
        "payrollId": null,
        "preparationStatus": "NOT_CREATED",
        "totalEarnings": null,
        "totalDeductions": null,
        "netPay": null,
        "revisionNo": 0
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false,
    "summary": {
      "targetEmployeeCount": 2,
      "notCreatedCount": 1,
      "calculatedCount": 0,
      "confirmedCount": 1,
      "totalEarnings": 3380000,
      "totalDeductions": 408580,
      "totalNetPay": 2971420
    }
  }
}
```
### Response Field
`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `hasNext`, `hasPrevious`, `summary`를 반환합니다. `totalElements`와 `totalPages`는 필터가 적용된 전체 급여 대상 직원을 기준으로 계산합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1` 또는 `INVALID_PAYROLL_REQUEST`</td>
<td>입력값 또는 급여 준비 상태가 올바르지 않습니다.</td>
<td>필수 Query, page/size, enum 또는 status가 올바르지 않습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 급여가 없는 활성 직원도 NOT_CREATED로 포함합니다.
- 필터 적용 후 페이지와 요약을 계산합니다.

---

## 2. 월 급여 초안 생성

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028141b04dd68347ec7194?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `employeeId`
Request Body
```json
{"year":2026,"month":8}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`201 Created`</td>
<td>`PAYROLL_201_1`</td>
<td>급여 초안을 생성했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 201,
  "code": "PAYROLL_201_1",
  "message": "급여 초안을 생성했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": null
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "DRAFT",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": null,
    "earnings": [],
    "deductions": [],
    "totalEarnings": null,
    "totalDeductions": null,
    "netPay": null,
    "memo": null,
    "statement": null,
    "version": 0
  }
}
```
### Response Field
생성된 Payroll 상세와 `version=0`을 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>year 또는 month 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>활성 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_ALREADY_EXISTS`</td>
<td>해당 직원의 급여가 이미 존재합니다.</td>
<td>같은 직원·귀속월의 최초 급여가 이미 있습니다.</td>
</tr>
<tr>
<td>`422 Unprocessable Entity`</td>
<td>`PAYROLL_POLICY_NOT_FOUND`</td>
<td>급여 정책이 없습니다.</td>
<td>급여 지급일 정책이 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 3. 급여 계산 및 재계산

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281dea2ddc501a6329f7c?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
```json
{"expectedVersion":0}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_2`</td>
<td>급여를 계산했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_2",
  "message": "급여를 계산했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 152000,
    "netPay": 3228000,
    "memo": null,
    "statement": null,
    "version": 2
  }
}
```
### Response Field
`CALCULATED` 상태의 Payroll 상세를 반환합니다. 재계산 시 MANUAL 지급항목은 유지합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>expectedVersion 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>payrollId가 존재하지 않습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT` 또는 `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·상태에서는 계산할 수 없습니다.</td>
<td>버전 불일치 또는 확정 급여입니다.</td>
</tr>
<tr>
<td>`422 Unprocessable Entity`</td>
<td>`COMPENSATION_NOT_FOUND` 또는 `PAYROLL_REFERENCE_DATA_MISSING`</td>
<td>급여 계산 기준 데이터가 부족합니다.</td>
<td>계약·통상시급·법정 기준·보험·세금 자료가 부족합니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 4. 급여 상세 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281e5a347cedbdc2466a1?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_3`</td>
<td>급여를 조회했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_3",
  "message": "급여를 조회했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 152000,
    "netPay": 3228000,
    "memo": null,
    "statement": null,
    "version": 2
  }
}
```
### Response Field
직원, 귀속월, 항목, 합계, Snapshot, statement, memo, version을 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 5. 급여 지급항목 및 메모 수정

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028167839cd6943bee85df?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
```json
{"expectedVersion":2,"memo":"확인 완료","adjustments":[{"itemId":1,"amount":3100000,"reason":"근태 정정 반영"}]}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_4`</td>
<td>급여를 수정했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_4",
  "message": "급여를 수정했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 152000,
    "netPay": 3228000,
    "memo": "확인 완료",
    "statement": null,
    "version": 3
  }
}
```
### Response Field
수정된 Payroll 상세를 반환합니다. 보험·세금 공제와 Snapshot은 수정할 수 없습니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`, `INVALID_PAYROLL_REQUEST`, `PAYROLL_ITEM_NOT_EDITABLE`</td>
<td>입력값 또는 수정 대상 항목이 올바르지 않습니다.</td>
<td>DTO 검증 실패, 공제항목 수정, 존재하지 않는 항목 등입니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT` 또는 `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·상태에서는 수정할 수 없습니다.</td>
<td>버전 불일치 또는 DRAFT 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 6. 수기 지급항목 추가

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e202814da91ae549114a40a9?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
```json
{"expectedVersion":2,"name":"특별수당","amount":100000}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`201 Created`</td>
<td>`PAYROLL_201_2`</td>
<td>지급항목을 추가했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 201,
  "code": "PAYROLL_201_2",
  "message": "지급항목을 추가했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      },
      {
        "itemId": 3,
        "type": "OTHER_ALLOWANCE",
        "name": "특별수당",
        "sourceType": "MANUAL",
        "amount": 100000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3480000,
    "totalDeductions": 152000,
    "netPay": 3328000,
    "memo": null,
    "statement": null,
    "version": 3
  }
}
```
### Response Field
`OTHER_ALLOWANCE`·`MANUAL` 항목이 추가된 Payroll 상세를 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1` 또는 `INVALID_PAYROLL_REQUEST`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>이름·금액·expectedVersion 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>payrollId가 존재하지 않습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT` 또는 `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·상태에서는 추가할 수 없습니다.</td>
<td>버전 불일치 또는 CALCULATED가 아닌 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 7. 수기 지급항목 삭제

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281c19f48f2dadf7af1b9?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`, `itemId`. Query: `expectedVersion` 필수.
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_5`</td>
<td>지급항목을 삭제했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_5",
  "message": "지급항목을 삭제했습니다.",
  "data": {
    "payrollId": 100,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 152000,
    "netPay": 3228000,
    "memo": null,
    "statement": null,
    "version": 4
  }
}
```
### Response Field
수기 항목이 삭제되고 합계가 재계산된 Payroll 상세를 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1` 또는 `PAYROLL_ITEM_NOT_EDITABLE`</td>
<td>입력값 또는 삭제 대상 항목이 올바르지 않습니다.</td>
<td>itemId 형식 오류, 비수기 항목 또는 존재하지 않는 항목입니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>payrollId가 존재하지 않습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT` 또는 `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·상태에서는 삭제할 수 없습니다.</td>
<td>버전 불일치 또는 CALCULATED가 아닌 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 변경 요청은 서버가 합계를 재계산하며 오래된 `expectedVersion`은 409로 거절합니다.

---

## 8. 급여 확정 및 명세서 생성

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281f9a1eedbb17437be36?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`payrollId`</td>
<td>Long 타입의 급여 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
```json
{
  "expectedVersion": 2
}
```
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>설명</td>
</tr>
<tr>
<td>`expectedVersion`</td>
<td>Long</td>
<td>true</td>
<td>상세 조회에서 받은 현재 Payroll 버전입니다. 0 이상이어야 합니다.</td>
</tr>
</table>
## 처리 흐름
1. Payroll과 `expectedVersion`을 검증합니다.
2. `CALCULATED → CONFIRMED` 상태 전이를 수행합니다.
3. `payroll_statement`를 `PENDING`으로 한 번만 생성합니다.
4. 트랜잭션 커밋 후 PDF를 생성해 Finance S3 버킷에 업로드합니다.
5. 성공 시 명세서는 `READY`, 실패 시 `FAILED`가 됩니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_6`</td>
<td>급여를 확정했습니다.</td>
<td>현재 확정 Payroll Aggregate를 반환합니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_6",
  "message": "급여를 확정했습니다.",
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
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      },
      {
        "itemId": 2,
        "type": "OVERTIME_PAY",
        "name": "연장근로수당",
        "sourceType": "ATTENDANCE",
        "amount": 180000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": "일자별 통상시급 × 6시간 × 1.5 합계",
        "calculationBasis": "{\"hours\":6.0000,\"multiplier\":1.5,\"payBasisAppliedByDate\":true}",
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 152000,
    "netPay": 3228000,
    "memo": null,
    "statement": {
      "statementId": 300,
      "status": "PENDING",
      "fileSize": null,
      "generatedAt": null,
      "failureReason": null
    },
    "version": 3
  }
}
```
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
</table>
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`data`</td>
<td>확정된 Payroll 상세 데이터입니다. 미리보기 응답과 동일한 Aggregate 구조입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>확정 후 `CONFIRMED`입니다.</td>
</tr>
<tr>
<td>`data.version`</td>
<td>확정으로 증가한 낙관적 락 버전입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>expectedVersion 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>payrollId가 존재하지 않습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT` 또는 `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·상태에서는 확정할 수 없습니다.</td>
<td>버전 불일치 또는 CALCULATED가 아닌 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 이미 `CONFIRMED`인 Payroll의 중복 요청은 멱등 성공으로 현재 결과를 반환합니다.
- PDF 생성 또는 S3 업로드 실패가 Payroll 확정을 되돌리지 않습니다.
- Payroll 한 건당 payroll_statement는 하나만 생성합니다.

---

## 9. 급여 정정본 생성

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028180b938d173b4ff8d34?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
```json
{"expectedVersion":3}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`201 Created`</td>
<td>`PAYROLL_201_3`</td>
<td>급여 정정본을 생성했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 201,
  "code": "PAYROLL_201_3",
  "message": "급여 정정본을 생성했습니다.",
  "data": {
    "payrollId": 127,
    "employee": {
      "employeeId": 10,
      "name": "이민준",
      "employmentType": "REGULAR"
    },
    "yearMonth": "2026-08",
    "scheduledPayDate": "2026-09-05",
    "status": "CALCULATED",
    "revisionNo": 2,
    "originalPayrollId": 100,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168,
        "overtimeHours": 6,
        "nightHours": 1,
        "holidayHours": 8,
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
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "originalAmount": null,
        "adjusted": false,
        "adjustmentReason": null,
        "calculationFormula": null,
        "calculationBasis": null,
        "editable": false
      }
    ],
    "totalEarnings": 3380000,
    "totalDeductions": 408580,
    "netPay": 2971420,
    "memo": null,
    "statement": null,
    "version": 0
  }
}
```
### Response Field
항목과 Snapshot을 복사한 다음 revisionNo의 Payroll 상세를 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>expectedVersion 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 최신 급여를 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_VERSION_CONFLICT`, `PAYROLL_REVISION_CONFLICT`, `INVALID_PAYROLL_STATE`</td>
<td>현재 버전·Revision·상태에서는 생성할 수 없습니다.</td>
<td>버전 불일치, 최신본 아님 또는 미확정 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 응답은 공통 `status`, `code`, `message`, `data` 구조입니다.
- 클라이언트는 서버 계산 결과와 응답의 `version`을 다음 변경 요청에 사용합니다.

---

## 10. 급여 정정 이력 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281848ea3e41b56dd1477?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `payrollId`
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_7`</td>
<td>급여 정정 이력을 조회했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_7",
  "message": "급여 정정 이력을 조회했습니다.",
  "data": [
    {
      "payrollId": 127,
      "employee": {
        "employeeId": 10,
        "name": "이민준",
        "employmentType": "REGULAR"
      },
      "yearMonth": "2026-08",
      "scheduledPayDate": "2026-09-05",
      "status": "CALCULATED",
      "revisionNo": 2,
      "originalPayrollId": 100,
      "snapshots": {
        "attendance": {
          "workDays": 21,
          "workHours": 168,
          "overtimeHours": 6,
          "nightHours": 1,
          "holidayHours": 8,
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
          "editable": true
        }
      ],
      "deductions": [
        {
          "itemId": 10,
          "type": "NATIONAL_PENSION",
          "name": "국민연금",
          "sourceType": "MOCK_INSURANCE",
          "amount": 152000,
          "originalAmount": null,
          "adjusted": false,
          "adjustmentReason": null,
          "calculationFormula": null,
          "calculationBasis": null,
          "editable": false
        }
      ],
      "totalEarnings": 3380000,
      "totalDeductions": 408580,
      "netPay": 2971420,
      "memo": null,
      "statement": null,
      "version": 0
    },
    {
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
          "nightHours": 1,
          "holidayHours": 8,
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
      "deductions": [
        {
          "itemId": 10,
          "type": "NATIONAL_PENSION",
          "name": "국민연금",
          "sourceType": "MOCK_INSURANCE",
          "amount": 152000,
          "originalAmount": null,
          "adjusted": false,
          "adjustmentReason": null,
          "calculationFormula": null,
          "calculationBasis": null,
          "editable": false
        }
      ],
      "totalEarnings": 3380000,
      "totalDeductions": 408580,
      "netPay": 2971420,
      "memo": null,
      "statement": {
        "statementId": 300,
        "status": "READY",
        "fileSize": 48217,
        "generatedAt": "2026-08-31T10:05:00",
        "failureReason": null
      },
      "version": 3
    }
  ]
}
```
### Response Field
각 Revision의 Payroll 상세 배열을 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>기준 급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 기준 급여와 같은 직원·귀속월의 모든 Revision을 최신순으로 반환합니다.
- 각 항목은 독립된 PayrollDetailResult와 version을 가집니다.

---

## 11. 급여명세서 미리보기

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028184a0cec2faf745382a?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`payrollId`</td>
<td>Long 타입의 급여 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
없음
## 처리 흐름
1. Payroll 존재 여부를 확인합니다.
2. 상태가 `CALCULATED` 또는 `CONFIRMED`인지 확인합니다.
3. Payroll, 급여항목, 근태·계약·규칙 Snapshot, 직원 정보를 조합합니다.
4. PDF나 S3 파일을 생성하지 않고 JSON 미리보기 데이터를 반환합니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_8`</td>
<td>급여명세서 미리보기를 조회했습니다.</td>
<td>계산 또는 확정된 급여명세서 데이터를 반환합니다.</td>
</tr>
</table>
Response Body
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
    "status": "CALCULATED",
    "revisionNo": 1,
    "originalPayrollId": null,
    "snapshots": {
      "attendance": {
        "workDays": 21,
        "workHours": 168.0000,
        "overtimeHours": 6.0000,
        "nightHours": 0.0000,
        "holidayHours": 0.0000,
        "paidLeaveHours": 8.0000
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
        "editable": true
      }
    ],
    "deductions": [
      {
        "itemId": 10,
        "type": "NATIONAL_PENSION",
        "name": "국민연금",
        "sourceType": "MOCK_INSURANCE",
        "amount": 152000,
        "editable": false
      }
    ],
    "totalEarnings": 3200000,
    "totalDeductions": 152000,
    "netPay": 3048000,
    "memo": null,
    "statement": null,
    "version": 2
  }
}
```
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
</table>
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`data.payrollId`</td>
<td>급여 식별자입니다.</td>
</tr>
<tr>
<td>`data.employee`</td>
<td>직원 식별자, 이름, 고용형태입니다.</td>
</tr>
<tr>
<td>`data.yearMonth`</td>
<td>급여 귀속월입니다.</td>
</tr>
<tr>
<td>`data.scheduledPayDate`</td>
<td>급여 지급 예정일입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>`CALCULATED` 또는 `CONFIRMED`입니다.</td>
</tr>
<tr>
<td>`data.revisionNo`</td>
<td>급여 Revision 번호입니다.</td>
</tr>
<tr>
<td>`data.snapshots`</td>
<td>계산 당시 근태·계약·법정 계산 기준 Snapshot입니다.</td>
</tr>
<tr>
<td>`data.earnings`</td>
<td>지급항목 목록입니다.</td>
</tr>
<tr>
<td>`data.deductions`</td>
<td>공제항목 목록입니다.</td>
</tr>
<tr>
<td>`data.totalEarnings`</td>
<td>지급 합계입니다.</td>
</tr>
<tr>
<td>`data.totalDeductions`</td>
<td>공제 합계입니다.</td>
</tr>
<tr>
<td>`data.netPay`</td>
<td>차인지급 예정액입니다.</td>
</tr>
<tr>
<td>`data.version`</td>
<td>낙관적 락 버전입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`INVALID_PAYROLL_STATE`</td>
<td>현재 급여 상태에서는 요청을 처리할 수 없습니다.</td>
<td>급여가 DRAFT 상태입니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 미리보기는 JSON 조회이며 PDF를 생성하거나 S3에 저장하지 않습니다.
- 저장된 Payroll Aggregate와 Snapshot을 단일 기준으로 사용합니다.

---

## 12. 급여명세서 다운로드 URL 발급

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281d38819d769b0e916bc?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`payrollId`</td>
<td>Long 타입의 급여 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
없음
## 처리 흐름
1. Payroll이 `CONFIRMED`인지 확인합니다.
2. 연결된 payroll_statement가 `READY`인지 확인합니다.
3. Finance S3 객체에 대한 300초 만료 presigned URL을 생성합니다.
4. 버킷명과 S3 object key를 제외한 다운로드 정보를 반환합니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_9`</td>
<td>급여명세서 다운로드 URL을 발급했습니다.</td>
<td>Finance S3의 단기 presigned URL을 반환합니다.</td>
</tr>
</table>
Response Body
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
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
</table>
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`data.statementId`</td>
<td>급여명세서 식별자입니다.</td>
</tr>
<tr>
<td>`data.payrollId`</td>
<td>명세서가 속한 급여 식별자입니다.</td>
</tr>
<tr>
<td>`data.fileName`</td>
<td>클라이언트에 표시할 PDF 파일명입니다.</td>
</tr>
<tr>
<td>`data.downloadUrl`</td>
<td>Finance S3 파일의 임시 다운로드 URL입니다.</td>
</tr>
<tr>
<td>`data.expiresInSeconds`</td>
<td>URL 유효시간이며 현재 300초입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_STATEMENT_NOT_READY`</td>
<td>급여명세서가 아직 준비되지 않았습니다.</td>
<td>급여가 미확정이거나 READY 명세서·object key가 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 자기 급여명세서라도 `PAYROLL:MANAGE` 권한이 없으면 다운로드할 수 없습니다.
- 발급된 URL은 300초 동안 유효하며, URL 자체는 만료 전까지 S3가 검증합니다.
- 응답에는 Finance 버킷명과 object key를 노출하지 않습니다.

---

## 13. 급여명세서 생성 재시도

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e202813aabb2d55aa9bd1f1a?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`payrollId`</td>
<td>Long 타입의 급여 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
없음
## 처리 흐름
1. Payroll이 `CONFIRMED`인지 확인합니다.
2. payroll_statement가 `FAILED`인지 확인합니다.
3. 상태를 `PENDING`으로 변경하고 비동기 생성을 시작합니다.
4. 성공 시 `READY`, 재실패 시 `FAILED`와 실패 사유를 저장합니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_10`</td>
<td>급여명세서 생성을 재시도합니다.</td>
<td>명세서 상태를 PENDING으로 변경하고 재생성을 시작합니다.</td>
</tr>
</table>
Response Body
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
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
</table>
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`data.statementId`</td>
<td>재시도하는 급여명세서 식별자입니다.</td>
</tr>
<tr>
<td>`data.payrollId`</td>
<td>명세서가 속한 급여 식별자입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>재시도 시작 직후 `PENDING`입니다.</td>
</tr>
<tr>
<td>`data.failureReason`</td>
<td>PENDING 전환 시 기존 실패 사유를 제거하므로 null입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여 또는 연결된 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_STATEMENT_RETRY_NOT_ALLOWED`</td>
<td>실패한 급여명세서만 재시도할 수 있습니다.</td>
<td>급여가 미확정이거나 명세서가 FAILED가 아닙니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 재시도는 기존 payroll_statement를 재사용하며 새 행을 만들지 않습니다.
- 재시도 실패가 확정된 Payroll 상태를 변경하지 않습니다.

---

## 14. 급여명세서 개별 이메일 발송

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028183bf20e31122a483e8?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`payrollId`</td>
<td>Long 타입의 급여 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
없음
## 처리 흐름
1. 급여가 확정된 최신 정정본인지 확인합니다.
2. 급여명세서가 `READY` 상태이고 직원 이메일이 등록되어 있는지 확인합니다.
3. `PENDING` 발송 이력을 생성하고 커밋 후 비동기 발송을 시작합니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`201 Created`</td>
<td>`PAYROLL_201_4`</td>
<td>급여명세서 이메일 발송을 시작했습니다.</td>
<td>생성된 발송 이력을 반환합니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 201,
  "code": "PAYROLL_201_4",
  "message": "급여명세서 이메일 발송을 시작했습니다.",
  "data": {
    "deliveryId": 501,
    "payrollId": 100,
    "status": "PENDING",
    "requestedAt": "2026-08-12T14:30:00"
  }
}
```
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
<tr>
<td>`data.deliveryId`</td>
<td>이메일 발송 이력 식별자입니다.</td>
</tr>
<tr>
<td>`data.payrollId`</td>
<td>급여 식별자입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>생성 시점 발송 상태이며 `PENDING`입니다.</td>
</tr>
<tr>
<td>`data.requestedAt`</td>
<td>발송 요청 시각입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>급여가 존재하지 않습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`INVALID_PAYROLL_STATE`</td>
<td>현재 급여 상태에서는 요청을 처리할 수 없습니다.</td>
<td>급여가 CONFIRMED가 아닙니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_STATEMENT_NOT_READY`</td>
<td>급여명세서가 아직 준비되지 않았습니다.</td>
<td>READY 명세서 또는 object key가 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_EMAIL_409_1`</td>
<td>최신 급여 정정본만 이메일로 발송할 수 있습니다.</td>
<td>최신 정정본이 아닙니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`PAYROLL_EMAIL_409_2`</td>
<td>이미 전달됐거나 발송 처리 중인 급여명세서입니다.</td>
<td>차단 상태의 발송 이력이 있습니다.</td>
</tr>
<tr>
<td>`422 Unprocessable Entity`</td>
<td>`PAYROLL_EMAIL_422_1`</td>
<td>직원 이메일이 등록되어 있지 않습니다.</td>
<td>직원 또는 이메일 정보가 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- API 성공은 메일 수신 완료가 아니라 발송 작업 등록 성공을 뜻합니다.
- 동일 명세서가 이미 전달됐거나 발송 처리 중이면 중복 발송하지 않습니다.
- Mailgun 발송 및 Webhook 결과에 따라 이후 상태가 변경됩니다.

---

## 15. 급여명세서 이메일 일괄 발송

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281a9aedad79257f3de6a?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
없음
Request Query Parameter
없음
Request Body
```json
{
  "year": 2026,
  "month": 8
}
```
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>설명</td>
</tr>
<tr>
<td>`year`</td>
<td>`Integer`</td>
<td>`true`</td>
<td>급여 귀속 연도이며 2000 이상입니다.</td>
</tr>
<tr>
<td>`month`</td>
<td>`Integer`</td>
<td>`true`</td>
<td>급여 귀속 월이며 1\~12입니다.</td>
</tr>
</table>
## 처리 흐름
1. 귀속월의 직원별 최신 급여를 조회합니다.
2. 각 급여를 발송 가능 여부에 따라 `PENDING` 또는 `SKIPPED`로 기록합니다.
3. `PENDING` 건은 커밋 후 비동기 발송을 시작합니다.
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`201 Created`</td>
<td>`PAYROLL_201_5`</td>
<td>급여명세서 이메일 일괄 발송을 시작했습니다.</td>
<td>생성된 배치와 전체 대상 수를 반환합니다.</td>
</tr>
</table>
Response Body
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
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
<tr>
<td>`data.batchId`</td>
<td>일괄 발송 배치 식별자입니다.</td>
</tr>
<tr>
<td>`data.payrollYearMonth`</td>
<td>급여 귀속월이며 해당 월 1일의 `yyyy-MM-dd` 형식입니다.</td>
</tr>
<tr>
<td>`data.targetCount`</td>
<td>`PENDING`과 `SKIPPED`를 모두 포함한 생성 이력 수입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>`PENDING`, `PROCESSING`, `AWAITING_DELIVERY`, `COMPLETED` 중 하나입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>year 또는 month가 허용 범위를 벗어납니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 미확정 급여는 `PAYROLL_NOT_CONFIRMED`, 준비되지 않은 명세서는 `STATEMENT_NOT_READY`, 이메일 없음은 `NO_EMAIL`, 중복·진행 중 발송은 `ALREADY_DELIVERED_OR_IN_PROGRESS`로 `SKIPPED` 처리합니다.
- 제외 건 때문에 전체 요청을 실패시키지 않습니다.
- 대상이 없으면 targetCount는 0이고 배치 상태는 `COMPLETED`입니다.

---

## 16. 급여명세서 이메일 일괄 발송 결과 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e202811ebd81c39457d7da97?pvs=204)

<callout icon="🔒" color="blue_bg">
	요청자는 `PAYROLL:MANAGE` 권한을 보유해야 합니다. 직원 본인 여부만으로는 접근할 수 없습니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다.</td>
</tr>
</table>
Request Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>description</td>
</tr>
<tr>
<td>`batchId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>일괄 발송 배치 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>description</td>
</tr>
<tr>
<td>`page`</td>
<td>`Integer`</td>
<td>`false`</td>
<td>0부터 시작하며 기본값은 0입니다.</td>
</tr>
<tr>
<td>`size`</td>
<td>`Integer`</td>
<td>`false`</td>
<td>1\~100이며 기본값은 20입니다.</td>
</tr>
</table>
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_15`</td>
<td>급여명세서 이메일 일괄 발송 결과를 조회했습니다.</td>
<td>상태별 집계와 페이지 결과를 반환합니다.</td>
</tr>
</table>
Response Body
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
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`, `code`, `message`</td>
<td>공통 성공 응답 필드입니다.</td>
</tr>
<tr>
<td>`data.batchId`</td>
<td>일괄 발송 배치 식별자입니다.</td>
</tr>
<tr>
<td>`data.payrollYearMonth`</td>
<td>급여 귀속월의 1일입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>배치 상태입니다.</td>
</tr>
<tr>
<td>`data.summary.*Count`</td>
<td>전체 및 `PENDING`, `SENDING`, `SENT`, `DELIVERED`, `FAILED`, `SKIPPED` 상태별 건수입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].deliveryId`</td>
<td>발송 이력 식별자입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].payrollId`</td>
<td>급여 식별자입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].employeeId`</td>
<td>직원 식별자입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].employeeName`</td>
<td>직원 이름입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].recipientEmail`</td>
<td>일부 마스킹된 수신 이메일입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].status`</td>
<td>개별 발송 상태입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].failureCode`</td>
<td>실패 또는 제외 사유 코드이며 없으면 null입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].failureReason`</td>
<td>실패 또는 제외 사유이며 없으면 null입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].requestedAt`</td>
<td>발송 요청 시각입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].sentAt`</td>
<td>Mailgun 접수 시각이며 없으면 null입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].deliveredAt`</td>
<td>수신 서버 전달 완료 시각이며 없으면 null입니다.</td>
</tr>
<tr>
<td>`data.deliveries.content[].failedAt`</td>
<td>영구 실패 시각이며 없으면 null입니다.</td>
</tr>
<tr>
<td>`data.deliveries.page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `hasNext`, `hasPrevious`</td>
<td>현재 페이지, 요청 크기, 전체 항목·페이지 수, 첫·마지막 페이지 여부, 다음·이전 페이지 존재 여부입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>page 또는 size가 허용 범위를 벗어납니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_EMAIL_404_1`</td>
<td>이메일 일괄 발송 내역을 찾을 수 없습니다.</td>
<td>batchId에 해당하는 배치가 없습니다.</td>
</tr>
</table>
## 상태 규칙
- `PENDING`: 전체 건이 아직 대기 중입니다.
- `PROCESSING`: 한 건 이상이 대기 또는 발송 처리 중이고 전체가 대기 상태는 아닙니다.
- `AWAITING_DELIVERY`: 처리 중인 건은 없지만 Mailgun 접수 후 Webhook 결과를 기다리는 `SENT` 건이 있습니다.
- `COMPLETED`: `DELIVERED`, `FAILED`, `SKIPPED`처럼 모든 건이 종결됐거나 대상이 없습니다.

---

## 17. 급여 정책 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281e285adf75cea58918e?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
없음
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_11`</td>
<td>급여 정책을 조회했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_11",
  "message": "급여 정책을 조회했습니다.",
  "data": {
    "id": 1,
    "payDayType": "FIXED_DAY",
    "payDay": 5,
    "paymentMonthOffset": 1
  }
}
```
### Response Field
`id`, `payDayType`, `payDay`, `paymentMonthOffset`을 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`422 Unprocessable Entity`</td>
<td>`PAYROLL_POLICY_NOT_FOUND`</td>
<td>급여 정책이 없습니다.</td>
<td>저장된 급여 정책이 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 정책이 없으면 422 PAYROLL_POLICY_NOT_FOUND입니다.
- 이 응답에는 낙관적 락 version 필드가 없습니다.

---

## 18. 급여 정책 수정

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028143962ec5767ebf9821?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
없음
Request Body
```json
{"payDayType":"FIXED_DAY","payDay":5,"paymentMonthOffset":1}
```
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>설명</td>
</tr>
<tr>
<td>`payDayType`</td>
<td>`PayDayType`</td>
<td>`true`</td>
<td>FIXED_DAY 또는 MONTH_END입니다.</td>
</tr>
<tr>
<td>`payDay`</td>
<td>`Integer`</td>
<td>조건부</td>
<td>FIXED_DAY이면 1\~31 필수, MONTH_END이면 null이어야 합니다.</td>
</tr>
<tr>
<td>`paymentMonthOffset`</td>
<td>`Integer`</td>
<td>`true`</td>
<td>귀속월 대비 지급월 오프셋이며 0\~12입니다.</td>
</tr>
</table>
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_12`</td>
<td>급여 정책을 수정했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_12",
  "message": "급여 정책을 수정했습니다.",
  "data": {
    "id": 1,
    "payDayType": "FIXED_DAY",
    "payDay": 5,
    "paymentMonthOffset": 1
  }
}
```
### Response Field
수정된 정책을 반환합니다. 기존 Payroll의 scheduledPayDate는 바꾸지 않습니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1` 또는 `INVALID_PAYROLL_REQUEST`</td>
<td>급여 요청 값이 올바르지 않습니다.</td>
<td>필드 검증 또는 지급일 유형별 조건에 맞지 않습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 수정된 정책은 이후 생성하는 급여의 지급 예정일 계산에 사용됩니다.
- 기존 급여의 scheduledPayDate는 변경하지 않습니다.

---

## 19. 직원 급여 설정 조회

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e2028110b817e8a9b12fe920?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `employeeId`
Request Body
없음
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_13`</td>
<td>직원 급여 설정을 조회했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_13",
  "message": "직원 급여 설정을 조회했습니다.",
  "data": {
    "employeeId": 10,
    "compensations": [
      {
        "id": 1,
        "userId": 10,
        "employmentType": "REGULAR",
        "salaryType": "MONTHLY",
        "baseSalary": 3200000,
        "hourlyWage": null,
        "weeklyContractHours": 40,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": "2026-08-15"
      },
      {
        "id": 2,
        "userId": 10,
        "employmentType": "REGULAR",
        "salaryType": "MONTHLY",
        "baseSalary": 3400000,
        "hourlyWage": null,
        "weeklyContractHours": 40,
        "effectiveFrom": "2026-08-16",
        "effectiveTo": null
      }
    ],
    "fixedAllowances": [
      {
        "id": 11,
        "employeeId": 10,
        "type": "MEAL",
        "name": "식대",
        "amount": 200000,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": null
      }
    ],
    "payBases": [
      {
        "id": 21,
        "employeeId": 10,
        "ordinaryHourlyWage": 20000,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": null
      }
    ]
  }
}
```
### Response Field
`employeeId`, `compensations`, `fixedAllowances`, `payBases`를 반환합니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>직원을 찾을 수 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 계약·고정수당·통상시급 이력을 적용 시작일 역순으로 반환합니다.
- 이 응답에는 낙관적 락 version 필드가 없습니다.

---

## 20. 직원 급여 설정 저장

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281c7a399d360103cb29f?pvs=204)

<callout icon="🔒" color="blue_bg">
	모든 요청은 `PAYROLL:MANAGE` 권한이 필요합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Authorization`</td>
<td>`Bearer {AccessToken}` 형식입니다.</td>
</tr>
</table>
Request Parameter / Query
Path: `employeeId`
Request Body
```json
{"compensation":{"compensationId":null,"employmentType":"REGULAR","salaryType":"MONTHLY","baseSalary":3200000,"hourlyWage":null,"weeklyContractHours":40,"effectiveFrom":"2026-08-01","effectiveTo":null},"fixedAllowances":[{"allowanceId":null,"allowanceType":"MEAL","allowanceName":"식대","amount":200000,"effectiveFrom":"2026-08-01","effectiveTo":null}],"payBasis":{"payBasisId":null,"ordinaryHourlyWage":18500,"effectiveFrom":"2026-08-01","effectiveTo":null}}
```
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_14`</td>
<td>직원 급여 설정을 저장했습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_14",
  "message": "직원 급여 설정을 저장했습니다.",
  "data": {
    "employeeId": 10,
    "compensations": [
      {
        "id": 1,
        "userId": 10,
        "employmentType": "REGULAR",
        "salaryType": "MONTHLY",
        "baseSalary": 3200000,
        "hourlyWage": null,
        "weeklyContractHours": 40,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": "2026-08-15"
      },
      {
        "id": 2,
        "userId": 10,
        "employmentType": "REGULAR",
        "salaryType": "MONTHLY",
        "baseSalary": 3400000,
        "hourlyWage": null,
        "weeklyContractHours": 40,
        "effectiveFrom": "2026-08-16",
        "effectiveTo": null
      }
    ],
    "fixedAllowances": [
      {
        "id": 11,
        "employeeId": 10,
        "type": "MEAL",
        "name": "식대",
        "amount": 200000,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": null
      }
    ],
    "payBases": [
      {
        "id": 21,
        "employeeId": 10,
        "ordinaryHourlyWage": 20000,
        "effectiveFrom": "2026-08-01",
        "effectiveTo": null
      }
    ]
  }
}
```
### Response Field
저장 후 `employeeId`, `compensations`, `fixedAllowances`, `payBases` 전체 이력을 반환합니다.
## 요청 필드 규칙
- `compensationId`, `allowanceId`, `payBasisId`는 기존 이력 수정 시 사용하며 신규 등록 시 생략할 수 있습니다.
- `compensation`, `fixedAllowances`, `payBasis`는 각각 생략할 수 있습니다.
- MONTHLY 계약은 `baseSalary`, HOURLY 계약은 `hourlyWage`가 필요합니다.
- `weeklyContractHours`는 0\~168입니다.
- 고정수당 `allowanceType`은 MEAL, POSITION, DUTY, TRANSPORTATION, OTHER 중 하나입니다.
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`COMMON_401_1` 또는 `AUTH_401_*`</td>
<td>인증이 필요합니다.</td>
<td>Access Token이 없거나 유효하지 않습니다.</td>
</tr>
<tr>
<td>`403 Forbidden`</td>
<td>`COMMON_403_1`</td>
<td>접근 권한이 없습니다.</td>
<td>`PAYROLL:MANAGE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_1` 또는 `INVALID_PAYROLL_REQUEST`</td>
<td>입력값 또는 급여 설정 값이 올바르지 않습니다.</td>
<td>DTO 검증, 적용 기간, 급여형태별 필수 금액 등이 올바르지 않습니다.</td>
</tr>
<tr>
<td>`404 Not Found`</td>
<td>`PAYROLL_NOT_FOUND`</td>
<td>급여를 찾을 수 없습니다.</td>
<td>활성 직원을 찾을 수 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`COMPENSATION_PERIOD_OVERLAP`</td>
<td>급여 설정 적용 기간이 겹칩니다.</td>
<td>계약·동일 고정수당·통상시급의 기간이 겹칩니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 요청에 포함된 설정 영역만 저장하고, 응답에는 세 영역의 전체 이력을 반환합니다.
- 신규 계약·통상시급 등록 시 기존 무기한 이력은 새 적용일 전날로 종료됩니다.

---

## 21. Mailgun 급여명세서 이메일 상태 Webhook

- Notion: [원문 페이지](https://app.notion.com/p/3ba13f22e20281128f24cf5dd7c5767a?pvs=204)

<callout icon="🔐" color="yellow_bg">
	사용자 Access Token은 사용하지 않습니다. Mailgun의 HTTP Webhook Signing Key로 HMAC 서명을 검증합니다.
</callout>
# **\[request\]**
Request Header
<table header-row="true">
<tr>
<td>name</td>
<td>description</td>
</tr>
<tr>
<td>`Content-Type`</td>
<td>`application/json`입니다.</td>
</tr>
</table>
Request Parameter
없음
Request Query Parameter
없음
Request Body
```json
{
  "signature": {
    "timestamp": "1786460400",
    "token": "mailgun-signature-token",
    "signature": "calculated-hmac-signature"
  },
  "event-data": {
    "event": "delivered",
    "severity": null,
    "timestamp": 1786460400,
    "user-variables": {
      "deliveryToken": "server-generated-delivery-token"
    },
    "message": {
      "headers": {
        "message-id": "mailgun-message-id"
      }
    },
    "delivery-status": {
      "message": "OK"
    }
  }
}
```
<table header-row="true">
<tr>
<td>name</td>
<td>type</td>
<td>required</td>
<td>설명</td>
</tr>
<tr>
<td>`signature.timestamp`</td>
<td>`String`</td>
<td>`true`</td>
<td>Mailgun 서명 생성 시각입니다.</td>
</tr>
<tr>
<td>`signature.token`</td>
<td>`String`</td>
<td>`true`</td>
<td>Mailgun 서명 토큰입니다.</td>
</tr>
<tr>
<td>`signature.signature`</td>
<td>`String`</td>
<td>`true`</td>
<td>검증할 HMAC 서명입니다.</td>
</tr>
<tr>
<td>`event-data.event`</td>
<td>`String`</td>
<td>`true`</td>
<td>`delivered` 또는 `failed` 등의 Mailgun 이벤트입니다.</td>
</tr>
<tr>
<td>`event-data.severity`</td>
<td>`String`</td>
<td>`false`</td>
<td>실패 이벤트의 `permanent` 여부입니다.</td>
</tr>
<tr>
<td>`event-data.timestamp`</td>
<td>`Number`</td>
<td>`false`</td>
<td>이벤트 Unix epoch 초입니다.</td>
</tr>
<tr>
<td>`event-data.user-variables.deliveryToken`</td>
<td>`String`</td>
<td>`true`</td>
<td>발송 시 서버가 Mailgun에 전달한 내부 매칭 토큰입니다.</td>
</tr>
<tr>
<td>`event-data.message.headers.message-id`</td>
<td>`String`</td>
<td>`false`</td>
<td>Mailgun 메시지 식별자입니다.</td>
</tr>
<tr>
<td>`event-data.delivery-status.message`</td>
<td>`String`</td>
<td>`false`</td>
<td>실패 사유 또는 전달 상태 메시지입니다.</td>
</tr>
</table>
# **\[response\]**
### 성공코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>description</td>
</tr>
<tr>
<td>`200 OK`</td>
<td>`PAYROLL_200_16`</td>
<td>이메일 발송 상태를 반영했습니다.</td>
<td>유효한 Webhook을 멱등 처리합니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "PAYROLL_200_16",
  "message": "이메일 발송 상태를 반영했습니다.",
  "data": null
}
```
### Response Field
<table header-row="true">
<tr>
<td>name</td>
<td>설명</td>
</tr>
<tr>
<td>`status`</td>
<td>HTTP 상태 코드입니다.</td>
</tr>
<tr>
<td>`code`</td>
<td>서비스 응답 코드입니다.</td>
</tr>
<tr>
<td>`message`</td>
<td>응답 메시지입니다.</td>
</tr>
<tr>
<td>`data`</td>
<td>반환 데이터가 없어 null입니다.</td>
</tr>
</table>
### 실패 코드
<table header-row="true">
<tr>
<td>HTTP 상태</td>
<td>code</td>
<td>message</td>
<td>발생 조건</td>
</tr>
<tr>
<td>`401 Unauthorized`</td>
<td>`PAYROLL_EMAIL_401_1`</td>
<td>Mailgun Webhook 서명이 올바르지 않습니다.</td>
<td>HMAC 서명 검증에 실패했습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- `delivered` 이벤트는 `DELIVERED`와 deliveredAt을 기록합니다.
- `permanent_fail` 또는 severity가 `permanent`인 `failed` 이벤트는 `FAILED`와 실패 사유를 기록합니다.
- 일시 실패 등 그 밖의 이벤트는 상태를 변경하지 않고 200으로 응답합니다.
- 유효한 deliveryToken과 연결되지 않은 이벤트도 상태를 변경하지 않고 200으로 응답합니다.
- 이 Webhook의 성공은 최종 사용자가 메일을 열람했다는 의미가 아니라 수신 메일 서버까지 전달됐음을 의미합니다.

