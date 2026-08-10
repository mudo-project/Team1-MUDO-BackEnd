ALTER TABLE workspace DROP INDEX uk_workspace_academy_active_name;
ALTER TABLE workspace ADD CONSTRAINT uk_workspace_active_name UNIQUE (active_name);
ALTER TABLE workspace DROP COLUMN academy_id;
