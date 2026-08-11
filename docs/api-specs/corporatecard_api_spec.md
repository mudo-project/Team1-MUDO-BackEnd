# corporatecard API 명세

> corporatecard 모듈은 아직 문서(`docs/`)가 없다. 이 파일은 신규 추가된 영수증 대사 검증
> 엔드포인트만 다룬다. `GET /api/corporate-card/transactions`, `GET /{transactionId}`,
> `POST /{transactionId}/submit`, `PUT /{transactionId}/expense`, `POST /batch-submit`은
> 아직 별도로 문서화되지 않았다.

## 1. 영수증-카드거래 대사 검증

`POST /api/corporate-card/transactions/{transactionId}/reconcile-receipt`

정산 상신된 법인카드 영수증 첨부파일에서 Gemini AI로 금액/일자/가맹점을 추출해, 실제 카드 승인
거래(`corporate_card_transactions`)와 일치하는지 확인한다. **대조 결과는 저장하지 않고 요청할
때마다 다시 계산한다.** 불일치가 있어도 결재 승인 자체를 막지 않으며, 참고 정보로만 제공한다.

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `transactionId` | 대사 검증할 법인카드 거래 ID입니다. |

Request Body

없음

# **[response]**

## 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 대사 검증 성공 |

Response Body

```json
{
  "status": 200,
  "code": "CORPORATE_CARD_RECEIPT_RECONCILED",
  "message": "영수증 대사 검증이 완료되었습니다.",
  "data": {
    "transactionId": 1,
    "actualAmount": 45000,
    "extractedAmount": 45000,
    "amountMatch": "MATCH",
    "actualMerchant": "스타벅스 강남점",
    "extractedMerchant": "스타벅스 강남점",
    "merchantMatch": "MATCH",
    "actualDate": "2026-08-05",
    "extractedDate": "2026-08-05",
    "dateMatch": "MATCH",
    "overallStatus": "MATCH"
  }
}
```

### Response Field

| name | 설명 |
| --- | --- |
| `data.transactionId` | 법인카드 거래 ID입니다. |
| `data.actualAmount` | 실제 카드 승인 금액(원)입니다. |
| `data.extractedAmount` | 영수증에서 AI로 추출한 금액입니다. 추출하지 못했으면 `null`입니다. |
| `data.amountMatch` | `MATCH`/`MISMATCH`/`UNKNOWN` 중 하나입니다. 추출값이 없으면 `UNKNOWN`입니다. |
| `data.actualMerchant` | 실제 카드 승인 가맹점명입니다. |
| `data.extractedMerchant` | 영수증에서 AI로 추출한 가맹점명입니다. 실제값과 부분 일치(포함 관계)만 해도 `MATCH`로 판정합니다. |
| `data.merchantMatch` | `MATCH`/`MISMATCH`/`UNKNOWN` 중 하나입니다. |
| `data.actualDate` | 실제 카드 승인 일자입니다. |
| `data.extractedDate` | 영수증에서 AI로 추출한 결제 일자입니다. |
| `data.dateMatch` | `MATCH`/`MISMATCH`/`UNKNOWN` 중 하나입니다. |
| `data.overallStatus` | 세 필드 중 하나라도 `MISMATCH`면 `MISMATCH`, 셋 다 `MATCH`면 `MATCH`, 그 외(추출 불가 포함)는 `UNKNOWN`입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_2` | 카드 사용내역을 찾을 수 없습니다. | 존재하지 않는 `transactionId`인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | `CORPORATE_CARD:EXPENSE` 권한이 없는 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 정산 상신 시 생성된 결재 문서를 더 이상 찾을 수 없는 경우(정상 상황에서는 발생하지 않음) |
| `404 Not Found` | `APPROVAL_404_4` | 첨부파일을 찾을 수 없습니다. | 결재 문서에 첨부파일이 하나도 없는 경우 |
| `409 Conflict` | `COMMON_409_1` | 아직 정산 상신되지 않은 사용내역입니다. | 정산 신청 자체를 안 했거나, 정산 정보만 저장하고 아직 결재 상신은 하지 않은 경우 |
| `409 Conflict` | `APPROVAL_409_7` | 첨부파일 원문 조회 기능이 없어 요약할 수 없습니다. | 지원하지 않는 첨부파일 형식(hwp 등)이거나, PDF/이미지 15MB 초과 또는 docx 텍스트 추출 실패로 원문을 읽을 수 없는 경우 |
| `502 Bad Gateway` | `APPROVAL_502_2` | 첨부파일 필드 추출에 실패했습니다. | Gemini API 호출이 실패했거나 응답을 파싱하지 못한 경우 |

### 알려진 제약

- 결재 문서에 첨부파일이 여러 개면 그중 **첫 번째** 것만 읽는다. 법인카드 정산 결재는 보통 영수증
  1장만 첨부하는 걸 전제로 한다.
- 가맹점명 비교는 완전 일치가 아니라 서로 포함 관계인지로 느슨하게 판단한다(OCR/AI 추출 과정에서
  지점명 표기가 달라질 수 있어서다). 예: 실제 "스타벅스 강남점" vs 추출 "스타벅스"는 `MATCH`.
