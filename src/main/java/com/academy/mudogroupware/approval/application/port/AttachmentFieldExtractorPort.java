package com.academy.mudogroupware.approval.application.port;

public interface AttachmentFieldExtractorPort {

    /**
     * 주어진 첨부파일 원문에서 금액/일자/가맹점 등 구조화된 필드를 추출한다. 찾지 못한 필드는 null로 채워진다.
     * 제공자(Gemini 등) 호출 자체가 실패하거나 응답을 파싱할 수 없으면 {@link AttachmentFieldExtractionException}을 던진다.
     */
    ExtractedReceiptFields extract(AttachmentContent content);
}
