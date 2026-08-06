# 강의 관리 API

기준일: 2026-08-06

## 1. 강의 등록

`POST /api/lectures`

권한: `LECTURE:MANAGE`

### Request

```json
{
  "name": "고1 수학 정규반",
  "grade": "HIGH_1",
  "termName": "2026 1학기",
  "subjectName": "수학",
  "teacherId": 12,
  "classroomName": "A101",
  "feeType": "PER_MONTH",
  "feeAmount": 300000,
  "schedules": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "19:00:00",
      "endTime": "21:00:00"
    }
  ]
}
```

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

`GET /api/lectures?termId=&grade=&subjectId=&teacherId=&classroomId=&dayOfWeek=&page=0&size=20`

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
        "grade": "HIGH_1",
        "termName": "2026 1학기",
        "subjectName": "수학",
        "teacherId": 12,
        "teacherName": "김선생",
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

### Response

```json
{
  "status": 200,
  "code": "LECTURE_200_2",
  "message": "강의 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "name": "고1 수학 정규반",
    "grade": "HIGH_1",
    "termName": "2026 1학기",
    "subjectName": "수학",
    "teacherId": 12,
    "teacherName": "김선생",
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

## 오류 코드

| code | HTTP | 메시지 |
|---|---:|---|
| `LECTURE_400_1` | 400 | 강의 이름은 비어 있을 수 없습니다. |
| `LECTURE_400_2` | 400 | 요일/시간표는 최소 1개 이상 지정해야 합니다. |
| `LECTURE_400_3` | 400 | 시작 시간은 종료 시간보다 빨라야 합니다. |
| `LECTURE_403_1` | 403 | 다른 학원의 강의에는 접근할 수 없습니다. |
| `LECTURE_404_1` | 404 | 강의를 찾을 수 없습니다. |
| `LECTURE_409_1` | 409 | 같은 교실/요일/시간대에 이미 다른 강의가 있습니다. |
