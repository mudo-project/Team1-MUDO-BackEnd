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

## 워크스페이스 목록 조회

### Endpoint

`GET /api/workspaces?scope=MINE|ALL`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `scope=MINE`은 인증된 사용자가 호출할 수 있다.
- 권한 모듈 연동 전에는 `scope=ALL` 요청을 조회 UseCase 호출 전에 `403 Forbidden`으로 차단한다.
- 권한 모듈 연동 후에는 `WORKSPACE:READ_ALL` Authority가 있는 사용자만 `scope=ALL`을 호출할 수 있다.

### Query Parameter

| name | type | required | default | description |
| --- | --- | --- | --- | --- |
| `scope` | String | false | `MINE` | `MINE`: 사용자가 참여한 워크스페이스, `ALL`: 권한 모듈 연동 후 같은 학원의 전체 활성 워크스페이스 |

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "WORKSPACE_200_1",
  "message": "워크스페이스 목록 조회에 성공했습니다.",
  "data": [
    {
      "workspaceId": 1,
      "name": "8월 학사 운영",
      "memberCount": 3
    }
  ]
}
```

| name | type | description |
| --- | --- | --- |
| `data[].workspaceId` | Long | 워크스페이스 번호 |
| `data[].name` | String | 워크스페이스 이름 |
| `data[].memberCount` | Long | 워크스페이스 참여자 수 |

조회 결과가 없으면 HTTP `200 OK`와 빈 배열을 반환한다.

```json
{
  "status": 200,
  "code": "WORKSPACE_200_1",
  "message": "워크스페이스 목록 조회에 성공했습니다.",
  "data": []
}
```

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | `scope`가 `MINE`, `ALL` 중 하나가 아닌 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 권한 모듈 연동 전 `scope=ALL`을 요청한 경우. 연동 후에는 `WORKSPACE:READ_ALL` 없이 요청한 경우 |

### Business Rules

- `scope`를 생략하면 `MINE`으로 처리한다.
- 목록은 사용자별 최근 접속 시각 내림차순이며, 최근 접속 기록이 없는 항목은 워크스페이스 생성 시각 내림차순으로 뒤에 배치한다.
- 최근 접속 시각과 생성 시각이 모두 같으면 워크스페이스 번호 내림차순으로 정렬한다.
- 삭제된 워크스페이스는 반환하지 않는다.
- `academyId`, `userId`는 요청 파라미터가 아니라 Access Token의 인증 정보에서 가져온다.

## 워크스페이스 최근 접속 기록

### Endpoint

`PUT /api/workspaces/{workspaceId}/recent-access`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 일반 사용자는 자신이 참여한 같은 학원의 활성 워크스페이스만 기록할 수 있다.
- `WORKSPACE:READ_ALL` Authority가 있으면 자신이 참여하지 않았더라도 같은 학원의 활성 워크스페이스를 기록할 수 있다.

### Path Variable

| name | type | required | description |
| --- | --- | --- | --- |
| `workspaceId` | Long | true | 최근 접속 시각을 기록할 워크스페이스 번호 |

### Success Response

HTTP `204 No Content`

응답 본문은 없다.

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 같은 학원의 접근 가능한 활성 워크스페이스가 아닌 경우 |

### Business Rules

- `academyId`, `userId`, `WORKSPACE:READ_ALL` 보유 여부는 인증 정보에서 가져온다.
- 최초 접속이면 최근 접속 기록을 생성하고, 기존 기록이 있으면 현재 서버 시각으로 갱신한다. MySQL 단일 upsert를 사용하므로 동일한 최초 접속 요청이 동시에 들어와도 중복 키 오류가 발생하지 않으며, 늦게 도착한 과거 시각은 더 최신 기록을 덮어쓰지 않는다.
