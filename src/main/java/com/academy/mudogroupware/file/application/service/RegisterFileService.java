package com.academy.mudogroupware.file.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;
import com.academy.mudogroupware.file.application.usecase.RegisterFileUseCase;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterFileService implements RegisterFileUseCase {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;

    @Override
    public Long register(RegisterFileCommand command) {
        FileMetadataEntity entity = FileMetadataEntity.create(command.academyId(), command.objectKey(),
                command.contentType());
        return fileMetadataJpaRepository.save(entity).getId();
    }
}
