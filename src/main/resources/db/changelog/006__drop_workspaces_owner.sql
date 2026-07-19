--liquibase formatted sql
--changeset matigfv:6

UPDATE workspace_members wm
    JOIN workspaces w ON w.id = wm.workspace_id
    SET wm.role = 'OWNER'
WHERE wm.user_id = w.owner_id;

ALTER TABLE workspaces
    DROP FOREIGN KEY fk_workspaces_owner,
    DROP INDEX uq_workspaces_owner_name,
    DROP COLUMN owner_id;
