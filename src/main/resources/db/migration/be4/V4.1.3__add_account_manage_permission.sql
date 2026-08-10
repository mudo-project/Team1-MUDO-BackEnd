INSERT INTO permission (code, resource, action, description)
SELECT 'ACCOUNT:MANAGE', 'ACCOUNT', 'MANAGE', '학원 구성원 계정 관리(역할 변경 등)'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'ACCOUNT:MANAGE');
