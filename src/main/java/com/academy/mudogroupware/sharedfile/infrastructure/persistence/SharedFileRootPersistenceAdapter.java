package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

// SharedFileRootRepository의 JPA 구현체. 도메인 모델은 id를 갖지 않으므로, 항상 고정 ID(1)로
// upsert(있으면 갱신, 없으면 생성)한다.
@Repository
@RequiredArgsConstructor
public class SharedFileRootPersistenceAdapter implements SharedFileRootRepository {

    private final SharedFileRootJpaRepository sharedFileRootJpaRepository;

    // root.getVersion()이 null이면 아직 저장된 적 없는 인스턴스(ready()/failed())이므로 새로 만들고,
    // 아니면 조회 시점에 읽었던 version을 그대로 실어 update한다. 저장 직전에 다시 조회하지 않는 이유는
    // SharedFileRootEntity.forUpdate() 주석 참고.
    @Override
    public SharedFileRoot save(SharedFileRoot root) {
        SharedFileRootEntity entity = root.getVersion() == null
                ? SharedFileRootEntity.create(root.getStatus(), root.getGoogleRootFolderId())
                : SharedFileRootEntity.forUpdate(root.getVersion(), root.getStatus(), root.getGoogleRootFolderId());
        SharedFileRootEntity saved = sharedFileRootJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SharedFileRoot> find() {
        return sharedFileRootJpaRepository.findById(SharedFileRootEntity.SINGLETON_ID).map(this::toDomain);
    }

    private SharedFileRoot toDomain(SharedFileRootEntity entity) {
        return SharedFileRoot.restore(entity.getStatus(), entity.getGoogleRootFolderId(), entity.getVersion());
    }
}
