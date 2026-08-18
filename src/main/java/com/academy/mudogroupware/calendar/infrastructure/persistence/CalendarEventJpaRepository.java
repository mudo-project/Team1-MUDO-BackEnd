package com.academy.mudogroupware.calendar.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, Long> {

    // eventEndAt이 없는(순간) 일정은 eventStartAt을 종료 시각으로 취급해 겹침 여부를 판단한다.
    // eventStartAt만 보고 걸러내면 조회 기간 이전에 시작해 계속 진행 중인 일정이 누락된다.
    @Query("SELECT e FROM CalendarEventEntity e "
            + "WHERE e.eventStartAt <= :to "
            + "AND COALESCE(e.eventEndAt, e.eventStartAt) >= :from")
    List<CalendarEventEntity> findAllOverlappingPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
