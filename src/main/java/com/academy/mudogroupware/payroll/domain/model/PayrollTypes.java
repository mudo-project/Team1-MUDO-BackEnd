package com.academy.mudogroupware.payroll.domain.model;

public final class PayrollTypes {
  private PayrollTypes() {}

  public enum EmploymentType { REGULAR, FIXED_TERM, PART_TIME }
  public enum SalaryType { MONTHLY, HOURLY }
  public enum PayrollStatus { DRAFT, CALCULATED, CONFIRMED }
  public enum PayDayType { FIXED_DAY, MONTH_END }
  public enum ItemCategory { EARNING, DEDUCTION }
  public enum SourceType { CONTRACT, ATTENDANCE, MOCK_INSURANCE, MOCK_TAX, MANUAL }
  public enum StatementStatus { PENDING, READY, FAILED }
  public enum ItemType {
    BASE_SALARY,
    HOURLY_PAY,
    MEAL_ALLOWANCE,
    POSITION_ALLOWANCE,
    DUTY_ALLOWANCE,
    TRANSPORTATION_ALLOWANCE,
    OVERTIME_PAY,
    NIGHT_PAY,
    HOLIDAY_PAY,
    WEEKLY_HOLIDAY_PAY,
    BONUS,
    OTHER_ALLOWANCE,
    NATIONAL_PENSION,
    HEALTH_INSURANCE,
    LONG_TERM_CARE,
    EMPLOYMENT_INSURANCE,
    INCOME_TAX,
    LOCAL_INCOME_TAX
  }
}
