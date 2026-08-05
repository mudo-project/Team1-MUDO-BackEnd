package com.academy.mudogroupware.memo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoJpaRepository extends JpaRepository<MemoEntity, Long> {
}
