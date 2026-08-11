package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRootStatus;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

// SharedFileRootRepository의 JPA 구현체. 도메인 모델은 id를 갖지 않으므로, 항상 고정 ID(1)로
// upsert(있으면 갱신, 없으면 생성)한다.
@Repository
@RequiredArgsConstructor
public class SharedFileRootPersistenceAdapter implements SharedFileRootRepository {

    private final SharedFileRootJpaRepository sharedFileRootJpaRepository;

    @Override
    public SharedFileRoot save(SharedFileRoot root) {
        SharedFileRootEntity entity = sharedFileRootJpaRepository.findById(SharedFileRootEntity.SINGLETON_ID)
                .map(existing -> {
                    existing.update(root.getStatus(), root.getGoogleRootFolderId());
                    return existing;
                })
                .orElseGet(() -> SharedFileRootEntity.create(root.getStatus(), root.getGoogleRootFolderId()));
        sharedFileRootJpaRepository.save(entity);
        return root;
    }

    @Override
    public Optional<SharedFileRoot> find() {
        return sharedFileRootJpaRepository.findById(SharedFileRootEntity.SINGLETON_ID).map(this::toDomain);
    }

    private SharedFileRoot toDomain(SharedFileRootEntity entity) {
        return entity.getStatus() == SharedFileRootStatus.READY
                ? SharedFileRoot.ready(entity.getGoogleRootFolderId())
                : SharedFileRoot.failed();
    }
}
