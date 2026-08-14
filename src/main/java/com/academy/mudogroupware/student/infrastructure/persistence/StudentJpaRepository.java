package com.academy.mudogroupware.student.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, Long> {

    @Query("""
            select s
            from StudentEntity s
            where s.deletedAt is null
              and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
            order by s.name asc, s.id asc
            """)
    Slice<StudentEntity> findAllByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<StudentEntity> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();

    @Modifying
    @Query(value = "update student set deleted_at = :deletedAt "
            + "where student_id = :id and deleted_at is null", nativeQuery = true)
    int markDeleted(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    // Retention 배치 전용: 소프트 삭제 후 threshold 시각까지 보관 기간이 경과한 학생만 하드 삭제 대상으로 잡는다.
    @Query(value = "select student_id from student "
            + "where deleted_at is not null and deleted_at <= :threshold "
            + "order by deleted_at asc limit :batchSize for update", nativeQuery = true)
    List<Long> findHardDeleteCandidateIds(@Param("threshold") LocalDateTime threshold,
                                           @Param("batchSize") int batchSize);

    // 후보 조회와 삭제 사이 데이터가 바뀌는 경우를 막기 위해 threshold 조건을 삭제 시점에도 다시 검사한다.
    @Modifying
    @Query(value = "delete from student "
            + "where student_id in :ids and deleted_at is not null and deleted_at <= :threshold",
            nativeQuery = true)
    int hardDeleteByIds(@Param("ids") List<Long> ids, @Param("threshold") LocalDateTime threshold);
}
