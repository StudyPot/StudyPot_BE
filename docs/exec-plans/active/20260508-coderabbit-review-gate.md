# EXEC_PLAN: [fix] CodeRabbit 리뷰 게이트로 PR 완료 흐름 전환

- Task slug: `coderabbit-review-gate`
- Base branch: `develop`
- Feature branch: `codex/coderabbit-review-gate`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/coderabbit-review-gate`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/coderabbit-review-gate`
- Jira issue: `SPT-71`
- Jira URL: https://studypot.atlassian.net/browse/SPT-71
- Jira summary: [harness] CodeRabbit 리뷰 게이트로 PR 완료 흐름 전환
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [ ] docs/operations/jira-board-sync.md
- [ ] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- 현재 `create-pr.sh`는 Copilot reviewer를 기본 요청하고 PR 체크리스트도 Copilot review 및 CTO/QA/Product/Final CTO role gate marker를 요구한다.
- 현재 CTO/QA/Product/Final CTO gate는 실제 subagent 실행이 아니라 PR comment marker(`... Gate: PASS`, `Head: <sha>`, `## 증거`)를 검증하는 CI/스크립트 계약이다.
- `review-gate-pass` GitHub Actions job은 실제 리뷰 주체가 아니라 최신 head marker를 확인하고 GitHub merge button을 막는 기계적 verifier 역할이다.
- 사용자 요청은 Copilot 기본 리뷰와 role gate 기본 경로를 제거하고, CodeRabbit agent-mode 리뷰를 한 번 실행한 뒤 0건이면 PASS, 지적사항이 있으면 한 번 수정 후 ADDRESSED 증거만으로 PR merge 준비가 되게 만드는 것이다.

## Goal
- PR 완료 하네스의 기본 리뷰 계약을 Copilot/role gate에서 CodeRabbit subagent 리뷰 1회 계약으로 전환한다.
- CI는 CodeRabbit 리뷰를 수행했다고 주장하지 않고, 최신 head에 CodeRabbit PASS 또는 ADDRESSED marker가 있는지만 검증한다.
- CodeRabbit CLI 실패, 미설치, 인증 실패, 네트워크 실패는 수동 리뷰로 대체하지 않고 명시적으로 실패하게 만든다.

## Approach
- `scripts/task/run-coderabbit-review.sh`를 추가해 `coderabbit review --agent` 결과를 NDJSON으로 저장하고 PR comment에 `CodeRabbit Subagent Review: PASS` 또는 `NEEDS_FIX` marker를 남긴다.
- `scripts/task/post-coderabbit-addressed.sh`를 추가해 CodeRabbit 지적사항을 한 번 수정한 증거를 `CodeRabbit Subagent Review: ADDRESSED` marker로 남긴다.
- `scripts/task/verify-coderabbit-review.sh`를 추가해 최신 PR head에 PASS 또는 ADDRESSED marker와 필수 증거가 있는지 검증한다.
- `create-pr.sh`, `verify-pr-ready.sh`, `finish-pr.sh`, `.github/workflows/pr-quality.yml`의 기본 경로를 CodeRabbit marker 검증으로 변경하고 Copilot 기본 요청 및 role gate 기본 요구를 제거한다.
- 문서와 harness static tests를 새 계약에 맞춰 갱신한다.

## Step Plan
1. [x] CodeRabbit review marker 검증 테스트를 먼저 작성하고 실패를 확인한다.
2. [x] CodeRabbit review 실행/수정완료/검증 스크립트를 구현한다.
3. [x] PR 생성, 준비 검증, 완료 알림, GitHub Actions review gate를 새 스크립트로 연결한다.
4. [x] AGENTS 및 operation/testing 문서를 새 기본 계약으로 업데이트한다.
5. [x] Harness tests와 Gradle verification을 실행하고 결과를 task state에 기록한다.

## Done Criteria
- [x] `bash scripts/tests/test_coderabbit_review_gate.sh`가 PASS한다.
- [x] `bash scripts/tests/test_pr_scripts_static.sh`가 PASS한다.
- [x] `bash scripts/tests/run.sh`가 PASS한다.
- [x] `./gradlew check build --no-daemon`가 PASS하거나, scaffold 부재 등 실제 blocker를 기록한다.
- [x] PR body/checklist와 docs에서 Copilot 기본 리뷰 및 role gate 기본 요구가 CodeRabbit subagent review 계약으로 대체되어 있다.

## Verification Notes
- RED 확인: `bash scripts/tests/test_coderabbit_review_gate.sh` failed because `scripts/task/verify-coderabbit-review.sh` did not exist.
- PASS: `bash scripts/tests/test_coderabbit_review_gate.sh`.
- PASS: `bash scripts/tests/test_pr_scripts_static.sh`.
- PASS: `bash scripts/tests/run.sh`.
- PASS: `git diff --check`.
- PASS: `./gradlew check build --no-daemon`.
- CodeRabbit review on PR #58 reported 94 issues because the first helper run did not pass the PR base branch and CodeRabbit reviewed against `main`. Follow-up RED: `bash scripts/tests/test_coderabbit_review_gate.sh` failed until the helper passed `--base develop`.
