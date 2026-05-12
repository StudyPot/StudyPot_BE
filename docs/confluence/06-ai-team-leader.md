# 06 AI 팀장 명세

## AI Purposes
- `DETAIL_KEYWORD_SUGGEST`
- `CURRICULUM_GENERATE`
- `RETROSPECTIVE_FEEDBACK`
- `TEAM_LEAD_CHAT`

## Rules
- Detail keyword suggestions are not stored unless selected.
- Curriculum uses onboarding responses submitted at host start.
- Retrospective uses a DB-first context builder before the LLM provider call.
- Retrospective context includes onboarding summary, current week, weekly tasks, member progress, task completions, incomplete reasons, relevant group rules/violations, prior feedback/adjustment, and conversation summary.
- `retrospective` remains the final feedback/adjustment result; `ai_conversation` is the chat/input interface and may link to a retrospective.
- AI team leader proposes next-week adjustments every week for difficulty, task split, support material, and coaching notes.
- Late joiner onboarding can affect future adjustments, not automatic full curriculum regeneration.
- All AI calls create `llm_usage`.
- `llm_usage.request_payload` stores redacted request/source metadata for audit. Raw secrets, OAuth tokens, provider credentials, and disallowed private raw notes are never stored in AI request logs.
- Vector store, GraphRAG, MCP, FastAPI service split, and broader agent orchestration are deferred to SPT-82 or later approved tasks.

## Source
- `docs/specs/ai-contract-v1.md`
- `docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md`
- `docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md`
