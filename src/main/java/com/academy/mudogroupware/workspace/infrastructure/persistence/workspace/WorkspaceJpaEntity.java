package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.global.infrastructure.persistence.SoftDeleteTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceJpaEntity extends SoftDeleteTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workspace_id")
  private Long id;

  @Column(name = "academy_id", nullable = false)
  private Long academyId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "created_by", nullable = false, updatable = false)
  private Long createdBy;

  @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkspaceMemberJpaEntity> members = new ArrayList<>();

  @Builder
  private WorkspaceJpaEntity(Long academyId, String name, Long createdBy) {
    this.academyId = academyId;
    this.name = name;
    this.createdBy = createdBy;
  }

  public static WorkspaceJpaEntity create(Long academyId, String name, Long createdBy) {
    return WorkspaceJpaEntity.builder()
        .academyId(academyId)
        .name(name)
        .createdBy(createdBy)
        .build();
  }

  public WorkspaceMemberJpaEntity addMember(Long userId) {
    WorkspaceMemberJpaEntity member = WorkspaceMemberJpaEntity.create(this, userId);
    members.add(member);
    return member;
  }
}
