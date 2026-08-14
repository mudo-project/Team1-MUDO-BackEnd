# SHAREDFILE_API.md

공유파일 API 계약. 실제 `SharedFileController`, 요청·응답 DTO, `SharedFileErrorCode`, Security 설정을 기준으로 작성한다. 제품 요구사항은 `FUNCTIONAL_SPEC.md`, Controller→UseCase→Service 호출 흐름은 `SHAREDFILE_API_FLOW.md`를 참고한다.

## 공통 사항

- 모든 API는 `Authorization: Bearer {accessToken}` 인증이 필요하다. 인증이 없으면 `401`이다.
- 콘텐츠 API(3~11번)는 `SHAREDFILE:MANAGE` 권한이 필요하다. 권한이 없으면 서비스 호출 전 `403 COMMON_403_1`로 거부되며 시스템 루트 상태는 변하지 않는다.
- 시스템 루트 재생성(2번)만 `SHAREDFILE:ROOT_MANAGE` 권한이 필요하다. `SHAREDFILE:MANAGE`만 있으면 재생성할 수 없고, `SHAREDFILE:ROOT_MANAGE`만 있으면 콘텐츠 API를 호출할 수 없다.
- 목록·검색 API는 offset 기반 `page`가 아니라 Google Drive의 page token을 그대로 넘기는 `cursor`/`hasNext`/`nextCursor`를 사용한다.

---

# **1. GET /api/shared-files/root — 시스템 루트 상태 조회**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Request Parameter

없음

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_1",
    "message": "시스템 루트 상태 조회에 성공했습니다.",
    "data": {
        "ready": true,
        "rootId": "1AbCdEfGhIjKlMnOpQrStUvWxYz"
    }
}
```

`ready`가 `false`면 `rootId`는 `null`로 내려간다(필드 자체는 생략되지 않음).

| name | 설명 |
| --- | --- |
| `data.ready` | 시스템 루트를 지금 사용할 수 있는지 여부. 행이 없거나 `FAILED`면 `false` |
| `data.rootId` | **(2026-08-14 추가)** 시스템 루트의 Drive 폴더 ID. `ready`가 `false`면 `null`. 생성·업로드 API(6~8번)의 `parentId`로 그대로 사용할 수 있다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 401 | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |

---

# **2. POST /api/shared-files/root/recreation — 시스템 루트 재생성**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Request Parameter

없음

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 재생성 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_2",
    "message": "시스템 루트 재생성에 성공했습니다.",
    "data": {
        "ready": true
    }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:ROOT_MANAGE` 없음 (`SHAREDFILE:MANAGE`만으로는 호출 불가) |
| 409 | `COMMON_409_1` | 요청이 현재 상태와 충돌합니다. | 이미 `READY`인 시스템 루트를 재생성하려 한 경우 |
| 409 | `GOOGLE_409_1` | (google 도메인 메시지) | Google 계정이 미연결·만료됐거나 필요한 scope가 부족한 경우 |
| 502 | `SHAREDFILE_502_1` | Google Drive 처리 중 오류가 발생했습니다. | Drive 루트 폴더 생성 실패 |

---

# **3. GET /api/shared-files/items — 현재 폴더 목록**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Request Parameter

| name | description |
| --- | --- |
| `parentId` | 상위 폴더 ID. 생략하면 시스템 루트 ID를 기본값으로 사용 |
| `cursor` | 다음 페이지 조회용 cursor. 첫 페이지는 생략 |
| `size` | 페이지 크기. 기본값 20, 1~100 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_3",
    "message": "폴더 목록 조회에 성공했습니다.",
    "data": {
        "items": [
            {
                "id": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
                "name": "9월 수업계획.docx",
                "mimeType": "application/vnd.google-apps.document",
                "viewUrl": "https://drive.google.com/file/d/xxx/view",
                "downloadable": true,
                "modifiedAt": "2026-08-12T00:00:00"
            }
        ],
        "hasNext": true,
        "nextCursor": "CiQAxxxx"
    }
}
```

| name | 설명 |
| --- | --- |
| `data.items[].id` | Google Drive 파일·폴더 ID |
| `data.items[].name` | 이름 |
| `data.items[].mimeType` | Google Drive MIME type |
| `data.items[].viewUrl` | Google 새 탭 미리보기 URL |
| `data.items[].downloadable` | 다운로드 가능 여부 |
| `data.items[].modifiedAt` | 마지막 수정 시각 |
| `data.hasNext` | 다음 페이지 존재 여부 |
| `data.nextCursor` | 다음 페이지 조회용 cursor. `hasNext`가 `false`면 `null` |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `COMMON_400_2` | 잘못된 요청입니다. | `size`가 1~100 범위를 벗어난 경우 |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `parentId`가 시스템 루트 밖인 경우 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **4. GET /api/shared-files/items/{itemId} — 파일·폴더 상세 조회**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Path Variable

| name | description |
| --- | --- |
| `itemId` | 조회할 Google Drive 파일·폴더 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_4",
    "message": "파일·폴더 상세 조회에 성공했습니다.",
    "data": {
        "id": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
        "name": "9월 수업계획.docx",
        "mimeType": "application/vnd.google-apps.document",
        "viewUrl": "https://drive.google.com/file/d/xxx/view",
        "downloadable": true,
        "modifiedAt": "2026-08-12T00:00:00"
    }
}
```

| name | 설명 |
| --- | --- |
| `data.id`, `data.name`, `data.mimeType`, `data.viewUrl`, `data.downloadable`, `data.modifiedAt` | 3번과 동일 |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `itemId`가 시스템 루트 밖인 경우 |
| 404 | `SHAREDFILE_404_1` | 파일 또는 폴더를 찾을 수 없습니다. | 존재하지 않는 `itemId` |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **5. GET /api/shared-files/items/search — 시스템 루트 전체 검색**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Request Parameter

| name | description |
| --- | --- |
| `keyword` | 검색어. 필수 |
| `type` | 결과 필터. `FILE` 또는 `FOLDER`. 생략하면 필터하지 않음 |
| `cursor` | 다음 페이지 조회용 cursor. 첫 페이지는 생략 |
| `size` | 페이지 크기. 기본값 20, 1~100 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 검색 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_5",
    "message": "검색에 성공했습니다.",
    "data": {
        "items": [],
        "hasNext": false,
        "nextCursor": null
    }
}
```

| name | 설명 |
| --- | --- |
| `data.items`, `data.hasNext`, `data.nextCursor` | 3번과 동일. 시스템 루트 밖 후보는 서버가 걸러내고 반환하지 않음 |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `COMMON_400_2` | 잘못된 요청입니다. | `keyword` 누락, `size`가 1~100 범위를 벗어난 경우 |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **6. POST /api/shared-files/folders — 하위 폴더 생성**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

Request Body

1. `parentId` 지정(특정 폴더 하위에 생성)

```json
{
    "parentId": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
    "name": "수업 운영"
}
```

2. `parentId` 생략(시스템 루트 바로 아래에 생성) — **(2026-08-14 추가)**

```json
{
    "name": "수업 운영"
}
```

| name | 설명 |
| --- | --- |
| `parentId` | **(2026-08-14: 필수 → 선택)** 상위 폴더 ID. 생략(또는 `null`)하면 시스템 루트 바로 아래에 생성한다. 빈 문자열이나 공백은 400으로 거부된다. |
| `name` | 생성할 폴더 이름. 필수, 공백 불가 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 201 Created | 생성 성공 |

Response Body

```json
{
    "status": 201,
    "code": "SHAREDFILE_201_1",
    "message": "폴더 생성에 성공했습니다.",
    "data": {
        "id": "1NewFolderId",
        "name": "수업 운영",
        "mimeType": "application/vnd.google-apps.folder",
        "viewUrl": "https://drive.google.com/drive/folders/xxx",
        "downloadable": false,
        "modifiedAt": "2026-08-12T00:00:00"
    }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `name` 누락·공백(Bean Validation) |
| 400 | `COMMON_400_1` | 입력값이 올바르지 않습니다. | **(2026-08-14 추가)** `parentId`가 빈 문자열이나 공백인 경우 |
| 400 | `SHAREDFILE_400_1` | 이름이 올바르지 않습니다. | 폴더 이름 검증 실패 |
| 401 | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `parentId`가 시스템 루트 밖인 경우 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **7. POST /api/shared-files/items/upload — 로컬 파일 업로드**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `multipart/form-data` |

Request Parameter

| name | description |
| --- | --- |
| `parentId` | **(2026-08-14: 필수 → 선택)** 업로드할 상위 폴더 ID. 생략(또는 `null`)하면 시스템 루트 바로 아래에 업로드한다. 빈 문자열이나 공백은 400으로 거부된다. |

Request Body (multipart part)

| name | 설명 |
| --- | --- |
| `file` | 업로드할 파일 한 개. 최대 100MB |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 201 Created | 업로드 성공 |

Response Body

```json
{
    "status": 201,
    "code": "SHAREDFILE_201_2",
    "message": "파일 업로드에 성공했습니다.",
    "data": {
        "id": "1UploadedFileId",
        "name": "9월 수업계획.pdf",
        "mimeType": "application/pdf",
        "viewUrl": "https://drive.google.com/file/d/xxx/view",
        "downloadable": true,
        "modifiedAt": "2026-08-12T00:00:00"
    }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `SHAREDFILE_400_1` | 이름이 올바르지 않습니다. | 업로드 파일의 원본 파일명이 비어있는 경우 |
| 400 | `SHAREDFILE_400_2` | 파일은 최대 100MB까지 업로드할 수 있습니다. | 100MB 초과 |
| 400 | `COMMON_400_1` | 입력값이 올바르지 않습니다. | **(2026-08-14 추가)** `parentId`가 빈 문자열이나 공백인 경우 |
| 401 | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `parentId`가 시스템 루트 밖인 경우 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **8. POST /api/shared-files/google-files — Google 파일 생성**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

Request Body

1. `parentId` 지정

```json
{
    "parentId": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
    "name": "9월 수업계획",
    "type": "DOCS"
}
```

2. `parentId` 생략(시스템 루트 바로 아래에 생성) — **(2026-08-14 추가)**

```json
{
    "name": "9월 수업계획",
    "type": "DOCS"
}
```

| name | 설명 |
| --- | --- |
| `parentId` | **(2026-08-14: 필수 → 선택)** 상위 폴더 ID. 생략(또는 `null`)하면 시스템 루트 바로 아래에 생성한다. 빈 문자열이나 공백은 400으로 거부된다. |
| `name` | 생성할 파일 이름. 필수, 공백 불가 |
| `type` | 생성할 Google Workspace 파일 유형. `DOCS`, `SHEETS`, `SLIDES` 중 하나. 필수 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 201 Created | 생성 성공 |

Response Body

```json
{
    "status": 201,
    "code": "SHAREDFILE_201_3",
    "message": "Google 파일 생성에 성공했습니다.",
    "data": {
        "id": "1NewGoogleFileId",
        "name": "9월 수업계획",
        "mimeType": "application/vnd.google-apps.document",
        "viewUrl": "https://docs.google.com/document/d/xxx/edit",
        "downloadable": true,
        "modifiedAt": "2026-08-12T00:00:00"
    }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `name`·`type` 누락 또는 `type` 값이 `DOCS`/`SHEETS`/`SLIDES`가 아닌 경우(Bean Validation) |
| 400 | `COMMON_400_1` | 입력값이 올바르지 않습니다. | **(2026-08-14 추가)** `parentId`가 빈 문자열이나 공백인 경우 |
| 400 | `SHAREDFILE_400_1` | 이름이 올바르지 않습니다. | 파일 이름 검증 실패 |
| 401 | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `parentId`가 시스템 루트 밖인 경우 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **9. PATCH /api/shared-files/items/{itemId} — 이름 변경·이동**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

Path Variable

| name | description |
| --- | --- |
| `itemId` | 변경할 Google Drive 파일·폴더 ID |

Request Body

1. 이름만 변경할 때

```json
{
    "name": "10월 수업계획.docx"
}
```

2. 이동만 할 때

```json
{
    "parentId": "1DestinationFolderId"
}
```

3. 이름 변경과 이동을 함께 할 때

```json
{
    "name": "10월 수업계획.docx",
    "parentId": "1DestinationFolderId"
}
```

| name | 설명 |
| --- | --- |
| `name` | 새 이름. 변경하지 않으려면 생략. 일반 업로드 파일은 확장자를 바꿀 수 없음 |
| `parentId` | 이동할 상위 폴더 ID. 이동하지 않으려면 생략 |

> `name`과 `parentId`가 둘 다 없으면 요청을 거부한다.

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 변경 성공 |

Response Body

```json
{
    "status": 200,
    "code": "SHAREDFILE_200_6",
    "message": "이름 변경·이동에 성공했습니다.",
    "data": {
        "id": "1AbCdEfGhIjKlMnOpQrStUvWxYz",
        "name": "10월 수업계획.docx",
        "mimeType": "application/vnd.google-apps.document",
        "viewUrl": "https://drive.google.com/file/d/xxx/view",
        "downloadable": true,
        "modifiedAt": "2026-08-12T00:00:00"
    }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `COMMON_400_2` | 잘못된 요청입니다. | `name`과 `parentId`가 둘 다 없는 경우 |
| 400 | `SHAREDFILE_400_1` | 이름이 올바르지 않습니다. | 일반 업로드 파일의 확장자를 바꾸려 한 경우 등 |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `itemId` 또는 이동할 `parentId`가 시스템 루트 밖이거나, 시스템 루트 자체를 변경하려 한 경우 |
| 404 | `SHAREDFILE_404_1` | 파일 또는 폴더를 찾을 수 없습니다. | 존재하지 않는 `itemId` |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **10. DELETE /api/shared-files/items/{itemId} — 휴지통 삭제**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Path Variable

| name | description |
| --- | --- |
| `itemId` | 삭제할 Google Drive 파일·폴더 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 204 No Content | 삭제 성공 (Google Drive 휴지통 이동. 영구 삭제 아님) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `itemId`가 시스템 루트 밖이거나 시스템 루트 자체인 경우 |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |

---

# **11. GET /api/shared-files/items/{itemId}/download — 원본·변환 다운로드**

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

Path Variable

| name | description |
| --- | --- |
| `itemId` | 다운로드할 Google Drive 파일 ID |

Request Parameter

| name | description |
| --- | --- |
| `format` | 변환 형식. `PDF`, `DOCX`, `XLSX`, `PPTX` 중 하나. 생략하면 원본 그대로 다운로드 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 다운로드 성공. `Content-Disposition: attachment; filename="..."`, 실제 파일 `Content-Type`으로 바이너리를 반환(공통 `GlobalApiResponse` 포맷 아님) |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `SHAREDFILE_400_3` | 지원하지 않는 다운로드 형식입니다. | 일반 업로드 파일에 `format`을 지정했거나, 원본 유형과 맞지 않는 `format`인 경우(예: Sheets에 `DOCX`) |
| 401 | `COMMON_401` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| 403 | `COMMON_403_1` | 접근 권한이 없습니다. | `SHAREDFILE:MANAGE` 없음 |
| 403 | `SHAREDFILE_403_1` | 시스템 루트 하위 항목만 조회·변경할 수 있습니다. | `itemId`가 시스템 루트 밖인 경우 |
| 404 | `SHAREDFILE_404_1` | 파일 또는 폴더를 찾을 수 없습니다. | 존재하지 않는 `itemId` |
| 409 | `SHAREDFILE_409_1` | 공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다. | 시스템 루트가 `FAILED`이거나 없는 경우 |
