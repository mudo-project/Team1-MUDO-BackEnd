package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.model.NoticeAttachment;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {

    private final NoticeJpaRepository noticeJpaRepository;

    @Override
    public Notice save(Notice notice) {
        NoticeEntity entity = notice.getId() != null
                ? updateExisting(notice)
                : toNewEntity(notice);

        return toDomain(noticeJpaRepository.save(entity));
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return noticeJpaRepository.findActiveById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Notice> findAll(String titleKeyword, int page, int size) {
        Slice<NoticeEntity> slice = noticeJpaRepository.findAllByTitleKeyword(
                titleKeyword, PageRequest.of(page, size));
        List<Notice> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public void deleteById(Long id) {
        noticeJpaRepository.deleteById(id);
    }

    private NoticeEntity toNewEntity(Notice domain) {
        NoticeEntity entity = NoticeEntity.builder()
                .authorUserId(domain.getAuthorUserId())
                .title(domain.getTitle())
                .content(domain.getContent())
                .pinned(domain.isPinned())
                .viewCount(domain.getViewCount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .retentionUntil(domain.getRetentionUntil())
                .build();
        domain.getAttachments().forEach(attachment -> entity.addAttachment(toAttachmentEntity(attachment)));
        return entity;
    }

    private NoticeEntity updateExisting(Notice domain) {
        NoticeEntity entity = noticeJpaRepository.getReferenceById(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setContent(domain.getContent());
        entity.setPinned(domain.isPinned());
        entity.setViewCount(domain.getViewCount());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setRetentionUntil(domain.getRetentionUntil());
        // isAttachmentsReplaced()가 false면(제목/내용만 수정) 첨부파일은 그대로 둔다. domain의
        // attachments는 로드된 기존 항목(id 보유)을 그대로 들고 있는데, 이걸 clear 후 id만 재사용해
        // 새로 만들면 이미 삭제 처리된 행 id를 신규 insert에 다시 쓰게 돼 위험하다 — 그래서 아예
        // 손대지 않는 쪽을 택했다.
        if (domain.isAttachmentsReplaced()) {
            replaceAttachments(entity, domain.getAttachments());
        }
        return entity;
    }

    // 도메인의 attachments를 "지금 있어야 할 전체 목록"으로 보고 통째로 교체한다. 여기서 만드는
    // NoticeAttachmentEntity는 항상 새 행(id 없음)이다 — notice_attachment에는 approval_step 같은
    // 유니크 제약이 없어서(FK 인덱스만 있음) clear 직후 별도 flush를 강제할 필요는 없다.
    private void replaceAttachments(NoticeEntity entity, List<NoticeAttachment> attachments) {
        entity.getAttachments().clear();
        attachments.forEach(attachment -> entity.addAttachment(
                NoticeAttachmentEntity.builder()
                        .fileId(attachment.getFileId())
                        .fileName(attachment.getFileName())
                        .build()));
    }

    private NoticeAttachmentEntity toAttachmentEntity(NoticeAttachment attachment) {
        return NoticeAttachmentEntity.builder()
                .id(attachment.getId())
                .fileId(attachment.getFileId())
                .fileName(attachment.getFileName())
                .build();
    }

    private Notice toDomain(NoticeEntity entity) {
        List<NoticeAttachment> attachments = entity.getAttachments().stream()
                .map(this::toAttachmentDomain)
                .toList();

        return Notice.restore(entity.getId(), entity.getAuthorUserId(), entity.getTitle(),
                entity.getContent(), entity.isPinned(), entity.getViewCount(), attachments, entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getDeletedAt(), entity.getRetentionUntil());
    }

    private NoticeAttachment toAttachmentDomain(NoticeAttachmentEntity entity) {
        return NoticeAttachment.restore(entity.getId(), entity.getFileId(), entity.getFileName());
    }
}
