CREATE TABLE category (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(20)
);

CREATE TABLE work_day (
    id    BIGSERIAL PRIMARY KEY,
    date  DATE NOT NULL UNIQUE,
    notes TEXT
);

CREATE TABLE time_entry (
    id           BIGSERIAL PRIMARY KEY,
    work_day_id  BIGINT    NOT NULL REFERENCES work_day (id) ON DELETE CASCADE,
    start_time   TIME      NOT NULL,
    end_time     TIME      NOT NULL,
    category_id  BIGINT    NOT NULL REFERENCES category (id),
    notes        TEXT
);

CREATE INDEX idx_time_entry_work_day ON time_entry (work_day_id);
CREATE INDEX idx_time_entry_category ON time_entry (category_id);
