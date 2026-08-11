package com.academy.mudogroupware.dataimport.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;

public final class DataImportJob {

    private final Long id;
    private final Long createdBy;
    private DataImportStatus status;
    private final List<String> sourceFileNames;
    private ImportDraft draft;
    private ImportResult result;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DataImportJob(Long id, Long createdBy, DataImportStatus status,
                          List<String> sourceFileNames, ImportDraft draft, ImportResult result,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        this.id = id;
        this.createdBy = createdBy;
        this.status = status;
        this.sourceFileNames = sourceFileNames != null ? List.copyOf(sourceFileNames) : List.of();
        this.draft = draft;
        this.result = result;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DataImportJob create(Long createdBy, List<String> sourceFileNames,
                                       ImportDraft draft, LocalDateTime now) {
        return new DataImportJob(null, createdBy, DataImportStatus.DRAFT, sourceFileNames, draft,
                null, now, now);
    }

    public static DataImportJob restore(Long id, Long createdBy, DataImportStatus status,
                                        List<String> sourceFileNames, ImportDraft draft, ImportResult result,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DataImportJob(id, createdBy, status, sourceFileNames, draft, result, createdAt,
                updatedAt);
    }

    public void updateDraft(ImportDraft draft, LocalDateTime now) {
        if (status != DataImportStatus.DRAFT) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
        }
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.draft = draft;
        this.updatedAt = now;
    }

    public void confirm(ImportResult result, LocalDateTime now) {
        if (status != DataImportStatus.DRAFT) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
        }
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.result = result;
        this.status = DataImportStatus.CONFIRMED;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public DataImportStatus getStatus() {
        return status;
    }

    public List<String> getSourceFileNames() {
        return sourceFileNames;
    }

    public ImportDraft getDraft() {
        return draft;
    }

    public ImportResult getResult() {
        return result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
