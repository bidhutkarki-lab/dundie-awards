CREATE TABLE dundie_awards (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    giver_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    awarded_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_dundie_awards_recipient FOREIGN KEY (recipient_id) REFERENCES employees (id),
    CONSTRAINT fk_dundie_awards_giver FOREIGN KEY (giver_id) REFERENCES employees (id),
    CONSTRAINT fk_dundie_awards_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_dundie_awards_recipient ON dundie_awards (recipient_id);
CREATE INDEX idx_dundie_awards_organization ON dundie_awards (organization_id);
