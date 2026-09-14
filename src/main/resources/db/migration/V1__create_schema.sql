CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dundie_awards INTEGER,
    organization_id BIGINT,
    CONSTRAINT fk_employees_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    event VARCHAR(255),
    occurred_at TIMESTAMP(6)
);
