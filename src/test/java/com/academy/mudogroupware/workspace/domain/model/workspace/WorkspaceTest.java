package com.academy.mudogroupware.workspace.domain.model.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceLastMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceMemberNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

  @Test
  void includesCreatorInMembersWhenCreated() {
    Workspace workspace = Workspace.create("개발팀", 10L, Set.of(20L));

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void removesCreatorDuplicateWhenCreated() {
    Workspace workspace = Workspace.create("개발팀", 10L, Set.of(10L, 20L));

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void preservesStoredMembersWithoutAddingCreatorWhenRestored() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(20L));

    assertThat(workspace.getMemberIds()).containsExactly(20L);
  }

  @Test
  void defensivelyCopiesAdditionalMembersWhenCreated() {
    Set<Long> additionalMemberIds = new LinkedHashSet<>(Set.of(20L));

    Workspace workspace = Workspace.create("개발팀", 10L, additionalMemberIds);
    additionalMemberIds.add(30L);

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void trimsNameWhenRenamed() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L));

    Workspace renamed = workspace.rename("  운영팀  ");

    assertThat(renamed.getName()).isEqualTo("운영팀");
    assertThat(renamed.getId()).isEqualTo(1L);
    assertThat(renamed.getMemberIds()).containsExactly(10L);
  }

  @Test
  void addsOnlyNewMembersAndKeepsExistingOnesWhenMembersAdded() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L, 20L));

    Workspace updated = workspace.addMembers(Set.of(20L, 30L));

    assertThat(updated.getMemberIds()).containsExactlyInAnyOrder(10L, 20L, 30L);
  }

  @Test
  void computesOnlyCandidatesNotAlreadyMembersAsNewlyAdded() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L, 20L));

    Set<Long> newlyAdded = workspace.newlyAddedMemberIds(Set.of(20L, 30L));

    assertThat(newlyAdded).containsExactly(30L);
  }

  @Test
  void removesTargetMemberWhenMoreThanOneMemberRemains() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L, 20L));

    Workspace updated = workspace.removeMember(20L);

    assertThat(updated.getMemberIds()).containsExactly(10L);
  }

  @Test
  void rejectsRemovingLastRemainingMember() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L));

    assertThatThrownBy(() -> workspace.removeMember(10L))
        .isInstanceOf(WorkspaceLastMemberException.class)
        .extracting("errorCode")
        .isEqualTo(WorkspaceErrorCode.LAST_MEMBER_CANNOT_LEAVE);
  }

  @Test
  void rejectsRemovingUserWhoIsNotAMember() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L, 20L));

    assertThatThrownBy(() -> workspace.removeMember(99L))
        .isInstanceOf(WorkspaceMemberNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo(WorkspaceErrorCode.MEMBER_NOT_FOUND);
  }

  @Test
  void recoversWithGivenFinalName() {
    Workspace workspace = Workspace.restore(1L, "개발팀", 10L, Set.of(10L, 20L));

    Workspace recovered = workspace.recover("개발팀(20260806153012)");

    assertThat(recovered.getName()).isEqualTo("개발팀(20260806153012)");
    assertThat(recovered.getId()).isEqualTo(1L);
    assertThat(recovered.getCreatedBy()).isEqualTo(10L);
    assertThat(recovered.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }
}
