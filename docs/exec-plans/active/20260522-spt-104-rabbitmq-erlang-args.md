# EXEC_PLAN: [infra] rumiclean RabbitMQ 부팅 인자 수정

- Task slug: `spt-104-rabbitmq-erlang-args`
- Base branch: `develop`
- Feature branch: `codex/spt-104-rabbitmq-erlang-args`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-104-rabbitmq-erlang-args`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-104-rabbitmq-erlang-args`
- Jira issue: `SPT-104`
- Jira URL: https://studypot.atlassian.net/browse/SPT-104
- Jira summary: [infra] rumiclean RabbitMQ 부팅 인자 수정
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/deployment.md
- [x] docs/operations/local-development.md
- [x] scripts/tests/test_rumiclean_migration_contracts.sh
- [x] deploy/rumiclean/docker-compose.yml
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md`: fixes must happen in a generated `codex/<slug>` worktree, with plan, tests, verification, PR, review gate, auto-merge, and cleanup.
- `ARCHITECTURE.md`: this is infrastructure/harness scope; no public API, DB schema, or product behavior contract changes.
- `docs/index.md`: infrastructure work uses `n/a-harness` and still needs the standard Gradle verification.
- `docs/operations/deployment.md`: rumiclean full-stack deployment owns the StudyPot RabbitMQ container and smoke requires `rabbitmq-diagnostics -q ping`.
- `docs/operations/local-development.md`: local RabbitMQ uses `rabbitmq:4-management` without custom `RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS`.
- `scripts/tests/test_rumiclean_migration_contracts.sh`: static contract test already asserts RabbitMQ service presence and smoke references; extend it to guard the RabbitMQ 4 boot-safe Erlang args.
- `deploy/rumiclean/docker-compose.yml`: current default `STUDYPOT_RABBITMQ_ERL_ARGS` expands to `-rabbit vm_memory_high_watermark.relative 0.4`, which failed on rumiclean RabbitMQ 4 startup.
- Runtime inspection: `studypot-rabbitmq` failed on rumiclean with `failed_to_prepare_configuration`; logs reported `syntax error before: '.': vm_memory_high_watermark.relative` and unknown config variables.

## Goal
Fix the rumiclean RabbitMQ deployment contract so `rabbitmq:4-management` starts cleanly on the real server and future deployments do not reintroduce the failing `-rabbit vm_memory_high_watermark.relative 0.4` default.

## Approach
- Treat the server failure as a deployment contract bug, not as a one-off server patch.
- Add a failing static test that rejects the known-bad `vm_memory_high_watermark.relative` default and requires a boot-safe `STUDYPOT_RABBITMQ_ERL_ARGS` default.
- Change the default `RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS` to a conservative Erlang scheduler setting that is valid for RabbitMQ 4 and small hosts.
- Keep RabbitMQ memory capping at the Docker `mem_limit` boundary for this task; do not add a new public route or broader broker tuning.
- Re-run static, compose config, and standard Gradle verification, then push through the normal PR/review-gate/finish flow.
- After merge, copy the corrected compose to rumiclean and restart RabbitMQ before continuing the full StudyPot migration.

## Progress Notes
- RED: `bash scripts/tests/test_rumiclean_migration_contracts.sh` failed because the compose did not contain the RabbitMQ 4-safe `STUDYPOT_RABBITMQ_ERL_ARGS:-+S 1:1` default.
- GREEN: replaced the RabbitMQ default with `+S 1:1 +sbwt none +sbwtdcpu none +sbwtdio none`, documented it, and added it to `.env.example`.
- Verification passed: `bash scripts/tests/test_rumiclean_migration_contracts.sh`, placeholder `docker compose config`, `bash scripts/tests/run.sh`, `git diff --check`, and `./gradlew check build --no-daemon`.

## Step Plan
1. Add a RED assertion to `scripts/tests/test_rumiclean_migration_contracts.sh` that fails while the compose still contains the RabbitMQ 4-incompatible `vm_memory_high_watermark.relative` default.
2. Update `deploy/rumiclean/docker-compose.yml` to use a valid default for `STUDYPOT_RABBITMQ_ERL_ARGS`.
3. Update `deploy/rumiclean/.env.example` and deployment docs so the operational override is explicit and no secret values are introduced.
4. Run `bash scripts/tests/test_rumiclean_migration_contracts.sh`, placeholder `docker compose config`, `bash scripts/tests/run.sh`, `./gradlew check build --no-daemon`, and `git diff --check`.
5. Commit, create PR, run CodeRabbit, address at most one actionable review loop, wait for review gate, and run `finish-pr.sh`.
6. Apply the merged compose to `/home/ec2-user/compose-studypot` and verify `studypot-rabbitmq` health on rumiclean.

## Done Criteria
- Static contract test fails on the known-bad RabbitMQ args and passes with the fixed compose.
- `deploy/rumiclean/docker-compose.yml` no longer defaults to `-rabbit vm_memory_high_watermark.relative 0.4`.
- Rumiclean compose config remains valid with placeholder env values.
- Standard repository verification passes.
- PR is merged through CodeRabbit and GitHub Actions Review Gate.
- `studypot-rabbitmq` on rumiclean reaches healthy and `rabbitmq-diagnostics -q ping` passes.
