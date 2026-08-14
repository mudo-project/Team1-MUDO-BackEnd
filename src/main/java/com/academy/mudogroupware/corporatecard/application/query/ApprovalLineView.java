package com.academy.mudogroupware.corporatecard.application.query;

public record ApprovalLineView(
        Long approverId,
        String approverName,
        String positionName,
        int stepOrder) { }
