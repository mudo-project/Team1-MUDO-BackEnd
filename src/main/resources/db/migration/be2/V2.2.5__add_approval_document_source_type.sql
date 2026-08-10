ALTER TABLE approval_document
    ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL'
        AFTER template_id;
