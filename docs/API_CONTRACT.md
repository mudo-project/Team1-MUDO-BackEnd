# API_CONTRACT.md

## 기본 경로

- Controller 경로에는 `/api`를 사용한다.
- 외부 요청 URL은 `/api/...` 형식을 사용한다.

```text
/api/users
/api/orders
```

## URI와 HTTP Method

- URI는 리소스 중심의 복수형 명사를 사용한다.
- URI에 동사를 사용하지 않는다.
- 하위 리소스는 부모 리소스 경로 아래에 표현한다.

```text
GET    /api/orders/{orderId}
POST   /api/orders
PATCH  /api/orders/{orderId}
DELETE /api/orders/{orderId}

GET    /api/orders/{orderId}/items
POST   /api/orders/{orderId}/items
```

- 상태 변경처럼 리소스 변경만으로 의미를 표현하기 어려운 작업은 하위 경로를 사용할 수 있다.

```text
PATCH /api/orders/{orderId}/approve
PATCH /api/orders/{orderId}/cancel
```

## 요청 규칙

### Header

```http
Content-Type: application/json
Authorization: Bearer {accessToken}
```

- 인증 API를 제외한 보호 리소스는 Access Token을 사용한다.
- 파일 업로드 API는 `multipart/form-data`를 사용한다.

### 입력 검증

- Request DTO에 Bean Validation을 사용한다.
- Path Variable과 Query Parameter도 형식과 범위를 검증한다.
- 검증 실패는 `400 Bad Request`로 응답한다.

### 날짜와 시간

- 날짜: `yyyy-MM-dd`
- API 날짜·시간은 `Asia/Seoul (UTC+09:00)` 기준의 ISO-8601 오프셋 형식을 사용한다.

```text
2026-08-02
2026-08-02T14:30:00+09:00
```

### 페이지네이션

목록 특성과 화면 요구사항에 따라 `Page`, `Slice`, `Cursor` 중 하나를 선택한다.

| 방식 | 선택 기준 | 요청 | 응답 |
| --- | --- | --- | --- |
| `Page` | 페이지 번호 이동과 전체 항목·페이지 수가 필요할 때 | `page`, `size` | `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `hasNext`, `hasPrevious` |
| `Slice` | 전체 개수 없이 이전·다음 순차 탐색 또는 더보기 UI만 필요할 때 | `page`, `size` | `content`, `page`, `size`, `hasNext` |
| `Cursor` | 데이터가 계속 추가되어 offset 조회 중 중복·누락 가능성이 있거나, 깊은 페이지의 offset 비용을 피해야 할 때 | 첫 요청은 cursor 생략, 이후 응답의 cursor와 `size` 전달 | `content` 또는 도메인 목록 필드, `hasNext`, 다음 요청용 cursor 필드 |

#### Page·Slice 공통 규칙

- `page`는 0부터 시작한다.
- 전체 개수와 번호 기반 페이지 이동이 필요하면 `Page`를 사용한다.
- 전체 개수가 불필요하면 추가 COUNT 쿼리를 생략할 수 있는 `Slice`를 우선 고려한다.

#### Cursor 규칙

- 첫 페이지 요청은 cursor를 생략한다.
- 다음 페이지 요청은 서버가 직전 응답으로 반환한 cursor를 변경하거나 해석하지 않고 그대로 전달한다.
- 단일 불투명 cursor는 요청 `cursor`, 응답 `nextCursor`를 기본 이름으로 사용한다.
- 정렬 키가 중복될 수 있어 복합 키셋 커서가 필요하면 의미가 드러나는 필드명을 사용한다. 예: 요청
  `cursorCreatedAt` + `cursorMessageId`, 응답 `nextCursorCreatedAt` + `nextCursorMessageId`.
- 복합 cursor 필드는 전부 전달하거나 전부 생략해야 한다.
- `hasNext`가 `false`이면 다음 cursor 필드는 `null`이다.
- DB 키셋 방식은 정렬 순서를 고정하고 유일한 타이브레이커를 포함한다. 예:
  `createdAt DESC, id DESC`.
- Cursor 응답은 전체 개수를 계산하지 않으므로 `page`, `totalElements`, `totalPages`를 제공하지 않는다.
- 한 API에서 offset 기반 `page`와 cursor를 동시에 사용하지 않는다. 기존 API의 방식을 변경하면 응답 계약 변경으로 취급한다.

## 성공 응답 형식

응답 본문이 있는 일반 REST API 성공 응답은 아래 형식을 사용한다.

```json
{
  "status": 200,
  "code": "DOMAIN_200_1",
  "message": "조회에 성공했습니다.",
  "data": {}
}
```

| 필드 | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 공통 또는 도메인별 성공 코드 |
| `message` | 성공 메시지 |
| `data` | 실제 응답 데이터. 없으면 `null` |

### 생성 성공 예시

```json
{
  "status": 201,
  "code": "DOMAIN_201_1",
  "message": "생성에 성공했습니다.",
  "data": {
    "id": 1
  }
}
```

- `204 No Content` 응답에는 응답 본문을 포함하지 않는다.

> **예외: workspace 도메인 삭제 API.** workspace 도메인의 삭제 API 5종은 `204 No Content` 대신 `200 OK` + `GlobalApiResponse` 본문(성공 코드·메시지 포함)을 반환한다. 호출자가 응답 본문만으로 어떤 작업이 성공했는지 구분할 수 있게 하기 위한 의도적인 예외이며, 다른 도메인의 삭제 API는 계속 `204 No Content` 규칙을 따른다.

### Controller 응답 조립

- 모든 응답 본문은 `global.presentation.api.common.GlobalApiResponse`를 사용한다. 도메인별 공통 응답 Wrapper를 중복 생성하지 않는다.
- 성공 코드는 각 도메인의 `presentation.api.common.<Domain>ResponseCode` enum이 `ResponseCode`를 구현해 소유한다.
- `200 OK` 응답은 `ResponseEntity.ok(GlobalApiResponse.ok(...))`를 사용한다. `ResponseEntity.status(200)`은 사용하지 않는다.
- `201 Created` 응답은 `ResponseEntity.status(HttpStatus.CREATED).body(GlobalApiResponse.created(...))`를 사용한다.
- `204 No Content` 응답은 `ResponseEntity.noContent().build()`를 사용한다.

```java
return ResponseEntity.ok(
        GlobalApiResponse.ok(
                WorkspaceResponseCode.WORKSPACE_FOUND,
                WorkspaceDetailResponse.from(result)
        )
);
```

## 오류 응답 형식

일반 예외와 검증 오류는 아래 형식을 사용한다.

```json
{
  "timestamp": "2026-08-02T14:30:00+09:00",
  "status": 400,
  "code": "COMMON_400_1",
  "message": "유효하지 않은 요청입니다.",
  "traceId": "a1b2c3d4",
  "details": {
    "errors": [
      {
        "field": "name",
        "reason": "이름은 필수입니다."
      }
    ]
  }
}
```

| 필드 | 설명 |
| --- | --- |
| `timestamp` | 오류 발생 시간 |
| `status` | HTTP 상태 코드 |
| `code` | 공통 또는 도메인 ErrorCode |
| `message` | 오류 메시지 |
| `traceId` | 서버 로그 추적용 식별자 |
| `details` | 검증 오류 또는 추가 문맥 정보 |

- 일반 오류의 `details`는 빈 객체 `{}`를 사용한다.
- 서버 내부 예외 정보, SQL, Stack Trace는 응답에 노출하지 않는다.
- 인증 실패를 포함한 모든 오류 응답은 동일한 공통 구조를 사용한다.

## HTTP 상태 코드 기준

| 상태 | 사용 기준 |
| --- | --- |
| `200 OK` | 조회, 수정, 처리 성공 |
| `201 Created` | 신규 리소스 생성 성공 |
| `204 No Content` | 응답 본문이 필요 없는 삭제 또는 처리 성공 |
| `400 Bad Request` | 요청 형식, 필수값, 검증 실패 |
| `401 Unauthorized` | 토큰 없음, 토큰 만료, 토큰 검증 실패 |
| `403 Forbidden` | 인증은 되었지만 권한 또는 소유권 없음 |
| `404 Not Found` | 대상 리소스 없음 |
| `409 Conflict` | 중복 생성, 현재 상태와 충돌 |
| `500 Internal Server Error` | 처리되지 않은 서버 내부 오류 |
| `502 Bad Gateway` | 외부 API 또는 연동 서버 호출 실패 |

## ErrorCode 규칙

- ErrorCode는 HTTP 상태 코드, 코드, 메시지를 함께 가진다.
- 공통 오류는 `COMMON_` 접두사를 사용한다.
- 도메인 오류는 도메인별 접두사를 사용한다.

```text
COMMON_400_1
COMMON_401_1
COMMON_403_1
COMMON_404_1
COMMON_409_1
COMMON_500_1

USER_404_1
ORDER_409_1
PAYMENT_400_1
```

- ErrorCode는 클라이언트 분기와 로그 분석에 사용한다.
- 메시지 변경이 필요해도 ErrorCode의 의미는 유지한다.

## 인증과 인가

- Access Token은 `Authorization: Bearer {token}` 형식을 사용한다.
- 인증 실패는 `401 Unauthorized`로 응답한다.
- 역할 기반 권한은 Security 설정과 UseCase 내부 소유권 검증으로 구성한다.
- 단순 역할 검증만으로 소유권 검증을 대체하지 않는다.
- 권한 부족 또는 타인의 리소스 접근은 `403 Forbidden`으로 응답한다.

## API 변경과 호환성

- 기존 응답 필드는 제거하거나 이름을 변경하지 않는다.
- 필드 추가는 기존 클라이언트에 영향을 주지 않는 경우에만 허용한다.
- Enum 값 추가 전 클라이언트의 예외 처리 여부를 확인한다.
- Request 필수값 추가는 호환성을 깨는 변경으로 본다.
- API 변경 시 대상 도메인의 세부 문서와 요청·응답 예시를 함께 수정한다.

## Swagger(OpenAPI) 문서화

- Controller를 새로 작성하거나 기존 Controller의 엔드포인트를 추가·수정할 때는 springdoc-openapi 어노테이션을 함께 작성한다. 코드 작성과 별도 작업으로 미루지 않는다.
- Controller 클래스에는 `@Tag(name = "도메인 이름", description = "...")`를 붙인다.
- 각 핸들러 메서드에는 `@Operation(summary = "...", description = "...")`을 붙인다. `summary`는 [세부 문서 API.md](#문서화-기준)의 해당 엔드포인트 설명과 일치시킨다.
- 각 핸들러 메서드에는 실제 구현한 HTTP 응답만 `@ApiResponses`로 명시한다. 구현하지 않은 오류 응답을 미리 문서화하지 않는다.
- Request DTO의 각 필드에는 `@Schema(description = "...", example = "...")`를 붙이고, 입력 제약은 Bean Validation으로 함께 표현한다.
- Response DTO와 응답 필드에는 `@Schema(description = "...", example = "...")`를 붙인다.
- 인증이 필요한 엔드포인트는 전역 `bearerAuth` 시큐리티 스킴(`OpenApiConfig`)이 기본 적용되므로 별도 어노테이션이 필요 없다. 인증이 필요 없는 엔드포인트(로그인 등)는 `@SecurityRequirements`(빈 값)로 명시적으로 뺀다.
- `/swagger-ui/index.html`에서 실제로 렌더링되는지 확인한 뒤 커밋한다.

## 문서화 기준

각 API 문서는 아래 내용을 포함한다.

```text
- Endpoint
- HTTP Method
- 인증 및 필요 권한
- Request Header
- Path Variable
- Query Parameter
- Request Body
- Success Response
- ErrorCode
- 비즈니스 규칙
```
