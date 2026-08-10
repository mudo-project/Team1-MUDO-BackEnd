package com.academy.mudogroupware.notice.domain.model;

public final class NoticeAttachment {

    private final Long id;
    private final Long fileId;
    private final String fileName;

    private NoticeAttachment(Long id, Long fileId, String fileName) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        this.id = id;
        this.fileId = fileId;
        this.fileName = fileName;
    }

    public static NoticeAttachment create(Long fileId, String fileName) {
        return new NoticeAttachment(null, fileId, fileName);
    }

    public static NoticeAttachment restore(Long id, Long fileId, String fileName) {
        return new NoticeAttachment(id, fileId, fileName);
    }

    public Long getId() {
        return id;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }
}
