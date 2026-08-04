package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findAllByUserId(Long userId);

    Optional<PushSubscriptionEntity> findByUserIdAndEndpoint(Long userId, String endpoint);

    void deleteByUserIdAndEndpoint(Long userId, String endpoint);
}
