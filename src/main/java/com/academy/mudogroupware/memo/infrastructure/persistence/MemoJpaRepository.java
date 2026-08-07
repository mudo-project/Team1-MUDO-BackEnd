package com.academy.mudogroupware.memo.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.memo.domain.model.MemoColor;

public interface MemoJpaRepository extends JpaRepository<MemoEntity, Long> {

    List<MemoEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<MemoEntity> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.title = :title, m.content = :content, m.updatedAt = :updatedAt "
            + "WHERE m.id = :id")
    void updateContent(@Param("id") Long id, @Param("title") String title, @Param("content") String content,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.color = :color, m.updatedAt = :updatedAt WHERE m.id = :id")
    void updateColor(@Param("id") Long id, @Param("color") MemoColor color,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.positionX = :positionX, m.positionY = :positionY, m.width = :width, "
            + "m.height = :height, m.updatedAt = :updatedAt WHERE m.id = :id")
    void updatePosition(@Param("id") Long id, @Param("positionX") int positionX,
                         @Param("positionY") int positionY, @Param("width") int width,
                         @Param("height") int height, @Param("updatedAt") LocalDateTime updatedAt);
}
