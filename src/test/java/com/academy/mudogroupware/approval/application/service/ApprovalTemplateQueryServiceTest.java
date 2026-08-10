package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

class ApprovalTemplateQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 10, 0);

    private final ApprovalTemplateRepository approvalTemplateRepository = mock(ApprovalTemplateRepository.class);
    private final ApproverDirectoryPort approverDirectoryPort = mock(ApproverDirectoryPort.class);

    private ApprovalTemplateQueryService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalTemplateQueryService(approvalTemplateRepository, approverDirectoryPort);
    }

    @Test
    void getTemplatesResolvesAllLineApproversInOneBatch() {
        ApprovalTemplate first = template(1L, "Vacation", List.of(10L, 11L));
        ApprovalTemplate second = template(2L, "Expense", List.of(12L));
        when(approvalTemplateRepository.findAll(0, 20))
                .thenReturn(PageResult.of(List.of(first, second), 0, 20, false));
        when(approverDirectoryPort.getApprovers(List.of(10L, 11L, 12L))).thenReturn(Map.of(
                10L, new ApproverInfo(10L, "Approver A"),
                11L, new ApproverInfo(11L, "Approver B"),
                12L, new ApproverInfo(12L, "Approver C")));

        PageResult<ApprovalTemplateSummaryView> result = service.getTemplates(7L, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).lines()).extracting(line -> line.approverName())
                .containsExactly("Approver A", "Approver B");
        assertThat(result.content().get(1).lines()).extracting(line -> line.approverName())
                .containsExactly("Approver C");
        verify(approverDirectoryPort).getApprovers(List.of(10L, 11L, 12L));
    }

    private ApprovalTemplate template(Long id, String name, List<Long> approverIds) {
        List<ApprovalTemplateLine> lines = approverIds.stream()
                .map(approverId -> ApprovalTemplateLine.create(approverIds.indexOf(approverId) + 1, approverId))
                .toList();
        return ApprovalTemplate.restore(id, name, 7L, lines, NOW, NOW);
    }
}
