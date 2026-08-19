package com.academy.mudogroupware.notice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.model.NoticeAttachment;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(NoticeRepositoryImpl.class)
class NoticeRepositoryImplDataJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    @Autowired
    private NoticeRepositoryImpl noticeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updateWithNewAttachmentsReplacesExistingOnes() {
        Notice created = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "old.pdf")), NOW));
        entityManager.flush();
        entityManager.clear();

        Notice toUpdate = noticeRepository.findById(created.getId()).orElseThrow();
        toUpdate.update("새 제목", "새 내용", List.of(NoticeAttachment.create(20L, "new.pdf")), NOW.plusHours(1));
        noticeRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        Notice reloaded = noticeRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("새 제목");
        assertThat(reloaded.getAttachments()).extracting(NoticeAttachment::getFileId).containsExactly(20L);
    }

    @Test
    void updateWithoutAttachmentsArgumentLeavesExistingAttachmentsUntouched() {
        Notice created = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "old.pdf")), NOW));
        entityManager.flush();
        entityManager.clear();

        Notice toUpdate = noticeRepository.findById(created.getId()).orElseThrow();
        // 3-인자 update() — 첨부파일은 손대지 않는 기존 호출 형태
        toUpdate.update("제목만 수정", "내용만 수정", NOW.plusHours(1));
        noticeRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        Notice reloaded = noticeRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("제목만 수정");
        assertThat(reloaded.getAttachments()).extracting(NoticeAttachment::getFileId).containsExactly(10L);
    }

    @Test
    void updateWithEmptyAttachmentListClearsAttachments() {
        Notice created = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "old.pdf")), NOW));
        entityManager.flush();
        entityManager.clear();

        Notice toUpdate = noticeRepository.findById(created.getId()).orElseThrow();
        toUpdate.update("제목", "내용", List.of(), NOW.plusHours(1));
        noticeRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        Notice reloaded = noticeRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getAttachments()).isEmpty();
    }
}
