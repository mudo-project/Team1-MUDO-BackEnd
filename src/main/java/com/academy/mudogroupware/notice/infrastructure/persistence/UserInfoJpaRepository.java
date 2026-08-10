package com.academy.mudogroupware.notice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoJpaRepository extends JpaRepository<UserInfoEntity, Long> {

    long countByStatus(String status);
}
