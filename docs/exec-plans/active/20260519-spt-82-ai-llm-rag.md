# EXEC_PLAN: [planning] AI LLM 아키텍처와 RAG 대안 기술 리서치

- Task slug: `spt-82-ai-llm-rag`
- Base branch: `develop`
- Feature branch: `codex/spt-82-ai-llm-rag`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-82-ai-llm-rag`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-82-ai-llm-rag`
- Jira issue: `SPT-82`
- Jira URL: https://studypot.atlassian.net/browse/SPT-82
- Jira summary: [planning] AI LLM 아키텍처와 RAG 대안 기술 리서치
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-82 issue description
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md
- [x] docs/architecture/backend-map.md
- [x] docs/confluence/00-doc-hub.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/06-ai-team-leader.md
- [x] docs/confluence/10-jira-mapping.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/change-request-template.md
- [x] scripts/tests/test_docs_source_of_truth.sh
- [x] Current AI implementation: `src/main/java/com/studypot/aistudyleader/{ai,curriculum,llm,retrospective}`
- [x] OpenAI Responses/file search/structured outputs/tools/Agents SDK docs
- [x] Spring AI RAG/Advisors docs
- [x] MCP architecture docs
- [x] Microsoft GraphRAG/DRIFT docs
- [x] Haystack Advanced RAG docs
- [x] LlamaIndex Workflows/HITL docs
- [x] FastAPI features docs
- [x] Ragas/OpenAI Evals docs

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] curriculum-core
- [x] n/a-harness

## Doc Notes
- SPT-82 is a planning/research task, not an API/DB/AI JSON schema implementation task.
- Existing approved boundary: MVP retrospective/chat retrieval uses a deterministic DB-first context builder; vector store, GraphRAG, MCP, FastAPI split, and broader agent orchestration were explicitly deferred to SPT-82 or later approved tasks.
- Current code already has a Spring Boot internal `LlmProviderClient` port and OpenAI Responses adapter path used by curriculum, retrospective feedback, and AI conversation generators.
- MySQL remains the durable source of truth for AI outputs, conversation records, retrospective state, curriculum state, and `llm_usage`; Redis/RabbitMQ are infrastructure boundaries, not durable AI state stores.
- SPT-82 should produce a documented default conclusion and transition criteria without changing locked OpenAPI, DB schema, enum, permission, notification, or AI JSON output shapes.

## Goal
Record the StudyPot-specific AI/LLM architecture and RAG alternative decision for work after SPT-81: keep MVP AI in the Spring Boot backend with DB-first context building by default, define when FastAPI/vector/graph/agent/MCP approaches become justified, and leave clear follow-up implementation boundaries.

## Approach
- Create a Proposed ADR for the SPT-82 technology decision instead of changing locked v1 contracts.
- Compare Spring Boot internal provider adapters, FastAPI service split, DB-first retrieval, vector/hybrid RAG, GraphRAG/DRIFT, agentic workflows, MCP/tool calling, streaming/BFF boundaries, and eval/observability against StudyPot's current data, permission, deployment, and audit constraints.
- Update existing architecture/AI docs only with clarifying links and deferred-decision notes; do not add API paths, DB tables/columns, enum values, permission actions, or AI schema fields.
- Add source links to official or primary docs where the research depends on current technology behavior.

## Step Plan
1. Write the SPT-82 Proposed ADR with decision, comparison matrix, transition triggers, and non-goals.
2. Add a concise architecture-map note and AI-contract reference pointing to the ADR while preserving locked behavior.
3. If needed, update Confluence AI draft references so SPT-81/38-46 handoff readers find the decision.
4. Run doc source-of-truth checks and standard verification.
5. Create PR, request CodeRabbit review, and finish the review gate.

## Done Criteria
- FastAPI split default is clearly decided with concrete transition conditions.
- DB-first context builder, vector/hybrid RAG, GraphRAG/DRIFT, agentic workflow, MCP/tool calling, structured output, streaming/BFF, and eval/observability are compared in StudyPot context.
- The result does not conflict with SPT-81/retrospective RAG boundary.
- No immediate OpenAPI, DB, enum, permission, notification, or AI schema change is introduced.
- Docs checks and `./gradlew check build --no-daemon` pass.
- CodeRabbit review gate is latest-head PASS or evidence-backed ADDRESSED before manual merge notification.

## Verification Log
- 2026-05-19 KST: `scripts/tests/test_docs_source_of_truth.sh` passed.
- 2026-05-19 KST: `git diff --check` passed.
- 2026-05-19 KST: `./gradlew check build --no-daemon` passed.
