package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({SharedFileRootPersistenceAdapter.class, TimeConfig.class})
class SharedFileRootPersistenceAdapterDataJpaTest {

    @Autowired
    private SharedFileRootPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void insertsNewRootWhenNoneExists() {
        SharedFileRoot saved = adapter.save(SharedFileRoot.ready("drive-folder-1"));
        entityManager.flush();

        assertThat(saved.getGoogleRootFolderId()).isEqualTo("drive-folder-1");
        assertThat(saved.getVersion()).isNotNull();

        Optional<SharedFileRoot> found = adapter.find();
        assertThat(found).isPresent();
        assertThat(found.get().getGoogleRootFolderId()).isEqualTo("drive-folder-1");
    }

    @Test
    void updatesExistingRootWhenVersionMatches() {
        adapter.save(SharedFileRoot.ready("drive-folder-1"));
        entityManager.flush();

        SharedFileRoot loaded = adapter.find().orElseThrow();
        Long originalVersion = loaded.getVersion();
        loaded.replaceWith("drive-folder-2");

        adapter.save(loaded);
        entityManager.flush();

        // save()의 반환값은 merge() 직후 상태라 flush 전이라 버전이 아직 안 올라가 있을 수 있다.
        // flush로 실제 UPDATE가 실행된 뒤 다시 조회해야 반영된 버전을 확인할 수 있다.
        SharedFileRoot reloaded = adapter.find().orElseThrow();
        assertThat(reloaded.getGoogleRootFolderId()).isEqualTo("drive-folder-2");
        assertThat(reloaded.getVersion()).isGreaterThan(originalVersion);
    }

    // 재현: A가 읽은 뒤(버전 0) B가 먼저 저장해 버전을 1로 올리면, A가 여전히 버전 0을 들고 저장을
    // 시도할 때 낙관적 락 충돌로 실패해야 한다 — B의 결과를 조용히 덮어써서는 안 된다.
    @Test
    void throwsOptimisticLockExceptionWhenAnotherTransactionAlreadyUpdatedTheRow() {
        adapter.save(SharedFileRoot.ready("drive-folder-1"));
        entityManager.flush();

        SharedFileRoot staleRead = adapter.find().orElseThrow();

        SharedFileRoot concurrentRead = adapter.find().orElseThrow();
        concurrentRead.replaceWith("drive-folder-2");
        adapter.save(concurrentRead);
        entityManager.flush();

        staleRead.replaceWith("drive-folder-3");

        assertThatThrownBy(() -> {
            adapter.save(staleRead);
            entityManager.flush();
        }).isInstanceOfAny(OptimisticLockingFailureException.class, OptimisticLockException.class);
    }
}
