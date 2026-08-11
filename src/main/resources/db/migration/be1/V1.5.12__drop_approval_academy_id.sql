-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- approval_document, template(결재 템플릿 공용 테이블) 테이블의 academy_id 컬럼을 제거한다.
-- approval_document -> template 복합 FK(template_id, academy_id)를 template_id 단일 FK로 단순화한다.

ALTER TABLE `approval_document`
  DROP FOREIGN KEY `fk_approval_document_template`;

ALTER TABLE `approval_document`
  DROP COLUMN `academy_id`;

ALTER TABLE `template`
  DROP INDEX `uk_template_id_academy`;

ALTER TABLE `template`
  DROP COLUMN `academy_id`;

ALTER TABLE `approval_document`
  ADD CONSTRAINT `fk_approval_document_template` FOREIGN KEY (`template_id`) REFERENCES `template` (`template_id`);
