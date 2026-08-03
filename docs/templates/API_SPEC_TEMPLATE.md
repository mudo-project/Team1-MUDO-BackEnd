# API 명세서 작성 규칙

- 기능 구현과 관련 테스트가 모두 통과한 후 작성한다.
- Controller, Request/Response DTO, Security 설정, 예외 코드 구현을 기준으로 작성하며, 구현되지 않은 요청·응답·실패 코드를 임의로 추가하지 않는다.
- 아래 전체 내용을 Notion에 그대로 붙여넣을 수 있는 Markdown 형식으로 작성한다.

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `pathVariable` | Path Variable 설명입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `parameter` | `String` | `true` | Query Parameter 설명입니다. |

Request Body

```json
{
  "field": "value"
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `field` | `String` | `true` | 요청 필드 설명입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 성공 설명입니다. |

Response Body

```json
{
  "status": 200,
  "code": "DOMAIN_200_1",
  "message": "요청에 성공했습니다.",
  "data": {
    "field": "value"
  }
}
```

### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.field` | 응답 필드 설명입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` |  | 요청 값이 올바르지 않습니다. | 요청 파라미터 또는 Body 값이 유효하지 않은 경우 |
| `401 Unauthorized` |  | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` |  | 접근 권한이 없습니다. | 해당 기능에 대한 권한이 없는 경우 |
| `404 Not Found` |  | 대상을 찾을 수 없습니다. | 요청한 리소스가 없는 경우 |
| `500 Internal Server Error` |  | 서버 내부 오류가 발생했습니다. | 예상하지 못한 서버 오류 |
