--liquibase formatted sql
--changeset matigfv:8

CREATE TABLE audit_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id     BIGINT NOT NULL,
    action           VARCHAR(50) NOT NULL,
    actor_user_id    BIGINT NOT NULL,
    target_user_id   BIGINT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_target FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_log_workspace ON audit_log (workspace_id, created_at);
