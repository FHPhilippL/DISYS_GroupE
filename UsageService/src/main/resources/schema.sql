CREATE TABLE IF NOT EXISTS usage_hourly (
                                            hour TIMESTAMP PRIMARY KEY,
                                            community_produced DOUBLE PRECISION,
                                            community_used DOUBLE PRECISION,
                                            grid_used DOUBLE PRECISION
);