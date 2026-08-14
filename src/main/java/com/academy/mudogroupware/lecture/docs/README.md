# lecture 모듈

강의 등록/조회/수정/삭제 백엔드 구현 기준 문서다. 강의는 학원 단위의 강의 종류, 학년, 시수, 과목, 담당 선생님, 교실, 수강료, 요일/시간표를 관리한다.

## 책임과 범위

- `Lecture`: 강의 본체. `classType`, `classroomCode`, `teacherName`, `subjectName`을 저장하며, 기존 연동용 `teacherId` 등 ID 필드는 선택값으로 유지한다.
- `Term`: 시수/학기/기간 이름. 강의 등록 시 `termName`이 있으면 생성/재사용한다.
- `Subject`: 과목 이름. 강의 등록 시 `subjectName`이 있으면 생성/재사용한다.
- `Classroom`: 교실 이름. 별도 교실관리 탭 없이 강의 등록 흐름에서 `classroomCode` 기준으로 자동 생성/재사용한다.
- `LectureSchedule`: 강의의 요일/시간 범위.
- `feeType`, `feeAmount`: 회차별/월별 수강료 정책.

## 공개 UseCase

- `CreateLectureUseCase`: 강의 등록.
- `UpdateLectureUseCase`: 강의 기본 정보와 요일/시간대 수정.
- `DeleteLectureUseCase`: 강의 삭제.
- `LectureQueryUseCase`: 강의 목록/상세 조회.

## 기능 명세

- 강의 등록: 권한이 있는 직원이 강의명, 강의 유형, 요일, 시간, 교실, 선택 정보(학년/시수/과목/담당 선생님/수강료)를 입력해 강의를 생성한다. 시즌·과목·교실 이름이 기존에 없으면 등록 흐름에서 자동 생성한다.
- 강의 수정: 권한이 있는 직원이 기존 강의의 강의명, 강의 유형, 학년, 시즌, 과목, 교실, 담당 선생님, 수강료, 요일/시간대를 수정한다. 수정 시 자기 자신을 제외한 다른 강의와 같은 교실·요일·시간대가 겹치면 저장하지 않는다.
- 강의 삭제: 권한이 있는 직원이 더 이상 운영하지 않는 강의를 삭제한다. 삭제된 강의는 실제 행을 즉시 지우지 않고 `deletedAt`으로 소프트 삭제하며, 목록/상세 조회와 시간 충돌 검사 대상에서 제외한다.

## 다른 모듈 연동

- student: `EnrolledStudentsPort`로 강의 상세의 수강 학생 목록을 조회한다.
- users: `TeacherDirectoryPort`를 users 모듈의 `LectureTeacherDirectoryAdapter`가 구현한다. lecture는 users Entity/Repository를 직접 보지 않고, 저장된 `teacherName`이 없고 `teacherId`가 있을 때만 `teacherId -> teacherName`을 fallback 조회한다.

## API 응답 상태

- 강의 목록/상세 응답은 `classType`, `classroomCode`, `teacherId`, `teacherName`을 함께 내려준다.
- 선생님이 다른 학원 소속이거나 존재하지 않으면 `teacherName`은 `null`이 될 수 있다. 강의 생성 단계의 활성 사용자 검증은 별도 정책이 확정되면 추가한다.
- 학생 목록은 student 모듈의 수강 등록 데이터가 기준이다.

## 권한 정책

- `LECTURE:READ`: 강의관리 탭 접근, 강의 목록/상세 조회.
- `LECTURE:MANAGE`: 강의 등록, 강의 수정, 강의 삭제, 강의 가격 설정. 강의 목록/상세 조회도 함께 허용한다.

## 주의사항

- 같은 `classroomCode`, 같은 요일, 겹치는 시간대에 이미 강의가 있으면 등록을 막는다.
- 수정 시에는 같은 충돌 규칙을 적용하되 현재 수정 중인 강의는 충돌 대상에서 제외한다.
- 삭제된 강의는 `deleted_at is null` 조건으로 조회/충돌 검사에서 제외한다. `SoftDeleteTimeEntity`는 자동 필터를 걸지 않으므로 Repository 쿼리에 조건을 직접 유지해야 한다.
- lecture에서 users/file/student 테이블을 직접 매핑하지 않는다.
- 실제 결제, 환불, POS 연동은 student/payment 후속 범위다.

## 데이터 생명주기 정책

- 종료되었거나 삭제된 강의 이력은 유료 플랜 전용 혜택으로 두지 않는다.
- 강의는 수강 이력, 출결, 상담, 매출 판단의 기준이 되므로 운영 이력으로 보관한다.
- 현재 구현은 삭제 시 `deleted_at`을 채우는 소프트 삭제이며, 일반 목록/상세 조회와 시간 충돌 검사에서는 제외한다.
- 강의 데이터는 별도 첨부파일이 없는 RDS 중심 데이터이므로 S3 생명주기 대상이 아니다.
- 마감 범위에서는 오래된 강의를 자동 하드 삭제하는 배치를 두지 않는다. 삭제된 강의도 과거 수강·출결·매출 이력 해석에 필요한 기준 정보로 남긴다.
- 플랜 차등은 종료 강의 보관기간이 아니라 장기 리포트, 고급 검색, 대량 다운로드 같은 운영 편의 기능에 둔다.
- 후속 구현에서는 종료 강의와 삭제 강의를 일반 운영 화면에서 분리하되, 이력 정합성에 필요한 참조는 유지한다.
- 담당 도메인 기준은 [DATA_LIFECYCLE_POLICY.md](../../../../../../../../docs/DATA_LIFECYCLE_POLICY.md)를 따른다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
