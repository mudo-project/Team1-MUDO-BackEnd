package com.academy.mudogroupware.student.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

    Optional<EnrollmentEntity> findByStudentIdAndLectureId(Long studentId, Long lectureId);

    Optional<EnrollmentEntity> findByStudentIdAndId(Long studentId, Long id);

    List<EnrollmentEntity> findAllByStudentIdAndStatusOrderByEnrolledAtDesc(
            Long studentId, EnrollmentStatus status);

    List<EnrollmentEntity> findAllByLectureIdAndStatus(Long lectureId, EnrollmentStatus status);

    @Query("""
            select e.studentId as studentId, count(e) as count
            from EnrollmentEntity e
            where e.status = :status
              and e.studentId in :studentIds
            group by e.studentId
            """)
    List<StudentEnrollmentCount> countByStudentIdsAndStatus(
            @Param("studentIds") List<Long> studentIds,
            @Param("status") EnrollmentStatus status
    );

    @Query("""
            select e.lectureId as lectureId, count(e) as count
            from EnrollmentEntity e
            where e.status = :status
              and e.lectureId in :lectureIds
            group by e.lectureId
            """)
    List<LectureEnrollmentCount> countByLectureIdsAndStatus(
            @Param("lectureIds") List<Long> lectureIds,
            @Param("status") EnrollmentStatus status
    );

    // Retention 배치 전용: 하드 삭제될 학생의 수강 이력(자식)을 먼저 지운다(FK 제약 때문에 부모보다 먼저 삭제해야 함).
    @Modifying
    @Query(value = "delete from student_enrollment where student_id in :studentIds", nativeQuery = true)
    int deleteAllByStudentIds(@Param("studentIds") List<Long> studentIds);

    /**
     * revenuereport의 결제-강의 매핑 전용 조회. 결제 건수만큼 커질 수 있는 IN절에서
     * EnrollmentEntity 전체를 로딩하지 않고 id/lectureId만 뽑는다.
     */
    @Query("select e.id as id, e.lectureId as lectureId from EnrollmentEntity e where e.id in :ids")
    List<EnrollmentLectureIdProjection> findLectureIdsByIdIn(@Param("ids") List<Long> ids);

    interface StudentEnrollmentCount {
        Long getStudentId();

        long getCount();
    }

    interface LectureEnrollmentCount {
        Long getLectureId();

        long getCount();
    }

    interface EnrollmentLectureIdProjection {
        Long getId();

        Long getLectureId();
    }
}
