# 🔄 공지사항 처리 Flow

## 1. 공지사항 작성 흐름

```text
AuthUser (JWT)
→ SecurityConfig: POST /api/notices, 인증 확인
→ NoticeController.createNotice
→ AuthUser.userId 추출
→ CreateNoticeRequest → CreateNoticeCommand(attachments 포함)
→ CreateNoticeService.createNotice
→ NoticeAuthorDirectoryPort.getAuthor(authorUserId) → academyId 조회
→ Notice.create(academyId, authorUserId, title, content, pinned, attachments, now)
→ NoticeRepository.save
→ NoticeRepositoryImpl → notice 테이블 + notice_attachment 다건 저장
→ GlobalApiResponse<NoticeCreateResponse>
```

- `academyId`는 요청 값이 아니라 작성자 정보에서 서버가 직접 조회합니다.

## 2. 공지사항 목록 조회 흐름

```text
AuthUser
→ NoticeController.getNotices(keyword, page, size)
→ NoticeQueryService.getNotices(requesterId, keyword, page, size)
→ NoticeAuthorDirectoryPort.getAuthor(requesterId) → academyId 조회
→ NoticeRepository.findAll(academyId, keyword, page, size)
→ NoticeJpaRepository.findAllByAcademyIdAndTitleKeyword(..., Pageable) → Slice<Entity>
   → JPQL: academy_id 일치 + (keyword null 이거나 title LIKE %keyword%)
   → ORDER BY is_pinned DESC, created_at DESC
→ 공지별로 작성자 정보 조회 + 읽음 여부(NoticeReadRepository.hasRead) 조회
→ GlobalApiResponse<SliceResponse<NoticeSummaryResponse>>
```

- 고정(pinned) 공지가 항상 최상단에 오도록 DB 정렬 단계에서부터 처리합니다 (애플리케이션 레벨 재정렬 없음).

## 3. 공지사항 상세 조회 흐름 (조회수·읽음 처리 포함)

```text
AuthUser
→ NoticeController.getNoticeDetail
→ NoticeQueryService.getNoticeDetail(noticeId, requesterId)   ※ 이 메서드만 쓰기 트랜잭션
→ NoticeRepository.findById(noticeId)
→ 요청자 academyId != 공지 academyId 이면 ForbiddenException
→ Notice.recordView()               → viewCount += 1
→ NoticeReadRepository.markRead(noticeId, requesterId)
   → 이미 읽은 기록 있으면 아무 것도 하지 않음 (조회수만 증가, 읽음 인원은 유지)
→ NoticeRepository.save(notice)     → view_count 갱신
→ NoticeAuthorDirectoryPort.getAuthor(작성자), countActiveUsers(academyId)
→ NoticeReadRepository.countReaders(noticeId)
→ GlobalApiResponse<NoticeDetailResponse>
```

- `조회수`(매 호출마다 증가)와 `읽은 인원수/전체 대상 인원수`(고유 인원, 학원 재직자 기준)는 서로 다른 계산입니다 — 화면에 "조회 13 · 읽음 3/5"로 따로 표시되는 것과 대응됩니다.
- 목록 조회(`getNotices`)는 `@Transactional(readOnly = true)`이지만, 상세 조회는 조회수·읽음 기록을 써야 해서 메서드 단위로 쓰기 트랜잭션을 다시 엽니다.

## 4. 읽은 사람 목록 조회 흐름

```text
AuthUser
→ NoticeController.getReaders
→ NoticeQueryService.getReaders(noticeId, requesterId)
→ NoticeRepository.findById + 학원 스코프 검증 (상세조회와 동일)
→ NoticeReadRepository.findReadTimestamps(noticeId) → { userId: readAt } Map
→ NoticeAuthorDirectoryPort.getAuthors(읽은 사용자 ID 목록) 배치 조회 (N+1 방지)
→ 최근 읽은 순으로 정렬
→ GlobalApiResponse<List<NoticeReaderResponse>>
```

## 5. 공지사항 고정/고정 해제 흐름

```text
AuthUser
→ NoticeController.pinNotice / unpinNotice
→ PinNoticeService.pin(noticeId, requesterId)
   → 작성자 본인 아니면 ForbiddenException
   → Notice.pin() → save
→ PinNoticeService.unpin(noticeId, requesterId)
   → (임시) 별도 권한 검증 없이 Notice.unpin() → save
→ 204 No Content
```

- 고정 해제는 원래 "권한 있는 사람들 모두" 가능해야 하지만, role 값 체계가 없어 지금은 제한 없이 열어뒀습니다. `users.role` 확정 후 검증을 추가해야 합니다.

---

## 📝 문서 정보

- 업데이트일: `2026-08-03`
- 변경 사항(요약):
  - `notice`/`notice_attachment`/`notice_read` 3개 테이블 기반 전체 흐름을 처음 작성했습니다.
  - 조회수와 읽음 인원을 분리 계산하는 흐름, 학원 스코프 검증 지점을 표시했습니다.
  - 읽은 사람 목록 조회(배치 사용자 조회 포함) 흐름을 추가했습니다.
