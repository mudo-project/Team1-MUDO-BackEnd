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
        return noticeJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Notice> findAll(Long academyId, String titleKeyword, int page, int size) {
        Slice<NoticeEntity> slice = noticeJpaRepository.findAllByAcademyIdAndTitleKeyword(
                academyId, titleKeyword, PageRequest.of(page, size));
        List<Notice> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    @Override
    public void deleteById(Long id) {
        noticeJpaRepository.deleteById(id);
    }

    private NoticeEntity toNewEntity(Notice domain) {
        NoticeEntity entity = NoticeEntity.builder()
                .academyId(domain.getAcademyId())
                .authorUserId(domain.getAuthorUserId())
                .title(domain.getTitle())
                .content(domain.getContent())
                .pinned(domain.isPinned())
                .viewCount(domain.getViewCount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
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
        return entity;
    }

    private NoticeAttachmentEntity toAttachmentEntity(NoticeAttachment attachment) {
        return NoticeAttachmentEntity.builder()
                .id(attachment.getId())
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .build();
    }

    private Notice toDomain(NoticeEntity entity) {
        List<NoticeAttachment> attachments = entity.getAttachments().stream()
                .map(this::toAttachmentDomain)
                .toList();

        return Notice.restore(entity.getId(), entity.getAcademyId(), entity.getAuthorUserId(), entity.getTitle(),
                entity.getContent(), entity.isPinned(), entity.getViewCount(), attachments, entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private NoticeAttachment toAttachmentDomain(NoticeAttachmentEntity entity) {
        return NoticeAttachment.restore(entity.getId(), entity.getFileUrl(), entity.getFileName(),
                entity.getFileType());
    }
}
