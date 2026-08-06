# 개인메모(Memo) API 명세서

> 아래 각 `## ` 섹션이 Notion 하위 페이지 1개(엔드포인트 1개)에 대응합니다. 섹션별로 그대로 복사해서 각 페이지에 붙여넣으세요.
> 공통 성공 응답 포맷: `{ "status", "code", "message", "data" }` (`GlobalApiResponse`). `204 No Content`는 본문 없음.
> 공통 실패 응답 포맷: `{ "timestamp", "status", "code", "message", "traceId", "details" }`
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요합니다 (미인증 시 `401 COMMON_401_1`).
> 모든 API는 요청자 본인 소유 메모로 스코프가 제한됩니다(다른 사용자 메모 접근 시 `403 MEMO_403_1`).

---

## 1. 개인메모 생성

`POST /api/memos`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "title": "9월 시간표 초안",
  "content": "수학A반 월·수·금 4시",
  "color": "YELLOW"
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | `true` | 메모 제목. 공백 불가, 최대 100자. |
| `content` | `String` | `false` | 메모 내용. |
| `color` | `String` | `true` | 메모 색상. `RED`, `YELLOW`, `GREEN`, `BLUE`, `PURPLE` 중 하나. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 메모 생성 성공 |

Response Body
```json
{
  "status": 201,
  "code": "MEMO_201_1",
  "message": "메모 생성에 성공했습니다.",
  "data": { "id": 1 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.id` | 생성된 메모 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `title` 공백/누락/100자 초과, `color` 누락/유효하지 않은 값 (Bean Validation이 도메인 검증보다 먼저 적용되어 `MEMO_400_*` 대신 공통 코드로 응답, `details.errors[]`에 필드별 사유 포함) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 2. 개인메모 목록조회

`GET /api/memos`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `sort` | `String` | `false` | 정렬 기준. `NEWEST`(최신순, 기본값) 또는 `OLDEST`(오래된순). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MEMO_200_1",
  "message": "메모 목록 조회에 성공했습니다.",
  "data": [
    {
      "id": 1,
      "title": "9월 시간표 초안",
      "content": "수학A반 월·수·금 4시",
      "color": "YELLOW",
      "positionX": null,
      "positionY": null,
      "width": null,
      "height": null,
      "createdAt": "2026-08-05T12:00:00",
      "updatedAt": "2026-08-05T12:00:00"
    }
  ]
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data[].id` | 메모 ID입니다. |
| `data[].title` | 메모 제목입니다. |
| `data[].content` | 메모 내용입니다. |
| `data[].color` | 메모 색상입니다. |
| `data[].positionX` / `positionY` / `width` / `height` | 자유배치 위치·크기입니다. 아직 자유배치하지 않았으면 `null`입니다. |
| `data[].createdAt` / `updatedAt` | 생성/수정 시각입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 3. 개인메모 수정

`PATCH /api/memos/{memoId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `memoId` | 수정할 메모의 ID입니다. |

Request Body
```json
{
  "title": "9월 시간표 최종",
  "content": "수학A반 월·수·금 4시 (교실 변경)"
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | `true` | 메모 제목. 공백 불가, 최대 100자. |
| `content` | `String` | `false` | 메모 내용. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공 (응답 본문 없음) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `title` 공백/누락/100자 초과 (Bean Validation이 도메인 검증보다 먼저 적용되어 `MEMO_400_*` 대신 공통 코드로 응답) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MEMO_403_1` | 본인의 메모가 아닙니다. | 다른 사용자의 메모를 수정하려는 경우 |
| `404 Not Found` | `MEMO_404_1` | 메모를 찾을 수 없습니다. | `memoId`에 해당하는 메모가 없음 |

---

## 4. 개인메모 색상변경

`PATCH /api/memos/{memoId}/color`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `memoId` | 색상을 변경할 메모의 ID입니다. |

Request Body
```json
{
  "color": "BLUE"
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `color` | `String` | `true` | 변경할 색상. `RED`, `YELLOW`, `GREEN`, `BLUE`, `PURPLE` 중 하나. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 색상 변경 성공 (응답 본문 없음) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `color` 누락/유효하지 않은 값 (Bean Validation이 도메인 검증보다 먼저 적용되어 `MEMO_400_*` 대신 공통 코드로 응답) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MEMO_403_1` | 본인의 메모가 아닙니다. | 다른 사용자의 메모를 변경하려는 경우 |
| `404 Not Found` | `MEMO_404_1` | 메모를 찾을 수 없습니다. | `memoId`에 해당하는 메모가 없음 |

---

## 5. 개인메모 위치변경

`PATCH /api/memos/{memoId}/position`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `memoId` | 위치·크기를 변경할 메모의 ID입니다. |

Request Body
```json
{
  "positionX": 120,
  "positionY": 80,
  "width": 240,
  "height": 160
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `positionX` | `Integer` | `true` | 보드 상의 X 좌표. |
| `positionY` | `Integer` | `true` | 보드 상의 Y 좌표. |
| `width` | `Integer` | `true` | 카드 너비. 0보다 커야 함. |
| `height` | `Integer` | `true` | 카드 높이. 0보다 커야 함. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 위치·크기 변경 성공 (응답 본문 없음) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `width`/`height`가 0 이하이거나 필수값 누락 (Bean Validation, `details.errors[]`에 필드별 사유 포함) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MEMO_403_1` | 본인의 메모가 아닙니다. | 다른 사용자의 메모를 변경하려는 경우 |
| `404 Not Found` | `MEMO_404_1` | 메모를 찾을 수 없습니다. | `memoId`에 해당하는 메모가 없음 |

---

## 6. 개인메모 삭제

`DELETE /api/memos/{memoId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `memoId` | 삭제할 메모의 ID입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 삭제 성공 (응답 본문 없음) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MEMO_403_1` | 본인의 메모가 아닙니다. | 다른 사용자의 메모를 삭제하려는 경우 |
| `404 Not Found` | `MEMO_404_1` | 메모를 찾을 수 없습니다. | `memoId`에 해당하는 메모가 없음 |
