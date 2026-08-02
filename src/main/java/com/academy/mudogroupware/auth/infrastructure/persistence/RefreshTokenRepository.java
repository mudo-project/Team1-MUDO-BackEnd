package com.academy.mudogroupware.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
  Optional<RefreshTokenJpaEntity> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
