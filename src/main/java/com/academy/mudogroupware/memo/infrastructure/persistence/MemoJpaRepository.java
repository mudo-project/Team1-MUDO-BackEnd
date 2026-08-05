package com.academy.mudogroupware.memo.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoJpaRepository extends JpaRepository<MemoEntity, Long> {

    List<MemoEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<MemoEntity> findAllByUserIdOrderByCreatedAtAsc(Long userId);
}
