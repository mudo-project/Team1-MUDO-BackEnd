# 워크스페이스 API

## 워크스페이스 생성

### Endpoint

`POST /api/workspaces`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 현재 구현은 인증된 사용자라면 호출할 수 있다.
- 워크스페이스 생성 권한인 `WORKSPACE:CREATE`은 권한 모듈 연동 후 `@PreAuthorize`로 적용 예정이다.

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` 형식의 Access Token |
| `Content-Type` | `application/json` |

### Request Body

```json
{
  "name": "8월 학사 운영",
  "memberIds": [12, 25]
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `name` | String | true | 워크스페이스 이름. 공백만 입력할 수 없고 최대 100자 |
| `memberIds` | Long[] | false | 추가 참여자 사용자 번호 목록. 생성자는 자동으로 참여하므로 빈 배열 또는 생략 가능 |

### Success Response

HTTP `201 Created`

```json
{
  "status": 201,
  "code": "WORKSPACE_201_1",
  "message": "워크스페이스 생성에 성공했습니다.",
  "data": {
    "workspaceId": 1
  }
}
```

| name | description |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 응답 코드 (`WORKSPACE_201_1`) |
| `message` | 응답 메시지 |
| `data.workspaceId` | 생성된 워크스페이스 번호 |

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 이름 누락·공백·100자 초과, 참여자 번호가 양수가 아닌 경우 |
| `400 Bad Request` | `WORKSPACE_400_1` | 생성자 또는 추가 참여자 중 같은 학원의 `ACTIVE` 사용자가 아닌 대상이 포함된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `409 Conflict` | `WORKSPACE_409_1` | 같은 학원에 동일한 활성 워크스페이스 이름이 이미 존재하는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- `academyId`, 생성자 ID는 요청 본문이 아니라 Access Token의 인증 정보에서 가져온다.
- 생성자는 `memberIds`에 포함하지 않아도 자동 참여한다.
- `memberIds`의 중복 값은 한 번만 처리한다.
- 생성자와 추가 참여자는 모두 같은 학원의 `ACTIVE` 사용자여야 한다.
- 이름의 앞뒤 공백을 제거한 뒤 같은 학원의 활성 워크스페이스 이름이 이미 있으면 `WORKSPACE_409_1`을 반환한다.
- 동시 생성으로 사전 중복 확인 뒤 DB unique 제약이 충돌한 경우에도 `WORKSPACE_409_1`을 반환한다.
