> 작성일: 2026-08-01
> 상태: 🚧 스키마 팀 컨벤션 정합화 완료 · role 기반 결재자 지정 설계 예정

## 🎯 변경 목적

학원 그룹웨어에 전자결재(템플릿 기반 다단계 순차 결재) 기능을 추가한다. 노션 기능명세서와 팀 ERD(erdcloud) 리뷰 결과를 반영해, 다른 도메인과 스키마 컨벤션(멀티테넌시, 공유 테이블, 다중 파일 첨부)을 맞춘다.

---

## ✅ 2026-08-03 · CodeRabbit 리뷰 반영 (데이터 격리 강화)

### 확정된 정책

- 결재 문서에 `resubmittedAt`을 추가해, 반려된 문서 1건당 재상신은 1회만 허용한다.
- 템플릿 목록 조회는 요청자의 `academyId`로 스코프를 제한한다 (`findAllByTypeAndAcademyId`).
- 결재 신청 시 신청자 학원과 템플릿 학원이 다르면 `ForbiddenException`을 던진다.
- `template`-`approval_document` FK를 `(template_id, academy_id)` 복합키로 바꿔 DB 레벨에서도 교차 학원 참조를 막는다.
- `approval_line_step`의 `role_id`/`approver_id`는 정확히 하나만 채워지도록 DB `CHECK` 제약과 도메인 검증을 함께 건다.

### 완료 기준

- [x] 재상신 반복 호출 시 두 번째 시도부터 `409`를 반환한다.
- [x] 다른 학원 템플릿이 목록/신청에 섞이지 않는다.
- [x] `./gradlew test` 통과.

---

## ✅ 2026-08-03 · 팀 ERD 최종 반영 및 부가 기능 3종

### 확정된 정책

- 테이블명을 팀 ERD 최종안으로 변경: `approval_document`, `approval_step`, `approval_line_step`, `approval_attachment`.
- `approval_line_step`에 `role_id`를 추가한다 (역할 기반 결재자 지정 스키마만 우선 반영, 해석 로직은 role 테이블이 없어 보류).
- 결재 문서 첨부파일을 단일 `file_id`에서 `approval_attachment`(다대다) 다중 첨부로 전환하고, AI 요약용 컬럼(`ai_summary`/`summary_status`/`summarized_at`)을 함께 추가한다.
- 반려된 결재의 재상신, 템플릿 목록의 결재자 이름 노출, 결재 대기 건수 조회 API를 추가한다.

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `ApprovalAttachment`, `AttachmentSummaryStatus` 추가, `ApprovalTemplateLine`에 `roleId` 추가, `ApprovalContent`에서 파일 필드 제거 |
| Application | 첨부파일 목록(`fileIds`)을 다루도록 Command/View 전체 수정, 재상신·대기건수 UseCase 추가 |
| Infrastructure | 엔티티/테이블명 전체 정합화, `ApprovalAttachmentEntity` 추가 |
| Migration | `V1.2.1`~`V1.2.3` 재작성 |

### 완료 기준

- [x] 결재 문서에 파일 여러 개를 첨부할 수 있다.
- [x] 반려된 결재를 같은 내용으로 재상신할 수 있다.
- [x] 사이드바 뱃지용 대기 건수 API가 목록 API 없이 동작한다.

---

## ✅ 2026-08-01 · DB 컨벤션 1차 정합화

### 확정된 정책

- 템플릿/문서에 `academy_id`(멀티테넌시) 컬럼을 추가한다.
- 컬럼명을 팀 공용 컨벤션에 맞춘다: `created_by`, `requester_user_id`, `approver_user_id`.
- 결재 템플릿은 approval 전용 테이블 대신 팀이 여러 기능에서 공유하는 `template` 테이블을 `type='APPROVAL'`로 재사용한다.

### 처리 흐름

```text
팀 erdcloud "템플릿" 테이블 스크린샷 리뷰
→ academy_id, file_id, type, created_by, updated_at 컬럼 존재 확인
→ 결재 템플릿 자체 테이블 대신 공유 template 재사용 결정
→ ApprovalTemplateEntity를 template 테이블에 매핑, type 컬럼에 "APPROVAL" 고정
```

---

## ✅ 2026-07-31 · 템플릿/문서 도메인 분리 (최초 리팩터링)

### 변경 목적 (AS-IS → TO-BE)

- AS-IS: 결재 "템플릿"을 만드는 시점에 제목·내용·결재선을 한 번에 입력해, 템플릿과 실제 결재 신청 건이 사실상 하나로 합쳐져 있었다.
- TO-BE: 노션 기능명세서 기준으로 "템플릿(틀, 재사용)"과 "문서(실제 신청 1건)"를 별도 Aggregate로 분리한다.

### 확정된 정책

- `ApprovalTemplate`: 이름 + 기본 결재선만 가진다. 내용(텍스트/파일)이 없다.
- `ApprovalDocument`: 템플릿을 참조하고, 제목·내용·실제 결재선·진행 상태를 가진다.
- 성공 응답은 팀 공용 `GlobalApiResponse<T>` 봉투로 감싼다.
- API 경로에 `/api/v1` 버전 프리픽스를 적용한다. (→ 2026-08-02에 되돌림, 아래 참고)

### 완료 기준

- [x] 템플릿 CRUD와 결재 신청·조회·승인·반려가 각각 독립된 API로 분리된다.
- [x] 순차 결재(1차 → 2차 → …) 로직이 도메인에 구현된다.

---

## ✅ 2026-08-02 · API 버전 프리픽스 제거

- `/api/v1/approvals`, `/api/v1/approval-templates`를 `/api/approvals`, `/api/approval-templates`로 되돌렸다.
- 사유: 아직 실배포 전이라 버전 호환성을 고려할 클라이언트가 없고, 프로젝트가 API 버전 정책을 전면 도입하지 않기로 결정했다 (CodeRabbit이 호환성 문제로 지적했으나, 사용자 명시적 결정으로 유지).

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
