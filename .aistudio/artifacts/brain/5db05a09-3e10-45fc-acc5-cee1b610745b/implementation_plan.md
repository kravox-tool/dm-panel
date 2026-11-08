# DM Panel - Kravox Tool Branding & Real Discord DM Delivery Plan

Update the application subtitle under the main ASCII banner to `=== KRAVOX TOOL ===` and audit/fortify all functions (DM-SPAM, CHAT, MASIVO, FLASH) to guarantee that entering any target User ID opens a real Discord DM channel and delivers direct messages via Discord's REST API.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed by the user during Phase 1 clarification:

- **Confirmed Decision 1 (Sub-Header Branding)**: The small text below the main ASCII `DM PANEL` banner will display the symmetrical cyber banner: `=== KRAVOX TOOL ===`.
- **Confirmed Decision 2 (Real Discord DM Delivery to User ID Across All Functions)**: Whenever a user inputs a Target User ID in DM-SPAM, CHAT, MASIVO, or FLASH:
  1. The bot invokes `POST https://discord.com/api/v10/users/@me/channels` with `recipient_id = <USER_ID>` to acquire the official 1-on-1 DM channel.
  2. The bot sends the payload directly to `POST https://discord.com/api/v10/channels/<CHANNEL_ID>/messages`.
  3. The real Discord message ID, channel ID, and HTTP 200 OK status are printed to the terminal console.

---

## 1. Overview & Core Concept

- **What It Does**: Enhances the visual branding with **KRAVOX TOOL** and guarantees that every operation targeting a Discord User ID performs genuine 1-on-1 direct message creation and delivery through Discord's REST API endpoints.
- **Target Audience**: Discord Bot developers requiring reliable, real-world DM testing and administration.
- **Key Value**: Distinctive branding and 100% verified real-world Discord Direct Message delivery.

---

## 2. User Experience & Visual Design

### Header Visual Hierarchy

```
  ██████╗ ███╗   ███╗    ██████╗  █████╗ ███╗   ██╗███████╗██╗
  ██╔══██╗████╗ ████║    ██╔══██╗██╔══██╗████╗  ██║██╔════╝██║
  ██║  ██║██╔████╔██║    ██████╔╝███████║██╔██╗ ██║█████╗  ██║
  ██║  ██║██║╚██╔╝██║    ██╔═══╝ ██╔══██║██║╚██╗██║██╔══╝  ██║
  ██████╔╝██║ ╚═╝ ██║    ██║     ██║  ██║██║ ╚████║███████╗███████╗
  ╚═════╝ ╚═╝     ╚═╝    ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═══╝╚══════╝╚══════╝
====================================================================
                        === KRAVOX TOOL ===
====================================================================
```

- **Header Text**: Clean red glowing ASCII block banner `DM PANEL` followed by `=== KRAVOX TOOL ===`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Universal DM Channel Resolution Strategy**
  - *Chosen Approach*: Standardize the two-step Discord REST API direct message flow across all four main features:
    1. Resolve/Open DM channel for target User ID (`POST /users/@me/channels`).
    2. Dispatch direct message payload (`POST /channels/{dmChannelId}/messages`).
  - *Why*: Guarantees that typing a 17-19 digit Discord User ID in any function (DM-SPAM, CHAT, MASIVO, FLASH) routes directly to that user's private DM inbox.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────┐
│                    User Enters Target User ID                 │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│              Step 1: POST /users/@me/channels                 │
│              Payload: {"recipient_id": "USER_ID"}             │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼ Returns DM Channel ID
┌───────────────────────────────────────────────────────────────┐
│            Step 2: POST /channels/{channel_id}/messages       │
│               Payload: {"content": "MESSAGE"}                 │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼ HTTP 200 OK
┌───────────────────────────────────────────────────────────────┐
│            Direct Message Delivered to Discord Inbox          │
│        Terminal Log: [HTTP 200 OK] DM entregado a @USER_ID    │
└───────────────────────────────────────────────────────────────┘
```

### Module Verification Matrix

| Function | User ID Target Handling | Discord REST API Endpoint |
|---|---|---|
| **DM-SPAM** | Recipient User ID | `POST /users/@me/channels` -> `POST /channels/{id}/messages` |
| **CHAT** | Recipient User ID | `POST /users/@me/channels` -> `POST /channels/{id}/messages` |
| **MASIVO** | Server Guild ID / User ID | `GET /guilds/{id}/channels` or Direct DM User Resolution |
| **FLASH** | Recipient User ID / Channel | `POST /users/@me/channels` -> `POST /messages` -> `DELETE /messages/{id}` |
