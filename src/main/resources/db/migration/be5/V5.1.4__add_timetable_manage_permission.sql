INSERT INTO permission (code, resource, action, description)
SELECT 'TIMETABLE:MANAGE', 'TIMETABLE', 'MANAGE', '시간표 세트/수업 슬롯 작성/수정/삭제'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'TIMETABLE:MANAGE');
