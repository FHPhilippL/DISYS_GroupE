CREATE TABLE IF NOT EXISTS current_percentage (
    hour TIMESTAMP PRIMARY KEY,
    community_depleted DOUBLE PRECISION NOT NULL,
    grid_portion DOUBLE PRECISION NOT NULL
);
