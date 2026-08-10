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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_line_step",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_approval_line_step_template_step",
                columnNames = {"template_id", "step_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_step_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ApprovalTemplateEntity template;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "role_id")
    private Long roleId;

    @Builder
    private ApprovalTemplateLineEntity(Long id, int stepOrder, Long approverId, Long roleId) {
        this.id = id;
        this.stepOrder = stepOrder;
        this.approverId = approverId;
        this.roleId = roleId;
    }

    void assignTemplate(ApprovalTemplateEntity template) {
        this.template = template;
    }
}
