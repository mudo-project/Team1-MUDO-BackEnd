package com.academy.mudogroupware.corporatecard.application.usecase;

import com.academy.mudogroupware.corporatecard.application.command.SubmitBatchCardExpensesCommand;
import com.academy.mudogroupware.corporatecard.application.query.BatchSubmitCardExpensesResult;

public interface SubmitBatchCardExpensesUseCase {
    BatchSubmitCardExpensesResult submit(SubmitBatchCardExpensesCommand command, Long userId);
}
