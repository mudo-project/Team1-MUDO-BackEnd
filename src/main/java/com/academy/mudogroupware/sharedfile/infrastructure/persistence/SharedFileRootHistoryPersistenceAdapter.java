package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SharedFileRootHistoryPersistenceAdapter implements SharedFileRootHistoryRepository {

    private final SharedFileRootHistoryJpaRepository jpaRepository;

    @Override
    public Optional<String> findGoogleRootFolderIdByEmail(String googleEmail) {
        return jpaRepository.findByGoogleEmail(googleEmail)
                .map(SharedFileRootHistoryEntity::getGoogleRootFolderId);
    }

    // 같은 이메일 행이 이미 있으면 그 행을 갱신하고, 없으면 새로 만든다. PK가 대리키(Long)라 이메일로
    // 먼저 조회해 존재 여부를 직접 분기해야 한다(SharedFileRootEntity처럼 자연키를 PK로 써서 save()의
    // merge()에 upsert를 맡기는 방식은 이 프로젝트 컨벤션이 아니다).
    @Override
    public void upsert(String googleEmail, String googleRootFolderId, LocalDateTime connectedAt) {
        SharedFileRootHistoryEntity entity = jpaRepository.findByGoogleEmail(googleEmail)
                .map(existing -> {
                    existing.updateFolder(googleRootFolderId, connectedAt);
                    return existing;
                })
                .orElseGet(() -> SharedFileRootHistoryEntity.create(googleEmail, googleRootFolderId, connectedAt));
        jpaRepository.save(entity);
    }
}
