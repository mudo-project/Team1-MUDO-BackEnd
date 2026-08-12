package com.academy.mudogroupware.payroll.application.result;

import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.AllowanceData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.CompensationData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollReferenceDataPort.PayBasisData;
import java.util.List;

public record EmployeePayrollSettingsResult(
    Long employeeId,
    List<CompensationData> compensations,
    List<AllowanceData> fixedAllowances,
    List<PayBasisData> payBases) {}
