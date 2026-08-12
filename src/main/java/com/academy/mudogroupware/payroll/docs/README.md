# Payroll 모듈

## 책임

- 직원별 급여 계약과 급여 정책을 관리한다.
- Users와 Attendance가 제공하는 조회 Port로 월 급여를 계산한다.
- 계산 당시 계약·근태·법정 기준을 Snapshot으로 보존한다.
- `DRAFT → CALCULATED → CONFIRMED` 상태와 정정 Revision을 관리한다.
- 확정 후 PDF 급여명세서를 생성해 Finance S3 버킷에 저장한다.

## 경계

- 테이블과 조회 계약에는 `academy_id`를 사용하지 않는다. 학원별 DB 스키마가 격리 경계다.
- 급여 및 급여명세서 API는 모두 `PAYROLL:MANAGE` 권한을 요구한다.
- 보험·세금·법 적용 데이터는 DB에 별도로 입력된 Mock 데이터를 조회하며 애플리케이션이 Seed하지 않는다.
- 직원·근태 Entity나 Repository를 Payroll이 직접 참조하지 않는다.

## S3

- 일반 파일은 Staff 버킷(`AWS_S3_STAFF_BUCKET_NAME`)을 사용한다.
- 급여명세서는 Finance 버킷(`AWS_S3_FINANCE_BUCKET_NAME`)만 사용한다.
- Key 형식은 `tenants/{tenantId}/payroll-statements/{yyyy}/{mm}/{uuid}.pdf`다.
