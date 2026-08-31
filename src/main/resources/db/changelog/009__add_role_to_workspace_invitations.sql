--liquibase formatted sql
--changeset matigfv:9

ALTER TABLE workspace_invitations
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'COLLABORATOR';
