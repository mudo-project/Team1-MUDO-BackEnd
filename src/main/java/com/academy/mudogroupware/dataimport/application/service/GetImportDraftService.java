package com.academy.mudogroupware.dataimport.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.dataimport.application.usecase.GetImportDraftUseCase;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetImportDraftService implements GetImportDraftUseCase {

    private final DataImportJobRepository dataImportJobRepository;

    @Override
    public ImportDraft getDraft(Long academyId, Long importId) {
        return loadScopedJob(academyId, importId).getDraft();
    }

    private DataImportJob loadScopedJob(Long academyId, Long importId) {
        DataImportJob job = dataImportJobRepository.findById(importId)
                .orElseThrow(() -> new DataImportException(DataImportErrorCode.IMPORT_NOT_FOUND));
        if (!job.getAcademyId().equals(academyId)) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ACCESS_DENIED);
        }
        return job;
    }
}
