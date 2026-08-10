package com.academy.mudogroupware.dataimport.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;
import com.academy.mudogroupware.dataimport.application.port.ImportAnalysisPort;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.application.port.ImportFileParserPort;
import com.academy.mudogroupware.dataimport.application.usecase.CreateOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateOnboardingImportService implements CreateOnboardingImportUseCase {

    private final DataImportJobRepository dataImportJobRepository;
    private final ImportFileParserPort importFileParserPort;
    private final ImportAnalysisPort importAnalysisPort;
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
        ImportDraft draft = importDraftBuilder.build(analyzeOrFallback(sheets));
        if (draft.isEmpty()) {
            throw new DataImportException(DataImportErrorCode.EMPTY_ANALYSIS_RESULT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> sourceFileNames = command.files().stream()
                .map(file -> file.fileName())
                .toList();
        DataImportJob job = DataImportJob.create(command.createdBy(), sourceFileNames, draft,
                now);
        return dataImportJobRepository.save(job).getId();
    }

    private List<ParsedImportSheet> analyzeOrFallback(List<ParsedImportSheet> sheets) {
        try {
            List<ParsedImportSheet> analyzedSheets = importAnalysisPort.analyze(sheets);
            return analyzedSheets != null ? analyzedSheets : sheets;
        } catch (RuntimeException e) {
            log.warn("event=data_import_analysis_fallback reason={}", e.getMessage());
            return sheets;
        }
    }
}
