# notification API

기준일: 2026-08-13

> 이 문서는 Spring Boot Controller, Response DTO, ErrorCode 구현 기준으로 작성한다.
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다(권한 코드는 없음, 로그인만 필요 — 모든 조회/처리는 요청자 본인 알림으로 한정된다).

---

## **1. 알림 목록 조회**

`GET /api/notifications`

권한: 없음(로그인만 필요, 본인 알림만 조회)

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`, 최솟값 `0` |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`, 최소 `1`, 최대 `100` |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 알림 목록 조회 성공 |

Response Body

```json
{
    "status": 200,
    "code": "NOTIFICATION_200_1",
    "message": "알림 목록 조회에 성공했습니다.",
    "data": {
        "content": [
            {
                "notificationId": 12,
                "type": "APPROVAL_LINE_ACTIVATED",
                "targetId": 100,
                "message": "결재 문서 [휴가 신청서] 결재 차례가 되었습니다",
                "read": false,
                "createdAt": "2026-08-13T09:00:00"
            }
        ],
        "page": 0,
        "size": 20,
        "hasNext": false
    }
}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.content` | 알림 목록. 생성일 최신순(같은 시각이면 `notificationId` 내림차순)으로 정렬. 알림이 없으면 빈 배열 |
| `data.content[].notificationId` | 알림 번호 |
| `data.content[].type` | 알림 타입 코드(문자열). 목록은 [NOTIFICATION_TYPES.md](NOTIFICATION_TYPES.md) 참고 |
| `data.content[].targetId` | 클릭 시 이동할 대상 ID(업무 ID, 결재 문서 ID 등). `type`에 따라 의미가 다름 |
| `data.content[].message` | 저장 시점에 완성된 알림 문구(최대 250자). 문구 템플릿이 나중에 바뀌어도 과거 알림은 그대로 유지된다 |
| `data.content[].read` | 읽음 여부 |
| `data.content[].createdAt` | 알림 생성 시각 |
| `data.page` | 요청한 페이지 번호 |
| `data.size` | 요청한 페이지 크기 |
| `data.hasNext` | 다음 페이지 존재 여부 |

읽음/안읽음 필터는 없다 — 항상 섞어서 반환된다.

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `page < 0`, `size < 1`, `size > 100`인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **2. 안읽은 알림 개수 조회**

`GET /api/notifications/unread-count`

권한: 없음(로그인만 필요, 본인 알림만 조회)

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 안읽은 알림 개수 조회 성공 |

Response Body

```json
{
    "status": 200,
    "code": "NOTIFICATION_200_2",
    "message": "안읽은 알림 개수 조회에 성공했습니다.",
    "data": {
        "unreadCount": 3
    }
}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.unreadCount` | 본인에게 온 알림 중 아직 읽지 않은 알림 개수. 없으면 `0` |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **3. 알림 읽음 처리**

`PATCH /api/notifications/{notificationId}/read`

권한: 없음(로그인만 필요, 본인 소유 알림만 처리 가능)

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Path Variable

| **name** | **description** |
| --- | --- |
| `notificationId` | 읽음 처리할 알림 ID |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 읽음 처리 성공(이미 읽은 알림이어도 200, 최초 읽은 시각을 그대로 유지) |

Response Body

```json
{
    "status": 200,
    "code": "NOTIFICATION_200_3",
    "message": "알림 읽음 처리에 성공했습니다.",
    "data": null
}
```

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `NOTIFICATION_404_1` | 알림을 찾을 수 없습니다. | 존재하지 않거나 본인 소유가 아닌 `notificationId`인 경우. 다른 사람 알림이 존재한다는 사실 자체를 노출하지 않기 위해 403 대신 404로 응답한다. |

---

## **4. 알림 개별 삭제**

`DELETE /api/notifications/{notificationId}`

권한: 없음(로그인만 필요, 본인 소유 알림만 삭제 가능)

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Path Variable

| **name** | **description** |
| --- | --- |
| `notificationId` | 삭제할 알림 ID |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 삭제 성공(소프트 삭제) |

Response Body

```json
{
    "status": 200,
    "code": "NOTIFICATION_200_4",
    "message": "알림 삭제에 성공했습니다.",
    "data": null
}
```

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `NOTIFICATION_404_1` | 알림을 찾을 수 없습니다. | 존재하지 않거나 본인 소유가 아닌 `notificationId`인 경우 |

---

## **5. 읽은 알림 일괄 삭제**

`DELETE /api/notifications?status=READ`

권한: 없음(로그인만 필요, 본인 알림만 대상)

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `status` | `String` | `true` | 삭제 대상 필터. `READ`만 지원(대소문자 무관) |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 읽은 알림 일괄 삭제 성공(안읽은 알림은 삭제되지 않음, 대상이 0건이어도 200) |

Response Body

```json
{
    "status": 200,
    "code": "NOTIFICATION_200_5",
    "message": "읽은 알림 일괄 삭제에 성공했습니다.",
    "data": null
}
```

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `status` 파라미터 자체가 없는 경우 |
| `400 Bad Request` | `NOTIFICATION_400_1` | 일괄 삭제는 status=READ만 지원합니다. | `status` 값이 `READ`가 아닌 경우(예: `UNREAD`) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
