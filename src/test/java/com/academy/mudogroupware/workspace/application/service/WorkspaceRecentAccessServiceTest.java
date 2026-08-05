package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceRecentAccessPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceRecentAccessServiceTest {

  @Mock private WorkspaceListQueryPort queryPort;
  @Mock private WorkspaceRecentAccessPort accessPort;

  private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T01:02:03Z"), ZoneOffset.UTC);
  private WorkspaceRecentAccessService service;

  @BeforeEach
  void setUp() {
    service = new WorkspaceRecentAccessService(queryPort, accessPort, clock);
  }

  @Test
  void recordsClockBasedAccessOnlyForAccessibleWorkspace() {
    when(queryPort.existsAccessible(100L, 1L, 10L, false)).thenReturn(true);

    service.recordRecentAccess(1L, 10L, 100L, false);

    verify(accessPort).upsert(10L, 100L, LocalDateTime.now(clock));
  }

  @Test
  void rejectsRecentAccessOutsideRequestersScope() {
    when(queryPort.existsAccessible(100L, 1L, 10L, false)).thenReturn(false);

    assertThatThrownBy(() -> service.recordRecentAccess(1L, 10L, 100L, false))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(accessPort);
  }
}
