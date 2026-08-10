package com.academy.mudogroupware.dataimport.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DataImportJobRepositoryImpl implements DataImportJobRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final DataImportJobJpaRepository dataImportJobJpaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public DataImportJob save(DataImportJob job) {
        DataImportJobEntity entity = DataImportJobEntity.of(
                job.getId(),
                job.getAcademyId(),
                job.getCreatedBy(),
                job.getStatus(),
                writeJson(job.getSourceFileNames()),
                writeJson(job.getDraft()),
                job.getResult() != null ? writeJson(job.getResult()) : null,
                job.getCreatedAt(),
                job.getUpdatedAt());
        return toDomain(dataImportJobJpaRepository.save(entity));
    }

    @Override
    public Optional<DataImportJob> findById(Long id) {
        return dataImportJobJpaRepository.findById(id).map(this::toDomain);
    }

    private DataImportJob toDomain(DataImportJobEntity entity) {
        return DataImportJob.restore(
                entity.getId(),
                entity.getAcademyId(),
                entity.getCreatedBy(),
                entity.getStatus(),
                readJson(entity.getSourceFileNames(), STRING_LIST_TYPE),
                readJson(entity.getDraftJson(), ImportDraft.class),
                entity.getResultJson() != null ? readJson(entity.getResultJson(), ImportResult.class) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize data import json", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize data import json", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize data import json", e);
        }
    }
}
