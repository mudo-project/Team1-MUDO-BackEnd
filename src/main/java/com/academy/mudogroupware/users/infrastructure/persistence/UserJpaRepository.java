package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.users.domain.model.UserStatus;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByRoleIdAndStatus(Long roleId, UserStatus status);

    @Modifying
    @Query("update UserEntity u set u.roleId = null where u.roleId = :roleId")
    void clearRoleId(@Param("roleId") Long roleId);

    @Query("select u.id from UserEntity u "
            + "where u.academyId = :academyId "
            + "and u.status = com.academy.mudogroupware.users.domain.model.UserStatus.ACTIVE "
            + "and u.id in :userIds")
    Set<Long> findActiveIdsByAcademyIdAndIdIn(
            @Param("academyId") Long academyId,
            @Param("userIds") Set<Long> userIds);

    List<UserEntity> findAllByStatusAndJoinedAtIsNotNull(UserStatus status);
}
