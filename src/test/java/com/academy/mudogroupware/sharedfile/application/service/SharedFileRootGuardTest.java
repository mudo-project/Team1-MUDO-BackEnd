package com.academy.mudogroupware.sharedfile.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;

class SharedFileRootGuardTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String ROOT_ID = "root-id";

    private final SharedFileDrivePort drivePort = mock(SharedFileDrivePort.class);
    private final SharedFileRootGuard guard = new SharedFileRootGuard(drivePort);

    @Test
    void requireDescendantPassesForDirectChildOfRoot() {
        DriveItem child = item("child-id", List.of(ROOT_ID));
        when(drivePort.getItem(ACCESS_TOKEN, "child-id")).thenReturn(Optional.of(child));

        assertThatCode(() -> guard.requireDescendant(ACCESS_TOKEN, ROOT_ID, "child-id"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireDescendantPassesForNestedChildOfRoot() {
        DriveItem grandchild = item("grandchild-id", List.of("child-id"));
        DriveItem child = item("child-id", List.of(ROOT_ID));
        when(drivePort.getItem(ACCESS_TOKEN, "grandchild-id")).thenReturn(Optional.of(grandchild));
        when(drivePort.getItem(ACCESS_TOKEN, "child-id")).thenReturn(Optional.of(child));

        assertThatCode(() -> guard.requireDescendant(ACCESS_TOKEN, ROOT_ID, "grandchild-id"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireDescendantThrowsWhenItemIsOutsideRoot() {
        DriveItem outside = item("outside-id", List.of());
        when(drivePort.getItem(ACCESS_TOKEN, "outside-id")).thenReturn(Optional.of(outside));

        assertThatThrownBy(() -> guard.requireDescendant(ACCESS_TOKEN, ROOT_ID, "outside-id"))
                .isInstanceOf(SharedFileOutOfRootException.class);
    }

    @Test
    void requireDescendantThrowsWhenItemIsMissing() {
        when(drivePort.getItem(ACCESS_TOKEN, "missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireDescendant(ACCESS_TOKEN, ROOT_ID, "missing-id"))
                .isInstanceOf(SharedFileItemNotFoundException.class);
    }

    @Test
    void requireDescendantThrowsWhenTargetIsTheSystemRootItself() {
        assertThatThrownBy(() -> guard.requireDescendant(ACCESS_TOKEN, ROOT_ID, ROOT_ID))
                .isInstanceOf(SharedFileOutOfRootException.class);
    }

    private DriveItem item(String id, List<String> parentIds) {
        return new DriveItem(id, "name", "application/octet-stream", parentIds, null, true, null, false);
    }
}
