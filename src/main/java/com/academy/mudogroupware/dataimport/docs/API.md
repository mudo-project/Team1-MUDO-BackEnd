# 데이터 가져오기 API

기준일: 2026-08-10

> 문서 기준: Spring Boot Controller, Request/Response DTO, ErrorCode 구현 기준으로 작성한다.  
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다.  
> 모든 API는 `STUDENT:MANAGE`와 `LECTURE:MANAGE` 권한이 모두 필요하다.

## 권한 정책

| 권한 코드 | 적용 기능 |
| --- | --- |
| `STUDENT:MANAGE` + `LECTURE:MANAGE` | 초기 데이터 파일 업로드, 초안 조회, 초안 수정, 확정, 결과 조회 |

---

## **1. 초기 데이터 파일 업로드**

`POST /api/data-imports/onboarding/files`

학생/강의/수강 관계 파일을 업로드해 등록 초안을 생성한다. 파일 업로드만으로 실제 학생/강의/수강 데이터가 저장되지는 않는다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |
| `Content-Type` | `multipart/form-data` |

Request Part

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `studentFile` | `MultipartFile` | `false` | 학생 정보 파일입니다. 이름, 학년, 학교, 연락처, 보호자 연락처, 메모 등을 포함합니다. |
| `lectureFile` | `MultipartFile` | `false` | 강의 정보 파일입니다. 강의명, 학년, 학기, 과목, 강사 ID, 교실, 요일, 시간, 수강료 등을 포함합니다. |
| `enrollmentFile` | `MultipartFile` | `false` | 수강 관계 파일입니다. 어떤 학생이 어떤 강의를 듣는지 연결 정보를 포함합니다. |

파일은 최소 1개 이상 업로드해야 한다. 지원 확장자는 `.csv`, `.xlsx`이다. 서버에 `DATA_IMPORT_AI_BASE_URL`이 있으면 FastAPI AI 분석 엔진을 먼저 호출하고, 실패 시 Spring 내부 Gemini 보정 또는 기존 parser 결과로 초안을 생성한다. 프론트는 FastAPI를 직접 호출하지 않는다.

프론트 연동 샘플:

| **업로드 Part** | **샘플 파일** |
| --- | --- |
| `studentFile` | `dataimport/docs/samples/students.csv` |
| `lectureFile` | `dataimport/docs/samples/lectures.csv` |
| `enrollmentFile` | `dataimport/docs/samples/enrollments.csv` |

샘플의 `teacherId`는 로컬 DB 상황에 맞게 초안 수정 단계에서 실제 담당자 사용자 ID로 바꿔서 확정하는 것을 권장한다.

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 가져오기 작업 생성 성공 |

Response Body

```json
{"status":201,"code":"DATA_IMPORT_201_1","message":"가져오기 작업 생성에 성공했습니다.","data":{"importId":1}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.importId` | 생성된 가져오기 작업 ID입니다. 이후 초안 조회/수정/확정에 사용합니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `DATA_IMPORT_400_1` | 업로드할 파일이 필요합니다. | 업로드된 파일이 하나도 없는 경우 |
| `400 Bad Request` | `DATA_IMPORT_400_2` | 지원하지 않는 파일 형식입니다. | `.csv`, `.xlsx`가 아닌 파일을 업로드한 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | `STUDENT:MANAGE`, `LECTURE:MANAGE` 중 하나라도 없는 경우 |
| `409 Conflict` | `DATA_IMPORT_409_1` | 분석된 등록 후보가 없습니다. | 파일은 읽혔지만 등록 후보 행이 없는 경우 |

---

## **2. 가져오기 초안 조회**

`GET /api/data-imports/onboarding/{importId}/draft`

업로드 분석 결과로 생성된 학생/강의/수강 등록 후보를 조회한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `importId` | 조회할 가져오기 작업 ID입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 가져오기 초안 조회 성공 |

Response Body

```json
{"status":200,"code":"DATA_IMPORT_200_1","message":"가져오기 초안 조회에 성공했습니다.","data":{"students":[{"rowId":"S1","selected":true,"status":"READY","name":"김민수","grade":"HIGH_1","school":"무도고","phone":"010-1111-2222","parentPhone":"010-3333-4444","note":"수학 선행","messages":[]}],"lectures":[{"rowId":"L1","selected":true,"status":"READY","name":"고1 수학","grade":"HIGH_1","termName":"2026 여름","subjectName":"수학","teacherId":10,"teacherName":"박선생","classroomName":"A101","feeType":"PER_MONTH","feeAmount":300000,"schedules":[{"dayOfWeek":"MONDAY","startTime":"19:00:00","endTime":"21:00:00"}],"messages":[]}],"enrollments":[{"rowId":"E1","selected":true,"status":"READY","studentRowId":"S1","lectureRowId":"L1","studentName":"김민수","studentPhone":"010-1111-2222","lectureName":"고1 수학","teacherName":"박선생","messages":[]}]}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.students[]` | 학생 등록 후보 목록입니다. |
| `data.students[].rowId` | 가져오기 작업 안에서 쓰는 학생 후보 행 ID입니다. |
| `data.students[].selected` | 확정 시 저장 대상으로 선택됐는지 여부입니다. |
| `data.students[].status` | 행 상태입니다. `READY`, `NEEDS_REVIEW`, `DUPLICATE_SUSPECTED`, `ERROR` 중 하나입니다. |
| `data.students[].name` | 학생 이름입니다. |
| `data.students[].grade` | 학생 학년입니다. |
| `data.students[].school` | 학교명입니다. |
| `data.students[].phone` | 학생 연락처입니다. |
| `data.students[].parentPhone` | 보호자 연락처입니다. |
| `data.students[].note` | 학생 메모입니다. |
| `data.students[].messages[]` | 검토/오류 메시지입니다. |
| `data.lectures[]` | 강의 등록 후보 목록입니다. |
| `data.lectures[].rowId` | 가져오기 작업 안에서 쓰는 강의 후보 행 ID입니다. |
| `data.lectures[].selected` | 확정 시 저장 대상으로 선택됐는지 여부입니다. |
| `data.lectures[].status` | 행 상태입니다. |
| `data.lectures[].name` | 강의명입니다. |
| `data.lectures[].grade` | 강의 대상 학년입니다. |
| `data.lectures[].termName` | 학기/시즌 이름입니다. |
| `data.lectures[].subjectName` | 과목명입니다. |
| `data.lectures[].teacherId` | 담당 강사 사용자 ID입니다. |
| `data.lectures[].teacherName` | 파일에서 읽은 담당 강사 이름입니다. |
| `data.lectures[].classroomName` | 교실명입니다. |
| `data.lectures[].feeType` | 수강료 구분입니다. `PER_SESSION`, `PER_MONTH` 중 하나입니다. |
| `data.lectures[].feeAmount` | 수강료 금액입니다. |
| `data.lectures[].schedules[].dayOfWeek` | 요일입니다. |
| `data.lectures[].schedules[].startTime` | 강의 시작 시간입니다. |
| `data.lectures[].schedules[].endTime` | 강의 종료 시간입니다. |
| `data.lectures[].messages[]` | 검토/오류 메시지입니다. |
| `data.enrollments[]` | 수강 관계 후보 목록입니다. |
| `data.enrollments[].rowId` | 가져오기 작업 안에서 쓰는 수강 관계 후보 행 ID입니다. |
| `data.enrollments[].selected` | 확정 시 저장 대상으로 선택됐는지 여부입니다. |
| `data.enrollments[].status` | 행 상태입니다. |
| `data.enrollments[].studentRowId` | 연결된 학생 후보 행 ID입니다. |
| `data.enrollments[].lectureRowId` | 연결된 강의 후보 행 ID입니다. |
| `data.enrollments[].studentName` | 파일에서 읽은 학생명입니다. |
| `data.enrollments[].studentPhone` | 파일에서 읽은 학생 연락처입니다. |
| `data.enrollments[].lectureName` | 파일에서 읽은 강의명입니다. |
| `data.enrollments[].teacherName` | 파일에서 읽은 강사명입니다. |
| `data.enrollments[].messages[]` | 검토/오류 메시지입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | 필요한 권한이 없는 경우 |
| `403 Forbidden` | `DATA_IMPORT_403_1` | 해당 가져오기 작업에 접근할 권한이 없습니다. | 다른 학원의 가져오기 작업을 조회한 경우 |
| `404 Not Found` | `DATA_IMPORT_404_1` | 가져오기 작업을 찾을 수 없습니다. | `importId`에 해당하는 작업이 없는 경우 |

---

## **3. 가져오기 초안 수정**

`PATCH /api/data-imports/onboarding/{importId}/draft`

검토 화면에서 사용자가 수정/제외한 초안을 전체 교체한다. 프론트는 조회한 초안과 같은 구조로 수정된 전체 초안을 전송한다.
서버는 요청으로 받은 `status`, `messages`, `studentRowId`, `lectureRowId`를 그대로 신뢰하지 않고 필수값과 수강 관계 연결을 다시 계산해 저장한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `importId` | 수정할 가져오기 작업 ID입니다. |

Request Body

```json
{"students":[{"rowId":"S1","selected":true,"status":"READY","name":"김민수","grade":"HIGH_1","school":"무도고","phone":"010-1111-2222","parentPhone":"010-3333-4444","note":"수학 선행","messages":[]}],"lectures":[{"rowId":"L1","selected":true,"status":"READY","name":"고1 수학","grade":"HIGH_1","termName":"2026 여름","subjectName":"수학","teacherId":10,"teacherName":"박선생","classroomName":"A101","feeType":"PER_MONTH","feeAmount":300000,"schedules":[{"dayOfWeek":"MONDAY","startTime":"19:00:00","endTime":"21:00:00"}],"messages":[]}],"enrollments":[{"rowId":"E1","selected":true,"status":"READY","studentRowId":"S1","lectureRowId":"L1","studentName":"김민수","studentPhone":"010-1111-2222","lectureName":"고1 수학","teacherName":"박선생","messages":[]}]}
```

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `students` | `List` | `false` | 학생 등록 후보 목록입니다. |
| `lectures` | `List` | `false` | 강의 등록 후보 목록입니다. |
| `enrollments` | `List` | `false` | 수강 관계 후보 목록입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 가져오기 초안 수정 성공 |

Response Body

없음

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | 필요한 권한이 없는 경우 |
| `403 Forbidden` | `DATA_IMPORT_403_1` | 해당 가져오기 작업에 접근할 권한이 없습니다. | 다른 학원의 가져오기 작업을 수정한 경우 |
| `404 Not Found` | `DATA_IMPORT_404_1` | 가져오기 작업을 찾을 수 없습니다. | `importId`에 해당하는 작업이 없는 경우 |
| `409 Conflict` | `DATA_IMPORT_409_2` | 이미 확정된 가져오기 작업입니다. | 이미 확정된 작업의 초안을 수정한 경우 |

---

## **4. 가져오기 확정**

`POST /api/data-imports/onboarding/{importId}/confirm`

선택된 `READY` 후보만 실제 학생/강의/수강 데이터로 저장한다.
확정 직전에도 서버가 초안을 다시 검증한다. 선택되어 있던 행이 재검증 결과 `READY`가 아니면 전체 확정이 거절된다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `importId` | 확정할 가져오기 작업 ID입니다. |

Request Body

없음

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 가져오기 확정 성공 |

Response Body

```json
{"status":200,"code":"DATA_IMPORT_200_3","message":"가져오기 확정에 성공했습니다.","data":{"createdStudents":1,"createdLectures":1,"createdEnrollments":1,"skippedRows":0,"failedRows":0}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.createdStudents` | 실제 생성된 학생 수입니다. |
| `data.createdLectures` | 실제 생성된 강의 수입니다. |
| `data.createdEnrollments` | 실제 생성된 수강 등록 수입니다. |
| `data.skippedRows` | 선택되지 않아 저장하지 않은 후보 행 수입니다. |
| `data.failedRows` | 확정 처리 중 실패한 행 수입니다. 현재 구현은 실패 시 전체 요청을 예외 처리하므로 정상 응답에서는 `0`입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | 필요한 권한이 없는 경우 |
| `403 Forbidden` | `DATA_IMPORT_403_1` | 해당 가져오기 작업에 접근할 권한이 없습니다. | 다른 학원의 가져오기 작업을 확정한 경우 |
| `404 Not Found` | `DATA_IMPORT_404_1` | 가져오기 작업을 찾을 수 없습니다. | `importId`에 해당하는 작업이 없는 경우 |
| `409 Conflict` | `DATA_IMPORT_409_2` | 이미 확정된 가져오기 작업입니다. | 이미 확정된 작업을 다시 확정한 경우 |
| `409 Conflict` | `DATA_IMPORT_409_3` | 등록 가능한 상태가 아닌 행이 선택되어 있습니다. | 선택된 후보 중 `READY`가 아닌 행이 있는 경우 |
| `409 Conflict` | `DATA_IMPORT_409_4` | 수강 등록에 필요한 학생 또는 강의 후보를 찾을 수 없습니다. | 수강 관계 후보가 연결할 학생/강의 후보를 찾지 못한 경우 |

---

## **5. 가져오기 결과 조회**

`GET /api/data-imports/onboarding/{importId}/result`

확정 저장 결과를 조회한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `importId` | 결과를 조회할 가져오기 작업 ID입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 가져오기 결과 조회 성공 |

Response Body

```json
{"status":200,"code":"DATA_IMPORT_200_4","message":"가져오기 결과 조회에 성공했습니다.","data":{"createdStudents":1,"createdLectures":1,"createdEnrollments":1,"skippedRows":0,"failedRows":0}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.createdStudents` | 실제 생성된 학생 수입니다. |
| `data.createdLectures` | 실제 생성된 강의 수입니다. |
| `data.createdEnrollments` | 실제 생성된 수강 등록 수입니다. |
| `data.skippedRows` | 선택되지 않아 저장하지 않은 후보 행 수입니다. |
| `data.failedRows` | 확정 처리 중 실패한 행 수입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | 필요한 권한이 없는 경우 |
| `403 Forbidden` | `DATA_IMPORT_403_1` | 해당 가져오기 작업에 접근할 권한이 없습니다. | 다른 학원의 가져오기 작업 결과를 조회한 경우 |
| `404 Not Found` | `DATA_IMPORT_404_1` | 가져오기 작업을 찾을 수 없습니다. | `importId`에 해당하는 작업이 없는 경우 |
| `409 Conflict` | `DATA_IMPORT_409_5` | 아직 확정 결과가 없습니다. | 아직 확정하지 않은 작업의 결과를 조회한 경우 |

---

## 참고 상태값

| 상태값 | 설명 |
| --- | --- |
| `ImportRowStatus.READY` | 바로 저장 가능한 후보 행입니다. |
| `ImportRowStatus.NEEDS_REVIEW` | 사용자가 값을 확인하거나 수정해야 하는 후보 행입니다. |
| `ImportRowStatus.DUPLICATE_SUSPECTED` | 중복 가능성이 있는 후보 행입니다. |
| `ImportRowStatus.ERROR` | 필수값 누락 등으로 저장할 수 없는 후보 행입니다. |
| `FeeType.PER_SESSION` | 회차별 수강료입니다. |
| `FeeType.PER_MONTH` | 월별 수강료입니다. |

## 오류 코드 전체 목록

| code | HTTP | 메시지 |
| --- | --- | --- |
| `DATA_IMPORT_400_1` | 400 | 업로드할 파일이 필요합니다. |
| `DATA_IMPORT_400_2` | 400 | 지원하지 않는 파일 형식입니다. |
| `DATA_IMPORT_403_1` | 403 | 해당 가져오기 작업에 접근할 권한이 없습니다. |
| `DATA_IMPORT_404_1` | 404 | 가져오기 작업을 찾을 수 없습니다. |
| `DATA_IMPORT_409_1` | 409 | 분석된 등록 후보가 없습니다. |
| `DATA_IMPORT_409_2` | 409 | 이미 확정된 가져오기 작업입니다. |
| `DATA_IMPORT_409_3` | 409 | 등록 가능한 상태가 아닌 행이 선택되어 있습니다. |
| `DATA_IMPORT_409_4` | 409 | 수강 등록에 필요한 학생 또는 강의 후보를 찾을 수 없습니다. |
| `DATA_IMPORT_409_5` | 409 | 아직 확정 결과가 없습니다. |
