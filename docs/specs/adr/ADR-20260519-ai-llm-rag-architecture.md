# ADR-20260519 AI LLM RAG Architecture

## Status
- Proposed

## Context
- SPT-81 approved the MVP retrospective/chat boundary as a deterministic DB-first context builder before LLM provider calls.
- Current implementation already uses Spring Boot service ports around LLM work:
  - `LlmProviderClient`
  - `ProviderBackedCurriculumGenerator`
  - `ProviderBackedRetrospectiveFeedbackGenerator`
  - `ProviderBackedAiConversationAssistantResponseGenerator`
  - OpenAI Responses adapter under `curriculum.infrastructure.openai`
- Current StudyPot AI context is mostly structured and permission-sensitive: group membership, onboarding, curriculum weeks, weekly tasks, member progress, task completions, incomplete reasons, group rules, rule violations, prior retrospectives, conversation summaries, and `llm_usage`.
- MySQL remains the durable source of truth for AI outputs and audit state. Redis is only short-lived protection state, and RabbitMQ is only async dispatch state.
- The open technology questions are whether to split a FastAPI AI service now, when to introduce vector or graph retrieval, and whether agent frameworks or MCP should be part of the MVP path.

## Decision
- Keep the MVP default as a single Spring Boot backend:
  - `ContextBuilder -> LlmProviderClient -> ProviderAdapter`
  - store durable outcomes and `llm_usage` audit in MySQL
  - keep provider credentials server-side only
  - keep frontend/BFF boundary at StudyPot REST APIs and cookie session auth
- Do not split a FastAPI AI service for simple provider calls, structured output parsing, or DB-first context assembly.
- Do not introduce a vector store, GraphRAG, MCP server, or agent orchestration framework until a concrete follow-up task proves that the current DB-first boundary is insufficient.
- Keep OpenAI Responses API usage behind the existing provider port. Use structured outputs for deterministic JSON contracts, and evaluate built-in tools such as file search only when StudyPot has indexed non-DB study materials.
- Treat Spring AI as a future Java-native RAG candidate if the team wants framework-provided advisors/vector-store integrations without leaving the Spring Boot process.
- Treat FastAPI as a later service-split candidate only when Python-native ingestion/retrieval/orchestration materially outweighs the added network, deployment, auth, and observability cost.
- Treat streaming as a UX enhancement, not an architecture prerequisite. Adding SSE/WebSocket endpoints changes the API surface and needs a separate approved task.

## Comparison Matrix
| Option | Best Fit | StudyPot Default | Adoption Trigger | Main Cost |
| --- | --- | --- | --- | --- |
| Spring Boot internal DB-first context builder | Structured, permission-sensitive StudyPot state | Use now | Current retrospective/chat/curriculum flows | Context builder quality must be tested in backend |
| Spring Boot + OpenAI Responses structured outputs | Provider call with JSON output contracts | Use now | Existing `LlmProviderClient` port and OpenAI adapter | Provider-specific behavior stays behind adapter |
| Spring AI RAG advisors/vector store | Java-native RAG inside Spring | Watch | Need vector retrieval while staying single-process | New framework dependency and adapter migration |
| Hosted file search/vector store | Uploaded docs/links that need managed retrieval | Defer | Group study materials, official docs, or resource libraries become indexed sources | Provider lock-in, data residency/cost review |
| Hybrid/vector RAG | Unstructured documents with metadata filters | Defer | SQL context cannot answer from documents/links and there is a labeled retrieval eval set | Ingestion, chunking, embedding, reranking, evals |
| GraphRAG/DRIFT | Broad corpus questions needing global themes plus local evidence | Defer | Large cross-document knowledge base exists and simple semantic search misses relationship-heavy answers | Graph build/refresh cost and operational complexity |
| Agentic workflow framework | Multi-step tool use, handoff, human approval loops | Defer | AI must call multiple internal tools with approval and resumable workflow state | Trace/debug surface and permission risk |
| MCP | Standardized external tool/resource integration across AI hosts | Defer | Same StudyPot tool/resource must be exposed to multiple AI clients or vendors | Overkill for internal repository queries |
| FastAPI AI service | Python-native retrieval/indexing/eval/orchestration | Defer | Separate indexing pipeline, Python ML stack, or worker scaling becomes concrete | Extra service, auth boundary, deployment, tracing |
| Ragas/OpenAI Evals/golden set | Quality regression and retrieval grounding checks | Add before RAG production | 20+ representative prompts/expected sources exist | Dataset maintenance and CI cost |

## RAG Before/Later/Do Not
| Timing | Scope |
| --- | --- |
| Do now | DB-first context builders, source metadata in `llm_usage.request_payload`, redaction, structured outputs, failure audit, small golden prompt set for curriculum/retrospective/chat. |
| Do next when data exists | Hybrid retrieval over official docs, group-uploaded materials, resource links, and prior curriculum examples; metadata filters by group/topic/week/visibility. |
| Do later only with evidence | GraphRAG/DRIFT, agentic multi-tool workflows, MCP, separate FastAPI indexing/retrieval service, streaming endpoint. |
| Do not do for MVP | Expose provider keys to frontend, store final AI state in Redis/RabbitMQ/vector DB, bypass StudyPot permission checks through an external AI service, or let an agent autonomously mutate curriculum/todo state without host/member-visible audit. |

## FastAPI Service Split Criteria
FastAPI becomes a serious candidate only if at least two of the following are true:
- StudyPot needs Python-only libraries for parsing, embedding, reranking, graph construction, or evaluation.
- Retrieval/indexing needs independent scaling or background deployment separate from the Spring Boot API.
- The service owns a bounded async pipeline with durable handoff back to MySQL, not direct user-visible state.
- Observability can correlate request IDs across Spring Boot, RabbitMQ, FastAPI, provider traces, and `llm_usage`.
- Auth can be enforced server-to-server without weakening the existing cookie/session and group membership boundary.
- Deployment capacity on `oracle-was` or a new host has been measured and rollback behavior is documented.

## BFF and UX Boundary
- Frontend must never hold provider API keys or call the provider directly.
- Spring Boot remains the BFF/API boundary for cookie session auth, group permissions, rate limits, idempotency, `llm_usage`, and redaction.
- Chat streaming can be proposed later as SSE/WebSocket, but the initial response path may remain request/response JSON.
- If streaming is added, the event contract, cancellation behavior, partial failure logging, and OpenAPI/QA updates need a new approved implementation task.

## Evaluation Boundary
- Before vector or agentic RAG becomes production behavior, create a small golden set per purpose:
  - `CURRICULUM_GENERATE`: onboarding summary -> expected week/task shape checks.
  - `RETROSPECTIVE_FEEDBACK`: progress/incomplete reason -> expected context-source coverage and no private-note leakage.
  - `TEAM_LEAD_CHAT`: user message + week context -> expected grounded answer and conversation summary patch.
- Minimum automated checks:
  - required source keys are present in the built context
  - disallowed private fields are absent
  - output JSON validates against the locked schema
  - failure creates or records `llm_usage` without fabricated assistant content
- Ragas-style metrics become useful after there is retrieval over documents or a labeled retrieved-context set. OpenAI Evals can be used for provider-hosted eval runs once prompt/output criteria are stable.

## Sources
- OpenAI Responses file search and vector stores: https://platform.openai.com/docs/guides/tools-file-search/
- OpenAI Evals API: https://platform.openai.com/docs/api-reference/evals
- OpenAI Agents SDK tracing: https://openai.github.io/openai-agents-js/guides/tracing/
- Spring AI RAG reference: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- MCP architecture: https://modelcontextprotocol.io/docs/learn/architecture
- Microsoft DRIFT/GraphRAG research note: https://www.microsoft.com/en-us/research/blog/introducing-drift-search-combining-global-and-local-search-methods-to-improve-quality-and-efficiency/
- Haystack advanced RAG techniques: https://docs.haystack.deepset.ai/docs/advanced-rag-techniques
- LlamaIndex human-in-the-loop workflows: https://docs.llamaindex.ai/en/stable/understanding/agent/human_in_the_loop/
- FastAPI features: https://fastapi.tiangolo.com/features/
- Ragas metrics: https://docs.ragas.io/en/latest/concepts/metrics/available_metrics/

## Consequences
- Positive: MVP AI work can continue in the existing Spring Boot permission, transaction, and audit boundary.
- Positive: Future FastAPI/RAG work has explicit triggers instead of becoming hidden scope inside retrospective/chat tasks.
- Positive: Locked v1 API, DB, permission, notification, and AI JSON contracts stay unchanged.
- Negative: Advanced document retrieval remains deferred until StudyPot has real indexed materials and eval data.
- Negative: Python-first RAG experiments need a later task before they can affect runtime behavior.
- Migration or compatibility notes: No migration required. This ADR adds planning guidance only and does not change OpenAPI, DB schema, enums, permission rules, notification behavior, or AI output schemas.

## Affected Feature IDs
- `ai-team-leader`
- `retrospective-feedback`
- `curriculum-core`
- `n/a-harness`

## Affected Documents
- `docs/specs/ai-contract-v1.md`
- `docs/architecture/backend-map.md`
- `docs/confluence/00-doc-hub.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/06-ai-team-leader.md`
- `docs/confluence/10-jira-mapping.md`

## Linked Change Request
- `n/a` - no locked v1 product/API/DB/AI schema change is introduced. If a later task changes runtime contracts, create a Change Request and a new approved ADR.
