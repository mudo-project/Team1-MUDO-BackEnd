package com.academy.mudogroupware.memo.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<Memo> findById(Long id) {
        return memoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Memo> findMostRecentByUserId(Long userId) {
        return memoJpaRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId).map(this::toDomain);
    }

    @Override
    public List<Memo> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId) {
        return memoJpaRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Memo> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId) {
        return memoJpaRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return memoJpaRepository.countByUserId(userId);
    }

    @Override
    public void updateContent(Long id, String title, String content, LocalDateTime updatedAt) {
        memoJpaRepository.updateContent(id, title, content, updatedAt);
    }

    @Override
    public void updateColor(Long id, String color, LocalDateTime updatedAt) {
        memoJpaRepository.updateColor(id, color, updatedAt);
    }

    @Override
    public void updatePosition(Long id, int positionX, int positionY, int width, int height,
                                LocalDateTime updatedAt) {
        memoJpaRepository.updatePosition(id, positionX, positionY, width, height, updatedAt);
    }

    @Override
    public void deleteById(Long id) {
        memoJpaRepository.deleteByIdIfExists(id);
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
