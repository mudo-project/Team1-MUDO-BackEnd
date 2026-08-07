package com.academy.mudogroupware.memo.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.model.MemoColor;

public interface MemoRepository {

    Memo save(Memo memo);

    Optional<Memo> findById(Long id);

    List<Memo> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<Memo> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    long countByUserId(Long userId);

    // 필드 단위 원자적 UPDATE. 메모 전체를 재저장하면 동시에 다른 필드를 변경한 요청을 덮어쓸 수 있어 대신 사용한다.
    void updateContent(Long id, String title, String content, LocalDateTime updatedAt);

    void updateColor(Long id, MemoColor color, LocalDateTime updatedAt);

    void updatePosition(Long id, int positionX, int positionY, int width, int height, LocalDateTime updatedAt);

    void deleteById(Long id);
}
