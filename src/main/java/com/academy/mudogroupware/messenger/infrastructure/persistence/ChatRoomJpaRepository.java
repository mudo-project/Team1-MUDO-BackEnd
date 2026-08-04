package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoomEntity, Long> {

    @Query("select distinct r from ChatRoomEntity r join r.members m "
            + "where r.academyId = :academyId and m.userId = :userId")
    List<ChatRoomEntity> findAllByMember(@Param("academyId") Long academyId, @Param("userId") Long userId);
}
