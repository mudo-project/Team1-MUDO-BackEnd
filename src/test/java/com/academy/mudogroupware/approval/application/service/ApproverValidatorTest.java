package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

class ApproverValidatorTest {

    private final ApproverDirectoryPort approverDirectoryPort = mock(ApproverDirectoryPort.class);
    private final ApproverValidator approverValidator = new ApproverValidator(approverDirectoryPort);

    @Test
    void passesWhenAllApproversExist() {
        when(approverDirectoryPort.getApprovers(List.of(1L, 2L))).thenReturn(Map.of(
                1L, new ApproverInfo(1L, "이민준"),
                2L, new ApproverInfo(2L, "김지수")));

        assertThatCode(() -> approverValidator.validate(List.of(1L, 2L))).doesNotThrowAnyException();
    }

    @Test
    void throwsWhenApproverDoesNotExist() {
        when(approverDirectoryPort.getApprovers(List.of(1L, 999L))).thenReturn(Map.of(
                1L, new ApproverInfo(1L, "이민준")));

        assertThatThrownBy(() -> approverValidator.validate(List.of(1L, 999L)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.APPROVER_NOT_FOUND);
    }
}
