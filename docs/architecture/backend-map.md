# Architecture Map

## Project
- Name: `AI Study Leader`
- Repo: `/Users/hyunwoo/Documents/New project 3`
- Stack: `Java 21 + Gradle + Spring Boot`

## Map
- Root package: `com.studypot.aistudyleader`
- Shared kernel:
  - `shared.domain`: framework-free DDD base types such as `AggregateRoot` and `DomainEvent`.
  - `shared.application`: application-layer contracts such as `UseCase`.
- Global infrastructure:
  - `global.api`: API path and cursor pagination response contracts.
  - `global.error`: RFC 7807 `ProblemDetail` factory and validation exception handling.
  - `global.security`: stateless Spring Security scaffold with ProblemDetail authentication/authorization failures.
- Bounded contexts:
  - `identity`
  - `studygroup`
  - `onboarding`
  - `curriculum`
  - `weeklytodo`
  - `retrospective`
  - `ai`
  - `notification`
- Meeting/session specific packages are post-MVP unless a new Change Request and ADR reintroduce them.
- Each bounded context is prepared with:
  - `domain`: entities, value objects, domain services, domain events. No Spring or adapter dependencies.
  - `application`: use cases and ports. No adapter dependencies.
  - `adapter.in.web`: REST controllers and request/response DTO mapping.
  - `adapter.out.persistence`: persistence adapters and database mapping.

## Guardrails
- Prefer existing patterns over new abstractions.
- Keep tests close to changed behavior.
- Record cross-module decisions in `EXEC_PLAN -> Doc Notes`.
- `LayeredArchitectureTest` enforces domain/application dependency direction and production controller package placement.
