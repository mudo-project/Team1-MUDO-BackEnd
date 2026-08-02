package com.academy.mudogroupware.approval.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_template_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_template_id", nullable = false)
    private ApprovalTemplateEntity approvalTemplate;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Builder
    private ApprovalTemplateLineEntity(Long id, int stepOrder, Long approverId) {
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
    }

    void assignTemplate(ApprovalTemplateEntity approvalTemplate) {
        this.approvalTemplate = approvalTemplate;
    }
}
