package com.academy.mudogroupware.corporatecard.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.corporatecard.application.service.CorporateCardExpenseService;
import com.academy.mudogroupware.corporatecard.application.usecase.SubmitBatchCardExpensesUseCase;
import com.academy.mudogroupware.corporatecard.presentation.api.request.BatchSubmitCardExpensesRequest;
import com.academy.mudogroupware.corporatecard.presentation.api.request.SubmitCardExpenseRequest;
import com.academy.mudogroupware.corporatecard.presentation.api.request.SaveCardExpenseRequest;
import com.academy.mudogroupware.corporatecard.presentation.api.response.BatchSubmitCardExpensesResponse;
import com.academy.mudogroupware.corporatecard.presentation.api.response.CardExpenseResponse;
import com.academy.mudogroupware.corporatecard.presentation.api.response.CorporateCardTransactionPageResponse;
import com.academy.mudogroupware.corporatecard.presentation.api.response.ReceiptReconciliationResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "법인카드", description = "법인카드 사용내역 및 정산 API")
@RestController
@RequestMapping("/api/corporate-card/transactions")
@RequiredArgsConstructor
public class CorporateCardController {

    private final CorporateCardExpenseService service;
    private final SubmitBatchCardExpensesUseCase batchSubmitUseCase;

    @Operation(summary = "법인카드 사용내역 조회")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @GetMapping
    public GlobalApiResponse<CorporateCardTransactionPageResponse> getTransactions(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page는 0 이상, size는 1~100이어야 합니다.");
        }
        return GlobalApiResponse.ok("CORPORATE_CARD_TRANSACTIONS_RETRIEVED", "법인카드 사용내역 조회가 완료되었습니다.",
                CorporateCardTransactionPageResponse.from(service.getTransactions(page, size)));
    }

    @Operation(summary = "법인카드 사용내역 상세 조회")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @GetMapping("/{transactionId}")
    public GlobalApiResponse<CardExpenseResponse> getTransaction(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long transactionId) {
        return GlobalApiResponse.ok("CORPORATE_CARD_TRANSACTION_RETRIEVED", "법인카드 사용내역 조회가 완료되었습니다.",
                CardExpenseResponse.from(service.getTransaction(transactionId)));
    }

    @Operation(summary = "영수증-카드거래 대사 검증", description = "정산 상신된 영수증 첨부파일에서 AI로 금액/일자/가맹점을 추출해 "
            + "실제 카드 승인 거래와 일치하는지 확인한다. 결과는 저장하지 않고 요청 시마다 계산한다. 불일치여도 결재 자체를 막지 않고 참고용으로만 보여준다.")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @PostMapping("/{transactionId}/reconcile-receipt")
    public GlobalApiResponse<ReceiptReconciliationResponse> reconcileReceipt(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long transactionId) {
        return GlobalApiResponse.ok("CORPORATE_CARD_RECEIPT_RECONCILED", "영수증 대사 검증이 완료되었습니다.",
                ReceiptReconciliationResponse.from(service.reconcileReceipt(transactionId)));
    }

    @Operation(summary = "법인카드 사용내역 일괄 정산 상신")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @PostMapping("/batch-submit")
    public GlobalApiResponse<BatchSubmitCardExpensesResponse> submitBatch(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BatchSubmitCardExpensesRequest request) {
        BatchSubmitCardExpensesResponse response = BatchSubmitCardExpensesResponse.from(
                batchSubmitUseCase.submit(request.toCommand(), authUser.userId()));
        return GlobalApiResponse.ok("CORPORATE_CARD_EXPENSES_SUBMITTED",
                "법인카드 사용내역 일괄 상신 처리가 완료되었습니다.", response);
    }

    @Operation(summary = "법인카드 사용내역 정산 상신")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @PostMapping("/{transactionId}/submit")
    public ResponseEntity<GlobalApiResponse<CardExpenseResponse>> submit(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long transactionId,
            @Valid @RequestBody SubmitCardExpenseRequest request) {
        CardExpenseResponse response = CardExpenseResponse.from(
                service.submit(request.toCommand(transactionId, authUser.userId())));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created("CORPORATE_CARD_EXPENSE_SUBMITTED",
                        "법인카드 정산이 상신되었습니다.", response));
    }

    @Operation(summary = "법인카드 정산 정보 저장")
    @PreAuthorize("hasAuthority('CORPORATE_CARD:EXPENSE')")
    @PutMapping("/{transactionId}/expense")
    public GlobalApiResponse<CardExpenseResponse> saveExpense(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long transactionId,
            @Valid @RequestBody SaveCardExpenseRequest request) {
        CardExpenseResponse response = CardExpenseResponse.from(service.saveExpense(
                transactionId, authUser.userId(), request.category(), request.purpose()));
        return GlobalApiResponse.ok("CORPORATE_CARD_EXPENSE_SAVED",
                "법인카드 정산 정보가 저장되었습니다.", response);
    }
}
