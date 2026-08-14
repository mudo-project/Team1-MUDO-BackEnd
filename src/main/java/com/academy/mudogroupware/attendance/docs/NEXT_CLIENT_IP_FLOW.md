# Next 서버 기반 Wi-Fi IP 처리 흐름

> 기준일: 2026-08-14
>
> 범위: 현재 접속 IP 조회, Wi-Fi IP 등록, IP 기반 출근·퇴근 검증

## 전체 구조

브라우저는 Spring 백엔드를 직접 호출하지 않고 Next 서버를 통해 근태 API를 호출한다.

```text
브라우저
→ Next 서버: 사용자 IP 확인 및 서명
→ Spring Security: 사용자 인증
→ ClientIpResolver: IP 서명 검증
→ 근태 Controller와 Service
```

## 백엔드 흐름

### 현재 접속 IP 조회

```http
GET /api/attendance/wifi-ips/current
```

처리 순서:

```text
Access Token 인증
→ ClientIpResolver 호출
→ Next 서명 헤더 검증
→ timestamp 허용 범위 검증
→ IPv4 또는 IPv6 검증
→ 검증된 IP 반환
```

- 별도 메서드 권한은 없지만 전역 Security 설정에 따라 인증이 필요하다.
- DB에서 등록된 Wi-Fi IP를 조회하거나 비교하지 않는다.

성공 응답 예시:

```json
{
  "status": 200,
  "code": "ACADEMY_200_1",
  "message": "현재 접속 IP가 조회되었습니다.",
  "data": {
    "ipAddress": "203.0.113.10"
  }
}
```

### Wi-Fi IP 등록

```http
POST /api/attendance/wifi-ips
```

필요 권한:

```text
ATTENDANCE:WIFI_IP_MANAGE
```

요청 Body:

```json
{
  "confirmedIpAddress": "203.0.113.10",
  "note": "학원 공유기"
}
```

처리 순서:

```text
사용자 인증과 권한 확인
→ ClientIpResolver에서 서명된 사용자 IP 확인
→ confirmedIpAddress와 검증된 IP 비교
→ 기존 등록 IP인지 확인
→ 중복이 아니면 DB 저장
```

- `confirmedIpAddress`와 검증된 IP가 다르면 `WIFI_IP_CHANGED` 오류가 발생한다.
- Body의 IP만 신뢰하지 않고 Next 서버가 서명한 IP와 다시 비교한다.

### 출근

```http
POST /api/attendance/check-ins
```

처리 순서:

```text
사용자 인증
→ ClientIpResolver에서 서명된 IP 확인
→ 등록된 Wi-Fi IP인지 DB 조회
→ 근무 정책과 기존 출근 기록 확인
→ 출근 기록 저장
```

- 출근 요청 Body에는 IP를 넣지 않는다.
- 등록되지 않은 IP이면 출근을 거절한다.

### 퇴근

```http
POST /api/attendance/check-outs
```

처리 순서:

```text
사용자 인증
→ ClientIpResolver에서 서명된 IP 확인
→ 등록된 Wi-Fi IP인지 DB 조회
→ 미퇴근 출근 기록 조회
→ 퇴근 기록 저장
```

- 퇴근 요청 Body에는 IP를 넣지 않는다.
- 등록되지 않은 IP이면 퇴근을 거절한다.

### 등록된 허용 IP 목록 조회

```http
GET /api/attendance/wifi-ips
```

필요 권한:

```text
ATTENDANCE:WIFI_IP_MANAGE
```

처리 순서:

```text
사용자 인증과 권한 확인
→ DB에서 등록 IP 목록 조회
→ 생성일과 ID 오름차순 반환
```

이 API는 현재 접속 IP를 판별하지 않으므로 Next IP 서명 헤더가 필요하지 않다.

## Next 서버 구현

### 브라우저 요청 처리

브라우저는 Next Route Handler, Server Action 또는 기존 BFF 계층을 호출하고, Next 서버가 Spring 백엔드를 호출한다.

```text
브라우저 → Next 서버의 근태 API → Spring 백엔드의 근태 API
```

### 사용자 IP 추출

Next 서버는 배포 플랫폼이나 앞단 프록시가 설정한 신뢰할 수 있는 헤더에서 사용자 IP를 추출해야 한다.

- 브라우저가 전달한 `X-Client-IP`를 사용하지 않는다.
- 브라우저가 전달한 기존 IP 서명 헤더를 모두 제거한다.
- `X-Forwarded-For`는 앞단 프록시가 해당 값을 덮어쓰는 것이 보장될 때만 사용한다.
- 쉼표로 구분된 IP 목록의 첫 번째 값을 검증 없이 사용하지 않는다.
- Next 서버로 직접 접근하는 경로를 차단하거나 신뢰 경계를 명확히 설정한다.
- 실제 추출 헤더는 Vercel, ECS, CloudFront 등 Next 배포 환경에 맞게 결정한다.

### 서명 헤더

Next 서버는 IP 판별이 필요한 요청에 다음 헤더를 추가한다.

```http
X-Client-IP: 203.0.113.10
X-Client-IP-Timestamp: 1786675200
X-Client-IP-Signature: {Base64 URL-safe HMAC-SHA256 서명}
```

서명 payload는 다음 네 값을 줄바꿈 문자(`\n`)로 연결한다.

```text
{HTTP_METHOD}\n{BACKEND_REQUEST_PATH}\n{CLIENT_IP}\n{TIMESTAMP}
```

서명 규칙:

- HTTP Method는 대문자를 사용한다.
- Path에는 Spring 백엔드가 실제로 받는 경로를 사용한다.
- Path에 query string은 포함하지 않는다.
- IP 앞뒤 공백을 제거한다.
- timestamp는 Unix epoch seconds를 사용한다.
- 결과는 Base64 URL-safe 형식으로 padding 없이 인코딩한다.
- Next와 백엔드는 동일한 `CLIENT_IP_SIGNING_SECRET`을 사용한다.

### Next 서버 서명 예시

```ts
import "server-only";
import { createHmac } from "node:crypto";

function createClientIpSignature(
  method: string,
  backendPath: string,
  clientIp: string,
  timestamp: string,
) {
  const secret = process.env.CLIENT_IP_SIGNING_SECRET;

  if (!secret) {
    throw new Error("CLIENT_IP_SIGNING_SECRET is missing");
  }

  const payload = [
    method.toUpperCase(),
    backendPath,
    clientIp.trim(),
    timestamp,
  ].join("\n");

  return createHmac("sha256", secret)
    .update(payload, "utf8")
    .digest("base64url");
}
```

### Next 서버의 백엔드 호출 예시

```ts
async function callAttendanceBackend({
  method,
  backendPath,
  clientIp,
  accessToken,
  body,
}: {
  method: string;
  backendPath: string;
  clientIp: string;
  accessToken: string;
  body?: unknown;
}) {
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const signature = createClientIpSignature(
    method,
    backendPath,
    clientIp,
    timestamp,
  );

  return fetch(`${process.env.BACKEND_BASE_URL}${backendPath}`, {
    method,
    cache: "no-store",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Client-IP": clientIp,
      "X-Client-IP-Timestamp": timestamp,
      "X-Client-IP-Signature": signature,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
```

## API별 서명 적용

| 기능 | Method | 서명에 사용하는 백엔드 경로 | IP 서명 |
| --- | --- | --- | --- |
| 현재 IP 조회 | `GET` | `/api/attendance/wifi-ips/current` | 필요 |
| Wi-Fi IP 등록 | `POST` | `/api/attendance/wifi-ips` | 필요 |
| 출근 | `POST` | `/api/attendance/check-ins` | 필요 |
| 퇴근 | `POST` | `/api/attendance/check-outs` | 필요 |
| 등록 IP 목록 | `GET` | `/api/attendance/wifi-ips` | 불필요 |
| 등록 IP 삭제 | `DELETE` | `/api/attendance/wifi-ips/{wifiIpId}` | 불필요 |

Next의 공개 경로가 백엔드 경로와 달라도 서명에는 Spring 백엔드 경로를 사용한다.

## 재시도와 오류 처리

### 재시도

백엔드는 timestamp가 설정된 허용 범위를 초과하면 요청을 거절한다. 기본 허용 범위는 60초다.

재시도할 때는 다음 값을 다시 생성한다.

- timestamp
- IP 서명

기존 서명을 장시간 캐시하거나 재사용하지 않는다.

### 오류 처리

| HTTP 상태 | 프론트 처리 |
| --- | --- |
| `401 Unauthorized` | 로그인 만료 또는 Access Token 누락 처리 |
| `403 Forbidden` | 권한 부족 또는 IP 전달 서명 검증 실패 처리 |
| 미등록 IP 오류 | 허용된 Wi-Fi가 아니라는 안내 표시 |
| `5xx` | 일시적인 서버 오류 안내 |

Next는 백엔드 오류를 처리하지 않은 Server Component 예외로 그대로 전파하지 않는다.

로그에는 다음 값을 남기지 않는다.

- Access Token
- `CLIENT_IP_SIGNING_SECRET`
- IP 서명 전체 값
- 요청 또는 응답 객체 전체

## 배포 설정

백엔드 ECS:

```text
CLIENT_IP_SIGNING_SECRET={공유 시크릿}
CLIENT_IP_MAX_CLOCK_SKEW_SECONDS=60
```

Next 서버:

```text
CLIENT_IP_SIGNING_SECRET={백엔드와 동일한 공유 시크릿}
BACKEND_BASE_URL={Spring 백엔드 주소}
```

- `CLIENT_IP_SIGNING_SECRET`에 `NEXT_PUBLIC_` 접두사를 붙이지 않는다.
- 시크릿은 Git에 저장하지 않고 배포 환경의 Secret 저장소에서 주입한다.
- Next 서버와 백엔드 서버의 시간을 동기화한다.
- Next 구현 전 백엔드만 먼저 배포하면 IP 서명이 필요한 네 API가 `403 Forbidden`을 반환하므로 양쪽을 함께 배포한다.
