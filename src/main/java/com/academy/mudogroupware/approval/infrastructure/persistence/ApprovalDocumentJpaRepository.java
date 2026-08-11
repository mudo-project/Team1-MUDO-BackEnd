package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Collection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

public interface ApprovalDocumentJpaRepository extends JpaRepository<ApprovalDocumentEntity, Long> {

    @Query("select distinct d from ApprovalDocumentEntity d join d.lines l where l.approverId = :approverId")
    List<ApprovalDocumentEntity> findAllByApproverId(@Param("approverId") Long approverId);

    @Query("""
            select count(distinct d)
            from ApprovalDocumentEntity d
            join d.lines l
            where l.approverId = :approverId
              and d.status = :documentStatus
              and l.status = :lineStatus
            """)
    long countPendingByApproverId(@Param("approverId") Long approverId,
                                  @Param("documentStatus") ApprovalStatus documentStatus,
                                  @Param("lineStatus") ApprovalLineStatus lineStatus);

    @Query("select distinct d from ApprovalDocumentEntity d join d.lines l where l.approverId = :approverId")
    Slice<ApprovalDocumentEntity> findAllByApproverId(@Param("approverId") Long approverId, Pageable pageable);

    Slice<ApprovalDocumentEntity> findAllByCreatorId(Long creatorId, Pageable pageable);

    Slice<ApprovalDocumentEntity> findAllBy(Pageable pageable);

    @Query("""
            select distinct d
            from ApprovalDocumentEntity d
            join d.lines l
            where l.approverId = :approverId
              and l.status in :statuses
              and not exists (
                  select h.id
                  from ApprovalHistoryHiddenEntity h
                  where h.approvalDocumentId = d.id
                    and h.userId = :approverId
              )
            """)
    Slice<ApprovalDocumentEntity> findHistoryByApproverId(@Param("approverId") Long approverId,
                                                          @Param("statuses") Collection<ApprovalLineStatus> statuses,
                                                          Pageable pageable);
}
