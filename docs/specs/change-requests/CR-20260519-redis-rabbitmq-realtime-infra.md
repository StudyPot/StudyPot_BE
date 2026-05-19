# CR-20260519-redis-rabbitmq-realtime-infra

## Status
- Approved

## Request
- Clarify the runtime role split between MySQL, Redis, and RabbitMQ after SPT-78 and SPT-84.
- Keep MySQL as the durable source of truth for user, AI, curriculum, retrospective, conversation, notification, idempotency, retry, read-state, and audit records.
- Use Redis only for short-lived protection state such as rate limit counters and duplicate generation locks.
- Use RabbitMQ only for asynchronous dispatch, worker isolation, retry handoff, and broker dead-letter boundaries.
- Keep the current production deployment contract API-only on `oracle-was` and MySQL-only on `oracle-db`; Redis/RabbitMQ production activation requires a later deployment task.
- Keep the current RabbitMQ listener inside the Spring Boot application process until a later task explicitly splits an API container from a worker container.

## Reason
- SPT-78 added Redis rate limiting and future duplicate lock policy.
- SPT-84 added RabbitMQ notification queue and worker infrastructure behind disabled-by-default configuration.
- Without an explicit ADR, later realtime tasks can blur responsibilities between Redis Pub/Sub, Redis Streams, RabbitMQ, Kafka, a same-process listener, and a separate worker service.
- The current `oracle-was` deployment is memory-constrained and only runs `studypot-api`, so enabling Redis/RabbitMQ in production must not be implied by code-level support alone.

## Affected Feature IDs
- `n/a-harness`
- `identity-core`
- `curriculum-core`
- `retrospective-feedback`
- `ai-team-leader`
- `notification`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/notification-contract-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/architecture/backend-map.md`
- `docs/operations/local-development.md`
- `docs/operations/deployment.md`
- `docs/confluence/00-doc-hub.md`
- `docs/confluence/04-erd-data-model.md`
- `docs/confluence/06-ai-team-leader.md`

## Impact
- Product: No new user-visible workflow, MVP scope, notification type, or realtime UX is added by this change.
- API: No endpoint, path, request field, response field, auth behavior, or OpenAPI schema changes.
- DB: No table, column, enum, constraint, index, or migration changes. Durable state remains MySQL-owned.
- AI: Redis may protect costly LLM paths and RabbitMQ may dispatch async LLM jobs later, but AI result persistence and `llm_usage` audit stay in MySQL.
- Notification: RabbitMQ may dispatch in-app notification materialization/retry jobs, but the MVP channel remains `IN_APP` and notification idempotency, retry count, failure redaction, read state, and related resource links stay in MySQL.
- Permissions: No permission matrix change. Worker actions must execute through existing service boundaries and preserve member/owner visibility rules.
- QA: Static documentation checks must verify the CR/ADR links and production activation boundary. Future production activation tasks need runtime health and rollback checks for Redis/RabbitMQ.

## Compatibility
- Backward compatible: yes
- Migration required: no

## Decision
- Approved by: Product owner direction in Codex session to proceed with SPT-87 after the SPT-84 merge and Redis/RabbitMQ server architecture discussion.
- Date: 2026-05-19
- Linked ADR: [ADR-20260519-redis-rabbitmq-realtime-infra](../adr/ADR-20260519-redis-rabbitmq-realtime-infra.md)
