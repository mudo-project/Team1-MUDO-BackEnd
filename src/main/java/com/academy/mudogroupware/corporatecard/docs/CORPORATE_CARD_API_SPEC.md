# 법인카드 API 명세

- 기준일: 2026-08-13
- 기준: 현재 Controller, Request/Response DTO, Security, Service, ErrorCode 구현
- Notion 원본: [API 명세서 데이터베이스](https://app.notion.com/p/3b213f22e202808a8a1bee21d6a5a76d)
- 인증·인가: 모든 API에 `CORPORATE_CARD:EXPENSE` 권한이 필요합니다.
- 이 문서는 코드와 동기화한 Notion API 명세 내용을 도메인별로 모은 문서입니다.

## API 목록

1. [법인카드 사용내역 목록 조회](https://app.notion.com/p/3b613f22e20281d89feedf45ab9a8e24?pvs=204)
2. [법인카드 사용내역 상세 조회](https://app.notion.com/p/3b613f22e2028195adb8c58870d299ba?pvs=204)
3. [법인카드 정산 정보 저장](https://app.notion.com/p/3b813f22e202818cb416d2b88b4cf49e?pvs=204)
4. [법인카드 정산 상신](https://app.notion.com/p/3b613f22e202818f8a91e33a6e107821?pvs=204)
5. [법인카드 사용내역 일괄 결재 상신](https://app.notion.com/p/3b813f22e20281d8b836c9d773bca6d6?pvs=204)
6. [영수증-카드거래 대사 검증](https://app.notion.com/p/3bb13f22e202814db6abd78d6dcd59a8?pvs=204)

---

## 1. 법인카드 사용내역 목록 조회

- Notion: [원문 페이지](https://app.notion.com/p/3b613f22e20281d89feedf45ab9a8e24?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
## 처리 흐름
1. 전체 법인카드 거래를 승인 시각과 식별자 내림차순으로 페이지 조회합니다.
2. 각 거래의 정산 정보와 결재 상태를 조합합니다.
3. 학원 ID나 요청자 ID는 조회 조건으로 사용하지 않습니다.
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
<td>`CORPORATE_CARD_TRANSACTIONS_RETRIEVED`</td>
<td>법인카드 사용내역 조회가 완료되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "CORPORATE_CARD_TRANSACTIONS_RETRIEVED",
  "message": "법인카드 사용내역 조회가 완료되었습니다.",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
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
<td>`data.content[]`</td>
<td>법인카드 거래 목록입니다.</td>
</tr>
<tr>
<td>`data.content[].transactionId`</td>
<td>거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.content[].approvedAt`</td>
<td>승인 시각입니다.</td>
</tr>
<tr>
<td>`data.content[].merchantName`</td>
<td>가맹점명입니다.</td>
</tr>
<tr>
<td>`data.content[].cardName`</td>
<td>카드 이름입니다.</td>
</tr>
<tr>
<td>`data.content[].amount`</td>
<td>승인 금액입니다.</td>
</tr>
<tr>
<td>`data.content[].expenseCategory`</td>
<td>사용 분류이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.content[].status`</td>
<td>정산·결재 상태입니다.</td>
</tr>
<tr>
<td>`data.page`\~`data.hasPrevious`</td>
<td>페이지 메타데이터입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>Bean Validation 또는 요청 형식 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 페이지 번호는 0 이상, 크기는 1\~100이어야 합니다.
- 정산 정보가 없으면 expenseCategory는 null이고 status는 UNWRITTEN입니다.

---

## 2. 법인카드 사용내역 상세 조회

- Notion: [원문 페이지](https://app.notion.com/p/3b613f22e2028195adb8c58870d299ba?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
<td>`transactionId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
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
<td>`CORPORATE_CARD_TRANSACTION_RETRIEVED`</td>
<td>법인카드 사용내역 조회가 완료되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "CORPORATE_CARD_TRANSACTION_RETRIEVED",
  "message": "법인카드 사용내역 조회가 완료되었습니다.",
  "data": {
    "transactionId": 100,
    "approvedAt": "2026-08-03T14:22:00",
    "approvalNumber": "30281746",
    "merchantName": "자연분식",
    "cardName": "법인1",
    "cardNumberMasked": "**** 1234",
    "installmentMonths": 0,
    "amount": 84000,
    "expenseId": null,
    "userId": null,
    "expenseCategory": null,
    "purpose": null,
    "approvalDocumentId": null,
    "status": "UNWRITTEN"
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
<td>`data.transactionId`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.approvedAt`</td>
<td>카드 승인 시각입니다.</td>
</tr>
<tr>
<td>`data.approvalNumber`</td>
<td>카드 승인 번호입니다.</td>
</tr>
<tr>
<td>`data.merchantName`</td>
<td>가맹점명입니다.</td>
</tr>
<tr>
<td>`data.cardName`</td>
<td>법인카드 이름입니다.</td>
</tr>
<tr>
<td>`data.cardNumberMasked`</td>
<td>마스킹된 카드 번호입니다.</td>
</tr>
<tr>
<td>`data.installmentMonths`</td>
<td>할부 개월 수입니다.</td>
</tr>
<tr>
<td>`data.amount`</td>
<td>승인 금액입니다.</td>
</tr>
<tr>
<td>`data.expenseId`</td>
<td>정산 정보 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.userId`</td>
<td>정산 작성자 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.expenseCategory`</td>
<td>사용 분류의 한글 표시명이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.purpose`</td>
<td>사용 목적이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.approvalDocumentId`</td>
<td>결재 문서 식별자이며 미상신 시 null입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>`UNWRITTEN`, `IN_PROGRESS`, `APPROVED`, `REJECTED` 중 하나입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_2`</td>
<td>요청별 예외 메시지</td>
<td>잘못된 식별자·분류·결재자 등 서비스 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 거래·정산 조회에는 학원 ID나 요청자 ID 조건이 없습니다.
- 존재하지 않는 transactionId는 현재 코드상 400 COMMON_400_2로 응답합니다.

---

## 3. 법인카드 정산 정보 저장

- Notion: [원문 페이지](https://app.notion.com/p/3b813f22e202818cb416d2b88b4cf49e?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
<td>`transactionId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
```json
{
  "expenseCategory": "식대",
  "purpose": "8월 정기 강사회의 점심 식대"
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
<td>`expenseCategory`</td>
<td>`String`</td>
<td>`true`</td>
<td>식대, 도서·교재, 사무용품, 교통비, 시설·비품, 교육비, 기타 중 하나입니다.</td>
</tr>
<tr>
<td>`purpose`</td>
<td>`String`</td>
<td>`true`</td>
<td>사용 목적입니다.</td>
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
<td>`CORPORATE_CARD_EXPENSE_SAVED`</td>
<td>법인카드 정산 정보가 저장되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "CORPORATE_CARD_EXPENSE_SAVED",
  "message": "법인카드 정산 정보가 저장되었습니다.",
  "data": {
    "transactionId": 100,
    "approvedAt": "2026-08-03T14:22:00",
    "approvalNumber": "30281746",
    "merchantName": "자연분식",
    "cardName": "법인1",
    "cardNumberMasked": "**** 1234",
    "installmentMonths": 0,
    "amount": 84000,
    "expenseId": 15,
    "userId": 31,
    "expenseCategory": "식대",
    "purpose": "8월 정기 강사회의 점심 식대",
    "approvalDocumentId": null,
    "status": "UNWRITTEN"
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
<td>`data.transactionId`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.approvedAt`</td>
<td>카드 승인 시각입니다.</td>
</tr>
<tr>
<td>`data.approvalNumber`</td>
<td>카드 승인 번호입니다.</td>
</tr>
<tr>
<td>`data.merchantName`</td>
<td>가맹점명입니다.</td>
</tr>
<tr>
<td>`data.cardName`</td>
<td>법인카드 이름입니다.</td>
</tr>
<tr>
<td>`data.cardNumberMasked`</td>
<td>마스킹된 카드 번호입니다.</td>
</tr>
<tr>
<td>`data.installmentMonths`</td>
<td>할부 개월 수입니다.</td>
</tr>
<tr>
<td>`data.amount`</td>
<td>승인 금액입니다.</td>
</tr>
<tr>
<td>`data.expenseId`</td>
<td>정산 정보 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.userId`</td>
<td>정산 작성자 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.expenseCategory`</td>
<td>사용 분류의 한글 표시명이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.purpose`</td>
<td>사용 목적이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.approvalDocumentId`</td>
<td>결재 문서 식별자이며 미상신 시 null입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>`UNWRITTEN`, `IN_PROGRESS`, `APPROVED`, `REJECTED` 중 하나입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>Bean Validation 또는 요청 형식 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_2`</td>
<td>요청별 예외 메시지</td>
<td>잘못된 식별자·분류·결재자 등 서비스 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`COMMON_409_1`</td>
<td>요청별 예외 메시지</td>
<td>현재 정산·결재 상태와 요청이 충돌합니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 최초 저장은 작성자 userId를 기록하고, 이후 수정 시 기존 작성자 ID를 유지합니다.
- 진행 중이거나 승인된 정산 정보는 수정할 수 없고 반려 건은 수정할 수 있습니다.

---

## 4. 법인카드 정산 상신

- Notion: [원문 페이지](https://app.notion.com/p/3b613f22e202818f8a91e33a6e107821?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
<td>`transactionId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
```json
{
  "expenseCategory": "식대",
  "purpose": "강사 전체 회의 점심 식대",
  "approverIds": [
    3,
    7
  ]
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
<td>`expenseCategory`</td>
<td>`String`</td>
<td>`true`</td>
<td>지원하는 한글 사용 분류명입니다.</td>
</tr>
<tr>
<td>`purpose`</td>
<td>`String`</td>
<td>`true`</td>
<td>사용 목적입니다.</td>
</tr>
<tr>
<td>`approverIds`</td>
<td>`Long[]`</td>
<td>`false`</td>
<td>결재자 식별자 목록입니다. null 또는 빈 목록이면 기본 결재선을 사용합니다.</td>
</tr>
</table>
## 처리 흐름
1. 거래와 기존 정산 정보를 잠금 조회합니다.
2. 반려 건이 아니면서 결재 문서가 연결돼 있으면 재상신을 거절합니다.
3. 요청 결재선 또는 템플릿 ID 1의 기본 결재선으로 결재 문서를 생성합니다.
4. 정산 정보에 새 결재 문서 ID를 연결합니다.
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
<td>`CORPORATE_CARD_EXPENSE_SUBMITTED`</td>
<td>법인카드 정산이 상신되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 201,
  "code": "CORPORATE_CARD_EXPENSE_SUBMITTED",
  "message": "법인카드 정산이 상신되었습니다.",
  "data": {
    "transactionId": 100,
    "approvedAt": "2026-08-03T14:22:00",
    "approvalNumber": "30281746",
    "merchantName": "자연분식",
    "cardName": "법인1",
    "cardNumberMasked": "**** 1234",
    "installmentMonths": 0,
    "amount": 84000,
    "expenseId": 15,
    "userId": 31,
    "expenseCategory": "식대",
    "purpose": "강사 전체 회의 점심 식대",
    "approvalDocumentId": 582,
    "status": "IN_PROGRESS"
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
<td>`data.transactionId`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.approvedAt`</td>
<td>카드 승인 시각입니다.</td>
</tr>
<tr>
<td>`data.approvalNumber`</td>
<td>카드 승인 번호입니다.</td>
</tr>
<tr>
<td>`data.merchantName`</td>
<td>가맹점명입니다.</td>
</tr>
<tr>
<td>`data.cardName`</td>
<td>법인카드 이름입니다.</td>
</tr>
<tr>
<td>`data.cardNumberMasked`</td>
<td>마스킹된 카드 번호입니다.</td>
</tr>
<tr>
<td>`data.installmentMonths`</td>
<td>할부 개월 수입니다.</td>
</tr>
<tr>
<td>`data.amount`</td>
<td>승인 금액입니다.</td>
</tr>
<tr>
<td>`data.expenseId`</td>
<td>정산 정보 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.userId`</td>
<td>정산 작성자 식별자이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.expenseCategory`</td>
<td>사용 분류의 한글 표시명이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.purpose`</td>
<td>사용 목적이며 미작성 시 null입니다.</td>
</tr>
<tr>
<td>`data.approvalDocumentId`</td>
<td>결재 문서 식별자이며 미상신 시 null입니다.</td>
</tr>
<tr>
<td>`data.status`</td>
<td>`UNWRITTEN`, `IN_PROGRESS`, `APPROVED`, `REJECTED` 중 하나입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>Bean Validation 또는 요청 형식 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_2`</td>
<td>요청별 예외 메시지</td>
<td>잘못된 식별자·분류·결재자 등 서비스 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`COMMON_409_1`</td>
<td>요청별 예외 메시지</td>
<td>현재 정산·결재 상태와 요청이 충돌합니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 기본 결재선도 요청 결재선도 없으면 상신할 수 없습니다.
- 결재자 목록에 null이나 중복 ID가 있으면 400 COMMON_400_2입니다.
- 반려된 정산만 다시 상신할 수 있습니다.

---

## 5. 법인카드 사용내역 일괄 결재 상신

- Notion: [원문 페이지](https://app.notion.com/p/3b813f22e20281d8b836c9d773bca6d6?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
  "items": [
    {
      "transactionId": 101
    },
    {
      "transactionId": 102
    }
  ],
  "approverIds": [
    3,
    7
  ]
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
<td>`items`</td>
<td>`Object[]`</td>
<td>`true`</td>
<td>한 건 이상의 상신 대상이며 중복 거래 ID를 허용하지 않습니다.</td>
</tr>
<tr>
<td>`items[].transactionId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
<tr>
<td>`approverIds`</td>
<td>`Long[]`</td>
<td>`false`</td>
<td>모든 항목에 적용할 결재자 ID 목록입니다. 없으면 기본 결재선을 사용합니다.</td>
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
<td>`CORPORATE_CARD_EXPENSES_SUBMITTED`</td>
<td>법인카드 사용내역 일괄 상신 처리가 완료되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "CORPORATE_CARD_EXPENSES_SUBMITTED",
  "message": "법인카드 사용내역 일괄 상신 처리가 완료되었습니다.",
  "data": {
    "successCount": 1,
    "failureCount": 1,
    "results": [
      {
        "transactionId": 101,
        "success": true,
        "approvalDocumentId": 582,
        "message": null
      },
      {
        "transactionId": 102,
        "success": false,
        "approvalDocumentId": null,
        "message": "먼저 정산 정보를 저장해야 결재를 상신할 수 있습니다."
      }
    ]
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
<td>`data.successCount`</td>
<td>성공 건수입니다.</td>
</tr>
<tr>
<td>`data.failureCount`</td>
<td>실패 건수입니다.</td>
</tr>
<tr>
<td>`data.results[].transactionId`</td>
<td>대상 거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.results[].success`</td>
<td>항목별 성공 여부입니다.</td>
</tr>
<tr>
<td>`data.results[].approvalDocumentId`</td>
<td>생성된 결재 문서 식별자이며 실패 시 null입니다.</td>
</tr>
<tr>
<td>`data.results[].message`</td>
<td>항목별 실패 메시지이며 성공 시 null입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_1`</td>
<td>입력값이 올바르지 않습니다.</td>
<td>Bean Validation 또는 요청 형식 검증에 실패했습니다.</td>
</tr>
<tr>
<td>`400 Bad Request`</td>
<td>`COMMON_400_2`</td>
<td>요청별 예외 메시지</td>
<td>잘못된 식별자·분류·결재자 등 서비스 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`COMMON_409_1`</td>
<td>요청별 예외 메시지</td>
<td>현재 정산·결재 상태와 요청이 충돌합니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 각 항목은 별도 트랜잭션으로 처리되어 부분 성공을 허용합니다.
- 항목별 서비스 실패는 HTTP 오류가 아니라 results\[\]에 기록됩니다.
- 요청 결재선이 없으면 템플릿 ID 1의 기본 결재선을 사용합니다.

---

## 6. 영수증-카드거래 대사 검증

- Notion: [원문 페이지](https://app.notion.com/p/3bb13f22e202814db6abd78d6dcd59a8?pvs=204)

<callout icon="🔒" color="blue_bg">
	`CORPORATE_CARD:EXPENSE` 권한이 필요합니다. Controller와 조회 Adapter에는 학원 ID 범위 조건이 없습니다.
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
<td>`transactionId`</td>
<td>`Long`</td>
<td>`true`</td>
<td>법인카드 거래 식별자입니다.</td>
</tr>
</table>
Request Query Parameter
없음
Request Body
없음
## 처리 흐름
1. 거래와 결재 문서가 연결된 정산 정보를 조회합니다.
2. 결재 첨부파일에서 AI로 금액·가맹점·일자를 추출합니다.
3. 실제 거래와 추출값의 일치 상태를 요청 시점에 계산하며 결과를 저장하지 않습니다.
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
<td>`CORPORATE_CARD_RECEIPT_RECONCILED`</td>
<td>영수증 대사 검증이 완료되었습니다.</td>
</tr>
</table>
Response Body
```json
{
  "status": 200,
  "code": "CORPORATE_CARD_RECEIPT_RECONCILED",
  "message": "영수증 대사 검증이 완료되었습니다.",
  "data": {
    "transactionId": 100,
    "actualAmount": 84000,
    "extractedAmount": 84000,
    "amountMatch": "MATCH",
    "actualMerchant": "자연분식",
    "extractedMerchant": "자연분식",
    "merchantMatch": "MATCH",
    "actualDate": "2026-08-03",
    "extractedDate": "2026-08-03",
    "dateMatch": "MATCH",
    "overallStatus": "MATCH"
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
<td>`data.transactionId`</td>
<td>거래 식별자입니다.</td>
</tr>
<tr>
<td>`data.actualAmount`, `data.extractedAmount`</td>
<td>실제·추출 금액입니다.</td>
</tr>
<tr>
<td>`data.amountMatch`</td>
<td>금액 일치 상태입니다.</td>
</tr>
<tr>
<td>`data.actualMerchant`, `data.extractedMerchant`</td>
<td>실제·추출 가맹점명입니다.</td>
</tr>
<tr>
<td>`data.merchantMatch`</td>
<td>가맹점 일치 상태입니다.</td>
</tr>
<tr>
<td>`data.actualDate`, `data.extractedDate`</td>
<td>실제·추출 승인일입니다.</td>
</tr>
<tr>
<td>`data.dateMatch`</td>
<td>날짜 일치 상태입니다.</td>
</tr>
<tr>
<td>`data.overallStatus`</td>
<td>전체 대사 상태입니다.</td>
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
<td>`400 Bad Request`</td>
<td>`COMMON_400_2`</td>
<td>요청별 예외 메시지</td>
<td>잘못된 식별자·분류·결재자 등 서비스 검증에 실패했습니다.</td>
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
<td>`CORPORATE_CARD:EXPENSE` 권한이 없습니다.</td>
</tr>
<tr>
<td>`409 Conflict`</td>
<td>`COMMON_409_1`</td>
<td>요청별 예외 메시지</td>
<td>현재 정산·결재 상태와 요청이 충돌합니다.</td>
</tr>
</table>
## 비즈니스 규칙
- 정산 상신 전이거나 결재 문서가 없으면 409 COMMON_409_1입니다.
- 불일치는 결재를 차단하지 않는 참고 정보입니다.
- 대사 결과는 저장하지 않습니다.

