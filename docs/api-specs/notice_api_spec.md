# 공지사항(Notice) API 명세서

> 아래 각 `## ` 섹션이 Notion 하위 페이지 1개(엔드포인트 1개)에 대응합니다. 섹션별로 그대로 복사해서 각 페이지에 붙여넣으세요.
> 공통 성공 응답 포맷: `{ "status", "code", "message", "data" }` (`GlobalApiResponse`)
> 공통 실패 응답 포맷: `{ "status", "code", "message", "data": null }`
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요합니다 (미인증 시 `401 COMMON_401_1`).
> 모든 API는 요청자 소속 학원으로 스코프가 제한됩니다.

---

## 1. 공지사항 작성

`POST /api/notices`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "title": "8월 정기 휴관 안내",
  "content": "8/15은 정기 휴관일입니다.",
  "pinned": true,
  "attachments": [
    { "fileUrl": "https://cdn.example.com/f1.pdf", "fileName": "안내문.pdf", "fileType": "application/pdf" }
  ]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | `true` | 공지 제목. 공백 불가. |
| `content` | `String` | `true` | 공지 내용. 공백 불가. |
| `pinned` | `boolean` | `false` | 상단 고정 여부. 기본값 `false`. |
| `attachments` | `List<Object>` | `false` | 첨부파일 목록(다중 첨부 가능). |
| `attachments[].fileUrl` | `String` | `true`(첨부 시) | 첨부파일 URL. |
| `attachments[].fileName` | `String` | `true`(첨부 시) | 첨부파일 이름. |
| `attachments[].fileType` | `String` | `false` | 첨부파일 MIME 타입. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 공지 작성 성공 |

Response Body
```json
{
  "status": 201,
  "code": "NOTICE_201_1",
  "message": "공지사항 작성에 성공했습니다.",
  "data": { "noticeId": 1 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.noticeId` | 생성된 공지 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `NOTICE_400_1` | 공지 제목은 비어 있을 수 없습니다. | `title`이 공백/누락 |
| `400 Bad Request` | `NOTICE_400_2` | 공지 내용은 비어 있을 수 없습니다. | `content`가 공백/누락 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

> ⚠️ 알려진 제한사항: 작성 권한(원장/대표 등)은 아직 별도로 제한하지 않고 인증된 사용자면 누구나 작성 가능합니다(`users.role` 값 체계 확정 후 반영 예정, 컨트롤러 TODO 참고).

---

## 2. 공지사항 목록 조회

`GET /api/notices`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `keyword` | `String` | `false` | 제목 검색 키워드. |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 요청자 소속 학원 공지 목록 조회 성공. 고정 공지가 항상 먼저, 그다음 최신순 정렬. |

Response Body
```json
{
  "status": 200,
  "code": "NOTICE_200_1",
  "message": "공지사항 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "8월 정기 휴관 안내",
        "authorName": "관리자",
        "authorRole": "원장",
        "pinned": true,
        "read": false,
        "hasAttachment": true,
        "createdAt": "2026-08-04T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.content[].id` | 공지 ID입니다. |
| `data.content[].title` | 공지 제목입니다. |
| `data.content[].authorName` | 작성자 이름입니다. |
| `data.content[].authorRole` | 작성자 역할(직책) 이름입니다. |
| `data.content[].pinned` | 상단 고정 여부입니다. |
| `data.content[].read` | 요청자의 읽음 여부입니다. |
| `data.content[].hasAttachment` | 첨부파일 존재 여부입니다. |
| `data.content[].createdAt` | 작성 시각(KST)입니다. |
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 3. 공지사항 상세 조회

`GET /api/notices/{noticeId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 조회할 공지 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 상세 조회 성공. 호출 시 조회수가 1 증가하고 읽음 처리가 자동 기록된다. |

Response Body
```json
{
  "status": 200,
  "code": "NOTICE_200_2",
  "message": "공지사항 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "title": "8월 정기 휴관 안내",
    "content": "8/15은 정기 휴관일입니다.",
    "authorUserId": 3,
    "authorName": "관리자",
    "authorRole": "원장",
    "pinned": true,
    "viewCount": 12,
    "readerCount": 5,
    "totalRecipientCount": 20,
    "createdAt": "2026-08-04T09:00:00",
    "updatedAt": "2026-08-04T09:00:00",
    "attachments": [
      { "id": 1, "fileUrl": "https://cdn.example.com/f1.pdf", "fileName": "안내문.pdf", "fileType": "application/pdf" }
    ]
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.id` | 공지 ID입니다. |
| `data.title` | 공지 제목입니다. |
| `data.content` | 공지 내용입니다. |
| `data.authorUserId` | 작성자 사용자 ID입니다. |
| `data.authorName` | 작성자 이름입니다. |
| `data.authorRole` | 작성자 역할(직책) 이름입니다. |
| `data.pinned` | 상단 고정 여부입니다. |
| `data.viewCount` | 누적 조회수입니다(같은 사용자가 여러 번 봐도 계속 증가). |
| `data.readerCount` | 읽은 인원 수입니다(같은 사용자가 여러 번 봐도 1명으로 유지). |
| `data.totalRecipientCount` | 전체 대상 인원 수(재직중인 학원 소속 사용자 수)입니다. |
| `data.createdAt` | 작성 시각(KST)입니다. |
| `data.updatedAt` | 최종 수정 시각(KST)입니다. |
| `data.attachments[].id` | 첨부파일 ID입니다. |
| `data.attachments[].fileUrl` | 첨부파일 URL입니다. |
| `data.attachments[].fileName` | 첨부파일 이름입니다. |
| `data.attachments[].fileType` | 첨부파일 MIME 타입입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_1` | 해당 공지사항을 조회할 권한이 없습니다. | 다른 학원 소속 공지 조회 시도 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |

---

## 4. 읽은 사람 목록 조회

`GET /api/notices/{noticeId}/readers`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 읽은 사람 목록을 조회할 공지 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 읽은 사람 목록 조회 성공. 최근에 읽은 사람 순으로 정렬. (페이지네이션 미적용 — 대상 인원 규모가 제한적이라 이번 범위에서 제외) |

Response Body
```json
{
  "status": 200,
  "code": "NOTICE_200_3",
  "message": "공지사항 읽은 사람 조회에 성공했습니다.",
  "data": [
    { "userId": 7, "name": "김학생", "role": "학생", "readAt": "2026-08-04T09:10:00" }
  ]
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data[].userId` | 읽은 사용자 ID입니다. |
| `data[].name` | 읽은 사용자 이름입니다. |
| `data[].role` | 읽은 사용자 역할(직책) 이름입니다. |
| `data[].readAt` | 읽은 시각(KST)입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_1` | 해당 공지사항을 조회할 권한이 없습니다. | 다른 학원 소속 공지 조회 시도 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |

---

## 5. 공지사항 수정

`PATCH /api/notices/{noticeId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 수정할 공지 ID |

Request Body
```json
{
  "title": "8월 정기 휴관 안내(수정)",
  "content": "8/15~8/16 정기 휴관일입니다."
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | `true` | 새 공지 제목. |
| `content` | `String` | `true` | 새 공지 내용. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공(본문 없음). 작성자 본인만 가능. |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `NOTICE_400_1` | 공지 제목은 비어 있을 수 없습니다. | `title`이 공백/누락 |
| `400 Bad Request` | `NOTICE_400_2` | 공지 내용은 비어 있을 수 없습니다. | `content`가 공백/누락 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_2` | 작성자 본인만 공지사항을 수정할 수 있습니다. | 작성자 본인이 아님 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |

---

## 6. 공지사항 삭제

`DELETE /api/notices/{noticeId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 삭제할 공지 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 삭제 성공(본문 없음). 현재는 작성자 본인만 가능(권한자 확대는 role 체계 확정 후). |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_3` | 작성자 본인만 공지사항을 삭제할 수 있습니다. | 작성자 본인이 아님 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |

---

## 7. 공지사항 고정

`POST /api/notices/{noticeId}/pin`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 고정할 공지 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 고정 성공(본문 없음). 작성자 본인만 가능. |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_4` | 작성자 본인만 공지사항을 고정할 수 있습니다. | 작성자 본인이 아님 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |

---

## 8. 공지사항 고정 해제

`DELETE /api/notices/{noticeId}/pin`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `noticeId` | 고정 해제할 공지 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 고정 해제 성공(본문 없음). 현재는 임시로 같은 학원 소속 인증 사용자면 누구나 가능(작성자 제한 없음, 권한자 제한은 role 체계 확정 후 추가 예정). |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `NOTICE_403_5` | 다른 학원의 공지사항에는 접근할 수 없습니다. | 요청자와 공지 소속 학원 불일치 |
| `404 Not Found` | `NOTICE_404_1` | 공지사항을 찾을 수 없습니다. | 존재하지 않는 `noticeId` |
