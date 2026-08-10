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
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "workspace",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_workspace_academy_active_name",
            columnNames = {"academy_id", "active_name"}))
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

  // deleted_at이 null일 때만 name을 노출하는 생성 컬럼. V3.1.2 마이그레이션의 active_name과 동일한 정의를
  // 테스트 스키마(H2)에도 반영해, 소프트 삭제된 워크스페이스는 이름 중복 검사에서 제외되도록 한다.
  // 운영은 ddl-auto: none이라 이 정의가 실제 DDL에 쓰이지 않고, 실제 제약은 마이그레이션의 생성 컬럼이 담당한다.
  @Column(
      name = "active_name",
      insertable = false,
      updatable = false,
      columnDefinition =
          "VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END)")
  private String activeName;

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

  public void rename(String name) {
    this.name = name;
  }

  public void removeMember(Long userId) {
    members.removeIf(member -> member.getUserId().equals(userId));
  }
}
