package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public interface LectureJpaRepository extends JpaRepository<LectureEntity, Long> {

    Optional<LectureEntity> findByIdAndDeletedAtIsNull(Long id);

    List<LectureEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("select l from LectureEntity l where "
            + "l.deletedAt is null "
            + "and (:termId is null or l.termId = :termId) "
            + "and (:grade is null or l.grade = :grade) "
            + "and (:subjectName is null or l.subjectName like concat('%', :subjectName, '%')) "
            + "and (:teacherName is null or l.teacherName like concat('%', :teacherName, '%')) "
            + "and (:classroomCode is null or l.classroomCode = :classroomCode) "
            + "and (:dayOfWeek is null or exists ("
            + "  select 1 from LectureScheduleEntity s where s.lecture = l and s.dayOfWeek = :dayOfWeek)) "
            + "order by l.id desc")
    Slice<LectureEntity> findAllByFilter(@Param("termId") Long termId,
                                          @Param("grade") Grade grade,
                                          @Param("subjectName") String subjectName,
                                          @Param("teacherName") String teacherName,
                                          @Param("classroomCode") String classroomCode,
                                          @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                          Pageable pageable);

    @Query("select count(s) > 0 from LectureScheduleEntity s "
            + "where s.lecture.deletedAt is null "
            + "and s.lecture.classroomCode = :classroomCode and s.dayOfWeek = :dayOfWeek "
            + "and s.startTime < :endTime and :startTime < s.endTime")
    boolean existsOverlap(@Param("classroomCode") String classroomCode,
                           @Param("dayOfWeek") DayOfWeek dayOfWeek,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);

    @Query("select count(s) > 0 from LectureScheduleEntity s "
            + "where s.lecture.deletedAt is null "
            + "and s.lecture.id <> :excludedLectureId "
            + "and s.lecture.classroomCode = :classroomCode and s.dayOfWeek = :dayOfWeek "
            + "and s.startTime < :endTime and :startTime < s.endTime")
    boolean existsOverlapExcludingLecture(@Param("excludedLectureId") Long excludedLectureId,
                                           @Param("classroomCode") String classroomCode,
                                           @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime);

    /**
     * 매출 리포트 집계 전용 조회. Lecture aggregate(schedules 포함)를 통째로 복원하지 않고
     * 필요한 열만 뽑는다 — 이전에는 findAll()로 전체를 복원하다가 스케줄러(트랜잭션 밖 호출)에서
     * schedules 지연로딩이 LazyInitializationException을 던졌었다.
     */
    @Query("select l.id as id, l.name as name, l.teacherName as teacherName, l.feeAmount as feeAmount "
            + "from LectureEntity l where l.deletedAt is null")
    List<LectureRevenueProjection> findAllRevenueProjection();

    interface LectureRevenueProjection {
        Long getId();

        String getName();

        String getTeacherName();

        Integer getFeeAmount();
    }
}
