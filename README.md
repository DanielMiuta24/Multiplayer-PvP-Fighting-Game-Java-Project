# Code Brawl: Network Fighters

**Author:** Daniel Miuta

## Overview
Arcade‑style multiplayer PvP built in Java. Players choose a character, join an arena, move in real time, attack, and rack up KOs. Stats are persisted (kills/deaths), and a Top‑10 leaderboard can be toggled in‑game.

## Game Flow
1. Login/Register
2. Pick character (max 3 per user)
3. Join arena
4. Real‑time movement/attacks; press **N** to view Top‑10
5. Respawn after KO

## Tech Stack
- **Language:** Java 17
- **Client:** JavaFX 21 (Canvas rendering), AnimationTimer
- **Server:** Custom TCP (RtServer/RtClient), fixed‑step game loop
- **Persistence:** SQLite via JDBC (AuthManager, DatabaseManager)

## OOP Highlights
- **Fighter** abstraction with subclasses: `Samurai`, `Shinobi`, `Warrior`
- **World** encapsulates gameplay & tick loop
- **AuthManager** encapsulates DB I/O and schema
- **ClientSession** encapsulates per‑connection protocol state

## Key Algorithms
1. **Movement Integration & Clamping** – integrate velocity each frame with diagonal normalization (1/√2) and clamp to arena bounds to prevent leaving the map.
2. **Melee Hit Detection & Damage** – nested loop checks range (squared), facing, cooldown; applies guard‑reduced damage, knockback, KO handling, and DB persistence.
3. **Top‑N Leaderboard** – single SQL query with `ORDER BY kills DESC, deaths ASC LIMIT N`; server builds compact `TOP` message for the client overlay.

## Database
- File: `data/codebrawl.db`
- Tables: `users`, `characters`, `character_stats`
- Triggers: `trg_limit_chars` (max 3 characters per user)
- Stats updated on KO; kills/deaths are persisted across sessions.

## Build & Run
### Server
```bash
mvn -q -DskipTests package
java -cp target/multiplayer-pvp-fighting-game-0.1.0.jar com.codebrawl.net.RtServer 12345
```
### Client
```bash
mvn -q javafx:run -Dexec.mainClass=com.codebrawl.ui.ClientApp
```
(or run `ClientApp` from your IDE)

## Controls
- **WASD** move
- **SPACE** basic attack
- **J** jump (client-side anim trigger)
- **K** guard
- **N** toggle Top‑10 overlay
- **ESC** pause menu

## Differences vs Proposal
- HUD integrated into canvas; leaderboard toggled with **N**
- Death animation freeze on last frame to avoid jitter
- DB‑backed stats with reliable updates on KO
- Snapshot protocol and rendering stabilized

## License
Internal student project; all rights reserved.
