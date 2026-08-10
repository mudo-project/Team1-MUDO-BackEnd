package com.academy.mudogroupware.dataimport.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;

public interface DataImportJobRepository {

    DataImportJob save(DataImportJob job);

    Optional<DataImportJob> findById(Long id);
}
