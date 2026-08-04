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

## ⚠️ 주요 오류

| HTTP | 코드 | 상황 |
| --- | --- | --- |
| `401` | `USER_401_1` | 아이디 또는 비밀번호가 올바르지 않음 (아이디 없음/비밀번호 불일치 공통) |
| `403` | `USER_403_1` | 계정 상태가 `ACTIVE`가 아니어서 로그인할 수 없음 |
| `401` | `USER_401_2` | 리프레시 토큰 쿠키가 없음 |
| `404` | `USER_404_1` | 리프레시 토큰의 사용자 정보를 찾을 수 없음 |
| `401` | `AUTH_401_1` | 리프레시 토큰 자체가 위조되었거나 형식이 올바르지 않음 |
| `401` | `AUTH_401_2` | 리프레시 토큰이 만료됨 |
| `401` | `AUTH_401_6` | 서버에 저장된 리프레시 토큰이 없음 |
| `401` | `AUTH_401_7` | 요청된 리프레시 토큰이 저장된 값과 일치하지 않음 (다른 기기 재로그인 등으로 무효화됨) |
| `400` | `COMMON_400_1` | 요청 형식 오류 (`username`/`password` 누락 또는 길이 초과) |
