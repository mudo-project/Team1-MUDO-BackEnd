# 📌 공지사항 API

> 기준일: 2026-08-03
> 공통 응답 형식: `status`, `code`, `message`, `data` (204 No Content는 본문 없음)

## 1. 공지사항 작성

`POST /api/notices`
권한: `NOTICE:WRITE`

#### Request

```json
{
  "title": "8월 급여 지급일 안내",
  "content": "8월 급여 지급일을 아래와 같이 안내드립니다...",
  "pinned": true,
  "attachments": [
    { "fileId": 10, "fileName": "2026년 8월 급여명세서 양식.xlsx" }
  ]
}
```

`fileId`는 미리 `POST /api/files/presigned-url` → S3 업로드 → `POST /api/files`로 발급받은 값이다. 자세한 흐름은 [file 모듈 API.md](../../file/docs/API.md) 참고.

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "NOTICE_201_1",
  "message": "공지사항 작성에 성공했습니다.",
  "data": { "noticeId": 1 }
}
```

#### 검증 및 정책

- `title`, `content`는 비어 있을 수 없습니다.
- 작성자(`AuthUser`)의 소속 학원이 공지의 학원으로 자동 지정됩니다.
- 첨부파일은 여러 개 등록할 수 있으며, 사진뿐 아니라 PDF 등 일반 파일도 가능합니다. `fileId`로 참조하며(`file` 모듈에서 발급), 서버는 실제 파일 존재 여부를 검증하지 않습니다.
- 카테고리(인사/시설/업무) 기능은 화면 시안에 없어 이번 범위에서 제외했습니다.

---

## 2. 공지사항 목록 조회

`GET /api/notices?keyword=&page=0&size=20`
권한: 인증 사용자 전체

- 요청자의 소속 학원 공지만 반환합니다.
- 고정(`pinned`)된 공지가 항상 먼저, 그 다음 최신순으로 정렬됩니다.
- `keyword`를 지정하면 제목에 포함된 공지만 반환합니다 (미지정 시 전체).
- 각 항목에 요청자의 읽음 여부(`read`)가 포함되므로, 프론트는 이 값으로 굵게/흐리게를 결정하면 됩니다.
- `page`(0부터, 기본값 `0`)·`size`(기본값 `20`) 쿼리 파라미터로 페이지네이션합니다. 전체 개수는 계산하지 않는 Slice 방식이라 `hasNext`로만 다음 페이지 존재 여부를 확인합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "NOTICE_200_1",
  "message": "공지사항 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "8월 급여 지급일 안내",
        "authorName": "김지수",
        "authorRole": "대표",
        "pinned": true,
        "read": false,
        "hasAttachment": true,
        "createdAt": "2026-08-01T09:10:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

## 3. 공지사항 상세 조회

`GET /api/notices/{noticeId}`
권한: 같은 학원 인증 사용자

- 호출 시점에 조회수(`viewCount`)가 1 증가하고, 읽음 처리가 자동으로 기록됩니다 (같은 사용자가 여러 번 봐도 읽은 인원 수는 1명으로 유지).
- 다른 학원의 공지를 조회하면 `403`을 반환합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "NOTICE_200_2",
  "message": "공지사항 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "title": "8월 급여 지급일 안내",
    "content": "8월 급여 지급일을 아래와 같이 안내드립니다...",
    "authorUserId": 7,
    "authorName": "김지수",
    "authorRole": "대표",
    "pinned": true,
    "viewCount": 13,
    "readerCount": 3,
    "totalRecipientCount": 5,
    "createdAt": "2026-08-01T09:10:00",
    "updatedAt": "2026-08-01T09:10:00",
    "attachments": [
      { "id": 1, "fileId": 10, "fileName": "2026년 8월 급여명세서 양식.xlsx" }
    ]
  }
}
```

- `viewCount`(조회수)와 `readerCount`/`totalRecipientCount`(읽음 인원)는 서로 다른 값입니다. 조회수는 볼 때마다 누적되고, 읽음 인원은 학원 소속 인원 중 몇 명이 읽었는지(중복 제거)를 뜻합니다.
- 첨부파일 다운로드가 필요하면 `attachments[].fileId`로 `GET /api/files/{fileId}/download-url`을 호출해 임시 다운로드 URL을 받습니다.

---

## 4. 읽은 사람 목록 조회

`GET /api/notices/{noticeId}/readers`
권한: 같은 학원 인증 사용자

- 상세 화면의 "읽음 3/5" 클릭 시 호출하는 API입니다.
- 최근에 읽은 사람 순으로 정렬됩니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "NOTICE_200_3",
  "message": "공지사항 읽은 사람 조회에 성공했습니다.",
  "data": [
    { "userId": 12, "name": "이민준", "role": "강사", "readAt": "2026-08-01T10:00:00" },
    { "userId": 7, "name": "김지수", "role": "대표", "readAt": "2026-08-01T09:10:05" }
  ]
}
```

---

## 5. 공지사항 수정

`PATCH /api/notices/{noticeId}`
권한: `NOTICE:WRITE` + 작성자 본인만

#### Request

```json
{
  "title": "8월 급여 지급일 안내(수정)",
  "content": "지급일이 변경되었습니다...",
  "attachments": [
    { "fileId": 42, "fileName": "변경안내.pdf" }
  ]
}
```

#### 규칙

- `title`, `content`는 필수다.
- `attachments`는 선택 필드다.
  - 필드를 아예 안 보내거나 `null`이면 기존 첨부파일을 그대로 둔다.
  - 빈 배열(`[]`)을 보내면 첨부파일을 전부 지운다.
  - 값을 보내면 그 목록으로 **전체 교체**한다(부분 추가/삭제가 아니다). 기존에 유지하고 싶은 파일도 다시 포함해서 보내야 한다.

#### Response · `204 No Content`

---

## 6. 공지사항 삭제

`DELETE /api/notices/{noticeId}`
권한: 작성자 본인만

#### Response · `204 No Content`

---

## 7. 공지사항 고정

`POST /api/notices/{noticeId}/pin`
권한: `NOTICE:PIN`

#### Response · `204 No Content`

## 8. 공지사항 고정 해제

`DELETE /api/notices/{noticeId}/pin`
권한: `NOTICE:PIN`

#### Response · `204 No Content`

---

## ⚠️ 오류 코드 (`NoticeErrorCode`)

notice 도메인 규칙 위반은 `NoticeErrorCode`(→ `NoticeException`)로 던집니다. 응답의 `code` 필드에 아래 값이 그대로 노출됩니다.

| code | HTTP | 메시지 |
| --- | --- | --- |
| `NOTICE_400_1` | 400 | 공지 제목은 비어 있을 수 없습니다. |
| `NOTICE_400_2` | 400 | 공지 내용은 비어 있을 수 없습니다. |
| `NOTICE_403_1` | 403 | 해당 공지사항을 조회할 권한이 없습니다. |
| `NOTICE_403_2` | 403 | 작성자 본인만 공지사항을 수정할 수 있습니다. |
| `NOTICE_403_3` | 403 | 작성자 본인만 공지사항을 삭제할 수 있습니다. |
| `NOTICE_403_5` | 403 | 다른 학원의 공지사항에는 접근할 수 없습니다. |
| `NOTICE_404_1` | 404 | 공지사항을 찾을 수 없습니다. |
| `NOTICE_404_2` | 404 | 사용자를 찾을 수 없습니다. |

인증 실패(`401`)는 `CommonErrorCode`(글로벌 공통)를 그대로 사용합니다.
