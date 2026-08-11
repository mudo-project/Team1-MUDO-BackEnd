package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

// PK가 항상 1이므로 커스텀 조회 메서드 없이 findById/save/existsById 등 기본 메서드만 사용한다.
public interface SharedFileRootJpaRepository extends JpaRepository<SharedFileRootEntity, Integer> {
}
