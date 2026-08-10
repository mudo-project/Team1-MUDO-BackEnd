# 학생 관리 Revision

> 작성일: 2026-08-05
> 상태: 초기 백엔드 구현 완료

## 변경 목적

회의 결과 학생을 별도 탭에서 관리해야 한다는 요구가 추가되었다. 학생은 직원 계정이 아니므로 `users`와 분리된 마스터 데이터로 관리하고, 학생 상세에서 현재 수강 중인 강의를 확인하거나 새 강의에 등록할 수 있어야 한다.

## 확정된 화면 정책

- 학생 관리 화면은 **왼쪽 상세 패널 + 오른쪽 목록 패널** 구조다.
- 오른쪽 목록은 가나다순 행 목록이며 검색을 지원한다.
- 오른쪽 목록에서 학생을 선택하면 페이지 이동 없이 왼쪽 상세가 갱신된다.
- 왼쪽 상세에는 학생 기본 정보와 현재 수강 중인 강의 목록이 표시된다.
- 학생 상세에서 강의를 선택하고 **"결제하기"** 버튼을 누르면 수강 등록이 완료된다.
- 실제 결제, POS/카드 단말기 연동, 환불, 영수증, 수납 관리는 이번 범위에서 제외한다.

## 구현 내용

### Domain

- `Student`: 학생 기본 정보와 학원 스코프를 가진다.
- `Enrollment`: 학생과 강의의 수강 연결을 가진다.
- `EnrollmentStatus`: `ACTIVE`, `ENDED` 상태를 가진다.
- `StudentErrorCode`, `StudentException`: student 전용 오류 코드를 제공한다.

### Application

- `CreateStudentUseCase`: 학생 등록
- `UpdateStudentUseCase`: 학생 수정
- `StudentQueryUseCase`: 학생 목록/상세 조회
- `EnrollStudentUseCase`: 수강 등록
- `EndEnrollmentUseCase`: 수강 종료
- 시간 생성은 `Clock`을 주입받아 처리한다.

### Persistence

- `student` 테이블 추가
- `student_enrollment` 테이블 추가
- 같은 학생과 같은 강의의 중복 수강 등록을 막기 위한 유니크 제약 추가
- 모든 데이터는 `academy_id`로 학원 스코프를 가진다.

### Presentation

- `/api/students` 기준 REST API 추가
- 목록 조회는 `Slice` 기반 페이지네이션 사용
- `size`는 최대 100으로 제한
- Request DTO는 Bean Validation으로 필수값과 길이를 검증

## 의도적으로 제외한 것

- **결제/POS/환불/영수증/미납 관리 — 이번 범위에서 영구 제외 확정(2026-08-06).** "결제하기" 버튼은 앞으로도 수강 등록 확정 동작만 수행한다. 필요해지면 별도 모듈(payment 등)로 완전히 분리해 새로 설계한다.
- 학생 로그인 계정 생성

## 완료된 연동 (갱신: 2026-08-06)

- ~~강의 검색 API의 실제 lecture 모듈 연결~~ → PR #116에서 완료. `LectureCatalogPortAdapter`(lecture)가 `LectureCatalogPort`(student 정의)를 구현.
- ~~rollcall/lecture에서 사용할 student 공개 Port의 실제 코드~~ → `EnrolledStudentsPortAdapter`(student)가 `EnrolledStudentsPort`(lecture 정의)를 구현, rollcall의 `LectureEnrollmentPort`도 이를 재사용.
- ~~users 권한 시드 변경 및 `@PreAuthorize` 적용~~ → 학생관리 업무는 조회/수정/수강 등록을 실제로 같은 담당자가 처리하므로 `STUDENT:MANAGE` 하나로 통합했다. 기존 조회/수강 등록 세부 권한은 실제 API 권한으로 사용하지 않는다. 실제 role 매핑(`role_permission`)은 admin의 role 관리 화면에서 처리한다.

## 남은 연동 작업

- 없음. (담당 선생님 이름 등 users 모듈 직원 검색 Port가 필요한 항목은 student 모듈 범위 밖 — lecture 모듈 문서 참고)

## 검증 기준

- 학생 등록 시 요청자 학원 스코프로 저장된다.
- 학생 목록은 가나다순으로 조회된다.
- 학생 상세에는 현재 수강 중인 강의만 표시된다.
- 같은 학생을 같은 강의에 중복 등록하면 실패한다.
- 다른 학원 학생에 수강 등록을 시도하면 실패한다.
- 시간 생성은 `Clock` 기반으로 테스트 가능하다.
