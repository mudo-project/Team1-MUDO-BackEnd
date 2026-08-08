# 📌 계정·권한(users) API

> 기준일: 2026-08-04
> 공통 응답 형식: `status`, `code`, `message`, `data` (204 No Content는 본문 없음)

## 1. 로그인

`POST /api/auth/login`
권한: 없음 (공개 엔드포인트)

#### Request

```json
{
  "username": "kim_teacher01",
  "password": "P@ssw0rd!"
}
```

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_1",
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

응답 헤더에 `Set-Cookie: refreshToken=...; Path=/; HttpOnly; Secure; SameSite=Lax`가 함께 내려갑니다. `Path=/`가 없으면 브라우저가 쿠키 경로를 요청 경로(`/api/auth`) 기준으로 좁혀서, 이후 `/api/token/reissue` 요청엔 쿠키가 실리지 않습니다. refreshToken은 응답 바디에 포함되지 않습니다.

#### 검증 및 정책

- `username`, `password`는 비어 있을 수 없고, 각각 50자·100자를 넘을 수 없습니다(DB 컬럼 길이 및 해싱 자원 낭비 방지).
- 아이디가 없거나 비밀번호가 틀려도 **동일한 오류 코드·메시지**로 응답합니다 — 아이디 존재 여부가 노출되지 않도록 하기 위함입니다.
- 계정 상태(`status`)가 `ACTIVE`가 아니면(`RESIGNED`/`INACTIVE`) 로그인할 수 없습니다.
- refreshToken 쿠키의 만료 시간은 `jwt.refresh-token-expiration` 설정값과 동일합니다(기본 14일).

---

## 2. 액세스 토큰 재발급

`POST /api/token/reissue`
권한: 없음 (단, `refreshToken` HttpOnly 쿠키 필요)

#### Request

별도 요청 바디 없음. 브라우저가 로그인 시 저장된 `refreshToken` 쿠키를 자동으로 함께 보냅니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "TOKEN_200_1",
  "message": "액세스 토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

#### 검증 및 정책

- **refreshToken은 로테이션하지 않습니다.** 재발급 응답에는 새 refreshToken 쿠키가 포함되지 않고, 기존 쿠키가 만료 전까지 그대로 유지됩니다.
- refreshToken은 JWT 자체 서명·만료뿐 아니라, 서버에 저장된 값과 일치하는지도 함께 검증합니다(다른 기기에서 재로그인하면 이전 refreshToken은 자동으로 무효화됩니다 — 계정당 세션 1개).
- 검증된 사용자 정보로 액세스 토큰만 새로 발급합니다. 이때 `role`은 요청 시점의 토큰이 아니라 DB에서 다시 조회한 최신 값을 사용합니다.

---

## 3. 로그아웃

`POST /api/auth/logout`
권한: 로그인 필요 (액세스 토큰)

#### Request

별도 요청 바디 없음. `Authorization: Bearer {accessToken}` 헤더 필요.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_2",
  "message": "로그아웃되었습니다.",
  "data": null
}
```

응답 헤더로 `Set-Cookie: refreshToken=; Path=/; Max-Age=0; ...`가 내려가 브라우저의 refreshToken 쿠키를 즉시 만료시킵니다.

#### 검증 및 정책

- 서버에 저장된 refreshToken을 삭제합니다. 이후 해당 refreshToken으로는 `/api/token/reissue`를 호출할 수 없습니다(`AUTH_401_6`).
- 이미 발급된 accessToken은 로그아웃 이후에도 자체 만료 시간까지는 유효합니다(별도 블랙리스트 없음) — accessToken은 단명이므로 허용된 범위로 판단했습니다.

---

## 4. 역할 생성

`POST /api/roles`
권한: `ROLE:MANAGE` 필요 (원장 등 역할 관리 권한을 가진 계정만)

#### Request

```json
{
  "name": "강사",
  "description": "수업 담당"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `name` | String | true | 역할 이름 (같은 학원 내에서 중복 불가, 최대 50자) |
| `description` | String | false | 역할 설명 (최대 255자) |

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "ROLE_201_1",
  "message": "역할 생성에 성공했습니다.",
  "data": {
    "roleId": 3
  }
}
```

#### 검증 및 정책

- `academyId`는 요청 바디로 받지 않고, 인증된 사용자(JWT)의 소속 학원으로 서버가 결정합니다 — 다른 학원에 역할을 만들 수 없습니다.
- 역할 이름은 같은 학원 안에서만 중복 체크합니다(다른 학원엔 같은 이름의 역할이 있어도 무방).
- 이름 중복은 애플리케이션 레벨 사전 체크(`USER_409_1`)와 DB `UNIQUE` 제약(`uk_role_academy_name`) 둘 다로 방어합니다 — 동시에 같은 이름으로 두 요청이 들어와도 항상 하나만 성공합니다.

---

## 5. 권한 카탈로그 조회

`GET /api/permissions`
권한: `ROLE:MANAGE` 필요

### Request

없음

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "PERMISSION_200_1",
  "message": "권한 카탈로그 조회에 성공했습니다.",
  "data": [
    {
      "permissionId": 1,
      "code": "ROLE:MANAGE",
      "resource": "ROLE",
      "action": "MANAGE",
      "description": "역할 생성/수정/삭제 및 권한 조립"
    }
  ]
}
```

### 검증 및 정책

- 시스템 전체 고정 권한 카탈로그를 그대로 반환합니다. 학원별로 다르지 않습니다.
- `description`은 프론트에서 그대로 표시할 수 있는 한글 설명입니다.

---

## 6. 역할 권한 조립

`PUT /api/roles/{roleId}/permissions`
권한: `ROLE:MANAGE` 필요

### Request

```json
{
  "permissionCodes": ["ROLE:MANAGE", "ACCOUNT:CREATE"]
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `permissionCodes` | String[] | true (빈 배열 허용) | 역할에 부여할 권한 코드 전체 목록 |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- 요청한 `permissionCodes`로 역할의 권한을 **전체 교체**합니다(기존 권한과의 합집합이 아님). 빈 배열을 보내면 역할의 모든 권한이 제거됩니다 — 의도된 동작입니다.
- `roleId`가 존재하지 않거나 요청자와 다른 학원 소속이면 동일하게 `USER_404_2`로 응답합니다 — 다른 학원의 역할 존재 여부가 노출되지 않도록 하기 위함입니다.
- `permissionCodes` 중 하나라도 존재하지 않는 코드가 있으면 `USER_400_1`로 거부하고, 어떤 코드가 없는지 `details.missingCodes`로 알려줍니다.

---

## 7. 학원 신청 목록 조회

`GET /api/academy-applications`
권한: `PLATFORM:SUPER_ADMIN` 필요 (SUPER ADMIN 계정만 — `ROLE:MANAGE` 등 카탈로그 권한과는 별개)

### Request

없음

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "ACADEMY_APPLICATION_200_1",
  "message": "학원 신청 목록 조회에 성공했습니다.",
  "data": [
    {
      "applicationId": 1,
      "requestedLoginId": "academy01",
      "academyName": "우리학원",
      "businessNo": "123-45-67890",
      "representativeName": "홍길동",
      "representativeEmail": "hong@example.com",
      "representativePhone": "010-0000-0000",
      "status": "PENDING",
      "rejectReason": null,
      "createdAt": "2026-08-07T10:00:00"
    }
  ]
}
```

### 검증 및 정책

- 페이지네이션이 없습니다(기존 역할/권한 목록 조회와 동일한 전례).
- `PLATFORM:SUPER_ADMIN`은 `@PreAuthorize` 권한 코드가 아니라 `SecurityConfig` 필터체인 URL 매칭으로 확인합니다 — 학원 관리자는 이 authority를 절대 자기 역할에 배정할 수 없습니다.
- 신청 접수(`POST`) API는 아직 없습니다(파일 업로드 인프라 선행 필요) — 테스트 데이터는 수동으로 DB에 넣어야 합니다.

---

## 8. 학원 신청 상세 조회

`GET /api/academy-applications/{applicationId}`
권한: `PLATFORM:SUPER_ADMIN` 필요

### Request

없음

### Response · `200 OK`

목록 조회와 동일한 필드 구성의 단건 객체를 반환합니다.

```json
{
  "status": 200,
  "code": "ACADEMY_APPLICATION_200_2",
  "message": "학원 신청 상세 조회에 성공했습니다.",
  "data": {
    "applicationId": 1,
    "requestedLoginId": "academy01",
    "academyName": "우리학원",
    "businessNo": "123-45-67890",
    "representativeName": "홍길동",
    "representativeEmail": "hong@example.com",
    "representativePhone": "010-0000-0000",
    "status": "PENDING",
    "rejectReason": null,
    "createdAt": "2026-08-07T10:00:00"
  }
}
```

### 검증 및 정책

- `applicationId`가 존재하지 않으면 `USER_404_3`으로 응답합니다.
- 목록 응답과 필드가 지금은 같지만, 프론트 상세 화면 디자인이 확정되면 상세 전용 필드가 늘어날 수 있어 처음부터 별도 엔드포인트로 분리해뒀습니다.

---

## 9. 학원 신청 승인

`POST /api/academy-applications/{applicationId}/approve`
권한: `PLATFORM:SUPER_ADMIN` 필요

### Request

별도 요청 바디 없음.

### Response · `200 OK`

승인 시 academy와 최초 관리자 계정을 같은 트랜잭션에서 함께 생성합니다. 이메일 발송 인프라가 아직 없어, 생성된 임시 비밀번호를 응답에 평문으로 1회 담아 SUPER ADMIN이 신청자에게 수동으로 전달합니다.

```json
{
  "status": 200,
  "code": "ACADEMY_APPLICATION_200_3",
  "message": "학원 신청을 승인했습니다.",
  "data": {
    "academyId": 10,
    "userId": 20,
    "temporaryPassword": "Xk9#mQ2pRt7$"
  }
}
```

### 검증 및 정책

- `applicationId`가 존재하지 않으면 `USER_404_3`, 이미 승인/반려된 신청서면 `USER_409_5`로 응답합니다.
- 새로 발급되는 계정은 `account_type=ADMIN`, `admin_scope=ACADEMY`, `must_change_pw=true`로 생성되어 최초 로그인 시 비밀번호 변경이 강제됩니다.

---

## 10. 학원 신청 반려

`POST /api/academy-applications/{applicationId}/reject`
권한: `PLATFORM:SUPER_ADMIN` 필요

### Request

```json
{
  "rejectReason": "사업자번호 확인 불가"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `rejectReason` | String | true | 반려 사유 (최대 255자) |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `applicationId`가 존재하지 않으면 `USER_404_3`, 이미 승인/반려된 신청서면 `USER_409_5`로 응답합니다.

---

## 11. 역할 목록 조회

`GET /api/roles`
권한: `ROLE:MANAGE` 필요

### Request

없음

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "ROLE_200_1",
  "message": "역할 목록 조회에 성공했습니다.",
  "data": [
    {
      "roleId": 3,
      "name": "강사",
      "description": "수업 담당"
    }
  ]
}
```

### 검증 및 정책

- 인증된 사용자(JWT)의 소속 학원(`academyId`) 역할만 반환합니다. 페이지네이션이 없습니다(기존 권한 카탈로그 조회와 동일한 전례).
- 권한 정보(`permissionCodes`)는 내려주지 않습니다 — 프론트 사이드바가 목록에서는 권한을 쓰지 않기 때문입니다. 필요하면 역할 상세 조회(후속 API)를 씁니다.

---

## 12. 역할 상세 조회

`GET /api/roles/{roleId}`
권한: `ROLE:MANAGE` 필요

### Request

없음 (path variable `roleId`)

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "ROLE_200_2",
  "message": "역할 상세 조회에 성공했습니다.",
  "data": {
    "roleId": 3,
    "name": "강사",
    "description": "수업 담당",
    "permissionCodes": ["NOTICE:READ", "TASK:MANAGE"]
  }
}
```

### 검증 및 정책

- 목록 조회와 달리 `permissionCodes`를 포함해 내려줍니다.
- 역할이 존재하지 않거나, 존재하더라도 요청자의 소속 학원(`academyId`)이 아니면 동일하게 `404 USER_404_2`로 응답합니다 — 다른 학원 역할의 존재 여부가 노출되지 않도록 하기 위함입니다.

---

## 13. 역할 수정

`PUT /api/roles/{roleId}`
권한: `ROLE:MANAGE` 필요

### Request

```json
{
  "name": "수석강사",
  "description": "수정된 설명"
}
```

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `name`은 비어 있을 수 없고 50자, `description`은 255자를 넘을 수 없습니다(역할 생성과 동일한 제약).
- 역할이 존재하지 않거나 다른 학원 소속이면 `404 USER_404_2`.
- 같은 학원 안에 **자기 자신을 제외하고** 같은 이름의 역할이 있으면 `409 USER_409_1` — 이름을 바꾸지 않는 수정 요청(설명만 변경)이 자기 자신과 충돌해 실패하지 않도록 자기 자신은 검사에서 제외합니다.
- 권한 목록(`permissionCodes`)은 이 API로 바꿀 수 없습니다 — 역할 권한 조립(`PUT /api/roles/{roleId}/permissions`)을 씁니다.

---

## 14. 역할 삭제

`DELETE /api/roles/{roleId}`
권한: `ROLE:MANAGE` 필요

### Request

없음 (path variable `roleId`)

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- 역할이 존재하지 않거나 다른 학원 소속이면 `404 USER_404_2`.
- **`ACTIVE` 상태인 구성원이 이 역할을 쓰고 있으면** `409 USER_409_2`로 거절합니다 — 삭제 전에 먼저 구성원의 역할을 다른 역할로 바꿔야 합니다.
- `RESIGNED`/`INACTIVE` 상태인 구성원만 이 역할을 쓰고 있다면(퇴사자에게 배정된 채로 남아있는 경우 등) 삭제를 막지 않습니다 — 계정을 물리적으로 삭제하는 기능이 없어 영원히 지울 수 없는 역할이 생기는 걸 방지하기 위함입니다. 대신 삭제 시 그 구성원들의 역할 배정은 자동으로 해제됩니다(`role_id`가 `null`이 됨).

---

## 15. 사용자 역할 변경

`PATCH /api/users/{userId}/role`
권한: `ACCOUNT:MANAGE` 필요

### Request

```json
{
  "roleId": 5
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `roleId` | Long | true | 배정할 역할 ID (같은 학원 소속 역할만 가능) |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `userId`가 존재하지 않거나, 요청자와 다른 학원 소속이거나, `accountType != MEMBER`(학원 관리자 계정)이면 전부 동일하게 `404 USER_404_1`로 응답합니다 — 다른 학원 계정 존재 여부나 관리자 계정 여부가 노출되지 않도록 하기 위함입니다.
- `roleId`가 존재하지 않거나 다른 학원 소속이면 `404 USER_404_2`로 응답합니다.
- 이미 같은 역할이어도 그대로 통과합니다(멱등).
- 역할 해제(역할 없음으로 되돌리는 것)는 이 API로 할 수 없습니다.

---

## ⚠️ 주요 오류

| HTTP | 코드 | 상황 |
| --- | --- | --- |
| `401` | `USER_401_1` | 아이디 또는 비밀번호가 올바르지 않음 (아이디 없음/비밀번호 불일치 공통) |
| `403` | `USER_403_1` | 계정 상태가 `ACTIVE`가 아니어서 로그인할 수 없음 |
| `401` | `USER_401_2` | 리프레시 토큰 쿠키가 없음 |
| `404` | `USER_404_1` | 리프레시 토큰의 사용자 정보를 찾을 수 없음, 또는 사용자 역할 변경 시 대상 계정이 존재하지 않거나 다른 학원 소속이거나 학원 관리자 계정임 |
| `409` | `USER_409_1` | 같은 학원 내에 이미 존재하는 역할 이름 |
| `409` | `USER_409_2` | 배정된 구성원이 있는 역할을 삭제하려 시도 |
| `400` | `USER_400_1` | 존재하지 않는 권한 코드로 역할 권한 조립 시도 |
| `404` | `USER_404_2` | 역할이 존재하지 않거나 다른 학원 소속 |
| `404` | `USER_404_3` | 학원 신청서가 존재하지 않음 |
| `409` | `USER_409_5` | 이미 검토된(승인/반려) 신청서를 다시 승인/반려 시도 |
| `401` | `AUTH_401_1` | 리프레시 토큰 자체가 위조되었거나 형식이 올바르지 않음 |
| `401` | `AUTH_401_2` | 리프레시 토큰이 만료됨 |
| `401` | `AUTH_401_6` | 서버에 저장된 리프레시 토큰이 없음 |
| `401` | `AUTH_401_7` | 요청된 리프레시 토큰이 저장된 값과 일치하지 않음 (다른 기기 재로그인 등으로 무효화됨) |
| `400` | `COMMON_400_1` | 요청 형식 오류 (`username`/`password` 누락 또는 길이 초과, 또는 역할 `name`/`description` 형식 오류) |
| `403` | `COMMON_403_1` | `ROLE:MANAGE` 권한이 없는 계정으로 역할 생성 시도, `PLATFORM:SUPER_ADMIN`이 아닌 계정으로 학원 신청 목록/상세 조회·승인·반려 시도, 또는 `ACCOUNT:MANAGE` 권한이 없는 계정으로 사용자 역할 변경 시도 |
