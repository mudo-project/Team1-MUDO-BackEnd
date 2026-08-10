package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public interface LectureJpaRepository extends JpaRepository<LectureEntity, Long> {

    @Query("select l from LectureEntity l where "
            + "(:termId is null or l.termId = :termId) "
            + "and (:grade is null or l.grade = :grade) "
            + "and (:subjectId is null or l.subjectId = :subjectId) "
            + "and (:teacherId is null or l.teacherId = :teacherId) "
            + "and (:classroomId is null or l.classroomId = :classroomId) "
            + "and (:dayOfWeek is null or exists ("
            + "  select 1 from LectureScheduleEntity s where s.lecture = l and s.dayOfWeek = :dayOfWeek)) "
            + "order by l.id desc")
    Slice<LectureEntity> findAllByFilter(@Param("termId") Long termId,
                                          @Param("grade") Grade grade,
                                          @Param("subjectId") Long subjectId,
                                          @Param("teacherId") Long teacherId,
                                          @Param("classroomId") Long classroomId,
                                          @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                          Pageable pageable);

    @Query("select count(s) > 0 from LectureScheduleEntity s "
            + "where s.lecture.classroomId = :classroomId and s.dayOfWeek = :dayOfWeek "
            + "and s.startTime < :endTime and :startTime < s.endTime")
    boolean existsOverlap(@Param("classroomId") Long classroomId,
                           @Param("dayOfWeek") DayOfWeek dayOfWeek,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);
}
