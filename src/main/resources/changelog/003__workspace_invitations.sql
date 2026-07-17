--liquibase formatted sql
--changeset matigfv:3

CREATE TABLE workspace_invitations (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id     BIGINT NOT NULL,
    invited_by_id    BIGINT NOT NULL,
    invited_user_id  BIGINT NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspace_invitations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_invitations_invited_by FOREIGN KEY (invited_by_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_invitations_invited_user FOREIGN KEY (invited_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_workspace_invitations_workspace_user UNIQUE (workspace_id, invited_user_id)
);
