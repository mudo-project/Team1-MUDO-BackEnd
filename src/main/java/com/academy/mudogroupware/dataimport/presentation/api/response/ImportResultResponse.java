package com.academy.mudogroupware.dataimport.presentation.api.response;

import com.academy.mudogroupware.dataimport.domain.model.ImportResult;

public record ImportResultResponse(
        int createdStudents,
        int createdLectures,
        int createdEnrollments,
        int skippedRows,
        int failedRows
) {

    public static ImportResultResponse from(ImportResult result) {
        return new ImportResultResponse(
                result.createdStudents(),
                result.createdLectures(),
                result.createdEnrollments(),
                result.skippedRows(),
                result.failedRows());
    }
}
