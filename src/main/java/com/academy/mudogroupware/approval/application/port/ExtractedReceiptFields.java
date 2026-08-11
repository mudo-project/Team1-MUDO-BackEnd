package com.academy.mudogroupware.approval.application.port;

import java.time.LocalDate;

/**
 * Gemini 구조화 출력으로 영수증류 첨부파일에서 뽑아낸 필드. 값을 찾지 못한 필드는 null이다.
 */
public record ExtractedReceiptFields(Long amount, LocalDate date, String merchant) {
}
