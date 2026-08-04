package com.academy.mudogroupware.messenger.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberInfoJpaRepository extends JpaRepository<ChatMemberInfoEntity, Long> {
}
