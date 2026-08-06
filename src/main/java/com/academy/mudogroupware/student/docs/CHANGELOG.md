# 학생 관리 Changelog

## 2026-08-06 - 권한 적용 및 결제 기능 범위 확정

- `STUDENT:READ`/`STUDENT:MANAGE`/`ENROLLMENT:MANAGE` 권한 코드를 시드하고(`V1.4.6`) `StudentController`의 모든 엔드포인트에 `@PreAuthorize`를 적용했다.
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
