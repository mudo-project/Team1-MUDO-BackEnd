# 강의 관리 API

기준일: 2026-08-13

## 1. 강의 등록

`POST /api/lectures`

권한: `LECTURE:MANAGE`

### Request

```json
{
  "name": "고1 수학 정규반",
  "classType": "CLASS",
  "dayOfWeek": "MONDAY",
  "classroomCode": "A101",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "grade": "HIGH_1",
  "teacherName": "김선생",
  "subjectName": "수학",
  "termName": "2026 1학기",
  "feeType": "PER_MONTH",
  "feeAmount": 300000
}
```

필수 입력은 `name`, `classType`, `dayOfWeek`, `classroomCode`, `startTime`, `endTime`이다.
`grade`, `teacherName`, `subjectName`, `termName`, `feeType`, `feeAmount`는 선택 입력이며 비워 둘 수 있다.
강의 등록 요청은 `teacherId` 대신 `teacherName` 중심으로 받는다.

### Response

```json
{
  "status": 201,
  "code": "LECTURE_201_1",
  "message": "강의 등록에 성공했습니다.",
  "data": {
    "lectureId": 1
  }
}
```

## 2. 강의 목록 조회

`GET /api/lectures?termId=&grade=&subjectName=&teacherName=&classroomCode=&dayOfWeek=&page=0&size=20`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Request Query Parameter

| name | type | required | description |
|---|---|---:|---|
| `termId` | `Long` | false | 학기/기간 id입니다. 값이 없으면 전체 학기/기간을 조회합니다. |
| `grade` | `Grade` | false | 학년 필터입니다. 아래 `Grade enum` 값 중 하나를 전달합니다. |
| `subjectName` | `String` | false | 과목명 필터입니다. 포함 검색으로 동작합니다. 예: `수학` |
| `teacherName` | `String` | false | 담당 선생님 이름 필터입니다. 포함 검색으로 동작합니다. 예: `김선생` |
| `classroomCode` | `String` | false | 강의실 코드/이름 필터입니다. 정확히 일치하는 값으로 조회합니다. 예: `A101` |
| `dayOfWeek` | `DayOfWeek` | false | 요일 필터입니다. 예: `MONDAY`, `TUESDAY` |
| `page` | `int` | false | 페이지 번호입니다. 0부터 시작하며 기본값은 `0`입니다. |
| `size` | `int` | false | 페이지 크기입니다. 기본값은 `20`입니다. |

### Grade enum

| value | description |
|---|---|
| `ELEMENTARY_1` | 초등학교 1학년 |
| `ELEMENTARY_2` | 초등학교 2학년 |
| `ELEMENTARY_3` | 초등학교 3학년 |
| `ELEMENTARY_4` | 초등학교 4학년 |
| `ELEMENTARY_5` | 초등학교 5학년 |
| `ELEMENTARY_6` | 초등학교 6학년 |
| `MIDDLE_1` | 중학교 1학년 |
| `MIDDLE_2` | 중학교 2학년 |
| `MIDDLE_3` | 중학교 3학년 |
| `HIGH_1` | 고등학교 1학년 |
| `HIGH_2` | 고등학교 2학년 |
| `HIGH_3` | 고등학교 3학년 |
| `RETAKE` | N수/재수 |

강의 목록 조회 필터는 강의 등록/시간표 화면에서 사용하는 값과 동일하게 `subjectName`, `teacherName`, `classroomCode`를 사용한다.
`subjectName`, `teacherName`은 포함 검색이며, `classroomCode`는 정확히 일치하는 강의실 코드/이름으로 조회한다.
빈 문자열로 전달한 쿼리값은 필터에서 제외된다.

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_1",
  "message": "강의 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "고1 수학 정규반",
        "classType": "CLASS",
        "grade": "HIGH_1",
        "termName": "2026 1학기",
        "subjectName": "수학",
        "teacherId": 12,
        "teacherName": "김선생",
        "classroomCode": "A101",
        "classroomName": "A101",
        "schedules": [
          {
            "dayOfWeek": "MONDAY",
            "startTime": "19:00:00",
            "endTime": "21:00:00"
          }
        ],
        "studentCount": 8
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

## 3. 강의 상세 조회

`GET /api/lectures/{lectureId}`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_2",
  "message": "강의 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "name": "고1 수학 정규반",
    "classType": "CLASS",
    "grade": "HIGH_1",
    "termName": "2026 1학기",
    "subjectName": "수학",
    "teacherId": 12,
    "teacherName": "김선생",
    "classroomCode": "A101",
    "classroomName": "A101",
    "feeType": "PER_MONTH",
    "feeAmount": 300000,
    "schedules": [
      {
        "dayOfWeek": "MONDAY",
        "startTime": "19:00:00",
        "endTime": "21:00:00"
      }
    ],
    "students": [
      {
        "id": 101,
        "name": "김민수",
        "grade": "HIGH_1"
      }
    ],
    "createdAt": "2026-08-06T10:00:00"
  }
}
```

## 4. 강의 수정

`PATCH /api/lectures/{lectureId}`

권한: `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Path Variable

| name | type | required | description |
|---|---|---:|---|
| `lectureId` | `Long` | true | 수정할 강의 id입니다. |

### Request Body

등록 API와 같은 단일 요일/시간대 형태를 사용합니다.

```json
{
  "name": "고2 수학 특강",
  "classType": "SPECIAL",
  "dayOfWeek": "TUESDAY",
  "classroomCode": "B201",
  "startTime": "10:00:00",
  "endTime": "12:00:00",
  "grade": "HIGH_2",
  "teacherName": "박선생",
  "subjectName": "수학",
  "termName": "2026 여름방학",
  "feeType": "PER_MONTH",
  "feeAmount": 320000
}
```

| name | type | required | 설명 |
|---|---|---:|---|
| `name` | `String` | true | 강의명입니다. |
| `classType` | `ClassType` | true | 강의 유형입니다. `CLASS`, `SPECIAL`, `CLINIC`, `STANDING`, `EXAM` 중 하나입니다. |
| `dayOfWeek` | `DayOfWeek` | true | 강의 요일입니다. 예: `MONDAY`, `TUESDAY` |
| `classroomCode` | `String` | true | 교실 코드/이름입니다. |
| `startTime` | `LocalTime` | true | 시작 시간입니다. 예: `10:00:00` |
| `endTime` | `LocalTime` | true | 종료 시간입니다. 예: `12:00:00` |
| `grade` | `Grade` | false | 대상 학년입니다. |
| `teacherName` | `String` | false | 담당 선생님 이름입니다. |
| `subjectName` | `String` | false | 과목명입니다. |
| `termName` | `String` | false | 시수/학기/기간명입니다. |
| `feeType` | `FeeType` | false | 수강료 정책입니다. `PER_SESSION`, `PER_MONTH` 중 하나입니다. |
| `feeAmount` | `Integer` | false | 수강료 금액입니다. |

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_3",
  "message": "강의 수정에 성공했습니다.",
  "data": null
}
```

### 주요 정책

- 수정 대상 강의가 없거나 삭제된 상태면 `LECTURE_404_1`을 반환합니다.
- 수정된 교실/요일/시간대가 자기 자신을 제외한 다른 강의와 겹치면 `LECTURE_409_1`을 반환합니다.
- `termName`, `subjectName`, `classroomCode`가 기존에 없으면 등록 API와 동일하게 자동 생성합니다.

## 5. 강의 삭제

`DELETE /api/lectures/{lectureId}`

권한: `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Path Variable

| name | type | required | description |
|---|---|---:|---|
| `lectureId` | `Long` | true | 삭제할 강의 id입니다. |

### Response

```text
204 No Content
```

### 주요 정책

- 강의 삭제는 `deleted_at`을 채우는 소프트 삭제입니다.
- 삭제된 강의는 목록/상세 조회, id 목록 조회, 시간 충돌 검사, 매출 리포트용 강의 조회에서 제외됩니다.
- 삭제 대상 강의가 없거나 이미 삭제된 상태면 `LECTURE_404_1`을 반환합니다.

## 6. 강의 담당 선생님 목록 조회

`GET /api/lectures/teachers`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_4",
  "message": "강의 담당 선생님 목록 조회에 성공했습니다.",
  "data": ["김선생", "이선생"]
}
```

### 주요 정책

- 삭제되지 않은 강의에 실제로 등록된 `teacherName`만 중복 없이, 이름순으로 반환합니다.
- 강의가 하나도 없는 담당자나 빈 값은 목록에 포함되지 않습니다.
- 강의 목록 조회(`GET /api/lectures`)의 `teacherName` 검색 셀렉트박스를 채우는 용도이며, 여기서 받은 이름으로 검색하면 항상 결과가 존재함을 보장합니다.

## 7. 강의 과목 목록 조회

`GET /api/lectures/subjects`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_5",
  "message": "강의 과목 목록 조회에 성공했습니다.",
  "data": ["수학", "영어"]
}
```

### 주요 정책

- 삭제되지 않은 강의에 실제로 등록된 `subjectName`만 중복 없이, 이름순으로 반환합니다.
- 강의가 하나도 없는 과목이나 빈 값은 목록에 포함되지 않습니다.
- 강의 목록 조회(`GET /api/lectures`)의 `subjectName` 검색 셀렉트박스를 채우는 용도이며, 여기서 받은 이름으로 검색하면 항상 결과가 존재함을 보장합니다.

## 8. 강의실 목록 조회

`GET /api/lectures/classrooms`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_6",
  "message": "강의실 목록 조회에 성공했습니다.",
  "data": ["A101", "B201"]
}
```

### 주요 정책

- 삭제되지 않은 강의에 실제로 등록된 `classroomCode`만 중복 없이, 코드순으로 반환합니다.
- 강의 목록 조회(`GET /api/lectures`)의 `classroomCode` 검색 셀렉트박스를 채우는 용도이며, 여기서 받은 코드로 검색하면 항상 결과가 존재함을 보장합니다.

## 9. 강의 시즌 목록 조회

`GET /api/lectures/terms`

권한: `LECTURE:READ` 또는 `LECTURE:MANAGE`

### Request Header

| name | description |
|---|---|
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_7",
  "message": "강의 시즌 목록 조회에 성공했습니다.",
  "data": [
    { "termId": 20, "termName": "2026 1학기" },
    { "termId": 10, "termName": "2026 여름학기" }
  ]
}
```

### 주요 정책

- 다른 셀렉트박스용 API(선생님/과목/교실)와 달리 강의 목록 조회 필터의 `termId`가 문자열이 아니라 id 기반이라, `termId`와 `termName`을 함께 반환합니다.
- 삭제되지 않은 강의에 실제로 연결된 시즌만 중복 없이, 이름순으로 반환합니다.
- 강의 목록 조회(`GET /api/lectures`)의 `termId` 검색 셀렉트박스를 채우는 용도이며, 여기서 받은 `termId`로 검색하면 항상 결과가 존재함을 보장합니다.

## 오류 코드

| code | HTTP | 메시지 |
|---|---:|---|
| `LECTURE_400_1` | 400 | 강의 이름은 비어 있을 수 없습니다. |
| `LECTURE_400_2` | 400 | 요일/시간표는 최소 1개 이상 지정해야 합니다. |
| `LECTURE_400_3` | 400 | 시작 시간은 종료 시간보다 빨라야 합니다. |
| `LECTURE_403_1` | 403 | 다른 학원의 강의에는 접근할 수 없습니다. |
| `LECTURE_404_1` | 404 | 강의를 찾을 수 없습니다. |
| `LECTURE_409_1` | 409 | 같은 교실/요일/시간대에 이미 다른 강의가 있습니다. |
