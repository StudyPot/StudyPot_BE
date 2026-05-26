# EXEC_PLAN: [fix] LLM 출력 예산 감사 payload redaction 보정

- Task slug: `spt-110-llm-budget-payload`
- Base branch: `develop`
- Feature branch: `codex/spt-110-llm-budget-payload`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-110-llm-budget-payload`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-110-llm-budget-payload`
- Jira issue: `SPT-110`
- Jira URL: https://studypot.atlassian.net/browse/SPT-110
- Jira summary: [fix] LLM 출력 예산 감사 payload redaction 보정
- Status: `local_verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] scripts/task/verify-ai-db-first-prod.sh
- [x] scripts/tests/test_ai_db_first_prod_verification_contracts.sh
- [x] src/main/java/com/studypot/aistudyleader/llm/domain/LlmUsage.java
- [x] src/main/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiLlmProvider.java
- [x] src/test/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiLlmProviderTest.java
- [x] src/test/java/com/studypot/aistudyleader/llm/domain/LlmUsageTest.java

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- SPT-109 배포 후 `scripts/task/verify-ai-db-first-prod.sh`가 실제 AI 호출과 DB-first context 검증은 통과했지만, `llm_usage.request_payload.maxOutputTokens` assertion에서 실패했다.
- 원인은 `LlmUsage`의 보안 redaction 정책이 key name에 `token`이 포함되면 값을 `[REDACTED]` 처리하기 때문이며, 이 정책은 민감정보 보호를 위해 유지해야 한다.
- 수정 범위는 provider 감사 metadata key와 이를 읽는 검증 스크립트/테스트로 제한한다. 실제 OpenAI 요청 body의 `max_output_tokens`/`max_completion_tokens`는 변경하지 않는다.

## Goal
- LLM 출력 예산이 감사 payload와 운영 검증에서 redaction 없이 확인되도록 key name을 보정한다.
- 보안 redaction 정책은 완화하지 않고, 실서버 golden path 검증이 출력 예산 evidence까지 통과하도록 한다.

## Approach
- TDD로 provider request payload가 `token` 단어를 포함하지 않는 출력 예산 metadata key를 사용하도록 먼저 회귀 테스트를 바꾼다.
- `OpenAiLlmProvider`의 감사 metadata key만 `maxOutputTokens`에서 `outputBudget`으로 변경한다.
- `verify-ai-db-first-prod.sh`와 정적 contract test가 `outputBudget`을 기준으로 예산 초과 여부를 검증하게 바꾼다.
- `LlmUsageTest`에 redaction 정책 문서화 테스트를 추가해 `maxOutputTokens`는 redaction 대상이고 `outputBudget`은 운영 metadata로 보존됨을 고정한다.

## Step Plan
1. Provider 테스트를 `outputBudget` 기대값으로 변경하고 RED 실패를 확인한다.
2. `LlmUsageTest`에 redaction 정책 보존 테스트를 추가한다.
3. `OpenAiLlmProvider` 감사 payload key를 `outputBudget`으로 변경한다.
4. 운영 검증 스크립트와 contract test를 `outputBudget` 기준으로 변경한다.
5. focused Gradle 테스트, shellcheck/contract test, `./gradlew check build --no-daemon`을 실행한다.
6. 커밋 후 `scripts/task/create-pr.sh`, CodeRabbit review, review gate, `scripts/task/finish-pr.sh`로 merge/cleanup까지 완료한다.
7. 배포 완료 후 `scripts/task/verify-ai-db-first-prod.sh`로 실서버 golden path와 output budget evidence를 재검증한다.

## Done Criteria
- `OpenAiLlmProviderTest`와 `LlmUsageTest`가 출력 예산 metadata/redaction 계약을 검증한다.
- `scripts/tests/test_ai_db_first_prod_verification_contracts.sh`가 최신 검증 스크립트 계약을 통과한다.
- `./gradlew check build --no-daemon`이 성공한다.
- PR review gate와 CodeRabbit marker를 통과한 뒤 develop에 merge된다.
- GitHub Actions 배포가 성공하고 `studypot-api`가 새 image digest/tag로 실행된다.
- `scripts/task/verify-ai-db-first-prod.sh`가 운영 `https://studypot.rumiclean.com` 대상으로 성공한다.

## Verification
- RED: `./gradlew test --tests OpenAiLlmProviderTest --tests LlmUsageTest --no-daemon` failed on missing `outputBudget` provider metadata.
- GREEN focused: `./gradlew test --tests OpenAiLlmProviderTest --tests LlmUsageTest --no-daemon`
- Harness contract: `bash scripts/tests/test_ai_db_first_prod_verification_contracts.sh`
- Shell: `shellcheck --external-sources scripts/task/verify-ai-db-first-prod.sh scripts/tests/test_ai_db_first_prod_verification_contracts.sh`
- Static harness: `bash scripts/tests/run.sh`
- Full backend: `./gradlew check build --no-daemon`
