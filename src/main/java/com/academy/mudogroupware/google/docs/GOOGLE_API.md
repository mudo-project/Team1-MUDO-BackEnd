# 구글 연동 API

## 구글 계정 연동 시작

### Endpoint

`POST /api/google/connections/authorize-url?switchAccount={true|false}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 원장(academy 관리자) 계정만 호출할 수 있다. `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`로 검증하며, `account_type=ADMIN` + `admin_scope=ACADEMY`인 계정에게만 이 authority가 부여된다.

### Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `switchAccount` | boolean | false | `true`이면 계정 교체를 위해 구글 계정 선택 화면을 강제로 띄운다. 생략 시 `false` |

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "GOOGLE_200_1",
  "message": "구글 인증 URL 발급에 성공했습니다.",
  "data": {
    "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&state=..."
  }
}
```

프론트엔드는 이 URL을 새 창/팝업으로 열어 구글 동의 화면을 진행시킨다.

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 원장(academy 관리자) 계정이 아닌 경우 |
| `502 Bad Gateway` | `GOOGLE_502_1` | `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`/`GOOGLE_REDIRECT_URI` 중 하나라도 설정되지 않았거나 구글 인증 URL 생성 자체가 실패한 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- `state`에 요청자의 `userId`·`switchAccount` 여부를 HMAC-SHA256으로 서명해 담는다(10분 유효). 콜백에서 이 서명을 검증해 위조·재사용을 막는다.
- "재연결"과 "계정 교체" 모두 이 엔드포인트를 사용한다. 별도 엔드포인트를 두지 않는다.

## 구글 OAuth 콜백

### Endpoint

`GET /api/google/connections/callback?code={code}&state={state}` (구글이 리다이렉트)

### 인증 및 권한

- 인증 헤더가 없다. 구글이 사용자 브라우저를 이 경로로 직접 리다이렉트하기 때문이다. `SecurityConfig`에서 이 경로만 `permitAll`이다.
- 신원 확인은 `state` 서명 검증으로 대체한다.

### Success Response

HTTP `302 Found`, `Location: {GOOGLE_OAUTH_FRONTEND_REDIRECT_URI}?googleConnection=success`

### Error Response

HTTP `302 Found`, `Location: {GOOGLE_OAUTH_FRONTEND_REDIRECT_URI}?googleConnection=failed`

`state`가 없거나 위조·만료됐거나, 구글이 `error` 파라미터를 보냈거나, 토큰 교환/사용자 정보 조회가 실패하면 이 응답으로 대체한다(별도 JSON 오류 응답 없음 — 브라우저 리다이렉트이기 때문).

### Business Rules

- 인가 코드를 구글 토큰 엔드포인트에서 액세스·리프레시 토큰으로 교환하고, 액세스 토큰으로 구글 사용자 이메일을 조회한다.
- 리프레시 토큰이 응답에 없으면(구글이 `access_type=offline`+`prompt=consent`를 지켰다면 발생하지 않아야 함) 실패로 처리한다.
- 같은 학원에 이미 연동이 있으면, 기존 리프레시 토큰을 구글에 폐기(revoke) 요청한 뒤 기존 행을 삭제하고 새 연동으로 교체한다.

## 구글 연동 상태 조회

### Endpoint

`GET /api/google/connections`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 원장(academy 관리자) 계정만 호출할 수 있다. `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`로 검증한다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "GOOGLE_200_2",
  "message": "구글 연동 상태 조회에 성공했습니다.",
  "data": {
    "googleEmail": "academy@mudo.co.kr",
    "connectedByUserId": 7,
    "scope": "openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/drive.file",
    "connectedAt": "2026-07-01T14:22:00",
    "refreshTokenExpiresAt": null,
    "lastCheckedAt": "2026-08-03T09:00:00",
    "status": "CONNECTED"
  }
}
```

연동된 계정이 없으면 `data`가 `null`이다(별도 404를 두지 않는다 — "연동 안 됨"은 정상적으로 있을 수 있는 상태다). 이 경우는 `NOT_CONNECTED` 상태에 해당하며, 응답 본문에는 별도 `status` 값을 넣지 않는다.

| 응답 필드 | 규칙 |
| --- | --- |
| `refreshTokenExpiresAt` | Google 토큰 응답에 `refresh_token_expires_in`이 실제로 있을 때만 만료 시각을 반환한다. 없으면 `null`이며, 이는 만료가 아니라 만료 시각을 알 수 없음을 뜻한다. |
| `scope` | 동의받은 scope의 공백 구분 문자열이다. 공유파일에 필요한 `drive.file`이 없을 수 있으므로, 프론트는 이 값을 자체 권한 상태로 해석하지 않는다. |
| `status` | 연결·토큰 건강 상태다. scope 부족만으로 `FAILED`가 되지 않는다. |

`status`는 `NOT_CONNECTED` / `CONNECTED` / `EXPIRING` / `EXPIRED` / `FAILED` 중 하나다. 실제 리프레시 토큰 만료 시각이 구글 응답에 있을 때만 만료 여부를 계산하며, 만료 7일 전부터 `EXPIRING`으로 표시한다. `FAILED`는 저장된 리프레시 토큰으로 실제 액세스 토큰 재발급에 실패한 경우에만 반환한다. `drive.file` scope가 없는 기존 연결은 상태 조회에서는 `CONNECTED`를 유지할 수 있으며, 공유파일이 실제 접근 토큰을 요청할 때 `GOOGLE_409_1`로 재연결을 안내한다.

### 응답 호환성 변경

- 기존 `tokenExpiresAt` 필드는 `refreshTokenExpiresAt`으로 변경됐다.
- `V5.1.6__align_google_refresh_token_expiration.sql`은 기존에 임의로 저장된 만료일을 모두 `NULL`로 초기화한다. 배포 직후 기존 연결의 `refreshTokenExpiresAt`은 `null`로 응답될 수 있다.
- 이전 `tokenExpiresAt` 별칭은 제공하지 않는다. 프론트는 `refreshTokenExpiresAt`만 사용해야 한다.

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 원장(academy 관리자) 계정이 아닌 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 구글 연동 상태 확인

### Endpoint

`POST /api/google/connections/check`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 원장(academy 관리자) 계정만 호출할 수 있다. `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`로 검증한다.

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 원장(academy 관리자) 계정이 아닌 경우 |
| `404 Not Found` | `GOOGLE_404_1` | 연동된 구글 계정이 없는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 저장된 리프레시 토큰으로 실제 액세스 토큰 재발급을 시도해 유효성을 확인한다(구글이 재발급을 거부하면, 예: 관리자가 구글 계정 설정에서 앱 권한을 취소한 경우, 실패로 판정한다).
- 확인 결과에 따라 `lastCheckedAt`을 갱신하고 실패 여부(`failed`)를 반영한다. 성공/실패와 무관하게 `connectedAt`과 구글이 실제로 반환한 리프레시 토큰 만료 시각은 바뀌지 않는다.

## 구글 연동 해제

### Endpoint

`DELETE /api/google/connections`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 원장(academy 관리자) 계정만 호출할 수 있다. `@PreAuthorize("hasAuthority('ACADEMY:OWNER')")`로 검증한다.

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 원장(academy 관리자) 계정이 아닌 경우 |
| `404 Not Found` | `GOOGLE_404_1` | 연동된 구글 계정이 없는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 저장된 리프레시 토큰을 구글에 폐기(revoke) 요청한 뒤 연동 행을 삭제한다. 구글 드라이브에 저장된 파일 자체는 삭제하지 않는다(이번 범위에는 템플릿 관리가 포함되지 않는다).
