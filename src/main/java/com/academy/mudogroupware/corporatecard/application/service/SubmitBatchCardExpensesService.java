package com.academy.mudogroupware.corporatecard.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.corporatecard.application.command.SubmitBatchCardExpensesCommand;
import com.academy.mudogroupware.corporatecard.application.command.SubmitCardExpenseCommand;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;
import com.academy.mudogroupware.corporatecard.application.port.CardExpensePort;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort;
import com.academy.mudogroupware.corporatecard.application.query.BatchSubmitCardExpensesResult;
import com.academy.mudogroupware.corporatecard.application.usecase.SubmitBatchCardExpensesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitBatchCardExpensesService implements SubmitBatchCardExpensesUseCase {
    private static final Long CORPORATE_CARD_APPROVAL_TEMPLATE_ID = 1L;

    private final CorporateCardExpenseService singleSubmitService;
    private final CorporateCardTransactionPort transactionPort;
    private final ApprovalSubmissionPort approvalSubmissionPort;
    private final CardExpensePort expensePort;

    @Override
    public BatchSubmitCardExpensesResult submit(
            SubmitBatchCardExpensesCommand command, Long userId) {
        log.info("event=corporate_card_expense_batch_submit_시작 userId={}, count={}",
                userId, command.items() == null ? 0 : command.items().size());
        try {
        validateTransactionIds(command.items());
        List<Long> commonApproverIds = normalize(command.approverIds());
        Map<Long, String> preValidationFailures = commonApproverIds == null
                ? validateDefaultApprovalLines(command.items())
                : Map.of();

        List<BatchSubmitCardExpensesResult.ItemResult> results = new ArrayList<>();
        for (SubmitBatchCardExpensesCommand.Item item : command.items()) {
            String preValidationFailure = preValidationFailures.get(item.transactionId());
            if (preValidationFailure != null) {
                results.add(new BatchSubmitCardExpensesResult.ItemResult(
                        item.transactionId(), false, null, preValidationFailure));
                continue;
            }
            try {
                var expense = expensePort.findByTransactionId(item.transactionId()).orElse(null);
                if (expense == null) {
                    throw new IllegalStateException("먼저 정산 정보를 저장해야 결재를 상신할 수 있습니다.");
                }
                SubmitCardExpenseCommand singleCommand = new SubmitCardExpenseCommand(
                        item.transactionId(), userId, expense.category(), expense.purpose(), commonApproverIds);
                var submitted = singleSubmitService.submit(singleCommand);
                results.add(new BatchSubmitCardExpensesResult.ItemResult(
                        item.transactionId(), true, submitted.approvalDocumentId(), null));
            } catch (RuntimeException e) {
                results.add(new BatchSubmitCardExpensesResult.ItemResult(
                        item.transactionId(), false, null, e.getMessage()));
            }
        }

        int successCount = (int) results.stream().filter(BatchSubmitCardExpensesResult.ItemResult::success).count();
        BatchSubmitCardExpensesResult result = new BatchSubmitCardExpensesResult(
                successCount, results.size() - successCount, results);
        log.info("event=corporate_card_expense_batch_submit_완료 userId={}, successCount={}, failureCount={}",
                userId, result.successCount(), result.failureCount());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=corporate_card_expense_batch_submit_실패 userId={}, errorType={}",
                    userId, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void validateTransactionIds(List<SubmitBatchCardExpensesCommand.Item> items) {
        if (items == null || items.isEmpty()
                || items.stream().anyMatch(item -> item == null || item.transactionId() == null)
                || new HashSet<>(items.stream().map(SubmitBatchCardExpensesCommand.Item::transactionId).toList()).size() != items.size()) {
            throw new IllegalArgumentException("중복되지 않은 transactionId를 한 건 이상 입력해야 합니다.");
        }
    }

    private Map<Long, String> validateDefaultApprovalLines(
            List<SubmitBatchCardExpensesCommand.Item> items) {
        List<Long> expected = null;
        Map<Long, String> failures = new HashMap<>();
        for (SubmitBatchCardExpensesCommand.Item item : items) {
            var transaction = transactionPort.find(item.transactionId()).orElse(null);
            if (transaction == null) {
                failures.put(item.transactionId(), "카드 사용내역을 찾을 수 없습니다.");
                continue;
            }
            List<Long> current = approvalSubmissionPort.findDefaultApproverIds(CORPORATE_CARD_APPROVAL_TEMPLATE_ID);
            if (current.isEmpty()) {
                failures.put(item.transactionId(), "법인카드 결재선을 지정해야 합니다.");
                continue;
            }
            if (expected == null) {
                expected = current;
            } else if (!expected.equals(current)) {
                throw new IllegalStateException("선택한 카드 사용내역의 기본 결재선이 서로 다릅니다.");
            }
        }
        return failures;
    }

    private List<Long> normalize(List<Long> approverIds) {
        if (approverIds == null || approverIds.isEmpty()) {
            return null;
        }
        if (approverIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(approverIds).size() != approverIds.size()) {
            throw new IllegalArgumentException("결재선에는 중복된 결재자가 포함될 수 없습니다.");
        }
        return List.copyOf(approverIds);
    }
}
