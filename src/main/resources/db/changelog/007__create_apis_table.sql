--liquibase formatted sql
--changeset matigfv:7

CREATE TABLE apis (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_apis_name UNIQUE (name)
);

INSERT INTO apis (name)
SELECT DISTINCT api FROM onboardings_done;

ALTER TABLE onboardings_done
    ADD COLUMN api_id BIGINT;

UPDATE onboardings_done o
    JOIN apis a ON a.name = o.api
    SET o.api_id = a.id;

ALTER TABLE onboardings_done
    MODIFY COLUMN api_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_onboardings_done_api FOREIGN KEY (api_id) REFERENCES apis (id),
    DROP INDEX uq_onboardings_done_user_api,
    DROP COLUMN api,
    ADD CONSTRAINT uq_onboardings_done_user_api UNIQUE (user_id, api_id);
