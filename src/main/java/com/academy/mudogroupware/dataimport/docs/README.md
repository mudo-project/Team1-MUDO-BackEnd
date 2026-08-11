# 데이터 가져오기(dataimport)

기준일: 2026-08-10

## 책임

`dataimport` 패키지는 학원이 서비스를 처음 사용할 때 기존 학생/강의/수강 관계 데이터를 파일로 업로드하고, 서버가 등록 초안을 만든 뒤, 사용자가 검토/수정/제외/확정한 데이터만 실제 도메인에 저장하는 기능을 담당한다.

실제 저장은 `student`, `lecture` 도메인의 공개 UseCase를 통해서만 수행한다. `dataimport`는 학생/강의/수강 Entity 또는 Repository를 직접 참조하지 않는다.

## 전체 흐름

1. 사용자가 학생 정보, 강의 정보, 수강 관계 파일을 업로드한다.
2. 서버가 CSV/XLSX 파일을 읽고, FastAPI AI 분석 엔진 또는 Gemini 헤더 보정으로 학생/강의/수강 등록 후보 초안을 만든다.
3. 프론트는 초안 화면에서 행별로 수정, 제외, 선택 여부를 조정한다.
4. 사용자가 확정하면 선택된 `READY` 행만 실제 학생/강의/수강 데이터로 저장된다.
5. 확정 결과에서 생성된 학생/강의/수강 수와 스킵 수를 확인한다.

## 업로드 파일 역할

| 역할 | Request Part | 내용 |
| --- | --- | --- |
| 학생 정보 | `studentFile` | 이름, 학년, 학교, 연락처, 보호자 연락처, 메모 |
| 강의 정보 | `lectureFile` | 강의명, 학년, 학기, 과목, 강사 ID, 강사명, 교실, 요일, 시간, 수강료 |
| 수강 관계 | `enrollmentFile` | 어떤 학생이 어떤 강의를 듣는지에 대한 연결 정보 |

## 지원 형식

- `.csv`
- `.xlsx`

PDF, 이미지, OCR, 장기 파일 저장은 이번 범위에서 제외한다.

## 프론트 연동 샘플

아래 CSV는 Swagger 또는 프론트 업로드 화면에서 그대로 사용할 수 있다.

| 파일 | 업로드 Part |
| --- | --- |
| [students.csv](./samples/students.csv) | `studentFile` |
| [lectures.csv](./samples/lectures.csv) | `lectureFile` |
| [enrollments.csv](./samples/enrollments.csv) | `enrollmentFile` |

샘플의 `teacherId`는 로컬 DB 상황에 맞게 초안 수정 화면에서 실제 담당자 ID로 바꿔서 확정하는 것을 권장한다. 현재 강의 생성 로직은 `teacherId` 존재 여부를 강제 검증하지 않지만, 실제 사용 화면에서 `teacherName`을 안정적으로 보여주려면 users에 존재하는 담당자 ID를 써야 한다.

## Swagger 검증

Swagger에서 프론트 연동 전 백엔드 흐름을 확인할 때는 [SWAGGER_TEST.md](./SWAGGER_TEST.md)를 따른다.
FastAPI AI 분석 엔진의 1차 판단 근거와 Spring-FastAPI 연동 계약은 [FASTAPI_AI_ENGINE_DESIGN.md](./FASTAPI_AI_ENGINE_DESIGN.md)를 따른다.

검증 전제:

- 로그인 가능한 로컬 계정이 필요하다.
- 해당 계정은 `STUDENT:MANAGE`와 `LECTURE:MANAGE` 권한을 모두 가져야 한다.
- 저장까지 확인하려면 강의 후보의 `teacherId`를 실제 담당자 ID로 맞추는 것이 좋다.

## AI 분석

- 기본 동작은 deterministic parser이다. API 키가 없어도 CSV/XLSX 헤더 alias 기반으로 초안을 만든다.
- `DATA_IMPORT_AI_BASE_URL`이 있으면 Spring이 FastAPI AI 분석 엔진을 먼저 호출한다.
- FastAPI는 파일 원본이 아니라 Spring이 파싱한 role, fileName, headers, rows 구조를 받는다.
- FastAPI는 표준 컬럼명 매핑 또는 row별 정규화 값을 반환한다.
- FastAPI가 꺼져 있거나 실패하면 Spring 내부 Gemini 헤더 보정으로 fallback한다.
- `GEMINI_API_KEY`가 있으면 Spring 내부 Gemini adapter가 파일 헤더와 최대 5개 샘플 행을 보고 표준 컬럼명 매핑을 제안한다.
- FastAPI와 Gemini가 모두 실패해도 기존 parser 결과만 사용해 초안을 만든다.
- AI 결과도 서버의 필수값/상태/수강 관계 검증을 반드시 다시 통과해야 한다.
- FastAPI 요청에는 학생 연락처와 보호자 연락처 같은 개인정보가 포함될 수 있다.
- `http://localhost:8000` 설정은 로컬 개발 전용이다. 운영 FastAPI URL은 HTTPS여야 하며 `DATA_IMPORT_AI_API_KEY`가 필수다.

로컬 환경 변수:

```text
DATA_IMPORT_AI_BASE_URL=http://localhost:8000
DATA_IMPORT_AI_PATH=/api/import/analyze
DATA_IMPORT_AI_API_KEY=선택값
DATA_IMPORT_AI_CONNECT_TIMEOUT_MS=2000
DATA_IMPORT_AI_READ_TIMEOUT_MS=8000
GEMINI_API_KEY=구글 AI Studio에서 발급한 키
GEMINI_MODEL=gemini-2.0-flash
```

운영 환경 변수:

```text
DATA_IMPORT_AI_BASE_URL=https://ai.example.com
DATA_IMPORT_AI_PATH=/api/import/analyze
DATA_IMPORT_AI_API_KEY=필수 공유 키
DATA_IMPORT_AI_CONNECT_TIMEOUT_MS=2000
DATA_IMPORT_AI_READ_TIMEOUT_MS=8000
```

`DATA_IMPORT_AI_BASE_URL`이 없으면 FastAPI 호출은 비활성화된다. `GEMINI_MODEL`은 생략하면 `gemini-2.0-flash`를 사용한다. FastAPI와 Gemini 설정이 모두 없으면 AI 보정 없이 기존 parser로 동작한다.

## FastAPI 1차 연동 판단

1차 구현은 FastAPI를 AI 분석 엔진으로만 사용한다. Spring Boot는 인증/권한, 파일 업로드, 초안 저장, 사용자의 수정/확정, 실제 학생/강의/수강 등록을 계속 담당한다.

현재 타깃은 엑셀로 운영하던 중소~중형 학원의 초기 데이터 이관이다. 따라서 Redis, Kafka, SQS 같은 큐 기반 대용량 비동기 구조는 제외하고 요청-응답 기반 FastAPI 호출만 붙인다.

Spring -> FastAPI 계약:

```text
POST {DATA_IMPORT_AI_BASE_URL}{DATA_IMPORT_AI_PATH}
Header: X-Data-Import-Ai-Key: {DATA_IMPORT_AI_API_KEY} // 운영 필수, localhost 개발은 선택
```

요청 요약:

```json
{"sheets":[{"role":"STUDENT","fileName":"students.csv","headers":["학생명","학년"],"rows":[{"rowNumber":2,"values":{"학생명":"김민수","학년":"고1"}}]}]}
```

응답 요약:

```json
{"sheets":[{"role":"STUDENT","fileName":"students.csv","headerMappings":{"학생명":"name","학년":"grade"},"rows":[{"rowNumber":2,"values":{"name":"김민수","grade":"HIGH_1"}}]}]}
```

FastAPI가 반환한 알 수 없는 컬럼명은 무시한다. 반환된 값도 Spring의 draft builder와 validator를 다시 통과해야 한다.

## 행 상태

| 상태 | 의미 |
| --- | --- |
| `READY` | 바로 저장 가능한 행 |
| `NEEDS_REVIEW` | 일부 값 확인이 필요한 행 |
| `DUPLICATE_SUSPECTED` | 중복 가능성이 있는 행. 현재 자동 중복 판정은 확장 예정 |
| `ERROR` | 필수값 누락 등으로 저장할 수 없는 행 |

## 저장 안전 규칙

- 파일 업로드만으로 실제 학생/강의/수강 데이터가 생성되지 않는다.
- 사용자가 확정해야 실제 저장이 수행된다.
- 사용자가 초안을 수정해 보내면 서버가 필수값, 상태, 수강 관계 연결을 다시 계산해 저장한다.
- 확정 시 선택된 행 중 `READY`가 아닌 행이 있으면 전체 확정이 거절된다.
- 확정 직전에도 서버가 초안을 다시 검증하므로, 프론트가 보낸 `status` 값만으로 저장 가능 여부를 신뢰하지 않는다.
- 수강 관계는 같은 가져오기 작업 안에서 생성된 학생 후보와 강의 후보를 기준으로 연결한다.
- 이미 확정된 가져오기 작업의 초안은 수정할 수 없다.

## 권한

모든 API는 아래 두 권한을 모두 가진 사용자만 호출할 수 있다.

```text
STUDENT:MANAGE
LECTURE:MANAGE
```

## 제외 범위

- PDF/OCR 분석
- 카드 결제, POS, 영수증, 환불
- 업로드 파일 원본 장기 보관
- 기존 DB 데이터와의 정교한 중복 병합

## 문서

- [API.md](./API.md)
