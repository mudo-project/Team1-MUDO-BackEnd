# 📌 계정·권한(users) API

> 기준일: 2026-08-12
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
  "description": "수업 담당",
  "color": "#FF5733"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `name` | String | true | 역할 이름 (중복 불가, 최대 50자) |
| `description` | String | false | 역할 설명 (최대 255자) |
| `color` | String | false | 역할 뱃지 색상. 형식 검증 없이 그대로 저장/반환합니다(프론트 책임), 최대 20자. 안 보내면 `null` |

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

- 역할 이름은 시스템 전체에서 중복 체크합니다.
- 이름 중복은 애플리케이션 레벨 사전 체크(`USER_409_1`)와 DB `UNIQUE` 제약(`uk_role_name`) 둘 다로 방어합니다 — 동시에 같은 이름으로 두 요청이 들어와도 항상 하나만 성공합니다.

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
- `roleId`가 존재하지 않으면 `USER_404_2`로 응답합니다.
- `permissionCodes` 중 하나라도 존재하지 않는 코드가 있으면 `USER_400_1`로 거부하고, 어떤 코드가 없는지 `details.missingCodes`로 알려줍니다.

---

## 7. 역할 목록 조회

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
      "description": "수업 담당",
      "color": "#FF5733",
      "memberCount": 4
    }
  ]
}
```

### 검증 및 정책

- 시스템 전체 역할 목록을 반환합니다. 페이지네이션이 없습니다(기존 권한 카탈로그 조회와 동일한 전례).
- 권한 정보(`permissionCodes`)는 내려주지 않습니다 — 프론트 사이드바가 목록에서는 권한을 쓰지 않기 때문입니다. 필요하면 역할 상세 조회(후속 API)를 씁니다.
- `memberCount`는 저장된 값이 아니라 조회 시점에 `status = ACTIVE`인 구성원 수를 계산합니다(퇴사자는 세지 않음).

---

## 8. 역할 상세 조회

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
    "color": "#FF5733",
    "memberCount": 4,
    "permissionCodes": ["NOTICE:READ", "TASK:MANAGE"]
  }
}
```

### 검증 및 정책

- 목록 조회와 달리 `permissionCodes`를 포함해 내려줍니다.
- 역할이 존재하지 않으면 `404 USER_404_2`로 응답합니다.
- `memberCount`는 목록 조회와 동일하게 `status = ACTIVE`인 구성원만 계산합니다.

---

## 9. 역할 수정

`PUT /api/roles/{roleId}`
권한: `ROLE:MANAGE` 필요

### Request

```json
{
  "name": "수석강사",
  "description": "수정된 설명",
  "color": "#00FF00"
}
```

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `name`은 비어 있을 수 없고 50자, `description`은 255자를 넘을 수 없습니다(역할 생성과 동일한 제약). `color`는 검증 없이 그대로 저장됩니다.
- 역할이 존재하지 않으면 `404 USER_404_2`.
- **자기 자신을 제외하고** 같은 이름의 역할이 있으면 `409 USER_409_1` — 이름을 바꾸지 않는 수정 요청(설명만 변경)이 자기 자신과 충돌해 실패하지 않도록 자기 자신은 검사에서 제외합니다.
- 권한 목록(`permissionCodes`)은 이 API로 바꿀 수 없습니다 — 역할 권한 조립(`PUT /api/roles/{roleId}/permissions`)을 씁니다.

---

## 10. 역할 삭제

`DELETE /api/roles/{roleId}`
권한: `ROLE:MANAGE` 필요

### Request

없음 (path variable `roleId`)

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- 역할이 존재하지 않으면 `404 USER_404_2`.
- **`ACTIVE` 상태인 구성원이 이 역할을 쓰고 있으면** `409 USER_409_2`로 거절합니다 — 삭제 전에 먼저 구성원의 역할을 다른 역할로 바꿔야 합니다.
- `RESIGNED`/`INACTIVE` 상태인 구성원만 이 역할을 쓰고 있다면(퇴사자에게 배정된 채로 남아있는 경우 등) 삭제를 막지 않습니다 — 계정을 물리적으로 삭제하는 기능이 없어 영원히 지울 수 없는 역할이 생기는 걸 방지하기 위함입니다. 대신 삭제 시 그 구성원들의 역할 배정은 자동으로 해제됩니다(`role_id`가 `null`이 됨).

---

> (2026-08-12) 기존 "11. 사용자 역할 변경"(`PATCH /api/users/{userId}/role`) API는 폐기되고 구성원 정보 수정(관리자, 15-4번 항목)으로 병합됐습니다. 프론트가 정보 수정과 역할 변경을 한 API 호출로 처리하고 싶다는 요청에 따른 변경입니다. 이 경로는 더 이상 라우팅되지 않습니다(매핑된 컨트롤러 없음 — 미매핑 경로 전체가 404 대신 500을 반환하는 기존 이슈는 이 변경과 무관하게 별도로 남아있습니다).

---

## 12. 학원 구성원 검색

`GET /api/users?keyword=`
권한: 없음 (로그인만 되면 호출 가능)

### Request

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `keyword` | String | false | 이름 부분 일치 검색(대소문자 무관). 없으면 전체 목록 반환 |

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_3",
  "message": "구성원 검색에 성공했습니다.",
  "data": [
    { "userId": 12, "name": "김강사", "username": "kim_teacher01" }
  ]
}
```

### 검증 및 정책

- `accountType` 무관하게 전체 포함합니다(일반 직원 + 학원 관리자).
- `status = ACTIVE`인 계정만 검색 대상입니다.
- 워크스페이스 생성/멤버 추가, 채팅방 생성 API가 받는 `memberIds`/`participantIds`에 채워 넣을 userId를 찾는 용도입니다.

---

## 12-1. 구성원 목록 조회(관리자)

`GET /api/users/members`
권한: `ACCOUNT:MANAGE` 필요

### Request

Query Parameter

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `keyword` | String | false | 이름 또는 역할명 부분 일치 검색어(대소문자 무관). 없으면 전체 반환 |
| `roleId` | Long | false | 특정 역할의 구성원만 조회. 없으면 전체 역할 포함 |
| `status` | String | false | 특정 재직 상태(`ACTIVE`/`RESIGNED`/`INACTIVE`)의 구성원만 조회. 없으면 전체 상태 포함 |
| `page` | int | false | 페이지 번호(0부터 시작). 기본값 0 |
| `size` | int | false | 페이지 크기(1~100). 기본값 20 |

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_4",
  "message": "구성원 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "userId": 10,
        "name": "최현우",
        "email": "hwchoi@academy.kr",
        "phone": "010-4567-8901",
        "roleId": 8,
        "roleName": "강사",
        "joinedAt": "2023-03-02T00:00:00",
        "status": "ACTIVE",
        "attendanceStatus": "PRESENT"
      }
    ],
    "page": 0,
    "size": 2,
    "totalElements": 12,
    "totalPages": 6,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 검증 및 정책

- 기존 "학원 구성원 검색"(12번 항목)과 달리 `ACTIVE`뿐 아니라 `RESIGNED`/`INACTIVE`도 포함한 전체 구성원을 반환합니다 — 관리자 전용 관리 화면 API이기 때문입니다. `status` 파라미터로 재직/휴직/퇴사 탭별 필터링이 가능하며, 지정하지 않으면 전체 상태를 반환합니다.
- `status`에 `ACTIVE`/`RESIGNED`/`INACTIVE` 외의 값을 넘기면 `400 COMMON_400_1`로 실패합니다.
- `keyword`/`roleId`/`status`는 서버에서 AND 조건으로 함께 적용된 뒤 정렬·페이지네이션이 이루어집니다.
- `roleId`가 없는 계정(예: 역할 미배정)은 `roleId`/`roleName` 모두 `null`로 내려갑니다.
- 결과는 `roleName`, `name` 순으로 정렬됩니다 — 역할별로 묶어 보여주는 조직도 화면에서, `roleId`를 지정해 역할 탭마다 별도로 호출하는 방식을 전제로 합니다.
- `page`/`size` 범위를 벗어나면(`page<0`, `size`가 1~100 밖) `400`으로 실패합니다.
- `attendanceStatus`는 `ACTIVE` 구성원만 `PRESENT`/`ABSENT`/`OFF`/`LEAVE` 중 하나로 채워지고, `RESIGNED`/`INACTIVE` 구성원은 `null`입니다. 근태 정책(`AttendancePolicy`)이 등록되어 있지 않으면 이 API 전체가 `404 ATTENDANCE_404_1`로 실패합니다. 근태 조회는 현재 페이지에 포함된 구성원만 대상으로 합니다.
- (2026-08-12) 응답이 공용 `SliceResponse`(`content`/`page`/`size`/`hasNext`)에서 이 API 전용 `totalElements`/`totalPages`/`first`/`last`/`hasPrevious`를 추가한 형태로 바뀌었습니다 — `1 2 3 4` 번호 기반 페이지네이션 UI를 프론트에서 그릴 수 있게 하기 위함입니다. `totalElements`는 이미 인메모리에 올라와 있는 필터링된 전체 리스트의 크기를 그대로 쓰므로 추가 DB 조회 비용이 없습니다(DB 레벨 페이지네이션 전환은 여전히 보류 상태, 아래 REVISION.md 참고).

---

## 13. 직원 계정 발급

`POST /api/users`
권한: `ACCOUNT:MANAGE` 필요

### Request

```json
{
  "username": "teacher01",
  "name": "김강사",
  "phone": "010-1111-2222",
  "email": "teacher01@example.com",
  "roleId": 8
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `username` | String | true | 로그인 아이디, 최대 50자, 전역 유니크 |
| `name` | String | true | 이름, 최대 50자 |
| `phone` | String | false | 전화번호, 최대 20자. 비워두면 본인이 나중에 `PATCH /api/users/me`(15-1번 항목)로 채워 넣을 수 있다(`V4.1.7`) |
| `email` | String | false | 이메일, 최대 100자, 형식 검증(`@Email`). 비워두면 본인이 나중에 채워 넣을 수 있다(`V4.1.7`) |
| `roleId` | Long | true | 배정할 역할 ID |

### Response · `201 Created`

```json
{
  "status": 201,
  "code": "USER_201_1",
  "message": "직원 계정이 발급되었습니다.",
  "data": {
    "userId": 8,
    "username": "teacher01",
    "passwordSetupLink": "http://localhost:3000/password-setup?username=teacher01&tempPassword=USb8MGQYrq%21p"
  }
}
```

### 검증 및 정책

- `username`이 이미 존재하면 `409 USER_409_6`으로 거절합니다(사전 체크 + DB 유니크 제약 이중 방어).
- `roleId`가 존재하지 않으면 `404 USER_404_2`로 응답합니다.
- 계정은 `accountType=MEMBER`, 임시 비밀번호로 발급되며 `mustChangePw`가 `true`로 저장됩니다.
- `phone`/`email`은 선택 입력이다(`V4.1.7`). 원장이 직원 전체의 연락처를 일일이 입력하지 않아도 되고, 비워두면 본인이 나중에 `PATCH /api/users/me`(15-1번 항목)로 채워 넣을 수 있습니다.
- 응답의 `passwordSetupLink`는 이 호출 한 번에만 내려가며, 링크 안의 임시 비밀번호는 서버에 별도로 저장되지 않습니다. 학원 관리자가 직원에게 직접 전달해야 합니다(카카오톡/문자 등) — 이메일 등 자동 발송은 아직 없습니다(후속 작업). 이 링크로 `POST /api/users/password-setup`(14번 항목)을 호출하면 최초 비밀번호 설정이 끝납니다.

---

## 14. 최초 비밀번호 설정

`POST /api/users/password-setup`
권한: 없음 (공개 엔드포인트)

### Request

```json
{
  "username": "teacher01",
  "tempPassword": "USb8MGQYrq!p",
  "newPassword": "MyOwnPassword1!"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `username` | String | true | 계정 아이디 |
| `tempPassword` | String | true | 비밀번호 설정 링크에 담겨온 임시 비밀번호 원문 |
| `newPassword` | String | true | 새로 정할 비밀번호, 8~100자 |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- 검증 순서: `username`으로 계정 조회 → `must_change_pw == true`인지 확인 → `tempPassword`가 저장된 해시와 일치하는지 확인. 셋 중 하나라도 실패하면 전부 동일하게 `400 USER_400_2`로 응답합니다(계정 존재·상태 비노출).
- 성공하면 `password`가 새 값으로 교체되고 `must_change_pw`가 `false`로 바뀝니다. 그 순간부터 같은 링크(옛 임시 비밀번호)는 해시가 안 맞아 자동으로 무효화되고, `must_change_pw`가 이미 `false`라 이 계정에 대해 이 엔드포인트 자체가 다시는 통과하지 않습니다(1회성 보장에 별도 만료시간을 두지 않음).
- 직원 계정 발급(13번 항목) 경로가 이 API로 최초 설정을 완료합니다. 학원 관리자(원장) 계정을 만드는 API는 없습니다 — 학원 신청/승인 플로우 폐기 이후, 새로 배포한 서버의 최초 관리자 계정은 앱 API를 거치지 않고 DB에 직접 SQL로 계정·역할·권한을 심어서 만듭니다(이 엔드포인트를 통한 최초 설정 흐름은 적용되지 않음).

---

## 15. 내 정보 조회

`GET /api/users/me`
권한: 없음 (로그인만 되면 호출 가능, 본인 정보만 조회)

### Request

없음

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_5",
  "message": "내 정보 조회에 성공했습니다.",
  "data": {
    "userId": 10,
    "name": "최현우",
    "email": null,
    "phone": null,
    "roleId": 8,
    "roleName": "강사",
    "joinedAt": "2023-03-02T00:00:00",
    "status": "ACTIVE"
  }
}
```

### 검증 및 정책

- `roleId`가 없는 계정(예: 역할 미배정)은 `roleId`/`roleName` 모두 `null`로 내려갑니다.
- 인증 실패는 401. 토큰 발급 이후 계정이 삭제되는 등 JWT의 `userId`에 해당하는 계정을 찾을 수 없으면 `404 USER_404_1`로 응답합니다.

---

## 15-1. 내 정보 수정

`PATCH /api/users/me`
권한: 없음 (로그인만 되면 호출 가능, 본인 정보만 수정)

### Request

```json
{
  "phone": "010-1234-5678",
  "email": "me@academy.kr"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `phone` | String | false | 전화번호, 최대 20자. 보내지 않으면 기존 값 유지 |
| `email` | String | false | 이메일, 최대 100자, 형식 검증(`@Email`). 보내지 않으면 기존 값 유지 |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `phone`/`email`만 수정 가능합니다 — 이름·역할·입사일은 본인이 바꿀 수 없고 관리자만 바꿀 수 있습니다(관리자용 구성원 정보 수정은 별도 PR에서 이어집니다).
- 값을 보내지 않은 필드는 기존 값을 그대로 유지하는 부분 수정(partial update)입니다 — PATCH를 보낼 때마다 전체 필드를 다시 채울 필요가 없습니다.
- `email`은 형식 검증(`@Email`)을 통과해야 합니다(`null`은 부분 수정 의미로 계속 허용). 형식이 안 맞으면 `400 COMMON_400_1`로 거절합니다.
- `email`이 다른 계정과 중복되면 `409 USER_409_7`로 거절합니다.
- 같은 계정을 동시에 수정하는 두 요청이 겹치면(예: 본인이 `/me`로 수정하는 중 관리자가 같은 계정을 수정) `409 USER_409_8`로 나중 요청이 실패합니다(2026-08-12, `@Version` 낙관적 락 도입). 그 외 실패 케이스 없음(인증 실패만 401).

---

## 15-2. 내 비밀번호 변경

`PATCH /api/users/me/password`
권한: 없음 (로그인만 되면 호출 가능, 본인 비밀번호만 변경)

### Request

```json
{
  "currentPassword": "OldPass1234",
  "newPassword": "NewPass1234"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `currentPassword` | String | true | 현재 비밀번호, 최대 100자 |
| `newPassword` | String | true | 새로 정할 비밀번호, 8~100자 |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `currentPassword`가 저장된 해시와 일치하는지 확인합니다. 틀리면 `400 USER_400_3`으로 실패합니다.
- 이미 인증된 본인 요청이라(JWT로 신원이 이미 확인됨) `POST /api/users/password-setup`(14번 항목)처럼 계정 존재 여부를 숨기는 마스킹된 에러 메시지를 쓰지 않고, "현재 비밀번호가 올바르지 않습니다"라는 구체적인 메시지를 그대로 반환합니다.
- 통과하면 비밀번호가 새 해시로 교체됩니다. `mustChangePw` 값과 무관하게 동작합니다.

---

## 15-3. 구성원 상세 조회(관리자)

`GET /api/users/{userId}`
권한: `ACCOUNT:MANAGE` 필요

### Request

없음 (path variable `userId`)

### Response · `200 OK`

```json
{
  "status": 200,
  "code": "USER_200_6",
  "message": "구성원 상세 조회에 성공했습니다.",
  "data": {
    "userId": 10,
    "name": "최현우",
    "email": "hwchoi@academy.kr",
    "phone": "010-4567-8901",
    "roleId": 8,
    "roleName": "강사",
    "joinedAt": "2023-03-02T00:00:00",
    "status": "ACTIVE"
  }
}
```

### 검증 및 정책

- 내 정보 조회(15번 항목)와 동일한 `GetUserDetailService`/`UserDetailResult`/`UserDetailResponse`를 재사용합니다.
- `userId`가 존재하지 않거나 `accountType != MEMBER`(학원 관리자 계정)이면 전부 동일하게 `404 USER_404_1`로 응답합니다 — 관리자 계정 여부가 노출되지 않도록 하기 위함입니다(구성원 정보 수정 API와 동일한 정책).

---

## 15-4. 구성원 정보 수정(관리자)

`PATCH /api/users/{userId}`
권한: `ACCOUNT:MANAGE` 필요

### Request

```json
{
  "name": "최현우",
  "phone": "010-1234-5678",
  "email": "hwchoi@academy.kr",
  "joinedAt": "2023-03-02T00:00:00",
  "roleId": 9
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `name` | String | false | 이름, 최대 50자. 보내지 않으면 기존 값 유지 |
| `phone` | String | false | 전화번호, 최대 20자. 보내지 않으면 기존 값 유지 |
| `email` | String | false | 이메일, 최대 100자, 형식 검증(`@Email`). 보내지 않으면 기존 값 유지 |
| `joinedAt` | LocalDateTime | false | 입사일. 보내지 않으면 기존 값 유지 |
| `roleId` | Long | false | 배정할 역할 ID. 보내지 않으면 기존 역할 유지(2026-08-12 추가) |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- 내 정보 수정(15-1번 항목)과 동일하게 값을 보내지 않은 필드는 기존 값을 그대로 유지하는 부분 수정(partial update)입니다.
- 본인 수정과 달리 `name`/`joinedAt`도 관리자 권한으로 바꿀 수 있습니다.
- `userId`가 존재하지 않거나 `accountType != MEMBER`이면 전부 동일하게 `404 USER_404_1`로 응답합니다(구성원 상세 조회와 동일한 정책).
- `email`은 형식 검증(`@Email`)을 통과해야 합니다(`null`은 부분 수정 의미로 계속 허용). 형식이 안 맞으면 `400 COMMON_400_1`로 거절합니다.
- 내 정보 수정(15-1번 항목)과 동일하게, 같은 계정을 동시에 수정하는 요청이 겹치면 `409 USER_409_8`로 나중 요청이 실패합니다(`@Version` 낙관적 락).
- `email`이 다른 계정과 중복되면 `409 USER_409_7`로 거절합니다.
- (2026-08-12) `roleId`를 보내면 역할도 함께 바뀝니다 — 기존 별도 API였던 "사용자 역할 변경"(`PATCH /api/users/{userId}/role`)이 이 API로 병합됐습니다(프론트 요청). `roleId`가 존재하지 않으면 `404 USER_404_2`로 거절하고, 이 경우 이름/연락처/이메일/입사일 등 다른 필드도 전혀 반영되지 않습니다 — 역할 존재 검증을 프로필 필드 갱신보다 먼저 수행하기 때문입니다. 이미 같은 역할이어도 그대로 통과합니다(멱등). 역할 해제(역할 없음으로 되돌리는 것)는 이 API로 할 수 없습니다.

---

## 15-5. 구성원 재직 상태 변경(관리자)

`PATCH /api/users/{userId}/status`
권한: `ACCOUNT:MANAGE` 필요

### Request

```json
{
  "status": "RESIGNED"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `status` | String | true | 변경할 재직 상태: `ACTIVE`/`RESIGNED`/`INACTIVE` |

### Response · `204 No Content`

본문 없음.

### 검증 및 정책

- `ACTIVE`/`RESIGNED`/`INACTIVE` 양방향 전환을 지원합니다(예: 퇴사 처리 이후 복직 시 다시 `ACTIVE`로 되돌리는 것도 가능).
- `userId`가 존재하지 않거나 `accountType != MEMBER`이면 전부 동일하게 `404 USER_404_1`로 응답합니다(구성원 상세 조회·정보 수정과 동일한 정책).

---

## ⚠️ 주요 오류

| HTTP | 코드 | 상황 |
| --- | --- | --- |
| `401` | `USER_401_1` | 아이디 또는 비밀번호가 올바르지 않음 (아이디 없음/비밀번호 불일치 공통) |
| `403` | `USER_403_1` | 계정 상태가 `ACTIVE`가 아니어서 로그인할 수 없음 |
| `401` | `USER_401_2` | 리프레시 토큰 쿠키가 없음 |
| `404` | `USER_404_1` | 리프레시 토큰의 사용자 정보를 찾을 수 없음, 구성원 상세 조회(15-3번 항목)·구성원 정보 수정(15-4번 항목, 역할 변경 포함)·구성원 재직 상태 변경(15-5번 항목) 시 대상 계정이 존재하지 않거나 학원 관리자 계정임, 또는 내 정보 조회(15번 항목) 시 JWT의 userId에 해당하는 계정을 찾을 수 없음(계정 삭제 등) |
| `409` | `USER_409_1` | 이미 존재하는 역할 이름 |
| `409` | `USER_409_2` | 배정된 구성원이 있는 역할을 삭제하려 시도 |
| `400` | `USER_400_1` | 존재하지 않는 권한 코드로 역할 권한 조립 시도 |
| `400` | `USER_400_2` | 비밀번호 설정 실패(아이디 없음/이미 설정 완료/임시비밀번호 불일치 공통) |
| `404` | `USER_404_2` | 역할이 존재하지 않음(계정 발급·구성원 정보 수정 시 `roleId` 검증 공통) |
| `409` | `USER_409_6` | 이미 사용 중인 아이디로 직원 계정 발급 시도 |
| `409` | `USER_409_7` | 내 정보 수정(15-1번 항목) 또는 구성원 정보 수정(15-4번 항목) 시 이미 사용 중인 이메일로 변경 시도 |
| `400` | `USER_400_3` | 내 비밀번호 변경(15-2번 항목) 시 현재 비밀번호가 일치하지 않음 |
| `409` | `USER_409_8` | 내 정보 수정(15-1번 항목) 또는 구성원 정보 수정(15-4번 항목) 시 동시 수정 충돌(다른 요청이 먼저 반영됨) |
| `401` | `AUTH_401_1` | 리프레시 토큰 자체가 위조되었거나 형식이 올바르지 않음 |
| `401` | `AUTH_401_2` | 리프레시 토큰이 만료됨 |
| `401` | `AUTH_401_6` | 서버에 저장된 리프레시 토큰이 없음 |
| `401` | `AUTH_401_7` | 요청된 리프레시 토큰이 저장된 값과 일치하지 않음 (다른 기기 재로그인 등으로 무효화됨) |
| `400` | `COMMON_400_1` | 요청 형식 오류 (`username`/`password` 누락 또는 길이 초과, 또는 역할 `name`/`description` 형식 오류) |
| `403` | `COMMON_403_1` | `ROLE:MANAGE` 권한이 없는 계정으로 역할 생성·목록 조회 시도, 또는 `ACCOUNT:MANAGE` 권한이 없는 계정으로 구성원 정보 수정(역할 변경 포함) 시도 |
