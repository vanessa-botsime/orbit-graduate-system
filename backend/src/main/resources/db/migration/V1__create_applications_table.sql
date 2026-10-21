CREATE TABLE applications (
    id          BIGSERIAL PRIMARY KEY,
    company     VARCHAR(150) NOT NULL,
    role        VARCHAR(150) NOT NULL,
    status      VARCHAR(30)  NOT NULL DEFAULT 'APPLIED',
    deadline    DATE,
    applied_on  DATE,
    notes       TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
