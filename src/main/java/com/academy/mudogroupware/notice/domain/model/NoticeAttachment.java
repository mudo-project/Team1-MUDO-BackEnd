package com.academy.mudogroupware.notice.domain.model;

public final class NoticeAttachment {

    private final Long id;
    private final String fileUrl;
    private final String fileName;
    private final String fileType;

    private NoticeAttachment(Long id, String fileUrl, String fileName, String fileType) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl must not be blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        this.id = id;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public static NoticeAttachment create(String fileUrl, String fileName, String fileType) {
        return new NoticeAttachment(null, fileUrl, fileName, fileType);
    }

    public static NoticeAttachment restore(Long id, String fileUrl, String fileName, String fileType) {
        return new NoticeAttachment(id, fileUrl, fileName, fileType);
    }

    public Long getId() {
        return id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }
}
