CREATE TABLE ad_placements (
    id              BIGSERIAL PRIMARY KEY,
    slot_key        VARCHAR(50)  NOT NULL UNIQUE,
    page            VARCHAR(100) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    provider_unit_id VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO ad_placements (slot_key, page, enabled) VALUES
    ('landing_hero_below', 'LANDING', TRUE),
    ('search_results_below', 'SEARCH', TRUE),
    ('dashboard_sidebar', 'DASHBOARD', TRUE);
