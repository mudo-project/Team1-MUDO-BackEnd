> 작성일: 2026-08-03
> 상태: 🚧 초기 스캐폴딩 완료 · 카테고리·인가정책 확정 예정

## 🎯 변경 목적

학원 그룹웨어에 공지사항 기능을 추가한다. 팀 노션 기능명세서와 실제 화면 시안(Figma)을 기준으로 데이터 모델을 확정하고, approval 모듈 개발 중 발견된 실수(학원 데이터 격리 누락 등)를 처음부터 반영한다.

---

## ✅ 2026-08-04 · 저장 시각을 한국 시간(KST)으로 고정 (approval 뒤늦은 반영)

### 배경

approval 모듈에서 서버 시간대(UTC)와 무관하게 KST로 저장하도록 `Clock` 기반으로 고친 적이 있는데, notice는 그 작업 범위에서 빠져 있었다. `Notice.create()`/`update()`와 `NoticeReadRepositoryImpl.markRead()`가 여전히 `LocalDateTime.now()`를 직접 호출하고 있었던 걸 뒤늦게 발견해서 동일하게 고쳤다.

### 확정된 정책

- `Notice.create(..., LocalDateTime now)`/`update(..., LocalDateTime now)`가 시각을 파라미터로 받는다. `CreateNoticeService`/`UpdateNoticeService`가 `Clock`을 주입받아 `LocalDateTime.now(clock)`을 넘긴다.
- `NoticeReadRepositoryImpl`은 도메인 계층을 거치지 않는 순수 인프라 기록(읽음 시각)이라, 서비스 계층 경유 없이 `Clock`을 직접 주입받아 처리한다.
- 새로 추가한 도메인 클래스(`Notice`)와 인프라 클래스(`NoticeReadRepositoryImpl`)에 유닛 테스트를 함께 추가했다 (notice 모듈에 유닛 테스트가 전무했던 상태였다).

### 완료 기준

- [x] notice 코드에 `LocalDateTime.now()` 직접 호출이 남아있지 않다.
- [x] `./gradlew test` 통과 (신규 유닛 테스트 10케이스 포함).

---

## ✅ 2026-08-04 · 전용 ErrorCode 도입 및 목록 API 페이지네이션

### 배경

`users`/`auth`, approval 모듈이 먼저 도입한 도메인 전용 `ErrorCode`/`Exception` 패턴과, `docs/API_CONTRACT.md`에 정의돼 있던 페이지네이션 규칙을 notice에도 동일하게 반영했다.

### 확정된 정책

- `NoticeErrorCode`(enum) + `NoticeException`(`BusinessException` 상속)을 추가하고, 기존 `BadRequestException`/`NotFoundException`/`ForbiddenException` 직접 사용을 전부 교체했다. 코드 체계는 `NOTICE_{HTTP상태}_{순번}`.
- 공지 목록 조회(`getNotices`)에 `page`/`size` 쿼리 파라미터와 Spring Data `Slice`(전체 개수 미계산) 기반 페이지네이션을 적용했다. 응답은 `global`의 공용 `SliceResponse<T>`로 감싼다. 상세 조회의 "읽은 사람 목록"(`getReaders`)은 한 공지당 인원 규모가 제한적이라 이번 범위에서는 페이지네이션하지 않았다.

### 완료 기준

- [x] notice 코드에 `global.domain.common.exception`의 범용 예외 직접 사용이 남아있지 않다.
- [x] `./gradlew compileJava` / `./gradlew test` 통과.

---

## ✅ 2026-08-04 · users 테이블 정합화(`resign_date` → `status`) 대응

### 배경

`users` 모듈 PR(#19)이 머지되면서 `V4.1.1__align_users_table_with_erd.sql` 마이그레이션이 `users.resign_date`/`hire_date` 컬럼을 삭제하고 `status`(`ACTIVE`/`RESIGNED`/`INACTIVE`)로 대체했다. notice의 임시 shim(`UserInfoEntity`)이 `resign_date`를 직접 매핑하고 있어, 이 마이그레이션이 반영된 순간 `countActiveUsers` 쿼리가 깨지는 상태였다.

### 확정된 정책

- `UserInfoEntity.resignDate`(LocalDate)를 제거하고 `status`(String) 컬럼 매핑으로 교체했다.
- `UserInfoJpaRepository.countByAcademyIdAndResignDateIsNull` → `countByAcademyIdAndStatus(academyId, "ACTIVE")`로 변경했다.
- "전체 대상 인원 수(읽음 분모)"의 의미를 "퇴사일이 없는 사용자"에서 "`status = ACTIVE`인 사용자"로 그대로 치환했다 — 마이그레이션이 기존 데이터의 `resign_date IS NOT NULL`을 `status = 'RESIGNED'`로, 나머지를 `ACTIVE` 기본값으로 채웠기 때문에 기존 의미와 동일하다.

### 완료 기준

- [x] `./gradlew compileJava` / `./gradlew test` 통과.
- [x] `resignDate`/`resign_date`에 대한 코드 참조가 남아있지 않다 (마이그레이션 파일 자체는 이력이라 그대로 둔다).

---

## ✅ 확정된 정책

- 공지 작성 시 작성자·작성일시는 자동 기록되고, 사진뿐 아니라 PDF 등 일반 파일도 여러 개 첨부할 수 있다.
- 고정(pinned)된 공지는 목록 최상단에 노출되고, 그 외에는 최신순으로 정렬한다.
- 상세 조회 시 조회수를 누적 증가시키고, 사용자별 읽음 여부를 별도로 기록한다. "조회수"와 "읽은 인원 수/전체 대상 인원 수"는 다른 지표로 분리한다.
- 목록·상세·읽은 사람 조회 모두 요청자의 소속 학원으로 스코프를 제한한다 (approval 모듈에서 CodeRabbit 리뷰로 발견된 테넌시 격리 버그를 재발 방지 차원에서 처음부터 반영).
- **카테고리(인사/시설/업무) 기능은 이번 범위에서 제외한다.** 기능명세서 텍스트에는 있었으나 실제 화면 시안에 카테고리 필터/태그가 없어, 화면을 기준으로 판단했다. 필요 시 `notice`에 `category` 컬럼(또는 별도 분류 테이블) 추가가 필요하다.

## 🧭 처리 흐름

```text
Figma 화면 시안 + 노션 기능명세서 리뷰
→ 카테고리 필터 UI 부재 확인 → 이번 범위에서 제외 결정
→ "읽음 3/5" 클릭 시 읽은 사람 목록이 필요함을 화면에서 재확인
→ notice / notice_attachment / notice_read 3개 테이블 설계
→ approval 모듈과 동일하게 학원(academyId) 스코프 검증을 처음부터 포함해 구현
```

## 🛡️ 검증 및 예외 처리

- `title`, `content`는 비어 있을 수 없다.
- 수정·삭제·고정은 작성자 본인만 가능하다 (삭제·고정해제의 "권한자" 조건은 `users.role` 값 체계 확정 후 추가 예정).
- 다른 학원 소속 공지를 상세/읽은사람 조회하면 `403`을 반환한다.
- 같은 사용자가 같은 공지를 여러 번 조회해도 읽은 인원 수는 1명으로 유지된다 (조회수만 계속 증가).

## 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Presentation | `NoticeController` 8개 엔드포인트 신규 (작성/목록/상세/읽은사람/수정/삭제/고정/고정해제) |
| Application | Command/Query/Port/UseCase/Service 신규 작성, `NoticeAuthorDirectoryPort`로 작성자·조회자 정보 추상화 |
| Domain | `Notice`(고정·조회수 포함), `NoticeAttachment` 신규 |
| Persistence | `NoticeEntity`, `NoticeAttachmentEntity`, `NoticeReadEntity` + `users` 테이블 읽기 전용 임시 shim(`UserInfoEntity`) |
| Migration | `V1.3.1`(notice, notice_attachment), `V1.3.2`(notice_read) 신규 작성 |

## 🧪 완료 기준

- [x] 공지 작성 시 여러 개 파일을 첨부할 수 있다.
- [x] 목록에서 고정 공지가 항상 먼저 보인다.
- [x] 상세 조회 시 조회수가 증가하고, 읽은 사람 목록을 별도로 조회할 수 있다.
- [x] 다른 학원의 공지는 조회되지 않는다.
- [x] `./gradlew test` 통과.
- [ ] 카테고리 기능 필요 여부 팀 확인
- [ ] 작성/삭제/고정해제 권한을 `users.role` 값 체계에 맞춰 반영

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
