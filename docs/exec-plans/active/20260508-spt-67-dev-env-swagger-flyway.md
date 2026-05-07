# EXEC_PLAN: [dev-env] Swagger/Flyway/local 실행 검증 하네스 구축

- Task slug: `spt-67-dev-env-swagger-flyway`
- Base branch: `develop`
- Feature branch: `codex/spt-67-dev-env-swagger-flyway`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-67-dev-env-swagger-flyway`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-67-dev-env-swagger-flyway`
- Jira issue: `SPT-67`
- Jira URL: https://studypot.atlassian.net/browse/SPT-67
- Jira summary: [dev-env] Swagger/Flyway/local 실행 검증 하네스 구축
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/operations/error-ledger.md
- [x] docs/operations/local-development.md
- [x] Spring Boot reference: starters and database initialization/Flyway
- [x] springdoc-openapi v4 documentation for Spring Boot 4 compatibility

## Related Feature IDs
- [x] n/a-harness
- [x] identity-core

## Doc Notes
- This task is development-environment and harness infrastructure; it must not change locked product API/DB semantics.
- Spring Boot 4 splits Flyway auto-configuration into the Flyway starter/module. Current runtime classpath has `flyway-core` and `flyway-mysql`, but no Spring Boot Flyway auto-configuration, so migrations can be present but not run on startup.
- springdoc-openapi 3.x is the Spring Boot 4 compatible line; use WebMVC UI integration and keep runtime documentation scoped to implemented `/api/v1/**` controllers.
- Swagger/OpenAPI, actuator health details, Flyway health, and DB table checks are local development evidence. Secrets remain in ignored `config/application-local.yml` or environment variables.
- User decision: request explicitly approved adding missing harness coverage for Swagger/local DB/browser verification.
- MySQL rejected V2 unique-index drops because two unique indexes were still supporting foreign keys; adding replacement non-unique FK indexes before the drop keeps the live-row uniqueness change valid.
- Auth session persistence was not wired in local startup because component-scan bean conditions evaluated before Boot JDBC auto-configuration exposed the `JdbcTemplate`; property-based configuration keeps no-DB tests isolated while wiring repositories when a datasource URL exists.
- Swagger/OpenAPI public routes must cover HEAD as well as GET so browser/proxy probes and `curl -I` do not falsely report 401.
- Copilot review feedback on PR 47 required tightening public diagnostics/Swagger routes to GET+HEAD only and documenting the local DB target as MySQL 8.0+ instead of generic MySQL-compatible.
- Follow-up Copilot feedback required disabling Swagger/OpenAPI by default, exposing it only when `studypot.openapi.public-docs-enabled` and springdoc enable flags are explicit, and moving local secrets out of `src/main/resources` to avoid packaging them into local build artifacts.

## Goal
Make the backend behave like a real local development service: startup should run Flyway against MySQL, Swagger UI should be accessible in a browser, local YAML should expose enough logging/health detail to debug DB/API wiring, and the harness should have reusable checks that prevent this class of "app starts but DB/docs are not actually ready" failure from returning.

## Approach
1. Add failing regression coverage first for missing Spring Boot Flyway auto-configuration, public Swagger/API docs access, and local-dev harness script contracts.
2. Add Spring Boot 4 Flyway starter dependency while keeping MySQL-specific Flyway support.
3. Add springdoc WebMVC UI integration and a small OpenAPI configuration bean for API title/security metadata without changing the locked external API contract.
4. Update security rules so health, Swagger UI, and API docs are reachable without bearer auth while `/api/v1/**` remains protected except public auth endpoints.
5. Expand safe default and local-example YAML for logging, actuator, DB/Flyway, and Swagger UI settings. Keep real local credentials in ignored `config/application-local.yml`.
6. Add a reusable local verification script that checks MySQL connectivity, creates the local DB when allowed, starts the app, verifies `/actuator/health`, `/v3/api-docs`, Swagger UI, Flyway history, expected table count, and auth-service wiring.
7. Run focused tests, full Gradle verification, a real local MySQL/Flyway smoke test, and browser verification. The in-app browser timed out on localhost navigation, so final visual proof uses the installed Google Chrome binary in headless screenshot mode.

## Step Plan
- [x] RED: add tests for Flyway auto-config dependency, Swagger/OpenAPI security access, and local-dev harness coverage; run focused tests and confirm expected failure.
- [x] GREEN: update Gradle dependencies, OpenAPI config, security matchers, YAML defaults/examples, and local verification script.
- [x] Run focused tests for persistence/config/security/harness contracts.
- [x] Prepare ignored worktree `application-local.yml` from local secrets without printing secrets.
- [x] Start or provision a local MySQL endpoint, run the app with `local` profile, confirm Flyway creates `flyway_schema_history` and ERD tables.
- [x] Verify `/actuator/health`, `/v3/api-docs`, `/swagger-ui.html`, and auth endpoint behavior by HTTP and browser.
- [x] Run `./gradlew check build --no-daemon` and record verification evidence.

## Verification Evidence
- `./gradlew test --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest.localDiagnosticsAndOpenApiDocsArePublic' --no-daemon` -> PASS.
- `./gradlew test --tests 'com.studypot.aistudyleader.global.security.SecurityConfigurationTest' --no-daemon` -> PASS after Copilot feedback; verifies read-only public diagnostics and denied non-read diagnostic methods.
- `./gradlew test --tests 'com.studypot.aistudyleader.global.security.*' --no-daemon` -> PASS after follow-up Copilot feedback; verifies Swagger/OpenAPI routes are denied when public docs are disabled.
- `bash scripts/tests/test_local_dev_verification_contracts.sh` -> PASS after moving local config to ignored `config/application-local.yml`.
- `STUDYPOT_LOCAL_DB_PORT=13306 STUDYPOT_LOCAL_DB_PASSWORD='' STUDYPOT_LOCAL_PORT=18080 scripts/task/verify-local-dev.sh` -> PASS; confirmed health, OpenAPI, Swagger UI, Flyway history, ERD tables, and auth wiring.
- Google Chrome headless screenshot rendered Swagger UI at `http://localhost:18080/swagger-ui.html`; captured title area shows `AI Study Leader API`, generated server URL, authorize button, auth-controller, and schemas.
- `bash scripts/tests/run.sh` -> PASS.
- `./gradlew check build --no-daemon` -> PASS.

## Done Criteria
- Spring Boot runtime classpath contains Flyway auto-configuration and local startup executes migrations.
- Swagger UI and `/v3/api-docs` are accessible locally without weakening authenticated `/api/v1/**` protection.
- Local YAML/example covers DB, Flyway, actuator, logging, and Swagger settings without committed secrets.
- Harness includes reusable local-dev verification for DB migration/table creation and Swagger/browser-facing readiness.
- Real local smoke test proves MySQL is reachable, migrations ran, expected tables exist, health is UP, and Swagger renders in browser.
- `./gradlew check build --no-daemon` passes.
