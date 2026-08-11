# 데이터 가져오기 Swagger 테스트 순서

기준일: 2026-08-11

## 1. 로컬 서버 준비

Docker Compose로 실행하는 경우:

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
```

IDE 또는 Gradle로 직접 실행하는 경우:

```powershell
.\gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger 주소:

```text
Docker Compose: http://localhost:8081/swagger-ui/index.html
직접 실행: http://localhost:8080/swagger-ui/index.html
```

## 2. 로컬 환경 변수

필수:

```text
DB_USERNAME=mudo
DB_PASSWORD=mudo
LOCAL_MYSQL_ROOT_PASSWORD=mudo
```

AI 헤더 매핑 보정 사용 시:

```text
GEMINI_API_KEY=구글 AI Studio에서 발급한 키
GEMINI_MODEL=gemini-2.0-flash
```

`GEMINI_API_KEY`가 없어도 파일 분석은 동작한다. 이 경우 AI 보정 없이 CSV/XLSX 헤더 alias 기반 parser로 초안을 만든다.

## 3. 로그인 계정 준비

이 저장소에는 모든 환경에서 보장되는 더미 로그인 계정이 없다.

Swagger 테스트에는 아래 조건을 만족하는 로컬 계정이 필요하다.

- 로그인 가능
- `STUDENT:MANAGE` 권한 보유
- `LECTURE:MANAGE` 권한 보유

학원 신청 승인 흐름으로 원장 계정을 만들거나, 이미 로컬 DB에 있는 관리자 계정을 사용한다.

## 4. Swagger 인증

1. `POST /api/auth/login`으로 로그인한다.
2. 응답의 access token을 복사한다.
3. Swagger 우측 상단 `Authorize`에 아래 형식으로 입력한다.

```text
Bearer {AccessToken}
```

## 5. 샘플 파일 업로드

`POST /api/data-imports/onboarding/files`

Request Part에 아래 파일을 넣는다.

| Part | 샘플 파일 |
| --- | --- |
| `studentFile` | `src/main/java/com/academy/mudogroupware/dataimport/docs/samples/students.csv` |
| `lectureFile` | `src/main/java/com/academy/mudogroupware/dataimport/docs/samples/lectures.csv` |
| `enrollmentFile` | `src/main/java/com/academy/mudogroupware/dataimport/docs/samples/enrollments.csv` |

성공하면 `data.importId`가 내려온다.

## 6. 초안 조회

`GET /api/data-imports/onboarding/{importId}/draft`

확인할 내용:

- `students[]`, `lectures[]`, `enrollments[]`가 탭별로 내려오는지
- 각 행의 `status`가 `READY`, `NEEDS_REVIEW`, `ERROR` 중 적절히 계산되는지
- 메시지가 있는 행은 프론트에서 수정 대상으로 표시할 수 있는지

## 7. 초안 수정

`PATCH /api/data-imports/onboarding/{importId}/draft`

프론트는 초안 조회 응답의 `data` 구조를 화면에서 수정한 뒤 그대로 전송한다.

권장 확인:

- 제외할 행은 `selected=false`로 보낸다.
- 강의 후보의 `teacherId`는 실제 담당자 사용자 ID로 맞춘다.
- 서버는 프론트가 보낸 `status`, `messages`, `studentRowId`, `lectureRowId`를 그대로 믿지 않고 다시 계산한다.

성공 응답은 `204 No Content`다.

## 8. 확정

`POST /api/data-imports/onboarding/{importId}/confirm`

성공하면 선택된 `READY` 행만 실제 학생/강의/수강 데이터로 저장된다.

성공 응답에서 확인할 값:

- `createdStudents`
- `createdLectures`
- `createdEnrollments`
- `skippedRows`
- `failedRows`

## 9. 결과 조회

`GET /api/data-imports/onboarding/{importId}/result`

확정 이후 저장 결과를 다시 조회한다.

## 10. 실패 흐름 확인

프론트 연동 전 최소 확인할 실패 케이스:

- 파일 없이 업로드하면 `DATA_IMPORT_400_1`
- `.pdf` 등 미지원 파일이면 `DATA_IMPORT_400_2`
- 권한 없는 계정이면 `COMMON_403_1`
- 확정 전 결과 조회는 `DATA_IMPORT_409_5`
- `READY`가 아닌 행을 선택한 채 확정하면 `DATA_IMPORT_409_3`

## 11. FastAPI 연동 판단

현재 dataimport 1차 기능은 Spring Boot 안에서 완료된다.

- 파일 파싱: Spring
- Gemini 헤더 매핑 보정: Spring
- 초안 저장/수정/확정: Spring
- 실제 등록: 기존 student/lecture UseCase

FastAPI는 지금 필수 연결 대상이 아니다. 나중에 PDF/OCR, 자유 형식 문서 해석, 대용량 비동기 분석을 붙일 때 Spring의 AI Port 뒤에 FastAPI Adapter를 추가하는 방식이 적절하다.
