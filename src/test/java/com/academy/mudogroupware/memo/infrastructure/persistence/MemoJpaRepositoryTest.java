package com.academy.mudogroupware.memo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class MemoJpaRepositoryTest {

    @Autowired
    private MemoJpaRepository memoJpaRepository;

    private Long insertMemo() {
        MemoEntity entity = MemoEntity.builder()
                .userId(1L)
                .title("제목")
                .content("내용")
                .color("D3A340")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return memoJpaRepository.save(entity).getId();
    }

    @Test
    void deleteByIdIfExistsRemovesTheRowAndReturnsOne() {
        Long id = insertMemo();

        int deleted = memoJpaRepository.deleteByIdIfExists(id);

        assertThat(deleted).isEqualTo(1);
        assertThat(memoJpaRepository.findById(id)).isEmpty();
    }

    @Test
    void deleteByIdIfExistsReturnsZeroInsteadOfThrowingWhenAlreadyDeleted() {
        // 같은 메모에 대한 삭제 요청이 동시에 두 번 오는 상황(#464)을 흉내낸다 — 두 번째 삭제 시도는
        // 예외 없이 조용히 0건을 반환해야 한다(JpaRepository 기본 deleteById()는 여기서 예외를 던졌다).
        Long id = insertMemo();
        memoJpaRepository.deleteByIdIfExists(id);

        int secondAttempt = memoJpaRepository.deleteByIdIfExists(id);

        assertThat(secondAttempt).isEqualTo(0);
    }

    @Test
    void deleteByIdIfExistsDoesNotThrowForNonExistentId() {
        assertThatCode(() -> memoJpaRepository.deleteByIdIfExists(999_999L)).doesNotThrowAnyException();
    }
}
