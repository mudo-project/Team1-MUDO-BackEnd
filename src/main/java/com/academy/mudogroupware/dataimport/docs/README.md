# 데이터 가져오기(dataimport)

기준일: 2026-08-10

## 책임

`dataimport` 패키지는 학원이 서비스를 처음 사용할 때 기존 학생/강의/수강 관계 데이터를 파일로 업로드하고, 서버가 등록 초안을 만든 뒤, 사용자가 검토/수정/제외/확정한 데이터만 실제 도메인에 저장하는 기능을 담당한다.

실제 저장은 `student`, `lecture` 도메인의 공개 UseCase를 통해서만 수행한다. `dataimport`는 학생/강의/수강 Entity 또는 Repository를 직접 참조하지 않는다.

## 전체 흐름

1. 사용자가 학생 정보, 강의 정보, 수강 관계 파일을 업로드한다.
2. 서버가 CSV/XLSX 파일을 읽고, Gemini API 키가 있으면 헤더 의미를 보정한 뒤 학생/강의/수강 등록 후보 초안을 만든다.
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

검증 전제:

- 로그인 가능한 로컬 계정이 필요하다.
- 해당 계정은 `STUDENT:MANAGE`와 `LECTURE:MANAGE` 권한을 모두 가져야 한다.
- 저장까지 확인하려면 강의 후보의 `teacherId`를 실제 담당자 ID로 맞추는 것이 좋다.

## AI 분석

- 기본 동작은 deterministic parser이다. API 키가 없어도 CSV/XLSX 헤더 alias 기반으로 초안을 만든다.
- `GEMINI_API_KEY`가 있으면 Gemini가 파일 헤더와 샘플 행을 보고 표준 컬럼명 매핑을 제안한다.
- Gemini에는 전체 파일이 아니라 헤더와 최대 5개 샘플 행만 전달한다.
- Gemini 호출 실패, 빈 응답, 파싱 실패가 발생하면 기존 parser 결과만 사용한다.
- AI 결과도 서버의 필수값/상태/수강 관계 검증을 반드시 다시 통과해야 한다.

로컬 환경 변수:

```text
GEMINI_API_KEY=구글 AI Studio에서 발급한 키
GEMINI_MODEL=gemini-2.0-flash
```

`GEMINI_MODEL`은 생략하면 `gemini-2.0-flash`를 사용한다. API 키가 없으면 AI 보정 없이 기존 parser로 동작한다.

## FastAPI 연동 판단

1차 구현은 Spring Boot 내부에서 CSV/XLSX 파싱과 Gemini 헤더 매핑 보정을 처리한다. 따라서 현재 Swagger 검증과 프론트 연동에는 별도 FastAPI 서버가 필요하지 않다.

FastAPI는 아래 범위로 확장할 때 별도 AI 서비스로 분리하는 것이 적절하다.

- PDF, 이미지, OCR 기반 데이터 추출
- 자유 형식 문서 전체 해석
- 대용량 파일 비동기 분석
- Python 전용 라이브러리가 필요한 정교한 정제/추천 로직

분리할 경우에도 프론트는 Spring API만 호출하고, Spring이 `dataimport` Port 뒤에서 FastAPI를 호출하는 구조를 유지한다.

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
