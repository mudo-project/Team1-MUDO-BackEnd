ALTER TABLE academy_application MODIFY COLUMN business_no VARCHAR(20) NULL;
ALTER TABLE academy_application ADD COLUMN plan VARCHAR(10) NULL;
UPDATE academy_application SET plan = 'FREE' WHERE plan IS NULL OR plan = '';
ALTER TABLE academy_application MODIFY COLUMN plan VARCHAR(10) NOT NULL;

-- 접수 시점에 사업자등록번호를 더 이상 받지 않으므로(신청서 business_no가 항상 null),
-- 승인 시 생성되는 academy.business_no도 같은 이유로 nullable로 완화한다.
-- uk_academy_business_no UNIQUE 제약은 유지된다(MySQL은 NULL 여러 개를 유니크 위반으로 보지 않음).
ALTER TABLE academy MODIFY COLUMN business_no VARCHAR(20) NULL;
