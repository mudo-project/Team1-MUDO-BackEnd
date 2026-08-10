# AI Onboarding Data Import Design

## Goal

학원이 기존에 쓰던 학생/강의/수강 관계 엑셀 또는 CSV 파일을 업로드하면, 백엔드가 등록 후보 초안을 만들고 사용자가 검토/수정/제외한 뒤 한 번에 실제 데이터로 저장한다.

핵심 원칙은 AI가 직접 저장하지 않는 것이다. AI와 파서는 초안을 만들고, 실제 저장은 사용자가 확정한 데이터에만 수행한다.

## Scope

이번 범위에 포함한다.

- 학생 정보 파일 업로드
- 강의 정보 파일 업로드
- 수강 관계 파일 업로드
- Excel `.xlsx`와 CSV 파일 파싱
- 업로드 파일 역할을 힌트로 사용한 초안 생성
- 학생/강의/수강 등록 후보 조회
- 사용자가 수정한 초안 전체 반영
- 선택된 후보만 실제 저장
- 중복/필수값 누락/확인 필요 상태 표시
- import 작업 결과 조회

이번 범위에서 제외한다.

- PDF, 이미지, OCR
- AI가 곧바로 DB에 저장하는 자동 등록
- 카드 결제, POS, 영수증, 환불
- 파일 원본 장기 보관
- 운영 서버의 실제 Gemini API 키 주입 자동화

## Package

새 패키지 `dataimport`를 둔다.

이 패키지는 초기 데이터 이관 흐름을 조립하는 오케스트레이션 도메인이다. 학생, 강의, 수강 데이터를 직접 소유하지 않고, 최종 확정 시 기존 공개 UseCase만 호출한다.

```text
com.academy.mudogroupware.dataimport
├─ presentation.api
├─ application
├─ domain
└─ infrastructure
```

`dataimport`는 `student`와 `lecture`의 JPA Entity/Repository를 직접 참조하지 않는다.

## Upload Model

프론트는 한 화면에서 세 종류 파일을 각각 업로드한다.

| file role | required | description |
| --- | --- | --- |
| `STUDENT` | false | 학생 이름, 학년, 학교, 연락처, 보호자 연락처, 메모 |
| `LECTURE` | false | 강의명, 강사, 과목, 학년, 요일, 시간, 수강료 |
| `ENROLLMENT` | false | 어떤 학생이 어떤 강의를 듣는지에 대한 관계 |

최소 1개 이상의 파일은 필요하다. 파일 역할은 AI와 파서에게 강한 힌트로 전달한다.

## API Flow

```text
POST /api/data-imports/onboarding/files
  -> 파일을 분석하고 import job 생성

GET /api/data-imports/onboarding/{importId}/draft
  -> 학생/강의/수강 등록 후보와 행별 상태 조회

PATCH /api/data-imports/onboarding/{importId}/draft
  -> 사용자가 화면에서 수정/제외한 초안 전체 반영

POST /api/data-imports/onboarding/{importId}/confirm
  -> 선택된 후보만 실제 학생/강의/수강 데이터로 저장

GET /api/data-imports/onboarding/{importId}/result
  -> 저장 결과 조회
```

## Draft Shape

초안은 세 탭으로 나뉜다.

### Student Candidates

- rowId
- selected
- status: `READY`, `NEEDS_REVIEW`, `DUPLICATE_SUSPECTED`, `ERROR`
- name
- grade
- school
- phone
- parentPhone
- note
- messages

### Lecture Candidates

- rowId
- selected
- status
- name
- teacherName
- subject
- grade
- classroom
- schedules
- feeType: `MONTHLY` 또는 `PER_SESSION`
- feeAmount
- messages

### Enrollment Candidates

- rowId
- selected
- status
- studentName
- studentPhone
- lectureName
- teacherName
- messages

수강 관계는 최종 저장 시 같은 import 안에서 생성된 학생/강의 후보와 기존 데이터 후보를 이름/연락처/강의명 기준으로 매칭한다. 매칭이 모호하면 저장하지 않고 `NEEDS_REVIEW` 또는 `ERROR`로 남긴다.

## Validation

학생 필수값:

- 이름
- 학년

강의 필수값:

- 강의명
- 1개 이상의 일정

수강 관계 필수값:

- 학생 식별 정보: 이름 또는 이름+연락처
- 강의 식별 정보: 강의명

중복 판단:

- 학생: 같은 학원 안에서 이름+연락처가 같으면 중복 의심
- 강의: 같은 학원 안에서 강의명+강사명 또는 강의명+시간표가 같으면 중복 의심
- 수강: 같은 학생이 같은 강의에 이미 ACTIVE 수강 중이면 오류

## AI Strategy

1차 구현은 deterministic parser를 기본으로 둔다.

- `.xlsx`: Apache POI로 첫 번째 sheet를 읽는다.
- `.csv`: UTF-8 우선으로 읽는다.
- 헤더 alias를 기준으로 필드를 매핑한다.
- 업로드 role을 사용해 어떤 후보로 해석할지 결정한다.

AI adapter는 `ImportAnalysisPort` 뒤에 둔다.

- Gemini API 키가 있으면 헤더 매핑, 컬럼 의미 추론, 값 정규화에 사용할 수 있다.
- API 키가 없거나 호출 실패하면 deterministic parser 결과만 사용한다.
- AI 결과도 반드시 백엔드 검증을 통과해야 한다.
- AI 결과가 검증을 우회할 수 없다.

## Persistence

새 테이블을 둔다.

```text
data_import_job
- id
- academy_id
- created_by
- status
- source_file_names
- draft_json
- result_json
- created_at
- updated_at
```

상태:

- `DRAFT`
- `CONFIRMED`
- `FAILED`

파일 원본은 저장하지 않는다. 업로드 요청 안에서 파싱하고, 원본 파일명과 초안 JSON만 저장한다.

## Confirm Behavior

확정 저장은 하나의 트랜잭션에서 수행한다.

순서:

1. 선택된 학생 후보 저장
2. 선택된 강의 후보 저장
3. 선택된 수강 관계 후보 저장
4. 결과 JSON 저장
5. import job 상태를 `CONFIRMED`로 변경

저장 중 하나라도 실패하면 전체 트랜잭션을 롤백한다.

## Permission

이 기능은 초기 학원 데이터 이관 성격이므로 강한 권한이 필요하다.

MVP 권한 조건:

```text
hasAuthority('STUDENT:MANAGE') and hasAuthority('LECTURE:MANAGE')
```

추후 운영성이 필요하면 별도 권한 `DATA_IMPORT:MANAGE`로 분리할 수 있다. 이번 구현에서는 새 권한을 늘리지 않고 기존 권한 조합을 사용한다.

## Error Handling

- 파일이 하나도 없으면 `400 Bad Request`
- 지원하지 않는 확장자면 `400 Bad Request`
- 분석 결과가 모두 비어 있으면 `409 Conflict`
- 없는 import job이면 `404 Not Found`
- 다른 학원의 import job 접근이면 `403 Forbidden`
- 이미 확정된 import job을 다시 수정/확정하면 `409 Conflict`

## Frontend Contract

프론트 화면은 2개 화면과 완료 결과 모달로 충분하다.

1. 파일 업로드 화면
   - 학생 정보 파일
   - 강의 정보 파일
   - 수강 관계 파일

2. AI 분석 결과 검토 화면
   - 탭: 학생 / 강의 / 수강 등록
   - 행 단위 수정
   - 행 제외 체크
   - 상태 배지: 등록 가능 / 확인 필요 / 중복 의심 / 오류
   - 문제 있는 행만 보기

3. 등록 완료 모달
   - 생성된 학생 수
   - 생성된 강의 수
   - 생성된 수강 등록 수
   - 실패/제외 행 수

## Tests

필수 테스트:

- 파일 역할이 없거나 파일이 없으면 실패
- 학생 파일에서 학생 후보 생성
- 강의 파일에서 강의 후보 생성
- 수강 관계 파일에서 수강 후보 생성
- 필수값 누락 행은 `ERROR`
- 중복 의심 행은 `DUPLICATE_SUSPECTED`
- 사용자가 제외한 행은 저장하지 않음
- 확정 시 기존 student/lecture use case 호출
- 이미 확정된 import job은 다시 확정할 수 없음
- 다른 학원 import job 접근 차단

## Implementation Notes

- 새 기능 파일은 기존 student retention 작업 파일을 건드리지 않고 추가한다.
- `dataimport`는 cross-domain orchestration만 맡는다.
- 학생/강의/수강의 실제 규칙은 기존 student/lecture 도메인 use case에 맡긴다.
- API 문서는 구현 후 `dataimport/docs/API.md`에 Notion 복사용 형식으로 작성한다.
