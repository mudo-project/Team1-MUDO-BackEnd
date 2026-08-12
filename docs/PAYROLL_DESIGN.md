# 급여 관리 백엔드 설계 V2

## 1. 목적

학원 그룹웨어의 직원 월 급여 관리 기능을 구현한다.

현재 단계에서는 실제 급여 지급, PDF 급여명세서 생성, 이메일 발송 기능을 구현하지 않는다.

이번 구현의 범위는 다음과 같다.

```text
직원 급여 계약
        +
학원 급여 정책
        +
근태 데이터
        +
법정 정책
        +
4대보험 Mock 데이터
        +
세금 Mock 데이터
        ↓
월 급여 계산
        ↓
관리자 검토 및 수정
        ↓
급여 확정
        ↓
급여 데이터 조회 / 미리보기
```

---

# 2. 현재 구현 범위

구현한다.

* 학원 급여 정책
* 직원 급여 계약정보
* 직원별 고정수당
* 월별 급여 생성
* 근태 데이터 연동
* 연장/야간/휴일근로시간 반영
* 월 급여 지급항목 계산
* 월 급여 공제항목 반영
* 국민연금 Mock 데이터
* 건강보험 Mock 데이터
* 장기요양보험 Mock 데이터
* 고용보험 Mock 데이터
* 소득세 Mock 데이터
* 지방소득세 Mock 데이터
* 급여 합계 계산
* 급여 상세 조회
* 급여 수정
* 급여 확정
* 급여 계산 근거 Snapshot
* 확정 급여 정정 Revision
* 월별 급여 목록 조회
* 급여 데이터 미리보기

---

# 3. 현재 구현하지 않는 기능

다음 기능은 이번 구현 범위에서 제외한다.

```text
실제 급여 송금

은행 API 연동

국민연금 실제 API 연동
건강보험 실제 API 연동
고용보험 실제 API 연동

국세청 실제 API 연동

PDF 급여명세서 생성
PDF 다운로드

이메일 급여명세서 발송

자동 발송
발송 예약
발송 이력

실제 지급 여부 확인
```

따라서 현재 백엔드에는 다음 개념을 만들지 않는다.

```text
PAID
paid_at

STATEMENT_ISSUED
statement_issued_at

payroll_statement
payroll_statement_delivery
payroll_auto_delivery_setting
```

이 기능들은 향후 급여명세서 발급 기능 구현 시 별도로 추가한다.

---

# 4. 전체 도메인 흐름

```text
[법정 정책]
statutory_policy

        +

[사업장 법 적용 정보]
workplace_labor_scope

        +

[급여 정책]
payroll_policy

        +

[직원 급여 계약]
employee_compensation
employee_fixed_allowance

        +

[통상임금 기준]
employee_pay_basis

        +

[근태]
Attendance

        +

[보험 Mock]
social_insurance_status
social_insurance_assessment

        +

[세금 Mock]
tax_assessment

        ↓

월 급여 생성

        ↓

DRAFT

        ↓

급여 계산

        ↓

CALCULATED

        ↓

관리자 검토 / 수정

        ↓

CONFIRMED
```

---

# 5. Payroll 상태

현재는 다음 3개 상태만 사용한다.

```text
DRAFT
CALCULATED
CONFIRMED
```

## DRAFT

급여 레코드가 생성되었지만 아직 계산이 완료되지 않은 상태.

---

## CALCULATED

급여 계산이 완료된 상태.

관리자가 상세 내용을 확인하고 수정할 수 있다.

```text
DRAFT
  ↓
CALCULATED
```

---

## CONFIRMED

관리자가 급여 계산 결과를 최종 확정한 상태.

```text
CALCULATED
  ↓
CONFIRMED
```

확정된 급여는 직접 수정할 수 없다.

---

# 6. 화면의 `미작성`은 PayrollStatus가 아니다

디자인에는 다음 상태가 존재한다.

```text
미작성
```

하지만 이것을 `PayrollStatus.NOT_CREATED` 같은 DB 상태로 만들지 않는다.

`미작성`은 해당 직원과 해당 월에 `payroll` 데이터 자체가 존재하지 않는 상태다.

예:

```text
직원 A + 2026-08

payroll 존재
→ CALCULATED

직원 B + 2026-08

payroll 없음
→ 화면에서는 "미작성"
```

즉 월 급여 목록 조회에서는:

```text
직원 목록
LEFT JOIN payroll
```

형태의 View 데이터를 반환한다.

---

# 7. 급여 정책과 지급 예정일

학원에는 급여 지급 기준이 존재한다.

예:

```text
2026년 8월 급여
→ 2026년 9월 5일 지급
```

이런 케이스를 지원해야 한다.

따라서 단순히:

```text
pay_day = 5
```

만 저장하면 부족하다.

---

# 8. payroll_policy

학원 하나당 하나의 스키마를 사용하므로 Payroll 테이블에는 `academy_id`를 저장하지 않는다.

학원 데이터의 격리는 애플리케이션이 연결한 스키마를 기준으로 보장한다.
이 문서에서 새로 정의하는 Payroll 테이블과 조회 Port는 `academy_id`를 입력이나 응답으로 사용하지 않는다.

다른 담당자가 소유한 도메인의 `academy_id` 제거는 이번 Payroll 구현 범위에 포함하지 않는다.

```text
payroll_policy

payroll_policy_id

pay_day_type

pay_day

payment_month_offset

created_at
updated_at
```

---

# 9. pay_day_type

```text
FIXED_DAY
MONTH_END
```

### FIXED_DAY

예:

```text
pay_day = 5
```

→ 매월 5일

지급월에 `pay_day`가 존재하지 않으면 해당 월의 마지막 날로 보정한다.

예:

```text
pay_day = 31
지급월 = 2026-02

scheduled_pay_date = 2026-02-28
```

### MONTH_END

해당 지급월의 마지막 날짜.

```text
2026-02
→ 2026-02-28
```

---

# 10. payment_month_offset

급여 귀속월과 지급월의 차이를 나타낸다.

```text
0 = 같은 달 지급
1 = 다음 달 지급
```

예:

```text
payroll_year_month = 2026-08
pay_day = 5
payment_month_offset = 1
```

결과:

```text
scheduled_pay_date = 2026-09-05
```

다른 예:

```text
payroll_year_month = 2026-08
pay_day = 25
payment_month_offset = 0
```

결과:

```text
scheduled_pay_date = 2026-08-25
```

---

# 11. scheduled_pay_date

월 급여가 생성되는 순간 계산하여 `payroll`에 Snapshot 형태로 저장한다.

```text
scheduled_pay_date
```

중요:

이 값은 **실제 지급 완료일이 아니다.**

현재 시스템에서는 급여 송금을 하지 않기 때문에 의미는:

```text
급여 지급 예정일
```

이다.

따라서:

```text
paid_at
payment_completed
PAID
```

등은 만들지 않는다.

---

# 12. 법정 정책

법정 기준은 학원 원장이 수정할 수 없다.

예:

```text
국민연금 기준
건강보험 기준
장기요양보험 기준
고용보험 기준

연장근로 가산 기준
야간근로 가산 기준
휴일근로 가산 기준

최저임금 기준
```

법정 기준은 적용기간이 존재하므로 Version 관리가 가능해야 한다.

---

# 13. statutory_policy

```text
statutory_policy

policy_id

policy_type

rate
employee_rate
employer_rate

minimum_base
maximum_base

effective_from
effective_to

created_at
updated_at
```

### policy_type 예

```text
NATIONAL_PENSION
HEALTH_INSURANCE
LONG_TERM_CARE
EMPLOYMENT_INSURANCE

OVERTIME
NIGHT_WORK
HOLIDAY_WORK

MINIMUM_WAGE
```

현재 프로젝트에서는 Seed/Mock 데이터로 구성한다.

학원 관리자가 수정하는 API는 제공하지 않는다.

---

# 14. 사업장 법 적용 정보

학원은 상시근로자 5인 이상/미만 여부에 따라 일부 법정 가산수당 적용 방식이 달라질 수 있다.

실제 상시근로자 수 계산 알고리즘은 이번 프로젝트에서 구현하지 않는다.

판정 결과를 Mock 데이터로 사용한다.

---

# 15. workplace_labor_scope

```text
workplace_labor_scope

labor_scope_id

year_month

regular_employee_count
is_five_or_more

created_at
updated_at
```

예:

```text
year_month = 2026-08

regular_employee_count = 7

is_five_or_more = true
```

---

# 16. 직원 급여 계약

직원의 기본적인 급여 조건을 저장한다.

## employee_compensation

```text
employee_compensation

compensation_id

user_id

employment_type
salary_type

base_salary
hourly_wage

weekly_contract_hours

effective_from
effective_to

created_at
updated_at
```

## 계약 유효기간 규칙

같은 직원의 급여 계약 유효기간은 서로 겹칠 수 없다.

계약 생성·변경 Application Service가 저장 전에 기간 중복을 검증한다.
동일 직원과 동일 `effective_from`의 중복은 DB Unique Constraint로도 방지한다.

```text
effective_from <= 적용일 <= effective_to
```

`effective_to = NULL`은 종료일이 정해지지 않은 현재 계약을 의미한다.

계약 변경일이 `2026-08-16`이면 기존 계약은 `2026-08-15`까지,
새 계약은 `2026-08-16`부터 적용한다.

```text
기존 계약 effective_to = 2026-08-15
새 계약 effective_from = 2026-08-16
```

급여 귀속월 중 계약이 변경되면 변경일을 기준으로 기간을 나누고,
각 기간에 유효한 계약 조건으로 급여를 분할 계산한다.

월급제 직원의 중도 입사와 중도 퇴사도 실제 재직기간을 기준으로 일할계산한다.

일할계산의 분모는 해당 급여 귀속월의 전체 달력 일수다.
주말과 비근무일도 전체 달력 일수와 실제 재직일수에 포함한다.

```text
구간 기본급
= 계약 월 기본급
  × 해당 계약이 적용된 달력 일수
  ÷ 해당 월의 전체 달력 일수
```

적용 시작일과 종료일은 모두 적용일수에 포함한다.

예:

```text
2026-08-10 입사

8월 전체 달력 일수 = 31일
실제 재직일수 = 22일

기본급 = 월 기본급 × 22 ÷ 31
```

월 중 계약이 변경되면 각 계약 구간을 같은 월의 전체 달력 일수로 나누어 계산한다.

```text
2026-08-16 계약 변경

기존 계약 금액 = 기존 월 기본급 × 15 ÷ 31
신규 계약 금액 = 신규 월 기본급 × 16 ÷ 31

최종 기본급 = 기존 계약 금액 + 신규 계약 금액
```

---

# 17. employment_type과 salary_type

## employment_type

고용형태는 다음 3가지를 사용한다.

```text
REGULAR
FIXED_TERM
PART_TIME
```

화면 표시명:

```text
REGULAR = 정규직
FIXED_TERM = 기간제
PART_TIME = 파트타임
```

## salary_type

```text
MONTHLY
HOURLY
```

주의:

화면에 표시되는:

```text
정규직
기간제
파트타임
```

는 `salary_type`이 아니다.

이는 고용형태다.

따라서:

```text
employment_type
```

과:

```text
salary_type
```

은 별개의 개념이다.

예:

```text
employment_type = FIXED_TERM
salary_type = MONTHLY
```

도 가능하다.

`employment_type`과 `salary_type`의 조합을 임의로 제한하지 않는다.

```text
REGULAR + MONTHLY
FIXED_TERM + MONTHLY
FIXED_TERM + HOURLY
PART_TIME + MONTHLY
PART_TIME + HOURLY
```

`강사`는 고용형태가 아니므로 `employment_type`에 추가하지 않는다.
현재 Payroll 범위에서는 별도의 `job_type`도 만들지 않는다.

이번 Payroll 구현 범위에서는 시급제 직원의 연차를 계산하지 않는다.
이는 급여형태만으로 연차 적용 여부를 판단하는 일반 법정 규칙이 아니라 현재 프로젝트의 제한된 범위다.

---

# 18. 직원 고정수당

고정수당은 별도 테이블에서 관리한다.

## employee_fixed_allowance

```text
employee_fixed_allowance

allowance_id

employee_id

allowance_type
allowance_name

amount

effective_from
effective_to

created_at
updated_at
```

예:

```text
MEAL
POSITION
DUTY
TRANSPORTATION
OTHER
```

---

# 19. 통상시급

연장/야간/휴일근로 계산에서 단순히 기본급만 사용하지 않는다.

이번 프로젝트에서는 복잡한 통상임금 판정 알고리즘을 구현하지 않는다.

Mock 또는 사전에 산정된 값을 사용한다.

## employee_pay_basis

```text
employee_pay_basis

pay_basis_id

employee_id

ordinary_hourly_wage

effective_from
effective_to

created_at
updated_at
```

---

# 20. 보험 가입 상태

보험 적용 여부를 학원 원장이 자유롭게 체크해서 설정하는 형태로 구현하지 않는다.

현재 프로젝트에서는 외부기관으로부터 조회한 값이라고 가정한다.

## social_insurance_status

```text
social_insurance_status

insurance_status_id

employee_id

national_pension_status
health_insurance_status
employment_insurance_status

effective_from
effective_to

exemption_reason

created_at
updated_at
```

상태 예:

```text
ENROLLED
EXEMPT
NOT_APPLICABLE
```

---

# 21. 장기요양보험 상태

다음 필드는 만들지 않는다.

```text
long_term_care_enabled
```

장기요양보험을 독립적으로 원장이 ON/OFF하는 구조로 만들지 않는다.

단, 실제 월 공제액에는 장기요양보험이 존재한다.

---

# 22. 월별 보험 공제 Mock

## social_insurance_assessment

```text
social_insurance_assessment

assessment_id

employee_id

year_month

national_pension_amount
health_insurance_amount
long_term_care_amount
employment_insurance_amount

created_at
updated_at
```

예:

```text
2026-08

국민연금        152,000
건강보험        115,000
장기요양보험     15,000
고용보험         30,000
```

현재 단계에서는 Mock 데이터다.

---

# 23. 보험 공제액 수정 규칙

다음 항목은 일반적인 급여 수정 화면에서 직접 변경할 수 없도록 한다.

```text
국민연금
건강보험
장기요양보험
고용보험
```

즉 API에서도:

```text
PATCH payroll
```

요청으로 해당 금액을 마음대로 덮어쓸 수 없게 한다.

보험 Mock 데이터의 출처는:

```text
social_insurance_assessment
```

이다.

급여 재계산 시 여기에서 값을 가져온다.

---

# 24. 세금 Mock 데이터

이번 프로젝트에서는 소득세 계산기를 완전히 구현하지 않는다.

월별 세금 결과를 Mock으로 제공한다.

## tax_assessment

```text
tax_assessment

tax_assessment_id

employee_id

year_month

income_tax_amount
local_income_tax_amount

created_at
updated_at
```

예:

```text
income_tax_amount = 87,800
local_income_tax_amount = 8,780
```

---

# 25. 세금 수정 규칙

다음 값 역시 일반적인 급여 수정 API에서 직접 수정하지 않는 것을 기본 정책으로 한다.

```text
소득세
지방소득세
```

이번 프로젝트에서는 외부 계산 결과 또는 Mock 결과로 간주한다.

즉 공제항목은 기본적으로:

```text
READ ONLY
```

이다.

---

# 26. 산재보험

산재보험은 직원 급여 공제항목에 포함하지 않는다.

따라서 현재 `payroll_item`의 공제항목에는:

```text
WORKERS_COMPENSATION_INSURANCE
```

를 만들지 않는다.

---

# 27. 근태 연동

Payroll 모듈에서는 Attendance 데이터베이스를 직접 참조하는 방식보다는 Port를 통해 조회한다.

이번 급여 구현에서는 타 도메인 조회 연동을 허용한다.

허용 범위는 다음과 같다.

```text
Payroll 모듈
→ 직원/근태 조회 Port와 최소 응답 DTO 정의

Users 모듈
→ 급여 대상 직원 조회 Adapter 구현

Attendance 모듈
→ 월 근태 집계 조회 Adapter 구현
```

대상 도메인에서는 위 조회 Adapter를 구현하는 데 필요한 Domain Repository,
Persistence Adapter, Spring Data JPA Repository와 최소 조회 코드를 추가할 수 있다.

대상 도메인의 기존 비즈니스 로직은 급여 조회를 위해 임의로 변경하지 않는다.
단, 휴일근로 기록을 위해 원장이 비근무일로 지정한 날에도 출퇴근을 저장할 수 있도록
Attendance의 비근무일 출근 제한은 변경한다.

Adapter 메서드에는 소비 도메인과 용도를 주석으로 남긴다.

```text
Consumer: payroll
Purpose: 월 급여 계산을 위한 직원 또는 근태 조회
```

```java
public interface PayrollAttendancePort {

    PayrollAttendanceResult getMonthlyAttendance(
        Long userId,
        YearMonth yearMonth
    );
}
```

---

# 28. PayrollAttendanceResult

최소 다음 정보를 제공한다.

```text
workDays
workHours

overtimeHours
nightHours
holidayHours

paidLeaveHours
```

이번 구현에서는 유급휴가만 취급한다.

```text
unpaidLeaveHours
absenceDays
```

는 조회 결과와 Snapshot에 포함하지 않는다.

근무시간은 분 단위 원본 값을 유지하며 임의로 절사하거나 반올림하지 않는다.

## 근무일 연장근로시간

근무일에 퇴근 유형이 `OVERTIME`인 경우에만 연장근로시간을 계산한다.

```text
scheduledEndAt
= 해당 근무일 + 해당 요일의 예정 퇴근시각

overtimeHours
= MAX(0, clockOutAt - scheduledEndAt)
```

요일별 근무 설정이 활성화되어 있으면 해당 요일의 종료시각을 사용하고,
별도 설정이 없으면 학원의 기본 종료시각을 사용한다.

```text
ClockOutType = NORMAL
→ 예정 퇴근시각을 넘었더라도 overtimeHours = 0

ClockOutType = OVERTIME
clockOutAt > scheduledEndAt
→ 차이만큼 overtimeHours 반영

ClockOutType = OVERTIME
clockOutAt <= scheduledEndAt
→ overtimeHours = 0
```

단순히 늦게 퇴근했다는 이유만으로 연장근로 처리하지 않는다.

## 휴일근로시간

원장이 근태 정책에서 `workday = false`로 지정한 날에 완료된 출퇴근 기록이 있으면
전체 실근로시간을 휴일근로시간으로 계산한다.

```text
holidayHours
= clockOutAt - clockInAt
```

휴일근로 기록에는 예정 퇴근시각을 적용하지 않는다.
해당 기록은 `ClockOutType`과 관계없이 휴일근로로 분류하며 연장근로시간으로 중복 계산하지 않는다.

휴일근로수당 계산 시에는 다음 구간을 구분한다.

```text
8시간 이내 휴일근로
8시간 초과 휴일근로
```

## 야간근로시간

오후 10시부터 다음 날 오전 6시까지의 실제 근로시간을 야간근로시간으로 계산한다.

야간근로시간은 연장근로 또는 휴일근로와 중복될 수 있다.

## 유급휴가시간

`APPROVED` 상태의 유급휴가만 반영한다.

```text
paidLeaveHours
= 휴가일에 해당하는 예정 근무시간
```

`PENDING`, `REJECTED` 휴가는 반영하지 않는다.
비근무일이 휴가 기간에 포함되어 있어도 유급휴가시간에 포함하지 않는다.

월급제 직원이 유급휴가를 사용해도 기본급은 차감하지 않는다.
유급휴가 사용을 이유로 별도의 추가 지급항목도 생성하지 않는다.

시급제 직원은 이번 구현 범위에서 `paidLeaveHours`를 급여 계산에 반영하지 않는다.

## 휴게시간

현재 근태 모델에는 별도 휴게시간이 없다.

```text
실근로시간
= clockOutAt - clockInAt
```

연장·야간·휴일근로시간 계산에서도 별도 휴게시간을 차감하지 않는다.

## 5인 미만 사업장

연장·야간·휴일근로시간 자체는 사업장 규모와 관계없이 집계하고 Snapshot에 저장한다.

법정 가산수당 적용 여부는:

```text
workplace_labor_scope.is_five_or_more
```

를 기준으로 PayrollCalculator가 결정한다.

---

# 29. 근태 Snapshot

Payroll 계산 이후 Attendance 원본이 수정되더라도 과거 계산 결과가 바뀌면 안 된다.

따라서 계산 시점의 근태를 별도로 Snapshot으로 저장한다.

## payroll_attendance_snapshot

```text
payroll_attendance_snapshot

snapshot_id

payroll_id

work_days
work_hours

overtime_hours
night_hours
holiday_hours

paid_leave_hours

created_at
```

재계산하는 동안에는 Snapshot을 교체할 수 있다.

`CONFIRMED` 이후에는 변경할 수 없다.

---

# 30. 직원 급여 계약 Snapshot

## payroll_compensation_snapshot

```text
payroll_compensation_snapshot

snapshot_id

payroll_id

applied_from
applied_to

employment_type
salary_type

base_salary
hourly_wage

ordinary_hourly_wage

weekly_contract_hours

created_at
```

급여 귀속월 중 계약이 변경되면 하나의 Payroll에 여러 Compensation Snapshot이 존재할 수 있다.

예:

```text
2026-08-01 ~ 2026-08-15
기존 계약 Snapshot

2026-08-16 ~ 2026-08-31
변경 계약 Snapshot
```

`applied_from`, `applied_to`는 해당 급여 계산에서 실제로 적용한 구간을 저장한다.

---

# 31. 계산 규칙 Snapshot

급여 계산 시점에 적용된 주요 법정 계산 기준도 남긴다.

## payroll_rule_snapshot

```text
payroll_rule_snapshot

snapshot_id

payroll_id

labor_scope_id

is_five_or_more

overtime_multiplier
night_multiplier

holiday_under_8_multiplier
holiday_over_8_multiplier

created_at
```

이 테이블은 "당시에 어떤 기준으로 계산했는지"를 추적하기 위한 것이다.

---

# 32. Payroll Aggregate

## payroll

```text
payroll

payroll_id

user_id

payroll_year_month

scheduled_pay_date

status

total_earnings
total_deductions
net_pay

revision_no
original_payroll_id

memo

calculated_at
confirmed_at

created_at
updated_at

version
```

---

# 33. payroll_year_month

급여 귀속월이다.

예:

```text
2026-08
```

Java에서는:

```java
YearMonth
```

를 사용한다.

DB에서는:

```text
DATE
```

를 사용하고 해당 월의 1일을 저장한다.

```text
2026-08
→ 2026-08-01
```

---

# 34. 금액 필드

금액은 모두:

```text
DECIMAL(15,2)
```

또는 프로젝트의 금액 convention을 사용한다.

Java에서는:

```java
BigDecimal
```

을 사용한다.

급여 계산에 `double`, `float`을 사용하지 않는다.

일할계산의 구간별 금액은 `BigDecimal`과 `MathContext.DECIMAL128` 수준의
충분한 소수 정밀도로 계산하며 중간에 원 단위 반올림하지 않는다.

모든 계약 구간 금액을 합산한 뒤 최종 기본급에서만 원 단위 `HALF_UP` 반올림한다.

```java
BigDecimal finalBaseSalary = segmentAmounts.stream()
    .reduce(BigDecimal.ZERO, BigDecimal::add)
    .setScale(0, RoundingMode.HALF_UP);
```

DB의 `DECIMAL(15,2)`에는 최종 원 단위 금액을 소수부 `.00` 형태로 저장할 수 있다.

---

# 35. Payroll Item

지급 및 공제 내역을 하나의 항목 구조로 관리한다.

## payroll_item

```text
payroll_item

payroll_item_id

payroll_id

item_category
item_type
item_name

amount

source_type

original_amount
is_adjusted
adjustment_reason

calculation_formula
calculation_basis

display_order

created_at
updated_at
```

---

# 36. item_category

```text
EARNING
DEDUCTION
```

---

# 37. 지급항목 item_type

```text
BASE_SALARY
HOURLY_PAY

MEAL_ALLOWANCE
POSITION_ALLOWANCE
DUTY_ALLOWANCE
TRANSPORTATION_ALLOWANCE

OVERTIME_PAY
NIGHT_PAY
HOLIDAY_PAY
WEEKLY_HOLIDAY_PAY

BONUS
OTHER_ALLOWANCE
```

---

# 38. 공제항목 item_type

```text
NATIONAL_PENSION
HEALTH_INSURANCE
LONG_TERM_CARE
EMPLOYMENT_INSURANCE

INCOME_TAX
LOCAL_INCOME_TAX
```

현재 단계에서는 자유로운:

```text
OTHER_DEDUCTION
```

추가 기능은 제공하지 않는다.

---

# 39. source_type

각 급여항목이 어디에서 만들어졌는지 기록한다.

```text
CONTRACT
ATTENDANCE
MOCK_INSURANCE
MOCK_TAX
MANUAL
```

### CONTRACT

직원 급여 계약이나 고정수당에서 가져옴.

```text
기본급
식대
직책수당
```

### ATTENDANCE

근태 기반 계산.

```text
연장근로수당
야간근로수당
휴일근로수당
```

### MOCK_INSURANCE

보험 Mock 데이터.

```text
국민연금
건강보험
장기요양보험
고용보험
```

### MOCK_TAX

세금 Mock 데이터.

```text
소득세
지방소득세
```

### MANUAL

관리자가 해당 월에 직접 추가한 지급항목.

---

# 40. 지급항목 수정

현재 디자인에서는 관리자가 다음 값을 수정할 수 있다.

```text
기본급
초과근무수당
식대
기타 지급항목
```

백엔드에서는 원본 데이터와 월별 수정값을 구분해야 한다.

예:

```text
원 계약 기본급
3,200,000
```

관리자가 이번 월 급여에서:

```text
3,100,000
```

으로 조정했다고 해서:

```text
employee_compensation.base_salary
```

를 수정하면 안 된다.

수정은 해당 월 `payroll_item`에만 반영한다.

---

# 41. 수정 이력

자동 생성된 항목을 관리자가 조정했다면:

```text
original_amount
is_adjusted
adjustment_reason
```

을 사용한다.

예:

```text
item_type = OVERTIME_PAY

original_amount = 180000
amount = 170000

is_adjusted = true

adjustment_reason =
"근태 정정사항 수기 반영"
```

법정/자동 계산 항목을 변경할 경우 `adjustment_reason`을 필수로 하는 것을 권장한다.

---

# 42. 공제항목 수정

다음 source는 일반 급여 수정 API에서 수정할 수 없다.

```text
MOCK_INSURANCE
MOCK_TAX
```

따라서:

```text
국민연금
건강보험
장기요양보험
고용보험
소득세
지방소득세
```

금액을 일반 Update DTO에 넣지 않는다.

이 값들은 재계산 시 Mock assessment를 통해 갱신한다.

---

# 43. 월별 추가 지급항목

디자인의:

```text
+ 항목 추가
```

기능은 직원의 정규 급여계약을 수정하는 기능이 아니다.

해당 월 Payroll에만 지급항목을 추가한다.

예:

```text
특별수당
성과수당
임시지원금
```

생성 시:

```text
source_type = MANUAL
item_type = OTHER_ALLOWANCE
```

로 저장한다.

---

# 44. 합계 계산

합계는 프론트에서 계산하지 않는다.

백엔드가 계산한다.

### total_earnings

```text
SUM(
  payroll_item.amount
  WHERE item_category = EARNING
)
```

### total_deductions

```text
SUM(
  payroll_item.amount
  WHERE item_category = DEDUCTION
)
```

공제액 자체는 양수로 저장한다.

예:

```text
국민연금 amount = 152000
```

UI에서 필요하면:

```text
-152,000
```

으로 표시한다.

---

# 45. net_pay

```text
net_pay
=
total_earnings
-
total_deductions
```

이 값은 실제 송금 완료금액이 아니다.

현재 시스템에서는:

```text
차인지급 예정액
```

이다.

---

# 46. 화면 합계 명칭

백엔드에서는 반드시 다음을 명확히 구분한다.

```text
totalEarnings
totalDeductions
netPay
```

예:

```json
{
  "totalEarnings": 3380000,
  "totalDeductions": 382980,
  "netPay": 2997020
}
```

프론트에서 `netPay`를 `총 지급액`이라고 표시하면 안 된다.

---

# 47. 계산방법 저장

근로시간에 따라 금액이 달라지는 항목은 계산 근거를 남긴다.

예:

```text
OVERTIME_PAY
```

### calculation_formula

사용자에게 보여줄 수 있는 문자열.

```text
18,500원 × 6시간 × 1.5
```

### calculation_basis

JSON.

```json
{
  "ordinaryHourlyWage": 18500,
  "overtimeHours": 6,
  "multiplier": 1.5
}
```

---

# 48. 연장/야간/휴일근로

각 수당은 하나의 `초과근무수당`으로 합치지 않는다.

도메인에서는 반드시 별도로 관리한다.

```text
OVERTIME_PAY
NIGHT_PAY
HOLIDAY_PAY
```

UI에서 합산해 보여주는 것은 가능하지만 Backend Type은 분리한다.

근태 분류 규칙은 다음과 같다.

```text
근무일 + ClockOutType.OVERTIME
→ 예정 퇴근 이후 시간을 연장근로로 분류

근무일 + ClockOutType.NORMAL
→ 연장근로로 분류하지 않음

비근무일 + 완료된 출퇴근 기록
→ 전체 실근로시간을 휴일근로로 분류

22:00 ~ 다음 날 06:00 실제 근로
→ 다른 분류와 관계없이 야간근로에도 반영
```

동일 시간에 휴일근로와 연장근로를 중복 적용하지 않는다.
야간근로는 연장근로 또는 휴일근로와 중복 적용할 수 있다.

---

# 49. 5인 이상 여부

PayrollCalculator는:

```text
workplace_labor_scope.is_five_or_more
```

를 확인한다.

5인 이상인 경우 법정 가산 계산 규칙을 적용한다.

5인 미만인 경우 같은 규칙을 무조건 적용해서는 안 된다.

실제 사업장의 상시근로자 판정은 Mock 결과를 사용한다.

---

# 50. PayrollCalculator

급여 계산 로직은 Controller 또는 Repository에 넣지 않는다.

```text
PayrollCalculator
```

가 담당한다.

주요 책임:

```text
기본급 항목 생성

시급 급여 생성

고정수당 생성

연장근로수당 계산
야간근로수당 계산
휴일근로수당 계산
주휴수당 반영

보험 Mock 공제 반영
세금 Mock 공제 반영

totalEarnings 계산
totalDeductions 계산
netPay 계산
```

## 월급제 기본급

귀속월 전체에 재직하고 계약 변경이 없으면 계약의 `base_salary` 전액을 반영한다.

중도 입사·퇴사 또는 월 중 계약 변경이 있으면 기간별로 일할계산한 금액을 합산한다.

```text
월 기본급
= 각 계약 적용기간의 일할계산 금액 합계
```

월 중 계약이 변경되면 각 구간의 `base_salary`를 해당 구간에만 적용한다.

각 구간은 해당 월의 전체 달력 일수를 분모로 계산한다.
구간별 중간 금액은 반올림하지 않고 모두 합산한 뒤 최종 기본급에서 원 단위 `HALF_UP` 반올림한다.

## 시급제 기본급

시급제 기본급은 실제 일반 근로시간을 기준으로 계산한다.

```text
HOURLY_PAY
= hourly_wage × 일반 근로시간
```

연장·야간·휴일근로시간은 일반 근로시간과 구분하여 각 수당 항목으로 계산한다.
이번 구현에서는 시급제 직원의 유급휴가시간을 `HOURLY_PAY`에 더하지 않는다.

## 주휴수당

주휴수당은 법정 적용 조건을 충족하는 경우에만 계산한다.

최소한 다음 조건을 확인한다.

```text
1주 소정근로시간이 15시간 이상인가
해당 주의 소정근로일을 충족했는가
```

시급제 직원에게 별도 `WEEKLY_HOLIDAY_PAY` 항목으로 생성한다.
월급제 직원은 기본급에 포함된 것으로 보고 별도 항목을 생성하지 않는다.

구체적인 주 단위 판정과 1일 소정근로시간 산식은 적용 시점의 법정 정책으로 관리한다.

---

# 51. 급여 계산 순서

```text
1. Payroll 존재 여부 확인

2. 급여 귀속월과 겹치는 직원 급여 계약 목록 조회

3. 직원 고정수당 조회

4. 통상시급 조회

5. 해당 월 근태 조회

6. 해당 월 사업장 법 적용정보 조회

7. 해당 기간 법정 정책 조회

8. 보험 상태 조회

9. 보험 Mock Assessment 조회

10. 세금 Mock Assessment 조회

11. 지급항목 생성

12. 공제항목 생성

13. total_earnings 계산

14. total_deductions 계산

15. net_pay 계산

16. Attendance Snapshot 생성

17. Compensation Snapshot 생성

18. Rule Snapshot 생성

19. status = CALCULATED

20. calculated_at 기록
```

---

# 52. Payroll 생성

급여 생성과 계산을 분리한다.

## 생성

```http
POST /api/payrolls/employees/{employeeId}
```

Request:

```json
{
  "year": 2026,
  "month": 8
}
```

처리:

```text
payroll 생성

status = DRAFT

scheduled_pay_date 계산
```

---

# 53. Payroll 계산

```http
PATCH /api/payrolls/{payrollId}/calculate
```

가능 상태:

```text
DRAFT
CALCULATED
```

성공 후:

```text
status = CALCULATED
```

---

# 54. 재계산

`CALCULATED` 상태에서는 확정 전에 재계산할 수 있다.

재계산 시:

```text
기존 자동생성 payroll_item 제거

기존 Snapshot 교체

최신 근태 조회

최신 Mock Assessment 조회

다시 계산
```

```text
자동 재계산 시 MANUAL 항목은 유지
```

자동생성 항목과 Snapshot만 교체하며 기존 `MANUAL` 지급항목은 삭제하지 않는다.
재계산 후 `MANUAL` 항목까지 포함하여 합계를 다시 계산한다.

---

# 55. 월 급여 목록 조회

디자인의 메인 페이지를 지원하기 위한 API.

```http
GET /api/payrolls
```

Query:

```text
year
month

employmentType
status
employeeName

page
size
```

예:

```http
GET /api/payrolls?year=2026&month=8&page=0&size=20
```

목록은 페이지네이션을 적용한다.

응답은 최소 다음 필드를 제공한다.

```text
content
page
size
hasNext
```

---

# 56. 월 급여 목록은 Employee + Payroll View다

Payroll이 없는 직원도 목록에 보여야 하므로 `payroll` 테이블만 조회하면 안 된다.

예:

```text
Employee A → CALCULATED

Employee B → CONFIRMED

Employee C → payroll 없음
```

응답:

```json
{
  "employeeId": 30,
  "employeeName": "윤예진",
  "payrollId": null,
  "preparationStatus": "NOT_CREATED"
}
```

`NOT_CREATED`는 조회 DTO의 상태이지 `payroll.status` Enum이 아니다.

---

# 57. 월 요약정보

목록 API 또는 별도 Summary API에서 다음 값을 제공한다.

```text
targetEmployeeCount

notCreatedCount

calculatedCount

confirmedCount

totalEarnings

totalDeductions

totalNetPay
```

현재 이메일 발송 기능이 없으므로 다음 Summary는 제공하지 않는다.

```text
발송 완료
미발송
발송 실패
발송률
```

---

# 58. 직원 급여 상세 조회

```http
GET /api/payrolls/{payrollId}
```

예시:

```json
{
  "payrollId": 100,
  "employee": {
    "employeeId": 10,
    "name": "이민준",
    "employmentType": "REGULAR"
  },

  "yearMonth": "2026-08",

  "scheduledPayDate": "2026-09-05",

  "status": "CALCULATED",

  "attendance": {
    "workDays": 21,
    "workHours": 168,
    "overtimeHours": 6,
    "nightHours": 0,
    "holidayHours": 0
  },

  "earnings": [
    {
      "itemId": 1,
      "type": "BASE_SALARY",
      "name": "기본급",
      "sourceType": "CONTRACT",
      "amount": 3200000
    },
    {
      "itemId": 2,
      "type": "OVERTIME_PAY",
      "name": "연장근로수당",
      "sourceType": "ATTENDANCE",
      "amount": 180000,
      "calculationFormula": "20,000원 × 6시간 × 1.5"
    }
  ],

  "deductions": [
    {
      "itemId": 10,
      "type": "NATIONAL_PENSION",
      "name": "국민연금",
      "sourceType": "MOCK_INSURANCE",
      "amount": 152000,
      "editable": false
    },
    {
      "itemId": 11,
      "type": "LONG_TERM_CARE",
      "name": "장기요양보험",
      "sourceType": "MOCK_INSURANCE",
      "amount": 15000,
      "editable": false
    }
  ],

  "summary": {
    "totalEarnings": 3380000,
    "totalDeductions": 382980,
    "netPay": 2997020
  },

  "memo": null
}
```

---

# 59. editable

DB에 `editable` 컬럼을 저장할 필요는 없다.

`sourceType`과 `status`를 기준으로 API에서 계산한다.

예:

```text
status = CALCULATED
sourceType = CONTRACT

→ editable = true
```

```text
status = CALCULATED
sourceType = MOCK_INSURANCE

→ editable = false
```

```text
status = CONFIRMED

→ 모든 항목 editable = false
```

---

# 60. 급여 수정 API

```http
PATCH /api/payrolls/{payrollId}
```

가능 상태:

```text
CALCULATED
```

주요 수정 범위:

```text
지급항목 금액 조정

MANUAL 지급항목 추가/수정/삭제

memo
```

수정 불가능:

```text
보험 공제

세금 공제

Snapshot 직접 변경

scheduled_pay_date 직접 변경

status 직접 변경
```

---

# 61. 지급항목 추가 API

하나의 Update API에 포함시킬 수도 있고 별도 API로 나눌 수도 있다.

별도 API 예:

```http
POST /api/payrolls/{payrollId}/earnings
```

Request:

```json
{
  "name": "특별수당",
  "amount": 100000
}
```

생성:

```text
itemCategory = EARNING

itemType = OTHER_ALLOWANCE

sourceType = MANUAL
```

---

# 62. 직원 기본급 수정과 Payroll 기본급 수정은 다르다

Payroll 화면에서:

```text
기본급 3,200,000
→ 3,300,000
```

으로 수정해도:

```text
employee_compensation
```

은 수정하지 않는다.

해당 월:

```text
payroll_item
```

만 수정한다.

직원의 앞으로의 기본급을 변경하려면 별도의:

```text
직원 급여 계약 변경 API
```

를 사용해야 한다.

---

# 63. 급여 확정

```http
PATCH /api/payrolls/{payrollId}/confirm
```

가능 상태:

```text
CALCULATED
```

성공:

```text
status = CONFIRMED

confirmed_at = CURRENT_TIMESTAMP
```

---

# 64. 급여 확정 전 Validation

확정 시 최소한 다음을 확인한다.

```text
급여 계산이 완료됐는가

기본적인 지급항목이 존재하는가

보험 Mock 데이터가 필요한 직원에게 데이터가 존재하는가

세금 Mock 데이터가 존재하는가

totalEarnings가 계산되어 있는가

totalDeductions가 계산되어 있는가

netPay가 계산되어 있는가

scheduledPayDate가 존재하는가
```

조건이 충족되지 않으면 확정하지 않는다.

---

# 65. 확정 이후 직접 수정 금지

다음 상태에서는:

```text
CONFIRMED
```

다음 작업을 허용하지 않는다.

```text
payroll_item 수정

payroll_item 삭제

Snapshot 수정

금액 재계산

memo 이외 핵심 급여 데이터 수정
```

확정 후에도 `memo`만 수정할 수 있다.
`memo` 변경은 급여 금액, 지급항목, Snapshot 또는 Revision을 변경하지 않는다.

---

# 66. 확정 이후 정정

확정된 급여에서 오류가 발견되면 기존 급여를 UPDATE하지 않는다.

Revision을 생성한다.

예:

```text
Payroll #100

2026-08

revision = 1

CONFIRMED
```

정정 필요:

```text
연장근로수당 +40,000
```

새 Payroll:

```text
Payroll #127

2026-08

revision = 2

original_payroll_id = 100
```

---

# 67. Revision 생성 API

```http
POST /api/payrolls/{payrollId}/revisions
```

원본 Payroll은 반드시:

```text
CONFIRMED
```

상태여야 한다.

---

# 68. Revision 생성 시

기존 확정본을 복사한다.

```text
payroll

payroll_item

attendance_snapshot

compensation_snapshot

rule_snapshot
```

복사 후 새로운 Revision을:

```text
CALCULATED
```

상태로 만든다.

관리자가 수정한 후 다시:

```text
CONFIRMED
```

할 수 있다.

---

# 69. revision_no

최초:

```text
revision_no = 1
original_payroll_id = NULL
```

첫 정정:

```text
revision_no = 2
original_payroll_id = 최초 payroll_id
```

두 번째 정정:

```text
revision_no = 3
original_payroll_id = 최초 payroll_id
```

항상 최초 Payroll을 가리키도록 한다.

---

# 70. UNIQUE Constraint

최소 다음 조합을 Unique로 한다.

```text
user_id
payroll_year_month
revision_no
```

---

# 71. 최신 급여 조회

같은 직원 / 같은 월에 여러 Revision이 존재하면 기본 목록에서는 가장 최신 Revision을 조회한다.

예:

```text
revision 1 → CONFIRMED
revision 2 → CONFIRMED

목록 → revision 2
```

과거 Revision은 상세 이력에서 확인할 수 있도록 한다.

---

# 72. 급여 삭제

현재 급여 기능에서는 물리 삭제 API를 제공하지 않는다.

`DRAFT` 삭제는 생성했지만 아직 계산하지 않은 급여 작성을 취소하여
해당 직원과 귀속월을 다시 `NOT_CREATED` 조회 상태로 되돌리는 기능이다.

이번 구현에서는 이 기능을 제공하지 않는다.

특히:

```text
CONFIRMED
```

는 절대 삭제하지 않는다.

`DRAFT`, `CALCULATED`, `CONFIRMED`를 포함한 모든 Payroll을 이력으로 보존한다.
`NOT_CREATED`는 해당 직원과 귀속월의 Payroll이 한 번도 생성되지 않은 경우에만 사용한다.

---

# 73. 급여 미리보기

PDF는 구현하지 않지만 디자인의 "명세서 미리보기" 화면을 위해 View DTO를 제공할 수 있다.

```http
GET /api/payrolls/{payrollId}/preview
```

PDF 파일을 반환하는 API가 아니다.

JSON 데이터를 반환한다.

---

# 74. Preview 가능 상태

```text
CALCULATED
CONFIRMED
```

미리보기는 단순 조회이므로 상태를 변경하지 않는다.

---

# 75. Preview Response 예

```json
{
  "title": "2026년 8월 급여명세서",

  "employee": {
    "name": "이민준",
    "employmentType": "정규직"
  },

  "payrollYearMonth": "2026-08",

  "scheduledPayDate": "2026-09-05",

  "earnings": [
    {
      "name": "기본급",
      "amount": 3200000
    },
    {
      "name": "연장근로수당",
      "amount": 180000,
      "workHours": 6,
      "calculationFormula": "20,000원 × 6시간 × 1.5"
    }
  ],

  "deductions": [
    {
      "name": "국민연금",
      "amount": 152000
    },
    {
      "name": "건강보험",
      "amount": 115000
    },
    {
      "name": "장기요양보험",
      "amount": 15000
    },
    {
      "name": "고용보험",
      "amount": 30000
    },
    {
      "name": "소득세",
      "amount": 87800
    },
    {
      "name": "지방소득세",
      "amount": 8780
    }
  ],

  "totalEarnings": 3380000,
  "totalDeductions": 408580,
  "netPay": 2971420
}
```

현재 단계에서는 이 데이터를 프론트가 Modal로 보여주면 된다.

---

# 76. Preview와 실제 급여 데이터

Preview를 별도 저장하지 않는다.

다음 데이터를 조합하여 반환한다.

```text
Payroll
+
PayrollItem
+
Snapshot
+
Employee 정보
```

따라서:

```text
목록
상세
미리보기
```

가 서로 다른 값을 가지면 안 된다.

모든 화면은 동일한 Payroll Aggregate를 사용해야 한다.

---

# 77. Transaction

Payroll 한 건 계산은 하나의 Transaction에서 처리한다.

```text
payroll 상태 변경

payroll_item 생성

attendance snapshot

compensation snapshot

rule snapshot

합계 저장
```

중간 오류가 발생하면 전체 롤백한다.

---

# 78. 동시성

같은 직원의 같은 월 Payroll에 대해 동시에 Calculate / Update / Confirm 요청이 들어오는 상황을 고려한다.

```text
@Version
```

을 사용한 Optimistic Lock을 적용한다.

예:

```text
payroll.version
```

컬럼을 추가한다.

```text
payroll

...
version BIGINT NOT NULL DEFAULT 0
```

관리자 A가 수정한 뒤 관리자 B의 오래된 화면에서 저장하면 충돌을 감지한다.

Aggregate Root인 `payroll`에 적용한다.

```text
PayrollJpaEntity.version
payroll.version BIGINT NOT NULL DEFAULT 0
```

상세 응답은 현재 `version`을 반환한다.
Calculate / Recalculate / Payroll Update / Manual Item Add·Update·Delete / Memo Update / Confirm 요청은
화면이 조회했던 `expectedVersion`을 전달해야 오래된 화면의 요청까지 감지할 수 있다.

충돌 시에는 `409 Conflict`를 반환한다.
단, 중복 Confirm 충돌 후 현재 상태가 이미 `CONFIRMED`라면 멱등 성공으로 처리한다.

---

# 79. 멱등성

`confirm`을 두 번 호출했다고 해서 추가 데이터가 생성되면 안 된다.

이미:

```text
CONFIRMED
```

라면 멱등 성공으로 처리하고 현재 확정 결과를 반환한다.

중복 호출로 `confirmed_at`을 다시 기록하거나 Snapshot, PayrollItem, Revision을 추가 생성하지 않는다.

Revision 생성 또한 중복 생성에 주의한다.

---

# 80. 권한

급여 데이터는 권한이 있는 사용자만 접근할 수 있다.

기존 권한 시스템이 존재한다면 그것을 사용한다.

예:

```text
PAYROLL_READ
PAYROLL_MANAGE
```

### PAYROLL_READ

```text
월 급여 목록
급여 상세
급여 미리보기
Revision 조회
```

### PAYROLL_MANAGE

```text
급여 생성
급여 계산
급여 수정
급여 확정
급여 Revision 생성
급여 정책 관리
직원 급여 계약 관리
```

---

# 81. Mock Data 구성 기준

Mock 데이터는 현재 애플리케이션 코드나 Flyway Seed로 추가하지 않는다.
필요한 시점에 별도의 DB 입력 SQL로 구성한다.

향후 DB 입력 SQL은 최소 다음 케이스를 포함한다.

```text
A
월급제
5인 이상 사업장
연장근로 존재
보험 전체 적용

B
월급제
연장근로 없음

C
시급제
주휴수당 발생

D
보험 적용 제외 항목 존재

E
월급제
승인된 유급휴가 존재

F
5인 미만 사업장

G
다음 달 5일 지급
```

---

# 82. Mock 데이터 일관성

Mock 데이터라고 하더라도 다음 데이터가 서로 모순되지 않도록 한다.

```text
목록 금액

상세 금액

미리보기 금액

PayrollItem

totalEarnings

totalDeductions

netPay
```

프론트 화면마다 별도의 하드코딩 금액을 사용하면 안 된다.

DB의 Payroll Aggregate를 단일 Source of Truth로 사용한다.

---

# 83. API 전체 초안

현재 구현 대상 API:

```text
GET
/api/payrolls

POST
/api/payrolls/employees/{employeeId}

PATCH
/api/payrolls/{payrollId}/calculate

GET
/api/payrolls/{payrollId}

PATCH
/api/payrolls/{payrollId}

POST
/api/payrolls/{payrollId}/earnings

DELETE
/api/payrolls/{payrollId}/earnings/{itemId}

PATCH
/api/payrolls/{payrollId}/confirm

POST
/api/payrolls/{payrollId}/revisions

GET
/api/payrolls/{payrollId}/revisions

GET
/api/payrolls/{payrollId}/preview
```

급여 정책:

```text
GET
/api/payroll/policies

PATCH
/api/payroll/policies
```

직원 급여 설정:

```text
GET
/api/payroll/employees/{employeeId}/compensation

PATCH
/api/payroll/employees/{employeeId}/compensation
```

---

# 84. 이번 단계에서 만들지 않는 API

```text
POST /statements/send

POST /statements/resend

GET /statements/pdf

GET /statements/download

POST /auto-delivery

PUT /auto-delivery
```

이메일 / PDF / 자동발송 관련 API는 구현하지 않는다.

---

# 85. Hexagonal Architecture

```text
payroll
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── CreatePayrollUseCase
│   │   │   ├── CalculatePayrollUseCase
│   │   │   ├── UpdatePayrollUseCase
│   │   │   ├── ConfirmPayrollUseCase
│   │   │   ├── CreatePayrollRevisionUseCase
│   │   │   ├── GetPayrollUseCase
│   │   │   └── GetPayrollPreviewUseCase
│   │   │
│   │   └── out
│   │       ├── PayrollRepositoryPort
│   │       ├── EmployeeCompensationPort
│   │       ├── PayrollAttendancePort
│   │       ├── SocialInsurancePort
│   │       ├── TaxAssessmentPort
│   │       ├── StatutoryPolicyPort
│   │       └── WorkplaceLaborScopePort
│   │
│   └── service
│       ├── CreatePayrollService
│       ├── CalculatePayrollService
│       ├── UpdatePayrollService
│       ├── ConfirmPayrollService
│       ├── CreatePayrollRevisionService
│       └── GetPayrollService
│
├── domain
│   ├── Payroll
│   ├── PayrollItem
│   ├── PayrollStatus
│   ├── PayrollItemType
│   ├── PayrollItemCategory
│   ├── PayrollItemSourceType
│   └── PayrollCalculator
│
└── adapter
    ├── in
    │   └── web
    │
    └── out
        ├── persistence
        ├── attendance
        ├── insurance
        └── tax
```

---

# 86. Mock Adapter

보험:

```text
SocialInsurancePort
        ↑
MockSocialInsuranceAdapter
```

세금:

```text
TaxAssessmentPort
        ↑
MockTaxAssessmentAdapter
```

사업장 법 적용:

```text
WorkplaceLaborScopePort
        ↑
MockWorkplaceLaborScopeAdapter
```

통상시급 역시 DB Mock 데이터에서 조회한다.

향후 실제 연동을 할 경우 Adapter만 교체할 수 있도록 한다.

---

# 87. 반드시 지켜야 할 Domain Rule

### Rule 1

Payroll이 존재하지 않는 상태와 `DRAFT`를 구분한다.

```text
Payroll 없음 = 미작성

Payroll.status = DRAFT
= 생성됐지만 계산 전
```

### Rule 2

급여 계산 결과는 항상 서버에서 합산한다.

프론트가 `totalEarnings`, `netPay`를 결정하지 않는다.

### Rule 3

보험/세금 공제 Mock 값은 일반 급여 수정 API에서 변경할 수 없다.

### Rule 4

장기요양보험을 공제항목에 포함한다.

### Rule 5

산재보험을 직원 급여 공제항목에 포함하지 않는다.

### Rule 6

고용형태와 급여형태를 구분한다.

```text
employmentType != salaryType
```

### Rule 7

확정 전까지는 재계산할 수 있다.

### Rule 8

`CONFIRMED` 상태에서는 직접 수정하거나 재계산할 수 없다.

### Rule 9

확정 이후 정정은 Revision을 생성한다.

### Rule 10

급여 계산 시 근태/계약/법적 기준 Snapshot을 저장한다.

### Rule 11

과거 급여는 현재 직원 급여 계약이 변경되어도 영향을 받지 않는다.

### Rule 12

월별 수기 지급항목은 직원의 기본 급여계약을 수정하지 않는다.

### Rule 13

급여 귀속월과 지급월을 구분한다.

```text
payment_month_offset
```

을 사용한다.

### Rule 14

`scheduled_pay_date`는 지급 예정일이며 실제 지급 완료를 의미하지 않는다.

### Rule 15

현재 시스템에는:

```text
PAID
STATEMENT_ISSUED
```

상태를 만들지 않는다.

### Rule 16

PDF / 이메일 / 자동발송은 현재 Payroll Domain 범위에 포함하지 않는다.

---

# 88. 핵심 테이블 최종 목록

현재 단계에서 필요한 핵심 테이블:

```text
statutory_policy

workplace_labor_scope

payroll_policy

employee_compensation

employee_fixed_allowance

employee_pay_basis

social_insurance_status

social_insurance_assessment

tax_assessment

payroll

payroll_item

payroll_attendance_snapshot

payroll_compensation_snapshot

payroll_rule_snapshot
```

향후 PDF/메일 기능 구현 시 별도로 추가:

```text
payroll_statement

payroll_statement_delivery

payroll_auto_delivery_setting
```

현재는 생성하지 않는다.

---

# 89. 최종 구현 흐름

```text
직원 선택
   ↓
Payroll 생성
   ↓
DRAFT
   ↓
급여 계산
   ↓
계약 / 근태 / 보험 / 세금 조회
   ↓
PayrollItem 생성
   ↓
Snapshot 생성
   ↓
CALCULATED
   ↓
관리자 상세 검토
   ↓
필요한 지급항목 수정
   ↓
미리보기
   ↓
급여 확정
   ↓
CONFIRMED
```

확정 후 오류가 발견되면:

```text
CONFIRMED
   ↓
Revision 생성
   ↓
CALCULATED
   ↓
수정
   ↓
CONFIRMED
```

---

# 90. 구현 우선순위

### 1단계

```text
payroll_policy

employee_compensation

employee_fixed_allowance

employee_pay_basis
```

### 2단계

Mock:

```text
statutory_policy

workplace_labor_scope

social_insurance_status

social_insurance_assessment

tax_assessment
```

### 3단계

```text
Payroll
PayrollItem
```

Domain 구현.

### 4단계

Attendance Port 연결.

### 5단계

PayrollCalculator 구현.

### 6단계

```text
DRAFT
→ CALCULATED
→ CONFIRMED
```

상태 전이 구현.

### 7단계

급여 상세 수정 및 월별 추가 지급항목 구현.

### 8단계

Snapshot 구현.

### 9단계

Revision 구현.

### 10단계

목록 / 상세 / 미리보기 API 구현.

---

# 91. 현재 단계의 최종 목표

이번 단계의 최종 목표는 다음이다.

```text
직원 급여조건
+
근태
+
Mock 보험
+
Mock 세금
+
법정 계산 기준

        ↓

월 급여 생성

        ↓

지급 / 공제 항목 계산

        ↓

관리자 검토 및 수정

        ↓

과거 계산 근거 Snapshot

        ↓

급여 확정

        ↓

확정 이후 Revision 이력 관리
```

PDF 생성, 파일 다운로드, 이메일 전송, 자동 발송은 이 급여 계산 도메인이 안정화된 이후 별도의 기능으로 확장한다.
