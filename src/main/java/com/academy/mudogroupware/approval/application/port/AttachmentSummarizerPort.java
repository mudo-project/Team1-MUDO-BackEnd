package com.academy.mudogroupware.approval.application.port;

public interface AttachmentSummarizerPort {

    /**
     * 주어진 첨부파일 원문을 AI로 요약한다. 제공자(Gemini 등) 호출에 실패하면
     * {@link AttachmentSummarizationException}을 던진다.
     */
    String summarize(AttachmentContent content);
}
