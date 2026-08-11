ALTER TABLE lecture
  ADD COLUMN class_type VARCHAR(20) NULL AFTER name,
  ADD COLUMN subject_name VARCHAR(50) NULL AFTER subject_id,
  ADD COLUMN teacher_name VARCHAR(50) NULL AFTER teacher_id,
  ADD COLUMN classroom_code VARCHAR(50) NULL AFTER classroom_id,
  MODIFY grade VARCHAR(20) NULL,
  MODIFY term_id BIGINT NULL,
  MODIFY subject_id BIGINT NULL,
  MODIFY teacher_id BIGINT NULL,
  MODIFY classroom_id BIGINT NULL;
