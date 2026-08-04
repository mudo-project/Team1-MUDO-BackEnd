package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
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
        Workspace.builder()
            .academyId(1L)
            .name("\uac1c\ubc1c\ud300")
            .createdBy(10L)
            .memberIds(Set.of(10L, 20L))
            .build();

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
        Workspace.builder()
            .academyId(1L)
            .name("\uac1c\ubc1c\ud300")
            .createdBy(10L)
            .memberIds(Set.of(10L))
            .build());

    assertThat(workspaceRepository.existsByAcademyIdAndName(1L, "\uac1c\ubc1c\ud300")).isTrue();
    assertThat(workspaceRepository.existsByAcademyIdAndName(2L, "\uac1c\ubc1c\ud300")).isFalse();
    assertThat(workspaceRepository.existsByAcademyIdAndName(1L, "\uc6b4\uc601\ud300")).isFalse();
  }
}
