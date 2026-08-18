package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootConnectionHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SharedFileRootConnectionHistoryPersistenceAdapter implements SharedFileRootConnectionHistoryRepository {

    private final SharedFileRootConnectionHistoryJpaRepository jpaRepository;

    @Override
    public Optional<String> findGoogleRootFolderIdByEmail(String googleEmail) {
        return jpaRepository.findById(googleEmail)
                .map(SharedFileRootConnectionHistoryEntity::getGoogleRootFolderId);
    }

    // PK가 이메일(자연키)이라 save()가 findById 후 merge하는 방식으로 동작 — 이미 있으면 갱신,
    // 없으면 삽입되는 upsert가 별도 존재 확인 없이 이뤄진다.
    @Override
    public void upsert(String googleEmail, String googleRootFolderId, LocalDateTime connectedAt) {
        jpaRepository.save(new SharedFileRootConnectionHistoryEntity(googleEmail, googleRootFolderId, connectedAt));
    }
}
