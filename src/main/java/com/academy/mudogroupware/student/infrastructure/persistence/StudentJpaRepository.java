package com.academy.mudogroupware.student.infrastructure.persistence;

import java.time.LocalDateTime;
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
            where s.academyId = :academyId
              and s.deletedAt is null
              and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
            order by s.name asc, s.id asc
            """)
    Slice<StudentEntity> findAllByAcademyIdAndKeyword(
            @Param("academyId") Long academyId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<StudentEntity> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Query(value = "update student set deleted_at = :deletedAt "
            + "where student_id = :id and deleted_at is null", nativeQuery = true)
    int markDeleted(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
