package com.academy.mudogroupware.memo.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MemoJpaRepository extends JpaRepository<MemoEntity, Long> {

    Optional<MemoEntity> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<MemoEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<MemoEntity> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    // 개수 확인과 생성을 같은 트랜잭션에서 원자적으로 처리하기 위한 잠금이다. 이 잠금이 없으면
    // 동시 요청 2개가 똑같이 199개를 읽고 둘 다 저장에 성공해 상한(200개)이 깨질 수 있다(CodeRabbit 리뷰,
    // 2026-08-07). 같은 user_id의 요청끼리만 직렬화되고, 다른 사용자의 요청과는 무관하다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.title = :title, m.content = :content, m.updatedAt = :updatedAt "
            + "WHERE m.id = :id")
    void updateContent(@Param("id") Long id, @Param("title") String title, @Param("content") String content,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.color = :color, m.updatedAt = :updatedAt WHERE m.id = :id")
    void updateColor(@Param("id") Long id, @Param("color") String color,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE MemoEntity m SET m.positionX = :positionX, m.positionY = :positionY, m.width = :width, "
            + "m.height = :height, m.updatedAt = :updatedAt WHERE m.id = :id")
    void updatePosition(@Param("id") Long id, @Param("positionX") int positionX,
                         @Param("positionY") int positionY, @Param("width") int width,
                         @Param("height") int height, @Param("updatedAt") LocalDateTime updatedAt);

    // JpaRepository의 기본 deleteById()는 대상이 없으면 EmptyResultDataAccessException을 던져,
    // 같은 메모에 대한 삭제 요청이 동시에 두 번 오면(더블클릭 등) 나중 요청이 500으로 응답됐다(#464).
    // 벌크 삭제 쿼리는 대상이 없어도 0건을 반환할 뿐 예외를 던지지 않아 재삭제가 idempotent해진다.
    // clearAutomatically = true: 벌크 쿼리는 영속성 컨텍스트(1차 캐시)를 자동으로 갱신하지 않아, 같은
    // 트랜잭션에서 삭제 직후 findById를 호출하면 캐시된 엔티티가 그대로 반환될 수 있어 명시적으로 비운다.
    @Modifying(clearAutomatically = true)
    @Query("delete from MemoEntity m where m.id = :id")
    int deleteByIdIfExists(@Param("id") Long id);
}
