package com.academy.mudogroupware.corporatecard.application.query;

import java.time.LocalDate;

import com.academy.mudogroupware.corporatecard.application.port.ApprovalAttachmentFieldsPort.ExtractedReceiptFields;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardTransactionPort.TransactionView;

/**
 * 실제 카드 승인 거래(amount/merchantName/approvedAt)와 영수증에서 AI로 추출한 값을 비교한 결과.
 * 대조 결과는 저장하지 않고 요청 시마다 계산만 한다.
 */
public record ReceiptReconciliationView(
        Long transactionId,
        Long actualAmount,
        Long extractedAmount,
        FieldMatch amountMatch,
        String actualMerchant,
        String extractedMerchant,
        FieldMatch merchantMatch,
        LocalDate actualDate,
        LocalDate extractedDate,
        FieldMatch dateMatch,
        OverallStatus overallStatus) {

    public enum FieldMatch {
        MATCH, MISMATCH, UNKNOWN
    }

    public enum OverallStatus {
        MATCH, MISMATCH, UNKNOWN
    }

    public static ReceiptReconciliationView of(TransactionView transaction, ExtractedReceiptFields extracted) {
        LocalDate actualDate = transaction.approvedAt() != null ? transaction.approvedAt().toLocalDate() : null;

        FieldMatch amountMatch = compareExact(transaction.amount(), extracted.amount());
        FieldMatch merchantMatch = compareMerchant(transaction.merchantName(), extracted.merchant());
        FieldMatch dateMatch = compareExact(actualDate, extracted.date());

        OverallStatus overall = overallStatus(amountMatch, merchantMatch, dateMatch);

        return new ReceiptReconciliationView(transaction.id(), transaction.amount(), extracted.amount(), amountMatch,
                transaction.merchantName(), extracted.merchant(), merchantMatch,
                actualDate, extracted.date(), dateMatch, overall);
    }

    private static OverallStatus overallStatus(FieldMatch amountMatch, FieldMatch merchantMatch,
                                                FieldMatch dateMatch) {
        if (amountMatch == FieldMatch.MISMATCH || merchantMatch == FieldMatch.MISMATCH
                || dateMatch == FieldMatch.MISMATCH) {
            return OverallStatus.MISMATCH;
        }
        if (amountMatch == FieldMatch.MATCH && merchantMatch == FieldMatch.MATCH && dateMatch == FieldMatch.MATCH) {
            return OverallStatus.MATCH;
        }
        return OverallStatus.UNKNOWN;
    }

    private static <T> FieldMatch compareExact(T actual, T extracted) {
        if (extracted == null) {
            return FieldMatch.UNKNOWN;
        }
        if (actual == null) {
            return FieldMatch.UNKNOWN;
        }
        return actual.equals(extracted) ? FieldMatch.MATCH : FieldMatch.MISMATCH;
    }

    // 가맹점명은 OCR/AI 추출 과정에서 지점명 표기가 다를 수 있어(예: "스타벅스"/"스타벅스 강남점") 완전
    // 일치 대신 서로 포함 관계인지로 느슨하게 비교한다.
    private static FieldMatch compareMerchant(String actual, String extracted) {
        if (extracted == null || extracted.isBlank() || actual == null || actual.isBlank()) {
            return FieldMatch.UNKNOWN;
        }
        String normalizedActual = normalize(actual);
        String normalizedExtracted = normalize(extracted);
        boolean related = normalizedActual.contains(normalizedExtracted)
                || normalizedExtracted.contains(normalizedActual);
        return related ? FieldMatch.MATCH : FieldMatch.MISMATCH;
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase();
    }
}
