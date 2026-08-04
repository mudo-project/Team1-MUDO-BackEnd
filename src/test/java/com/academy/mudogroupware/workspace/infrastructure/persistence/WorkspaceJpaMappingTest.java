package com.academy.mudogroupware.workspace.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskSkipJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskTemplateJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskCommentJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskCommentMentionJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceMemberJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Lob;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkspaceJpaMappingTest {

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void taskCommentContentMapsToRequiredTextColumn() throws NoSuchFieldException {
    Field contentField = TaskCommentJpaEntity.class.getDeclaredField("content");
    Column column = contentField.getAnnotation(Column.class);

    assertThat(column).isNotNull();
    assertThat(column.nullable()).isFalse();
    assertThat(column.columnDefinition()).isEqualTo("TEXT");
    assertThat(contentField.isAnnotationPresent(Lob.class)).isFalse();
  }

  @Test
  void workspacePersistenceEntitiesAreScanned() {
    Set<Class<?>> entityTypes =
        entityManagerFactory.getMetamodel().getEntities().stream()
            .map(EntityType::getJavaType)
            .collect(Collectors.toSet());

    assertThat(entityTypes)
        .contains(
            WorkspaceJpaEntity.class,
            WorkspaceMemberJpaEntity.class,
            RecurringTaskTemplateJpaEntity.class,
            TaskJpaEntity.class,
            TaskCommentJpaEntity.class,
            TaskStatusHistoryJpaEntity.class,
            TaskCommentMentionJpaEntity.class,
            RecurringTaskSkipJpaEntity.class);
  }
}
