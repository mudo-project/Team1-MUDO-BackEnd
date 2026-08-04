package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Set;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    @Query("select u.id from UserEntity u "
            + "where u.academyId = :academyId "
            + "and u.status = com.academy.mudogroupware.users.domain.model.UserStatus.ACTIVE "
            + "and u.id in :userIds")
    Set<Long> findActiveIdsByAcademyIdAndIdIn(
            @Param("academyId") Long academyId,
            @Param("userIds") Set<Long> userIds);
}
