package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.PushSubscription;
import com.academy.mudogroupware.approval.domain.repository.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PushSubscriptionRepositoryImpl implements PushSubscriptionRepository {

    private final PushSubscriptionJpaRepository pushSubscriptionJpaRepository;

    @Override
    public PushSubscription save(PushSubscription pushSubscription) {
        PushSubscriptionEntity entity = pushSubscriptionJpaRepository
                .findByUserIdAndEndpoint(pushSubscription.getUserId(), pushSubscription.getEndpoint())
                .map(existing -> {
                    existing.setP256dh(pushSubscription.getP256dh());
                    existing.setAuth(pushSubscription.getAuth());
                    return existing;
                })
                .orElseGet(() -> PushSubscriptionEntity.builder()
                        .userId(pushSubscription.getUserId())
                        .endpoint(pushSubscription.getEndpoint())
                        .p256dh(pushSubscription.getP256dh())
                        .auth(pushSubscription.getAuth())
                        .createdAt(pushSubscription.getCreatedAt())
                        .build());

        return toDomain(pushSubscriptionJpaRepository.save(entity));
    }

    @Override
    public List<PushSubscription> findAllByUserId(Long userId) {
        return pushSubscriptionJpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByUserIdAndEndpoint(Long userId, String endpoint) {
        pushSubscriptionJpaRepository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    private PushSubscription toDomain(PushSubscriptionEntity entity) {
        return PushSubscription.restore(entity.getId(), entity.getUserId(), entity.getEndpoint(),
                entity.getP256dh(), entity.getAuth(), entity.getCreatedAt());
    }
}
