package com.academy.mudogroupware.memo.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.memo.domain.model.Memo;
import com.academy.mudogroupware.memo.domain.repository.MemoRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemoRepositoryImpl implements MemoRepository {

    private final MemoJpaRepository memoJpaRepository;

    @Override
    public Memo save(Memo memo) {
        MemoEntity entity = toEntity(memo);
        return toDomain(memoJpaRepository.save(entity));
    }

    private MemoEntity toEntity(Memo memo) {
        return MemoEntity.builder()
                .id(memo.getId())
                .userId(memo.getUserId())
                .title(memo.getTitle())
                .content(memo.getContent())
                .color(memo.getColor())
                .positionX(memo.getPositionX())
                .positionY(memo.getPositionY())
                .width(memo.getWidth())
                .height(memo.getHeight())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .build();
    }

    private Memo toDomain(MemoEntity entity) {
        return Memo.restore(entity.getId(), entity.getUserId(), entity.getTitle(), entity.getContent(),
                entity.getColor(), entity.getPositionX(), entity.getPositionY(), entity.getWidth(),
                entity.getHeight(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
