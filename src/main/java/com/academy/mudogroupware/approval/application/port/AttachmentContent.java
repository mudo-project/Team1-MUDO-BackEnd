package com.academy.mudogroupware.approval.application.port;

/**
 * AttachmentContentPort가 반환하는 첨부파일 원문.
 * 텍스트 계열(txt/json/xml/csv, docx에서 추출한 텍스트 포함)은 TEXT로,
 * Gemini가 직접 이해할 수 있는 바이너리(PDF/이미지)는 BINARY로 표현한다.
 */
public record AttachmentContent(Kind kind, String text, byte[] binaryData, String mimeType) {

    public enum Kind {
        TEXT,
        BINARY
    }

    public static AttachmentContent text(String text) {
        return new AttachmentContent(Kind.TEXT, text, null, null);
    }

    public static AttachmentContent binary(byte[] binaryData, String mimeType) {
        return new AttachmentContent(Kind.BINARY, null, binaryData, mimeType);
    }
}
