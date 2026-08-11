INSERT INTO permission (code, resource, action, description)
SELECT 'WORKSPACE:READ_ALL', 'WORKSPACE', 'READ_ALL',
       '같은 학원의 모든 활성 워크스페이스와 그 하위(업무 상세·댓글 목록·반복 업무 템플릿 목록)를 참여 여부와 무관하게 조회할 수 있습니다.'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'WORKSPACE:READ_ALL');

INSERT INTO permission (code, resource, action, description)
SELECT 'WORKSPACE:CREATE', 'WORKSPACE', 'CREATE', '워크스페이스를 새로 생성할 수 있습니다.'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'WORKSPACE:CREATE');
