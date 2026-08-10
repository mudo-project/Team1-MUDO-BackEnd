# 학생 관리 Changelog

## 2026-08-10 - 소프트 삭제된 학생 자동 하드 삭제 배치 추가

- `global`의 Global Retention Scheduler 보일러플레이트([global/docs/BOILER_PLATE.md](../../global/docs/BOILER_PLATE.md))를 처음으로 실제 구현했다. `RetentionJob`/`RetentionJobResult`/`GlobalRetentionScheduler`가 `global.scheduler`에 새로 생겼다.
- `StudentRetentionJob`/`StudentRetentionService`/`StudentRetentionProperties`/`StudentRetentionPort`/`StudentRetentionAdapter`를 추가했다. 소프트 삭제(`deleted_at`) 후 30일이 지난 학생을 매일 03:00(KST) 배치로 하드 삭제한다.
- 삭제 순서는 수강 이력(자식) → 학생(부모)이며, 삭제 시점에 `deleted_at < threshold` 조건을 다시 검사해 후보 조회 이후 상태가 바뀐 경우를 방어한다.
- 단위 테스트(서비스 로직, 자식→부모 삭제 순서), Global Scheduler 테스트(전체 Job 실행/실패 격리/빈 목록), 실제 H2 DB 기반 DataJpaTest(후보 조회 필터링/배치 크기 제한/실제 삭제)를 추가했다.

## 2026-08-08 - 학생관리 권한 단순화

- 학생관리 탭 접근, 목록/상세 조회, 학생 등록/수정, 수강 등록/종료를 `STUDENT:MANAGE` 하나로 통합했다.
- 기존 조회/수강 등록 세부 권한은 실제 API에서 사용하지 않도록 정리했다.

## 2026-08-08 - 학생 삭제(소프트 삭제) 추가

- `DELETE /api/students/{studentId}`를 추가했다. `student` 테이블에 `deleted_at` 컬럼을 추가하고(`V1.5.3`) `SoftDeleteTimeEntity`를 상속해 소프트 삭제로 구현했다.
- 삭제된 학생은 목록/상세 조회와 `findById` 기반 조회(수정 등)에서 제외된다.
- 수강 이력(`student_enrollment`)은 삭제하지 않아 데이터 유실이 없다.

## 2026-08-06 - 권한 적용 및 결제 기능 범위 확정

- 학생관리 권한 코드를 시드하고(`V1.4.6`) `StudentController`의 모든 엔드포인트에 `@PreAuthorize`를 적용했다.
- 결제/POS/환불/영수증/미납 관리는 이번 범위에서 영구 제외로 확정했다(추가 구현 없음).

## 2026-08-05 - 학생 관리 백엔드 초기 구현

- 학생 기본 정보 등록/수정/목록/상세 조회 API를 추가했다.
- 학생 상세에서 현재 수강 중인 강의 목록을 조회할 수 있게 했다.
- 학생을 강의에 등록하는 수강 등록 API를 추가했다.
- 실제 결제 없이 "결제하기" 버튼 흐름에서 수강 등록만 확정하도록 범위를 제한했다.
- 수강 종료 API를 추가해 `ACTIVE` 수강 등록을 `ENDED`로 변경할 수 있게 했다.
- `student`, `student_enrollment` 테이블 마이그레이션을 추가했다.
- 학생 데이터와 수강 등록 데이터에 `academy_id` 스코프를 적용했다.
- 목록 조회는 `Slice` 기반으로 구현하고 `size` 최대값을 100으로 제한했다.
- 생성/수정/수강 등록/수강 종료 시각은 `Clock` 기반으로 처리했다.
- student 전용 ErrorCode와 BusinessException을 추가했다.
- 학생 서비스 단위 테스트를 추가했다.

자세한 설계 배경은 [REVISION.md](REVISION.md), API 기준은 [API.md](API.md)를 참고한다.
