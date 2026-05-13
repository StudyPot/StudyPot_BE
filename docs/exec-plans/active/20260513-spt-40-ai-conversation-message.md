# EXEC_PLAN: [ai-chat] ai_conversation_message 저장/조회 구현

- Task slug: `spt-40-ai-conversation-message`
- Base branch: `develop`
- Feature branch: `codex/spt-40-ai-conversation-message`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-40-ai-conversation-message`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-40-ai-conversation-message`
- Jira issue: `SPT-40`
- Jira URL: https://studypot.atlassian.net/browse/SPT-40
- Jira summary: [ai-chat] ai_conversation_message 저장/조회 구현
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- SPT-40 Jira acceptance: `ai_conversation_message` 저장/조회, sender type `USER`/`ASSISTANT`/`SYSTEM`, 메시지 본문, metadata JSON, LLM usage 연결, 대화별 커서 조회.
- SPT-81 반영 메모: 실제 LLM 호출과 provider adapter는 별도 티켓에서 붙이며, 이 티켓은 메시지 저장/조회 기반을 만든다.
- `docs/specs/openapi.yaml`의 공개 REST 표면은 `POST /api/v1/ai-conversations/{conversationId}/messages`만 정의되어 있다. v1 API spec은 `LOCKED_FOR_IMPLEMENTATION` 상태이므로 새 공개 GET endpoint는 이 티켓에서 추가하지 않는다.
- `docs/specs/ai-contract-v1.md`는 chat 실패 시 사용자 메시지는 저장하고 assistant content를 지어내지 않는다고 정의한다. 따라서 SPT-40은 provider 응답을 만들지 않고 저장/조회 경계를 제공한다.
- `docs/specs/auth-permissions-v1.md`는 멤버가 자신의 retrospective/conversation records만 볼 수 있고 raw message가 다른 멤버에게 노출되면 안 된다고 정의한다.

## Goal
`ai_conversation_message` 도메인/저장소/서비스/API 기반을 추가해 인증된 대화 멤버가 메시지를 저장하고, 내부 서비스가 같은 대화의 메시지를 커서 방식으로 조회할 수 있게 한다. 실제 AI provider 호출, `llm_usage` 생성, assistant 응답 생성은 후속 SPT-42 범위로 남긴다.

## Approach
1. `AiConversationMessage`와 sender type enum을 추가해 `USER`, `ASSISTANT`, `SYSTEM` 메시지, metadata, 선택적 `llm_usage_id`, 생성 시각을 표현한다.
2. 기존 `AiConversationRepository`에 대화 접근 컨텍스트, 메시지 insert, 커서 조회 메서드를 확장한다.
3. `JdbcAiConversationRepository`에서 `ai_conversation_message` insert와 `(created_at, id)` 기반 커서 조회 SQL을 구현하고 metadata JSON은 Jackson으로 직렬화/역직렬화한다.
4. `AiConversationService`에 사용자 메시지 저장 use case와 대화 메시지 조회 use case를 추가한다. 저장은 대화가 존재하고 인증 사용자에게 속하며 `OPEN` 상태인지 확인한다.
5. 컨트롤러에는 잠긴 OpenAPI에 존재하는 `POST /api/v1/ai-conversations/{conversationId}/messages`만 추가하고, 응답은 저장된 `USER` 메시지를 반환한다.
6. 조회는 공개 REST를 늘리지 않고 서비스/저장소 테스트로 검증한다.

## Step Plan
1. 현재 SPT-39 AI conversation 코드와 유사 도메인/JSON 저장소 패턴을 확인한다.
2. 도메인/서비스/저장소/컨트롤러 테스트를 먼저 작성하고 실패를 확인한다.
3. 최소 구현으로 테스트를 통과시킨다.
4. targeted test를 통과시킨 뒤 `./gradlew check build --no-daemon`을 실행한다.
5. 커밋 후 `scripts/task/create-pr.sh`로 PR을 만들고 CodeRabbit review gate를 처리한다.

## Done Criteria
- `POST /api/v1/ai-conversations/{conversationId}/messages`가 인증된 대화 멤버의 `USER` 메시지를 `201`로 저장/반환한다.
- 대화가 없거나 다른 사용자의 대화이면 권한/존재 오류가 반환된다.
- `CLOSED` 대화에는 새 메시지를 저장할 수 없다.
- 빈 메시지 본문은 요청 검증에서 거부된다.
- repository/service 계층에서 대화별 커서 조회가 가능하고 `USER`/`ASSISTANT`/`SYSTEM`, metadata JSON, optional `llm_usage_id`가 보존된다.
- 실제 LLM 호출, provider adapter, `llm_usage` 생성, assistant 응답 생성은 구현하지 않고 후속 티켓 범위로 유지한다.
- 관련 테스트와 `./gradlew check build --no-daemon`이 통과한다.
- PR에는 Jira `SPT-40` 링크, 검증 결과, review gate 증거가 포함된다.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.ai.*' --no-daemon` failed at `compileTestJava` because the new message domain/service/repository types did not exist yet.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.ai.*' --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
