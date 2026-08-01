package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ApprovalContentType contentType;

    @Lob
    private String text;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "approvalTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    private List<ApprovalLineEntity> approvalLines = new ArrayList<>();

    @Builder
    private ApprovalTemplateEntity(Long id, String title, ApprovalContentType contentType, String text,
                                    String fileUrl, Long creatorId, ApprovalStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.contentType = contentType;
        this.text = text;
        this.fileUrl = fileUrl;
        this.creatorId = creatorId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addLine(ApprovalLineEntity line) {
        approvalLines.add(line);
        line.assignTemplate(this);
    }
}
