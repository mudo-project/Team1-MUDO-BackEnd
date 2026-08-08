INSERT INTO permission (code, resource, action, description)
SELECT 'CALENDAR:MANAGE', 'CALENDAR', 'MANAGE', '학원 공용 캘린더 일정 작성/수정/삭제'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'CALENDAR:MANAGE');
