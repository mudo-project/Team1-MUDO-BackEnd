package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.lecture.domain.model.Classroom;

public interface ClassroomRepository {

    Optional<Classroom> findByName(String name);

    // findByName과 동일하지만, 존재하는 행을 찾으면 트랜잭션이 끝날 때까지 잠근다.
    // 같은 강의실에 동시에 겹치는 시간대를 등록하려는 요청을 직렬화하기 위해 쓴다.
    Optional<Classroom> findByNameForUpdate(String name);

    List<Classroom> findAllById(List<Long> ids);

    Classroom save(Classroom classroom);
}
