# 학생 관리 API

> 기준일: 2026-08-05
> 공통 응답 형식: `status`, `code`, `message`, `data` (204 No Content는 본문 없음)

## 1. 학생 등록

`POST /api/students`

인증: 필요

#### Request

```json
{
  "name": "김민수",
  "grade": "HIGH_1",
  "school": "무도고",
  "phone": "010-1111-2222",
  "parentPhone": "010-3333-4444",
  "note": "수학 선행 중"
}
```

#### Response - `201 Created`

```json
{
  "status": 201,
  "code": "STUDENT_201_1",
  "message": "학생 등록에 성공했습니다.",
  "data": {
    "studentId": 1
  }
}
```

#### 규칙

- 학생은 로그인 계정이 아니며 `users` 테이블에 생성하지 않는다.
- `academyId`는 요청자의 인증 정보에서 가져온다.
- 이름과 학년은 필수다.
- 전화번호, 학부모 전화번호, 학교, 특이사항은 선택 입력이다.

---

## 2. 학생 목록 조회

`GET /api/students?keyword=&page=0&size=30`

인증: 필요

#### Query Parameter

| 이름 | 기본값 | 검증 | 설명 |
|---|---:|---|---|
| `keyword` | 없음 | 선택 | 학생 이름 검색어 |
| `page` | `0` | `0` 이상 | 0부터 시작하는 페이지 |
| `size` | `30` | `1` 이상 `100` 이하 | 한 번에 조회할 학생 수 |

#### Response - `200 OK`

```json
{
  "status": 200,
  "code": "STUDENT_200_1",
  "message": "학생 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "studentId": 1,
        "name": "김민수",
        "grade": "HIGH_1",
        "school": "무도고",
        "phone": "010-1111-2222",
        "parentPhone": "010-3333-4444",
        "activeEnrollmentCount": 2
      }
    ],
    "page": 0,
    "size": 30,
    "hasNext": false
  }
}
```

#### 규칙

- 같은 학원 학생만 조회한다.
- 기본 정렬은 이름 가나다순, 같은 이름이면 ID 오름차순이다.
- 전체 개수를 세지 않는 `Slice` 방식으로 응답한다.
- `size` 상한은 100으로 제한한다.

---

## 3. 학생 상세 조회

`GET /api/students/{studentId}`

인증: 필요

#### Path Variable

| 이름 | 설명 |
|---|---|
| `studentId` | 조회할 학생 ID |

#### Response - `200 OK`

```json
{
  "status": 200,
  "code": "STUDENT_200_2",
  "message": "학생 상세 조회에 성공했습니다.",
  "data": {
    "studentId": 1,
    "name": "김민수",
    "grade": "HIGH_1",
    "school": "무도고",
    "phone": "010-1111-2222",
    "parentPhone": "010-3333-4444",
    "note": "수학 선행 중",
    "createdAt": "2026-08-05T10:00:00",
    "updatedAt": "2026-08-05T10:00:00",
    "enrollments": [
      {
        "enrollmentId": 1,
        "lectureId": 100,
        "lectureName": "고1 수학",
        "teacherName": "박선생",
        "scheduleText": "월 19:00",
        "priceType": "MONTHLY",
        "priceAmount": 300000,
        "enrolledAt": "2026-08-05T10:00:00"
      }
    ]
  }
}
```

#### 규칙

- 학생이 요청자 학원 소속이 아니면 `403`을 반환한다.
- 현재 수강 중인 `ACTIVE` 수강 등록만 상세에 표시한다.
- 강의명/선생님/일정/가격 정보는 lecture 모듈 공개 Port로 보강한다. 현재 lecture 구현 전에는 값이 비어 있을 수 있다.

---

## 4. 학생 정보 수정

`PATCH /api/students/{studentId}`

인증: 필요

#### Request

```json
{
  "name": "김민수",
  "grade": "HIGH_1",
  "school": "무도고",
  "phone": "010-1111-2222",
  "parentPhone": "010-3333-4444",
  "note": "수학 심화반 희망"
}
```

#### Response - `204 No Content`

#### 규칙

- 학생이 요청자 학원 소속이 아니면 `403`을 반환한다.
- 수정 시각은 `Clock` 기반 KST 정책을 따른다.

---

## 5. 학생 삭제

`DELETE /api/students/{studentId}`

인증: 필요

#### Path Variable

| 이름 | 설명 |
|---|---|
| `studentId` | 삭제할 학생 ID |

#### Response - `204 No Content`

#### 규칙

- 소프트 삭제다. `student.deleted_at`에 삭제 시각을 기록할 뿐 row를 지우지 않는다.
- 삭제된 학생은 이후 목록/상세/수정/삭제 요청에서 `404`(`STUDENT_404_1`)로 취급된다.
- 학생이 요청자 학원 소속이 아니면 `403`을 반환한다.
- 수강 이력(`student_enrollment`)은 그대로 남아 있으므로 데이터 유실이 없다.

---

## 6. 학생 수강 등록

`POST /api/students/{studentId}/enrollments`

인증: 필요

#### Request

```json
{
  "lectureId": 100
}
```

#### Response - `201 Created`

```json
{
  "status": 201,
  "code": "STUDENT_201_2",
  "message": "수강 등록에 성공했습니다.",
  "data": {
    "enrollmentId": 1
  }
}
```

#### 규칙

- 화면에서는 버튼명을 "결제하기"로 둔다.
- 이번 범위에서는 실제 카드 결제/POS 연동 없이 수강 등록만 확정한다.
- 같은 학생이 같은 강의에 이미 `ACTIVE` 상태로 등록되어 있으면 `409`를 반환한다.
- 이전에 `ENDED` 상태였던 등록은 다시 `ACTIVE`로 되살릴 수 있다.

---

## 7. 학생 수강 종료

`PATCH /api/students/{studentId}/enrollments/{enrollmentId}/end`

인증: 필요

#### Response - `204 No Content`

#### 규칙

- 요청자 학원, 학생 ID, 수강 등록 ID가 모두 맞는 수강 등록만 종료한다.
- 종료 시 `Enrollment.status=ENDED`, `endedAt=현재 시각`으로 저장한다.

---

## 오류 코드

| code | HTTP | 메시지 |
|---|---:|---|
| `STUDENT_400_1` | 400 | 학생 이름은 비어 있을 수 없습니다. |
| `STUDENT_400_2` | 400 | 학생 학년은 필수입니다. |
| `STUDENT_400_3` | 400 | 등록할 강의는 필수입니다. |
| `STUDENT_403_1` | 403 | 해당 학생에 접근할 권한이 없습니다. |
| `STUDENT_404_1` | 404 | 학생을 찾을 수 없습니다. |
| `STUDENT_404_2` | 404 | 수강 등록 정보를 찾을 수 없습니다. |
| `STUDENT_409_1` | 409 | 이미 수강 중인 강의입니다. |
