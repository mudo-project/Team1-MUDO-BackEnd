package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ClassroomJpaRepository extends JpaRepository<ClassroomEntity, Long> {

    Optional<ClassroomEntity> findByName(String name);

    // 강의 등록 시 같은 강의실의 시간 충돌 검사(existsOverlap)와 저장이 원자적으로 일어나도록,
    // 이미 존재하는 강의실이면 이 조회로 행을 잠근 채 트랜잭션이 끝날 때까지 유지한다.
    // 동시에 같은 강의실에 겹치는 시간대를 등록하려는 두 트랜잭션은 여기서 직렬화된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClassroomEntity c where c.name = :name")
    Optional<ClassroomEntity> findByNameForUpdate(@Param("name") String name);
}
