INSERT INTO template (template_id, file_id, type, name, created_by)
SELECT 1, NULL, 'APPROVAL', '법인카드 정산', 1
WHERE NOT EXISTS (
    SELECT 1 FROM template t WHERE t.template_id = 1
);

UPDATE template
SET name = '법인카드 정산', type = 'APPROVAL'
WHERE template_id = 1;

ALTER TABLE template AUTO_INCREMENT = 2;
