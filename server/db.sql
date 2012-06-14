PRAGMA foreign_keys = ON;

CREATE TABLE Users (
    google_id TEXT PRIMARY KEY,
    c2dm_id TEXT,
    session TEXT,
    other_id INTEGER,
    FOREIGN KEY(other_id) REFERENCES Users(id)
    );

