package com.academy.mudogroupware.dataimport.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.dataimport.domain.model.DataImportStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "data_import_job")
public class DataImportJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DataImportStatus status;

    @Lob
    @Column(name = "source_file_names", nullable = false)
    private String sourceFileNames;

    @Lob
    @Column(name = "draft_json", nullable = false)
    private String draftJson;

    @Lob
    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DataImportJobEntity() {
    }

    private DataImportJobEntity(Long id, Long academyId, Long createdBy, DataImportStatus status,
                                String sourceFileNames, String draftJson, String resultJson,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.createdBy = createdBy;
        this.status = status;
        this.sourceFileNames = sourceFileNames;
        this.draftJson = draftJson;
        this.resultJson = resultJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static DataImportJobEntity of(Long id, Long academyId, Long createdBy, DataImportStatus status,
                                  String sourceFileNames, String draftJson, String resultJson,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DataImportJobEntity(id, academyId, createdBy, status, sourceFileNames, draftJson, resultJson,
                createdAt, updatedAt);
    }
}
