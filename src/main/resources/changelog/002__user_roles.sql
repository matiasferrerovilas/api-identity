--liquibase formatted sql
--changeset matigfv:2

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

ALTER TABLE users DROP COLUMN user_role;
