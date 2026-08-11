# file API

기준일: 2026-08-10

> 이 문서는 Spring Boot Controller, Response DTO, ErrorCode 구현 기준으로 작성한다.
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다(로그인만 하면 되고, 특정 권한 코드는 요구하지 않는다).

## 사용 흐름

1. `POST /api/files/presigned-url`로 업로드용 URL을 받는다.
2. 받은 `uploadUrl`로 클라이언트가 S3에 파일을 직접 `PUT` 업로드한다 (요청 헤더 `Content-Type`을 1번에서 보낸 `contentType`과 동일하게 지정해야 한다).
3. 업로드가 끝나면 `POST /api/files`로 메타데이터를 등록하고 `fileId`를 받는다.
4. 이 `fileId`를 결재 신청(`fileIds`), 공지 첨부(`fileId`), 메시지 첨부(`fileId`) 등 다른 기능에서 참조한다.
5. 파일을 다시 열람/다운로드해야 하면 `GET /api/files/{fileId}/download-url`을 호출한다. **여러 개를 한 화면에 표시해야 하면(메시지 목록 등) fileId 개수만큼 반복 호출하지 말고 `POST /api/files/download-urls`로 한 번에 조회한다.**

---

## **1. 업로드용 presigned URL 발급**

`POST /api/files/presigned-url`

이 시점엔 아직 `file_metadata`가 생성되지 않는다. 업로드 완료 후 반드시 2번(등록) API를 호출해야 `fileId`가 발급된다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "fileName": "휴가원.pdf",
  "contentType": "application/pdf"
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `fileName` | `String` | `true` | 원본 파일명. 서버가 objectKey를 만들 때 접미사로 사용한다. |
| `contentType` | `String` | `true` | MIME 타입. S3 업로드 시 실제 보내는 `Content-Type`과 일치해야 한다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | presigned URL 발급 성공 |

Response Body

```json
{"status":200,"code":"FILE_200_1","message":"업로드용 URL 발급에 성공했습니다.","data":{"objectKey":"tenants/academy-a/files/3f2c1a2e-휴가원.pdf","uploadUrl":"https://academy-files.s3.ap-northeast-2.amazonaws.com/..."}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.objectKey` | 서버가 생성한 S3 객체 키. 2번(등록) API 호출 시 그대로 다시 보내야 한다. |
| `data.uploadUrl` | S3에 `PUT`으로 직접 업로드할 수 있는 15분짜리 임시 URL. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `fileName`/`contentType` 중 하나라도 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **2. 파일 메타데이터 등록**

`POST /api/files`

S3 업로드가 끝난 뒤 호출한다. 실제 S3 객체 존재 여부는 검증하지 않는다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "objectKey": "tenants/academy-a/files/3f2c1a2e-휴가원.pdf",
  "contentType": "application/pdf"
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `objectKey` | `String` | `true` | 1번 API에서 받은 objectKey |
| `contentType` | `String` | `true` | MIME 타입 |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 파일 등록 성공 |

Response Body

```json
{"status":201,"code":"FILE_201_1","message":"파일 등록에 성공했습니다.","data":{"fileId":10}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.fileId` | 이후 다른 기능에서 첨부파일을 참조할 때 쓰는 ID. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `objectKey`/`contentType` 중 하나라도 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `409 Conflict` | `COMMON_409_1` | 요청이 현재 상태와 충돌합니다. | 같은 `objectKey`가 이미 등록된 경우(재시도로 중복 호출된 경우 등) |

---

## **3. 다운로드용 URL 조회**

`GET /api/files/{fileId}/download-url`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `fileId` | 조회할 파일 ID |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 다운로드용 URL 조회 성공 |

Response Body

```json
{"status":200,"code":"FILE_200_2","message":"다운로드용 URL 조회에 성공했습니다.","data":{"downloadUrl":"https://academy-files.s3.ap-northeast-2.amazonaws.com/..."}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.downloadUrl` | 1시간짜리 임시 다운로드 URL. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `FILE_404_1` | 파일을 찾을 수 없습니다. | `fileId`에 해당하는 파일이 없는 경우 |

---

## **4. 다운로드용 URL 일괄 조회**

`POST /api/files/download-urls`

메시지 목록처럼 한 화면에 여러 첨부파일을 표시해야 할 때, fileId 개수만큼 반복 호출하지 않도록 한 번에 조회한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "fileIds": [1, 2, 3]
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `fileIds` | `List<Long>` | `true` | 다운로드 URL을 조회할 파일 ID 목록. 비어 있으면 안 되고 **최대 100개**. 각 원소는 null이 아닌 양수여야 함. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 다운로드용 URL 일괄 조회 성공 |

Response Body

```json
{"status":200,"code":"FILE_200_3","message":"다운로드용 URL 일괄 조회에 성공했습니다.","data":{"downloadUrls":{"1":"https://...","2":"https://..."}}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.downloadUrls` | `fileId -> downloadUrl` 맵. **요청한 fileIds 중 존재하지 않는 ID는 이 맵에서 조용히 빠진다** (전체 요청이 실패하지 않음. 예: `[1,2,999]` 요청했는데 999가 없으면 `{1:..., 2:...}`만 응답). |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `fileIds`가 비어 있거나, 100개를 초과하거나, null/0 이하 원소가 포함된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## 알려진 제약

- 2번(등록) API는 S3에 실제로 업로드됐는지 확인하지 않는다.
- presigned PUT URL 자체에는 업로드 용량 제한이 없다.
