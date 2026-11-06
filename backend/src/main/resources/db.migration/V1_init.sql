CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE job_applications (
    id              UUID PRIMARY KEY,
    owner_id        UUID         NOT NULL REFERENCES users (id),
    company         VARCHAR(200) NOT NULL,
    role_title      VARCHAR(200) NOT NULL,
    job_url         VARCHAR(1000),
    job_description TEXT,
    status          VARCHAR(20)  NOT NULL,
    deadline        DATE,
    notes           TEXT,
    match_score     INTEGER,
    risk_level      VARCHAR(10),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_applications_owner    ON job_applications (owner_id);
CREATE INDEX idx_applications_deadline ON job_applications (deadline);

CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    event_id       UUID        NOT NULL UNIQUE,
    event_type     VARCHAR(40) NOT NULL,
    owner_id       UUID        NOT NULL,
    application_id UUID,
    detail         VARCHAR(500),
    occurred_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_owner_time ON audit_logs (owner_id, occurred_at DESC);
