-- Flyway: 초기 스키마
-- GroupWare 프로젝트 최초 테이블 생성

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ  NOT NULL,
    updated_by      VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_active
    ON users(email) WHERE deleted = FALSE;
