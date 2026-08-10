ALTER TABLE academy_application
  ADD COLUMN requested_login_id_active VARCHAR(50)
    GENERATED ALWAYS AS (
      CASE WHEN status IN ('PENDING', 'APPROVED') THEN requested_login_id ELSE NULL END
    ) STORED;

-- REJECTED 상태 신청서는 NULL로 빠지므로 유니크 제약 대상에서 제외된다
-- (MySQL은 UNIQUE 컬럼의 NULL 여러 개를 위반으로 보지 않음) — 반려된 아이디로 재신청 가능.
ALTER TABLE academy_application
  ADD CONSTRAINT uk_academy_application_requested_login_id_active
    UNIQUE (requested_login_id_active);
