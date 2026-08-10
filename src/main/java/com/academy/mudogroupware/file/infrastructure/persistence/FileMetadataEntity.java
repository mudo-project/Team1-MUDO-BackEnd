package com.academy.mudogroupware.file.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    private FileMetadataEntity(Long id, String objectKey, String contentType) {
        this.id = id;
        this.objectKey = objectKey;
        this.contentType = contentType;
    }

    public static FileMetadataEntity create(String objectKey, String contentType) {
        return new FileMetadataEntity(null, objectKey, contentType);
    }

    public static FileMetadataEntity restore(Long id, String objectKey, String contentType) {
        return new FileMetadataEntity(id, objectKey, contentType);
    }
}
