package com.academy.mudogroupware.global.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class AfterCommitLoggerTest {

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void runsImmediatelyWhenNoTransactionIsActive() {
    AtomicBoolean ran = new AtomicBoolean(false);

    AfterCommitLogger.run(() -> ran.set(true));

    assertThat(ran).isTrue();
  }

  @Test
  void deferredUntilAfterCommitWhenTransactionIsActive() {
    TransactionSynchronizationManager.initSynchronization();
    AtomicBoolean ran = new AtomicBoolean(false);

    AfterCommitLogger.run(() -> ran.set(true));
    assertThat(ran).isFalse();

    TransactionSynchronizationUtils.triggerAfterCommit();

    assertThat(ran).isTrue();
  }
}
