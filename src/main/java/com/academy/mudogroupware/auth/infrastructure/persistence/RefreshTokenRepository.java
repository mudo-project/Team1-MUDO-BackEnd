package com.academy.mudogroupware.auth.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity,Long>{Optional<RefreshTokenJpaEntity> findByUserId(Long userId); void deleteByUserId(Long userId);}
