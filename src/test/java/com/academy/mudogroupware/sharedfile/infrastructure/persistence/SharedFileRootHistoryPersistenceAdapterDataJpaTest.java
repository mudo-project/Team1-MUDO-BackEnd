package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({SharedFileRootHistoryPersistenceAdapter.class, TimeConfig.class})
class SharedFileRootHistoryPersistenceAdapterDataJpaTest {

    @Autowired
    private SharedFileRootHistoryPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findReturnsEmptyWhenEmailHasNoHistory() {
        Optional<String> found = adapter.findGoogleRootFolderIdByEmail("unknown@mudo.co.kr");

        assertThat(found).isEmpty();
    }

    @Test
    void upsertInsertsNewHistoryRowForFirstConnection() {
        adapter.upsert("academy@mudo.co.kr", "folder-a", LocalDateTime.of(2026, 8, 17, 0, 0));
        entityManager.flush();

        Optional<String> found = adapter.findGoogleRootFolderIdByEmail("academy@mudo.co.kr");
        assertThat(found).contains("folder-a");
    }

    // A -> B -> A 시나리오에서 A의 이력이 B가 연결돼 있는 동안에도 살아있어야 한다.
    @Test
    void keepsHistoryForOtherEmailsUntouched() {
        adapter.upsert("a@mudo.co.kr", "folder-a", LocalDateTime.of(2026, 8, 1, 0, 0));
        adapter.upsert("b@mudo.co.kr", "folder-b", LocalDateTime.of(2026, 8, 10, 0, 0));
        entityManager.flush();

        assertThat(adapter.findGoogleRootFolderIdByEmail("a@mudo.co.kr")).contains("folder-a");
        assertThat(adapter.findGoogleRootFolderIdByEmail("b@mudo.co.kr")).contains("folder-b");
    }

    // 같은 이메일로 재연동해서 새 폴더가 만들어진 경우(예: 옛 폴더가 지워져 재생성됨), 이력은 최신 값으로 갱신돼야 한다.
    @Test
    void upsertOverwritesFolderIdWhenSameEmailConnectsAgainWithADifferentFolder() {
        adapter.upsert("academy@mudo.co.kr", "folder-old", LocalDateTime.of(2026, 8, 1, 0, 0));
        entityManager.flush();

        adapter.upsert("academy@mudo.co.kr", "folder-new", LocalDateTime.of(2026, 8, 17, 0, 0));
        entityManager.flush();

        Optional<String> found = adapter.findGoogleRootFolderIdByEmail("academy@mudo.co.kr");
        assertThat(found).contains("folder-new");
    }
}
