CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT  NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  DATETIME DEFAULT CURRENT_TIMESTAMP
);

--  Note: Schema is also auto-created by AuthManager.initSchema() at runtime. This file is provided for reference/testing.

