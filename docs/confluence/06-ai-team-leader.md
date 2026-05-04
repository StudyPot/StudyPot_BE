# 06 AI 팀장 명세

## AI Purposes
- `DETAIL_KEYWORD_SUGGEST`
- `CURRICULUM_GENERATE`
- `RETROSPECTIVE_FEEDBACK`
- `TEAM_LEAD_CHAT`

## Rules
- Detail keyword suggestions are not stored unless selected.
- Curriculum uses onboarding responses submitted at host start.
- Retrospective uses weekly todo status, incomplete reasons, onboarding summary, and chat context.
- AI team leader proposes next-week adjustments every week for difficulty, task split, support material, and coaching notes.
- Late joiner onboarding can affect future adjustments, not automatic full curriculum regeneration.
- All AI calls create `llm_usage`.
- Raw secrets and tokens are never stored in AI request logs.

## Source
- `docs/specs/ai-contract-v1.md`
