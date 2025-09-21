
CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT NOT NULL COLLATE NOCASE UNIQUE,
  password_hash TEXT NOT NULL,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);


CREATE TABLE IF NOT EXISTS characters (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id    INTEGER NOT NULL,
  name       TEXT NOT NULL COLLATE NOCASE,
  clazz      TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, name),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);


CREATE TRIGGER IF NOT EXISTS trg_limit_chars
BEFORE INSERT ON characters
BEGIN
  SELECT CASE WHEN (
    (SELECT COUNT(*) FROM characters WHERE user_id = NEW.user_id) >= 3
  ) THEN RAISE(ABORT, 'char_limit_reached') END;
END;


CREATE TABLE IF NOT EXISTS character_stats (
  character_id INTEGER PRIMARY KEY,
  kills        INTEGER NOT NULL DEFAULT 0,
  deaths       INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(character_id) REFERENCES characters(id) ON DELETE CASCADE
);
