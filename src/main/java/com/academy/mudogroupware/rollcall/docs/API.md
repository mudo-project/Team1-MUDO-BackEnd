# 출결 API

> 기준일: 2026-08-06
> 공통 응답 형식: `status`, `code`, `message`, `data` (엑셀 다운로드와 204 응답 제외)

## 1. 강의 출결부 조회

`GET /api/rollcall/lectures/{lectureId}/attendance?date=2026-08-06`

인증: 필요
권한: `ROLLCALL:MANAGE`

#### Response - `200 OK`

```json
{
  "status": 200,
  "code": "ROLLCALL_200_1",
  "message": "강의 출결부 조회에 성공했습니다.",
  "data": {
    "lectureId": 1,
    "lectureName": "고1 수학 정규반",
    "date": "2026-08-06",
    "entries": [
      {
        "studentId": 101,
        "studentName": "김민수",
        "grade": "HIGH_1",
        "parentPhone": "010-3333-4444",
        "status": "ABSENT",
        "note": null
      }
    ],
    "summary": {
      "total": 1,
      "present": 0,
      "absent": 1,
      "late": 0,
      "online": 0,
      "etc": 0
    }
  }
}
```

#### 규칙

- 요청자 학원의 강의만 조회한다.
- 출결 기록이 없는 학생은 `status`, `note`가 `null`로 내려갈 수 있다.
- 하단 요약은 현재 출결 상태가 저장된 학생 수를 기준으로 계산한다.

---

## 2. 출결 저장

`PUT /api/rollcall/lectures/{lectureId}/attendance?date=2026-08-06`

인증: 필요
권한: `ROLLCALL:MANAGE`

#### Request

```json
{
  "entries": [
    {
      "studentId": 101,
      "status": "ABSENT",
      "note": null
    },
    {
      "studentId": 102,
      "status": "ETC",
      "note": "상담 후 조퇴"
    }
  ]
}
```

#### Response - `204 No Content`

#### 규칙

- 일부 학생만 보내도 저장할 수 있다.
- 같은 학생·강의·날짜 기록은 upsert로 처리한다.
- `ETC` 상태는 `note`가 필수다.

---

## 3. 출결부 엑셀 다운로드

`GET /api/rollcall/lectures/{lectureId}/attendance/export?date=2026-08-06`

인증: 필요
권한: `ROLLCALL:MANAGE`

#### Response - `200 OK`

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="attendance_1_2026-08-06.xlsx"
```

#### 규칙

- 번호, 학생명, 학년, 출결상태, 비고와 하단 요약이 포함된다.
- 출결 상태가 비어 있어도 다운로드할 수 있다.

---

## 4. 문자 발송 대상 조회

`GET /api/rollcall/lectures/{lectureId}/attendance/message-candidates?date=2026-08-06`

인증: 필요
권한: `ROLLCALL:MANAGE`

#### Response - `200 OK`

```json
{
  "status": 200,
  "code": "ROLLCALL_200_3",
  "message": "발송 대상 조회에 성공했습니다.",
  "data": [
    {
      "studentId": 101,
      "studentName": "김민수",
      "status": "ABSENT",
      "parentPhone": "010-3333-4444",
      "matchedTemplateId": 3,
      "matchedTemplateName": "결석 안내",
      "eligible": true
    }
  ]
}
```

#### 규칙

- 출결 상태가 저장된 학생만 후보에 포함한다.
- 학생 출결 상태에 맞는 문자 템플릿이 있으면 `eligible=true`다.
- 실제 SMS 발송은 이 API에서 하지 않는다.

---

## 5. 문자 템플릿 생성

`POST /api/rollcall/message-templates`

인증: 필요
권한: `ROLLCALL:TEMPLATE_MANAGE`

#### Request

```json
{
  "name": "결석 안내",
  "status": "ABSENT",
  "content": "[무도학원] 오늘 결석 처리되었습니다."
}
```

#### Response - `201 Created`

```json
{
  "status": 201,
  "code": "ROLLCALL_201_1",
  "message": "문자 템플릿 생성에 성공했습니다.",
  "data": {
    "templateId": 3
  }
}
```

#### 규칙

- 같은 학원에서 같은 출결 상태의 템플릿은 1개만 만들 수 있다.

---

## 6. 문자 템플릿 목록 조회

`GET /api/rollcall/message-templates`

인증: 필요
권한: `ROLLCALL:MANAGE` 또는 `ROLLCALL:TEMPLATE_MANAGE`

#### Response - `200 OK`

```json
{
  "status": 200,
  "code": "ROLLCALL_200_4",
  "message": "문자 템플릿 목록 조회에 성공했습니다.",
  "data": [
    {
      "id": 3,
      "name": "결석 안내",
      "status": "ABSENT",
      "content": "[무도학원] 오늘 결석 처리되었습니다.",
      "createdAt": "2026-08-06T10:00:00",
      "updatedAt": "2026-08-06T10:00:00"
    }
  ]
}
```

---

## 7. 문자 템플릿 수정

`PATCH /api/rollcall/message-templates/{templateId}`

인증: 필요
권한: `ROLLCALL:TEMPLATE_MANAGE`

#### Request

```json
{
  "name": "결석 안내 수정",
  "content": "[무도학원] 금일 결석으로 확인되었습니다."
}
```

#### Response - `204 No Content`

#### 규칙

- 템플릿의 `status`는 수정할 수 없다.

---

## 8. 문자 템플릿 삭제

`DELETE /api/rollcall/message-templates/{templateId}`

인증: 필요
권한: `ROLLCALL:TEMPLATE_MANAGE`

#### Response - `204 No Content`

---

## 오류 코드

| code | HTTP | 메시지 |
|---|---:|---|
| `ROLLCALL_400_1` | 400 | 기타 사유를 입력해야 합니다. |
| `ROLLCALL_404_1` | 404 | 문자 템플릿을 찾을 수 없습니다. |
| `ROLLCALL_404_2` | 404 | 강의를 찾을 수 없습니다. |
| `ROLLCALL_409_1` | 409 | 이미 해당 출결 상태의 템플릿이 있습니다. |

## SMS 발송 상태

현재 실제 SMS 발송 API는 없다. SMS 공급자, 발신번호, API 키, 실패 재시도 정책이 정해지면 별도 Port/Adapter와 발송 API를 추가한다.
