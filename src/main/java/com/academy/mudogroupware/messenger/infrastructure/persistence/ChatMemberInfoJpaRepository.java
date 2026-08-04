package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface ChatMemberInfoJpaRepository extends Repository<ChatMemberInfoEntity, Long> {

    Optional<ChatMemberInfoEntity> findById(Long id);

    List<ChatMemberInfoEntity> findAllById(Iterable<Long> ids);
}
