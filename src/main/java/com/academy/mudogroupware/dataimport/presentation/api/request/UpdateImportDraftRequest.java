package com.academy.mudogroupware.dataimport.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.dataimport.application.command.UpdateImportDraftCommand;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;

public record UpdateImportDraftRequest(
        List<ImportStudentCandidate> students,
        List<ImportLectureCandidate> lectures,
        List<ImportEnrollmentCandidate> enrollments
) {

    public UpdateImportDraftCommand toCommand(Long requesterId, Long importId) {
        return new UpdateImportDraftCommand(requesterId, importId, new ImportDraft(students, lectures, enrollments));
    }
}
