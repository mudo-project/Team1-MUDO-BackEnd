package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAlreadyActiveException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, WorkspacePersistenceAdapter.class, WorkspacePersistenceMapperImpl.class})
class WorkspacePersistenceAdapterDataJpaTest {

  @Autowired private WorkspacePersistenceAdapter workspaceRepository;

  @Test
  void savesWorkspaceAndReturnsItsPersistedAggregate() {
    Workspace workspace =
        Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of(20L));

    Workspace saved = workspaceRepository.save(workspace);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("\uac1c\ubc1c\ud300");
    assertThat(saved.getCreatedBy()).isEqualTo(10L);
    assertThat(saved.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void findsOnlyExistingActiveWorkspaceName() {
    assertThat(workspaceRepository.existsByName("\uac1c\ubc1c\ud300")).isFalse();

    workspaceRepository.save(
        Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of()));

    assertThat(workspaceRepository.existsByName("\uac1c\ubc1c\ud300")).isTrue();
    assertThat(workspaceRepository.existsByName("\uc6b4\uc601\ud300")).isFalse();
  }

  @Test
  void findsActiveWorkspaceForUpdateByIdIncludingMembers() {
    Workspace saved =
        workspaceRepository.save(Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of(20L)));

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void returnsEmptyWhenFindingNonExistentWorkspaceForUpdate() {
    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(999L);

    assertThat(found).isEmpty();
  }

  @Test
  void renamesWorkspace() {
    Workspace saved = workspaceRepository.save(Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of()));

    workspaceRepository.rename(saved.getId(), "\uc6b4\uc601\ud300");

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("\uc6b4\uc601\ud300");
  }

  @Test
  void rejectsRenameToDuplicateActiveNameInSameAcademy() {
    workspaceRepository.save(Workspace.create("\uc6b4\uc601\ud300", 10L, Set.of()));
    Workspace saved = workspaceRepository.save(Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of()));

    assertThatThrownBy(() -> workspaceRepository.rename(saved.getId(), "\uc6b4\uc601\ud300"))
        .isInstanceOf(WorkspaceNameConflictException.class);
  }

  // active_name 생성 컬럼(deleted_at이 null일 때만 name 노출)을 H2 스키마에도 반영했으므로,
  // 소프트 삭제된 워크스페이스의 이름은 유니크 제약에서 제외되어야 한다.
  @Test
  void allowsRenamingToNameOfSoftDeletedWorkspaceInSameAcademy() {
    Workspace deleted = workspaceRepository.save(Workspace.create("운영팀", 10L, Set.of()));
    workspaceRepository.delete(deleted.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));
    Workspace saved = workspaceRepository.save(Workspace.create("개발팀", 10L, Set.of()));

    workspaceRepository.rename(saved.getId(), "운영팀");

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("운영팀");
  }

  @Test
  void addsAndRemovesMembersToMatchTargetSet() {
    Workspace saved =
        workspaceRepository.save(Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of(20L, 30L)));

    workspaceRepository.updateMembers(saved.getId(), Set.of(10L, 40L));

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getMemberIds()).containsExactlyInAnyOrder(10L, 40L);
  }

  @Test
  void marksWorkspaceDeletedAndExcludesItFromActiveLookup() {
    Workspace saved = workspaceRepository.save(Workspace.create("\uac1c\ubc1c\ud300", 10L, Set.of()));

    workspaceRepository.delete(saved.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

    assertThat(workspaceRepository.findByIdForUpdate(saved.getId())).isEmpty();
  }

  @Test
  void returnsEmptyWhenFindingNonExistentWorkspaceForDeletedLookup() {
    Optional<Workspace> found = workspaceRepository.findDeletedByIdForUpdate(999L);

    assertThat(found).isEmpty();
  }

  @Test
  void rejectsFindingDeletedWorkspaceWhenItIsStillActive() {
    Workspace saved = workspaceRepository.save(Workspace.create("개발팀", 10L, Set.of()));

    assertThatThrownBy(() -> workspaceRepository.findDeletedByIdForUpdate(saved.getId()))
        .isInstanceOf(WorkspaceAlreadyActiveException.class);
  }

  @Test
  void findsDeletedWorkspaceForUpdate() {
    Workspace saved = workspaceRepository.save(Workspace.create("개발팀", 10L, Set.of(20L)));
    workspaceRepository.delete(saved.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

    Optional<Workspace> found = workspaceRepository.findDeletedByIdForUpdate(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("개발팀");
    assertThat(found.get().getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void recoversDeletedWorkspaceWithNewName() {
    Workspace saved = workspaceRepository.save(Workspace.create("개발팀", 10L, Set.of()));
    workspaceRepository.delete(saved.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

    workspaceRepository.recover(saved.getId(), "개발팀(20260806153012)");

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("개발팀(20260806153012)");
  }

  @Test
  void rejectsRecoverWhenNameConflictsWithActiveWorkspace() {
    workspaceRepository.save(Workspace.create("운영팀", 10L, Set.of()));
    Workspace saved = workspaceRepository.save(Workspace.create("개발팀", 10L, Set.of()));
    workspaceRepository.delete(saved.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

    assertThatThrownBy(() -> workspaceRepository.recover(saved.getId(), "운영팀"))
        .isInstanceOf(WorkspaceNameConflictException.class);
  }
}
