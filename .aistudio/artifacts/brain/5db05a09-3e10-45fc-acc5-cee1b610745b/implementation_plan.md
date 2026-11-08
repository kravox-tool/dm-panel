# DM Panel - Comprehensive UI & Discord REST API Feature Fix Plan

Fix prompt button layout overflow, remove function 6 (PYTHON), remove auto-numbered message suffixes (`[#1]`, `[#2]`), implement live 2-way DM console chat, update MASIVO to send direct messages to all server members, and resolve the FLASH token error.

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan addresses all 6 issues identified by the user in the mobile screenshot and brief:

- **Fix 1 (Prompt Bar Button Layout Overflow)**: Update `.prompt-bar` in `style.css` so the red `EJECUTAR` button and command input flex cleanly within screen bounds without overflowing horizontally.
- **Fix 2 (Remove Function 6)**: Remove `[6] PYTHON` entirely from `index.html`, `app.js`, `style.css`, and the quick action button row.
- **Fix 3 (Exact Message Content)**: Remove any appended sequence numbers (`[#1]`, `[#2]`) so messages are delivered exactly as authored.
- **Fix 4 (Live 2-Way CHAT Console)**: In `CHAT`, poll `GET /channels/{dmChannel}/messages` for incoming DMs from the target User ID and stream them to the console (`<- [@User]: message`), allowing real-time response from the CLI prompt (`-> [@User]: message`).
- **Fix 5 (MASIVO DM to Server Members)**: Refactor `MASIVO` to fetch guild members via `GET /guilds/{serverId}/members?limit=1000` and send a 1-on-1 Direct Message to every member's inbox.
- **Fix 6 (FLASH Token Passing Fix)**: Correct `launchFlash()` in `app.js` to pass `token` cleanly to `@JavascriptInterface sendFlashMessage()`.

---

## 1. Overview & Core Concept

- **What It Does**: Resolves all UI overflow issues and refactors feature behaviors so that DM-SPAM, CHAT, MASIVO, and FLASH execute real Discord REST API actions with exact message payloads and real-time DM streaming.
- **Target Audience**: Discord Bot developers requiring precision DM tools and clean mobile terminal UX.

---

## 2. User Experience & Visual Design

### Prompt Bar Layout Refactoring

```
┌───────────────────────────────────────────────────────────────┐
│ -> Elige una opcion: [1-5 o comando]                          │
├───────────────────────────────────────────────────────────────┤
│ [ EJECUTAR ]                                                  │
└───────────────────────────────────────────────────────────────┘
```

- **Responsive Prompt Bar**: Input on top row, full-width `EJECUTAR` button below or wrapped flex container, preventing right-edge clipping.
- **Menu Items**: Clean 5-function menu (`[1] DM-SPAM`, `[2] CHAT`, `[3] MASIVO`, `[4] FLASH`, `[5] INFO`, `[0] SALIR`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: MASIVO Server Member Resolution**
  - *Chosen Approach*: Query `GET /guilds/{serverId}/members?limit=1000` -> iterate through member User IDs -> call `POST /users/@me/channels` -> send DM payload.
  - *Why*: Directly aligns with user requirement to DM all members of a server.

- **Decision 2: CHAT Real-time Message Polling**
  - *Chosen Approach*: Maintain a 2-second background poll loop in `BotOperationManager` for the active CHAT DM channel ID, appending new incoming messages to the console log (`<- [@User]: text`).

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────┐
│                    DM Panel UI (app.js)                       │
│  [1] DM-SPAM | [2] CHAT | [3] MASIVO | [4] FLASH | [5] INFO    │
└───────────────────────────────┬───────────────────────────────┘
                                │ Calls AndroidBridge
┌───────────────────────────────▼───────────────────────────────┐
│               BotOperationManager.kt (IO Scope)               │
│                                                               │
│  ├─ CHAT: Polling GET /channels/{dmId}/messages -> UI Log     │
│  ├─ MASIVO: GET /guilds/{id}/members -> DM each member ID     │
│  └─ FLASH: POST /messages -> delay -> DELETE /messages/{id}   │
└───────────────────────────────────────────────────────────────┘
```

### Bug Fix Implementation Matrix

| Issue | Target File | Fix Applied |
|---|---|---|
| Prompt Button Overflow | `style.css` | Flex-direction column/wrap on `.prompt-bar` |
| Remove Function 6 | `index.html`, `app.js` | Delete option 6 from menu and handlers |
| Message Suffix Numbers | `BotOperationManager.kt` | Send raw `effectiveMsg` without `[#i]` |
| Live Chat 2-Way | `BotOperationManager.kt` | Implement DM polling listener loop |
| MASIVO DM Server Members | `BotOperationManager.kt` | Query `/guilds/{id}/members` -> DM each member |
| FLASH Token Error | `app.js` | Fix `launchFlash` token argument signature |
