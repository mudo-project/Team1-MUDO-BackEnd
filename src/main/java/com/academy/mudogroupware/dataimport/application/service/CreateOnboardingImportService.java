package com.academy.mudogroupware.dataimport.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.application.port.ImportFileParserPort;
import com.academy.mudogroupware.dataimport.application.usecase.CreateOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateOnboardingImportService implements CreateOnboardingImportUseCase {

    private final DataImportJobRepository dataImportJobRepository;
    private final ImportFileParserPort importFileParserPort;
    private final ImportDraftBuilder importDraftBuilder;
    private final Clock clock;

    @Override
    public Long create(CreateOnboardingImportCommand command) {
        if (command.files().isEmpty()) {
            throw new DataImportException(DataImportErrorCode.FILE_REQUIRED);
        }
        List<ParsedImportSheet> sheets = command.files().stream()
                .map(importFileParserPort::parse)
                .toList();
        ImportDraft draft = importDraftBuilder.build(sheets);
        if (draft.isEmpty()) {
            throw new DataImportException(DataImportErrorCode.EMPTY_ANALYSIS_RESULT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> sourceFileNames = command.files().stream()
                .map(file -> file.fileName())
                .toList();
        DataImportJob job = DataImportJob.create(command.academyId(), command.createdBy(), sourceFileNames, draft,
                now);
        return dataImportJobRepository.save(job).getId();
    }
}
