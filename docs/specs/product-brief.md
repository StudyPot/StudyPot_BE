# AI Study Leader Product Brief

## Provenance
- Captured from the Claude share discussion provided by the user:
  - `https://claude.ai/share/8872bb8c-2666-4000-9c22-fa369d279f50`
- This document preserves the product and MVP decisions so future Codex sessions can recover the original project context.
- If later source artifacts conflict with this summary, update this file and mirror the change into Obsidian.

## One-Line Definition
AI Study Leader is an AI-assisted study team leader that helps small study groups prepare for meetings, run recurring study routines, and receive post-meeting feedback through a web backend and Discord notifications.

## Target Users
- Bootcamp and SSAFY-style learners who study in short project or algorithm groups.
- Side-study groups with 2 to 3 active participants.
- Users who need a lightweight leader role but do not want one member to manually manage reminders, meeting prep, and feedback every time.

## MVP Scope
- Web-based study group management.
- Discord-based notifications and reminders.
- Structured study meeting records.
- AI-generated pre-meeting preparation support.
- AI-generated post-meeting feedback.
- Group rules and study preferences stored in flexible structured fields.
- Backend specification targets Java 21, Gradle, Spring Boot, REST `/api/v1`, OpenAPI 3.1, PostgreSQL, UUIDv7, and JSONB.

## Explicit Non-Scope For MVP
- Real-time STT.
- Voice meeting transcription.
- Real-time voice assistant behavior.
- Heavy synchronous meeting automation.
- Discord slash commands and Discord-first UX.
- Billing, uploaded files, and rule-version history.

## Product Direction
- The AI should act like a study leader, not just a passive summarizer.
- The first version should focus on asynchronous and structured inputs rather than live audio.
- Discord is treated as an important notification surface, while the backend remains the system of record.
- Meeting notes become the main input for reflection, accountability, and next-step suggestions.

## Core Workflow
1. A user creates or joins a study group.
2. The group defines study goals, rules, schedule, and operating preferences.
3. The system sends Discord reminders before a study session.
4. The AI prepares agenda or focus prompts based on group context and prior records.
5. Members record structured notes, progress, blockers, and decisions after the meeting.
6. The AI produces feedback, follow-up items, and next-session preparation material.

## Locked V1 Decisions
- Frontend is a separate client that consumes the backend REST API. This repository owns backend contracts only.
- Backend implementation baseline is Java 21 + Gradle + Spring Boot.
- AI provider and model are configurable, but AI output JSON schemas are fixed by `docs/specs/ai-contract-v1.md`.
- Discord MVP is notification-only:
  - session reminder
  - preparation brief ready
  - feedback ready
  - action item due
- V1 planning changes require the process in `docs/specs/change-control-v1.md`.
