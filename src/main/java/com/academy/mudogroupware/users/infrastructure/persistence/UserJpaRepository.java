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

    boolean existsByUsername(String username);

    boolean existsByRoleIdAndStatus(Long roleId, UserStatus status);

    @Modifying
    @Query("update UserEntity u set u.roleId = null where u.roleId = :roleId")
    void clearRoleId(@Param("roleId") Long roleId);

    @Modifying(clearAutomatically = true)
    @Query("update UserEntity u set u.password = :passwordHash, u.phone = :phone, u.email = :email, "
            + "u.mustChangePw = false where u.id = :userId and u.mustChangePw = true")
    int completePasswordSetupIfMustChange(@Param("userId") Long userId, @Param("passwordHash") String passwordHash,
                                           @Param("phone") String phone, @Param("email") String email);

    @Query("select u.id from UserEntity u "
            + "where u.status = com.academy.mudogroupware.users.domain.model.UserStatus.ACTIVE "
            + "and u.id in :userIds")
    Set<Long> findActiveIdsByIdIn(@Param("userIds") Set<Long> userIds);

    List<UserEntity> findAllByStatusAndJoinedAtIsNotNull(UserStatus status);

    List<UserEntity> findAllByStatusAndNameContainingIgnoreCase(UserStatus status, String keyword);

    List<UserEntity> findAllByStatus(UserStatus status);

    long countByStatus(UserStatus status);

    @Query("select u.roleId as roleId, count(u) as count from UserEntity u "
            + "where u.status = com.academy.mudogroupware.users.domain.model.UserStatus.ACTIVE "
            + "and u.roleId in :roleIds group by u.roleId")
    List<RoleMemberCountRow> countActiveByRoleIdIn(@Param("roleIds") Set<Long> roleIds);
}
