CREATE TABLE academy_application (
  application_id BIGINT NOT NULL AUTO_INCREMENT,
  requested_login_id VARCHAR(50) NOT NULL,
  academy_name VARCHAR(100) NOT NULL,
  business_no VARCHAR(20) NOT NULL,
  representative_name VARCHAR(50) NOT NULL,
  representative_email VARCHAR(100) NOT NULL,
  representative_phone VARCHAR(20) NOT NULL,
  business_license_file_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reject_reason VARCHAR(255) NULL,
  reviewed_by_user_id BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (application_id),
  CONSTRAINT fk_academy_application_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);

ALTER TABLE academy ADD COLUMN application_id BIGINT NULL;
ALTER TABLE academy ADD CONSTRAINT fk_academy_application FOREIGN KEY (application_id) REFERENCES academy_application (application_id) ON DELETE SET NULL;
