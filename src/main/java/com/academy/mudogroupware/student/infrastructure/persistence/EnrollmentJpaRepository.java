package com.academy.mudogroupware.student.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

    Optional<EnrollmentEntity> findByAcademyIdAndStudentIdAndLectureId(
            Long academyId, Long studentId, Long lectureId);

    Optional<EnrollmentEntity> findByAcademyIdAndStudentIdAndId(Long academyId, Long studentId, Long id);

    List<EnrollmentEntity> findAllByAcademyIdAndStudentIdAndStatusOrderByEnrolledAtDesc(
            Long academyId, Long studentId, EnrollmentStatus status);

    List<EnrollmentEntity> findAllByAcademyIdAndLectureIdAndStatus(
            Long academyId, Long lectureId, EnrollmentStatus status);

    @Query("""
            select e.studentId as studentId, count(e) as count
            from EnrollmentEntity e
            where e.academyId = :academyId
              and e.status = :status
              and e.studentId in :studentIds
            group by e.studentId
            """)
    List<StudentEnrollmentCount> countByStudentIdsAndStatus(
            @Param("academyId") Long academyId,
            @Param("studentIds") List<Long> studentIds,
            @Param("status") EnrollmentStatus status
    );

    @Query("""
            select e.lectureId as lectureId, count(e) as count
            from EnrollmentEntity e
            where e.academyId = :academyId
              and e.status = :status
              and e.lectureId in :lectureIds
            group by e.lectureId
            """)
    List<LectureEnrollmentCount> countByLectureIdsAndStatus(
            @Param("academyId") Long academyId,
            @Param("lectureIds") List<Long> lectureIds,
            @Param("status") EnrollmentStatus status
    );

    // Retention 배치 전용: 하드 삭제될 학생의 수강 이력(자식)을 먼저 지운다(FK 제약 때문에 부모보다 먼저 삭제해야 함).
    @Modifying
    @Query(value = "delete from student_enrollment where student_id in :studentIds", nativeQuery = true)
    int deleteAllByStudentIds(@Param("studentIds") List<Long> studentIds);

    interface StudentEnrollmentCount {
        Long getStudentId();

        long getCount();
    }

    interface LectureEnrollmentCount {
        Long getLectureId();

        long getCount();
    }
}
