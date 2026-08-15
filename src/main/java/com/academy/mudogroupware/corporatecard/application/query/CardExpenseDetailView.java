package com.academy.mudogroupware.corporatecard.application.query;

import java.util.List;

public record CardExpenseDetailView(
        CardExpenseView expense,
        List<ApprovalLineView> approvalLines) { }
