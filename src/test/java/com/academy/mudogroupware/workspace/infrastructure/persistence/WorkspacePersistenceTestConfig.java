package com.academy.mudogroupware.workspace.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.MyTaskListQueryAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskSkipPersistenceAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskTemplatePersistenceAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskTemplatePersistenceMapperImpl;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskPersistenceAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskPersistenceMapperImpl;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryPersistenceAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceDetailQueryAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceListQueryAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspacePersistenceAdapter;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspacePersistenceMapperImpl;

import org.springframework.context.annotation.Import;

// workspace 도메인의 H2 기반 @DataJpaTest들이 각자 다른 @Import 조합을 쓰면 Spring이 매번 새
// ApplicationContext를 만든다(MergedContextConfiguration이 달라지므로 캐시 재사용 불가). 이 테스트들이
// 전부 이 클래스 하나만 @Import하도록 통일하면, 설정이 동일해져 컨텍스트를 한 번만 만들고 재사용한다.
// 개별 테스트가 실제로 쓰지 않는 어댑터까지 같이 뜨는 오버헤드는, 테스트마다 따로 컨텍스트를
// 새로 만드는 비용보다 훨씬 작다. 동시성 검증용 *MySqlIntegrationTest는 @AutoConfigureTestDatabase
// 설정 자체가 달라 여기 포함하지 않는다(그대로 별도 컨텍스트 유지).
@Import({
        TimeConfig.class,
        MyTaskListQueryAdapter.class,
        RecurringTaskTemplatePersistenceAdapter.class,
        RecurringTaskSkipPersistenceAdapter.class,
        RecurringTaskTemplatePersistenceMapperImpl.class,
        TaskPersistenceAdapter.class,
        TaskStatusHistoryPersistenceAdapter.class,
        TaskPersistenceMapperImpl.class,
        WorkspaceDetailQueryAdapter.class,
        WorkspaceListQueryAdapter.class,
        WorkspacePersistenceAdapter.class,
        WorkspacePersistenceMapperImpl.class
})
public class WorkspacePersistenceTestConfig {
}
