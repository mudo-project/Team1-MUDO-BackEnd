INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'LECTURE:READ', 'LECTURE', 'READ', '강의관리 탭 접근 및 강의 목록/상세 조회'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'LECTURE:READ'
);

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'ROLLCALL:TEMPLATE_MANAGE', 'ROLLCALL', 'TEMPLATE_MANAGE', '문자 템플릿 생성/수정/삭제'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'ROLLCALL:TEMPLATE_MANAGE'
);

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'APPROVAL:SUBMIT', 'APPROVAL', 'SUBMIT', '결재 상신 및 재상신'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'APPROVAL:SUBMIT'
);

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'APPROVAL:TEMPLATE_MANAGE', 'APPROVAL', 'TEMPLATE_MANAGE', '결재 템플릿 생성/수정/삭제'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'APPROVAL:TEMPLATE_MANAGE'
);

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'NOTICE:WRITE', 'NOTICE', 'WRITE', '공지 작성 및 본인 공지 수정'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'NOTICE:WRITE'
);

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'NOTICE:PIN', 'NOTICE', 'PIN', '공지 고정 및 고정 해제'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'NOTICE:PIN'
);

UPDATE `permission`
SET `description` = '학생관리 탭 접근, 학생 목록/상세 조회, 학생 등록/수정, 수강 등록/종료'
WHERE `code` = 'STUDENT:MANAGE';

UPDATE `permission`
SET `description` = '출결관리 탭 접근, 출결 조회, 출결 저장/수정'
WHERE `code` = 'ROLLCALL:MANAGE';

DELETE rp
FROM `role_permission` rp
JOIN `permission` p ON p.`permission_id` = rp.`permission_id`
WHERE p.`code` IN ('STUDENT:READ', 'ENROLLMENT:MANAGE');

DELETE FROM `permission`
WHERE `code` IN ('STUDENT:READ', 'ENROLLMENT:MANAGE');
