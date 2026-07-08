--liquibase formatted sql

--changeset matigfv:1
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) UNIQUE,
    given_name     VARCHAR(255),
    family_name    VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_first_login BOOLEAN   NOT NULL DEFAULT TRUE,
    user_type      VARCHAR(255) NOT NULL,
    has_seen_tour  BOOLEAN   NOT NULL DEFAULT FALSE
);
--rollback DROP TABLE users;

--changeset matigfv:2
CREATE TABLE files (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id    UUID,
    owner_id     BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    size         BIGINT,
    ubicacion    VARCHAR(1024),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_parent FOREIGN KEY (parent_id) REFERENCES files (id) ON DELETE CASCADE,
    CONSTRAINT chk_files_type CHECK (type IN ('FOLDER', 'FILE'))
);
--rollback DROP TABLE files;

--changeset matigfv:3
CREATE INDEX idx_files_owner_id ON files (owner_id);
CREATE INDEX idx_files_parent_id ON files (parent_id);
--rollback DROP INDEX idx_files_owner_id;
--rollback DROP INDEX idx_files_parent_id;
