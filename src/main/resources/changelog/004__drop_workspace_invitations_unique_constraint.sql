--liquibase formatted sql
--changeset matigfv:4

ALTER TABLE workspace_invitations
    ADD INDEX idx_workspace_invitations_workspace (workspace_id),
    DROP INDEX uq_workspace_invitations_workspace_user;
