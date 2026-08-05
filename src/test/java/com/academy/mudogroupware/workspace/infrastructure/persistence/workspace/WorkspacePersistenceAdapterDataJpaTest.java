package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, WorkspacePersistenceAdapter.class, WorkspacePersistenceMapperImpl.class})
class WorkspacePersistenceAdapterDataJpaTest {

  @Autowired private WorkspacePersistenceAdapter workspaceRepository;

  @Test
  void savesWorkspaceAndReturnsItsPersistedAggregate() {
    Workspace workspace =
        Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of(20L));

    Workspace saved = workspaceRepository.save(workspace);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getAcademyId()).isEqualTo(1L);
    assertThat(saved.getName()).isEqualTo("\uac1c\ubc1c\ud300");
    assertThat(saved.getCreatedBy()).isEqualTo(10L);
    assertThat(saved.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void findsOnlyExistingActiveWorkspaceNameInAcademy() {
    assertThat(workspaceRepository.existsByAcademyIdAndName(1L, "\uac1c\ubc1c\ud300")).isFalse();

    workspaceRepository.save(
        Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of()));

    assertThat(workspaceRepository.existsByAcademyIdAndName(1L, "\uac1c\ubc1c\ud300")).isTrue();
    assertThat(workspaceRepository.existsByAcademyIdAndName(2L, "\uac1c\ubc1c\ud300")).isFalse();
    assertThat(workspaceRepository.existsByAcademyIdAndName(1L, "\uc6b4\uc601\ud300")).isFalse();
  }

  @Test
  void findsActiveWorkspaceForUpdateByIdIncludingMembers() {
    Workspace saved =
        workspaceRepository.save(Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of(20L)));

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
    Workspace saved = workspaceRepository.save(Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of()));

    workspaceRepository.rename(saved.getId(), "\uc6b4\uc601\ud300");

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("\uc6b4\uc601\ud300");
  }

  // 참고: H2(create-drop) 스키마는 운영 DB의 active_name 생성 컬럼을 그대로 재현하지 못해
  // (academy_id, name) 리터럴 유니크 제약으로 대체함 — 소프트 삭제된 이름 재사용 시나리오까지
  // 정확히 검증하려면 MySQL 컨테이너 기반 테스트가 필요하다.
  @Test
  void rejectsRenameToDuplicateActiveNameInSameAcademy() {
    workspaceRepository.save(Workspace.create(1L, "\uc6b4\uc601\ud300", 10L, Set.of()));
    Workspace saved = workspaceRepository.save(Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of()));

    assertThatThrownBy(() -> workspaceRepository.rename(saved.getId(), "\uc6b4\uc601\ud300"))
        .isInstanceOf(WorkspaceNameConflictException.class);
  }

  @Test
  void addsAndRemovesMembersToMatchTargetSet() {
    Workspace saved =
        workspaceRepository.save(Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of(20L, 30L)));

    workspaceRepository.updateMembers(saved.getId(), Set.of(10L, 40L));

    Optional<Workspace> found = workspaceRepository.findByIdForUpdate(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getMemberIds()).containsExactlyInAnyOrder(10L, 40L);
  }

  @Test
  void marksWorkspaceDeletedAndExcludesItFromActiveLookup() {
    Workspace saved = workspaceRepository.save(Workspace.create(1L, "\uac1c\ubc1c\ud300", 10L, Set.of()));

    workspaceRepository.delete(saved.getId(), java.time.LocalDateTime.of(2026, 8, 6, 12, 0));

    assertThat(workspaceRepository.findByIdForUpdate(saved.getId())).isEmpty();
  }
}
