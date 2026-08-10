package com.academy.mudogroupware.dataimport.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.dataimport.application.command.UpdateImportDraftCommand;
import com.academy.mudogroupware.dataimport.application.usecase.UpdateImportDraftUseCase;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateImportDraftService implements UpdateImportDraftUseCase {

    private final DataImportJobRepository dataImportJobRepository;
    private final Clock clock;

    @Override
    public void updateDraft(UpdateImportDraftCommand command) {
        DataImportJob job = dataImportJobRepository.findById(command.importId())
                .orElseThrow(() -> new DataImportException(DataImportErrorCode.IMPORT_NOT_FOUND));
        if (!job.getAcademyId().equals(command.academyId())) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ACCESS_DENIED);
        }
        job.updateDraft(command.draft(), LocalDateTime.now(clock));
        dataImportJobRepository.save(job);
    }
}
