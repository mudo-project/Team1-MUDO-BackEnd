package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이 엔티티는 approval 모듈 전용 테이블이 아니라, 팀이 여러 기능(결재/문자 등)에서 공용으로 쓰기로 한
 * 공유 "template" 테이블에 매핑된다. type 컬럼으로 구분하며, 이 모듈은 "APPROVAL" 값만 다룬다.
 */
@Entity
@Table(name = "template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateEntity extends BaseTimeEntity {

    public static final String TYPE = "APPROVAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(nullable = false, length = 30)
    private String type;

    @Setter
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_by", nullable = false)
    private Long creatorId;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    @BatchSize(size = 100)
    private List<ApprovalTemplateLineEntity> lines = new ArrayList<>();

    @Builder
    private ApprovalTemplateEntity(Long id, Long academyId, String name, Long creatorId) {
        this.id = id;
        this.academyId = academyId;
        this.fileId = null;
        this.type = TYPE;
        this.name = name;
        this.creatorId = creatorId;
    }

    public void addLine(ApprovalTemplateLineEntity line) {
        lines.add(line);
        line.assignTemplate(this);
    }

    public void clearLines() {
        lines.clear();
    }
}
