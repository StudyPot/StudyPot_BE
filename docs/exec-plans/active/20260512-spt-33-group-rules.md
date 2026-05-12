# EXEC_PLAN: [study-group-rules] group_rule/rule_violation 구현

- Task slug: `spt-33-group-rules`
- Base branch: `develop`
- Feature branch: `codex/spt-33-group-rules`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-33-group-rules`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-33-group-rules`
- Jira issue: `SPT-33`
- Jira URL: https://studypot.atlassian.net/browse/SPT-33
- Jira summary: [study-group-rules] group_rule/rule_violation 구현
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/architecture/backend-map.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] Jira SPT-33
- [x] Jira SPT-13

## Related Feature IDs
- [x] study-group-rules
- [x] weekly-todo

## Doc Notes
- SPT-33 goal is `group_rule` and `rule_violation` implementation. Jira acceptance requires task deadline rules, retrospective-required rules, custom operation notes, violation recording, and resolved/waived handling.
- SPT-13 says `study-group-rules` stores group-level task deadline, incomplete-reason, retrospective-required, and custom operation memo policies; violations are tracked from weekly todo and retrospective flows.
- `requirements-v1.md` keeps `study-group-rules` as P1, but this Jira task is the current implementation source for SPT-33.
- `db-schema-v1.sql` already contains `group_rule` and `rule_violation`; no schema change is planned.
- `api-contract-v1.md` and `openapi.yaml` do not define rule endpoints. To satisfy SPT-33 and Swagger manual verification without editing locked specs, this PR adds Spring controller endpoints only and records the additive API decision here:
  - `PUT /api/v1/groups/{groupId}/rules/{ruleType}` saves or replaces one live rule for the type.
  - `GET /api/v1/groups/{groupId}/rules` lists group rules.
  - `PATCH /api/v1/groups/{groupId}/rules/{ruleId}/deactivate` marks a rule inactive.
  - `DELETE /api/v1/groups/{groupId}/rules/{ruleId}` soft-deletes a rule.
  - `POST /api/v1/groups/{groupId}/rule-violations` records an `OPEN` violation.
  - `GET /api/v1/groups/{groupId}/rule-violations` lists violations.
  - `PATCH /api/v1/groups/{groupId}/rule-violations/{violationId}/resolve` resolves an open violation.
  - `PATCH /api/v1/groups/{groupId}/rule-violations/{violationId}/waive` waives an open violation.
- Access policy for this slice follows the explicit goal wording: authenticated current group members can access rule/violation resources; `LEFT` members are rejected for new violation handling. Owner-only policy can be tightened later if product decides it.
- AI retrospective generation, feedback generation, next-week adjustment, notification workers, external channels, frontend UI, and weekly todo completion/progress are out of scope for SPT-33.

## Goal
멤버가 그룹 운영 규칙을 저장/조회/비활성화/삭제하고, 운영 규칙 위반을 기록한 뒤 `OPEN -> RESOLVED` 또는 `OPEN -> WAIVED`로 처리할 수 있는 backend API, domain, repository, service, controller, tests를 만든다.

## Approach
1. `com.studypot.aistudyleader.studygroup.rules` 하위에 `domain`, `service`, `repository`, `controller` 패키지를 만든다.
2. 도메인은 `GroupRule`, `GroupRuleType`, `RuleViolation`, `RuleViolationStatus`, `RuleViolationType`, `GroupRuleMembership` 중심으로 만든다.
3. 서비스는 인증 사용자 membership을 확인하고 group/rule/member/resource 소속을 검증한다. `LEFT` 멤버는 새 violation 기록/처리를 할 수 없게 막는다.
4. repository는 기존 JDBC 패턴을 따른다. JSON 필드는 `ObjectMapper`로 직렬화/역직렬화하고 UUID는 `UuidBinary`를 사용한다.
5. controller는 기존 `StudyGroupController`/`CurriculumController`의 인증 subject 추출, `ObjectProvider`, validation 패턴을 따른다.
6. 테스트는 RED부터 작성한다. 최소 단위는 service, repository SQL/매핑, controller API이며 happy path, edge case, validation, permission, state transition 실패를 포함한다.

## Step Plan
1. RED: service test로 rule save/list/deactivate/delete, violation record/resolve/waive, non-member/LEFT/status transition rejection을 먼저 작성하고 실패를 확인한다.
2. RED: repository test로 SQL 필터, UUID/JSON 매핑, update 조건, soft delete, violation status update argument를 검증하고 실패를 확인한다.
3. RED: controller test로 인증 필요, request validation, rule 저장/조회, violation 기록/해결/면제 응답을 검증하고 실패를 확인한다.
4. GREEN: domain records/enums/exceptions/commands/results를 최소 구현한다.
5. GREEN: `GroupRuleService`, application/persistence configuration, repository contract/JDBC implementation을 구현한다.
6. GREEN: `GroupRuleController`와 `ApiExceptionHandler` 매핑을 구현한다.
7. REFACTOR: 중복 인증 helper나 test fixture는 과도하게 추상화하지 않고 지역적으로 정리한다.
8. Verify: targeted tests를 통과시킨 뒤 `./gradlew check build --no-daemon`를 실행한다.
9. Commit/PR: `[feat] 그룹 운영 규칙과 위반 기록 구현` 커밋 후 `scripts/task/create-pr.sh`, `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`, review gate 절차를 따른다.

## Done Criteria
- `group_rule`을 `TASK_DEADLINE`, `RETROSPECTIVE_REQUIRED`, `CUSTOM_NOTE` 타입으로 저장할 수 있다.
- 저장된 group rules를 조회할 수 있다.
- group rule을 inactive 처리하거나 soft delete 처리할 수 있다.
- `rule_violation`을 `OPEN` 상태로 기록할 수 있고 details에 `INCOMPLETE_REASON_MISSING`, `RETROSPECTIVE_MISSING`, `CUSTOM` 위반 유형을 담을 수 있다.
- `rule_violation`을 `RESOLVED` 또는 `WAIVED`로 처리할 수 있고 `resolved_at`, `resolved_note`를 저장한다.
- 인증 없는 API 호출은 거부된다.
- 그룹 멤버가 아닌 사용자는 group rule/rule violation 리소스에 접근할 수 없다.
- `LEFT` 멤버는 새 violation 기록/해결/면제 처리를 할 수 없다.
- 이미 `RESOLVED` 또는 `WAIVED` 된 violation은 다시 처리할 수 없다.
- repository SQL/매핑 테스트, controller API 테스트, service 권한/상태 테스트가 있다.
- 최종 `./gradlew check build --no-daemon`가 통과한다.
- PR은 develop 대상이며 CodeRabbit agent review와 review gate 절차를 완료한다.

## Verification Evidence
- [x] RED: `./gradlew test --tests com.studypot.aistudyleader.studygroup.rules.service.GroupRuleServiceTest --no-daemon` failed first because group rule production classes did not exist.
- [x] RED: `./gradlew test --tests com.studypot.aistudyleader.studygroup.rules.repository.JdbcGroupRuleRepositoryTest --no-daemon` failed first because JDBC repository classes did not exist.
- [x] RED: `./gradlew test --tests com.studypot.aistudyleader.studygroup.rules.controller.GroupRuleControllerTest --no-daemon` failed first with 404 for missing rule endpoints.
- [x] Targeted: `./gradlew test --tests com.studypot.aistudyleader.studygroup.rules.service.GroupRuleServiceTest --tests com.studypot.aistudyleader.studygroup.rules.repository.JdbcGroupRuleRepositoryTest --tests com.studypot.aistudyleader.studygroup.rules.controller.GroupRuleControllerTest --no-daemon` passed.
- [x] Full: `./gradlew check build --no-daemon` passed on 2026-05-12.
