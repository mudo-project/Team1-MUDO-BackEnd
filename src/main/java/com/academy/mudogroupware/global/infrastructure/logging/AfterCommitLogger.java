package com.academy.mudogroupware.global.infrastructure.logging;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 완료(_완료) 로그를 트랜잭션 커밋 이후에만 남기기 위한 헬퍼.
// 저장 직후 로그를 남기면 이후 커밋 시점에 제약조건 위반·deadlock 등으로 롤백돼도
// 성공 로그가 남아 실제 실패를 성공으로 오인할 수 있다.
public final class AfterCommitLogger {

  private AfterCommitLogger() {}

  public static void run(Runnable logStatement) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      logStatement.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            logStatement.run();
          }
        });
  }
}
