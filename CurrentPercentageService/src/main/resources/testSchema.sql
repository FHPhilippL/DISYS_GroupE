CREATE TABLE IF NOT EXISTS usage_hourly (
    hour TIMESTAMP PRIMARY KEY,
    community_produced DOUBLE PRECISION,
    community_used DOUBLE PRECISION,
    grid_used DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS current_percentage (
    hour TIMESTAMP PRIMARY KEY,
    community_depleted DOUBLE PRECISION NOT NULL,
    grid_portion DOUBLE PRECISION NOT NULL
);