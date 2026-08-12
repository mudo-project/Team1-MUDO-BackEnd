package com.academy.mudogroupware.payroll.application.port.out;

import com.academy.mudogroupware.payroll.application.result.PayrollDetailResult;

public interface PayrollStatementRenderer {
  byte[] render(PayrollDetailResult payroll);
}
