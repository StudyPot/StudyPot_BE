# EXEC_PLAN: [ci] 배포 헬스체크 대기 보강

- Task slug: `spt-89-deploy-health-wait`
- Base branch: `develop`
- Feature branch: `codex/spt-89-deploy-health-wait`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-89-deploy-health-wait`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-89-deploy-health-wait`
- Jira issue: `SPT-89`
- Jira URL: https://studypot.atlassian.net/browse/SPT-89
- Jira summary: [infra] Deploy workflow health check 대기 보강
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] .github/workflows/deploy.yml
- [x] scripts/tests/test_deployment_contracts.sh
- [x] scripts/tests/run.sh
- [x] docs/operations/deployment.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `Deploy` workflow의 merged commit 실행에서 build, GHCR push, remote pull/up은 성공했다.
- `Verify deployed health` 단계는 compose 직후 `studypot-api`가 아직 `health: starting`인 상태에서 단발 curl을 실행해 `curl: (56) Recv failure: Connection reset by peer`로 실패했다.
- 같은 배포 컨테이너는 이후 약 1분 부팅 후 `curl -fsS http://127.0.0.1:8080/actuator/health`에 `UP`을 반환했다.
- 현재 WAS 자원 조건에서는 health endpoint가 뜰 때까지 workflow가 제한된 시간 동안 기다리는 계약이 필요하다.

## Goal
`develop` 배포 workflow가 `docker compose up -d --force-recreate` 직후 느린 Spring Boot 부팅을 정상적으로 기다린 뒤 `/actuator/health` 성공 여부를 판정하도록 한다.

## Approach
- `Verify deployed health` 단계를 remote bash heredoc으로 바꾸고, `/actuator/health`를 최대 24회, 5초 간격으로 재시도한다.
- 각 시도마다 compose 상태와 Docker health/status를 출력해 CI 로그에서 대기 원인을 볼 수 있게 한다.
- 컨테이너가 `unhealthy`가 되거나 제한 시간을 넘기면 최근 앱 로그를 출력하고 실패한다.
- 배포 계약 테스트와 운영 문서에 health 대기 계약을 반영한다.

## Step Plan
1. EXEC_PLAN에 읽은 문서, 목표, 접근, 완료 기준을 기록한다.
2. `.github/workflows/deploy.yml`의 health 검증 단계를 재시도형으로 보강한다.
3. `scripts/tests/test_deployment_contracts.sh`에 retry/wait 계약 검사를 추가한다.
4. `docs/operations/deployment.md`에 느린 부팅 대기와 실패 시 로그 출력 기준을 기록한다.
5. `bash scripts/tests/test_deployment_contracts.sh`, `bash scripts/tests/run.sh`, YAML 파싱, `./gradlew check build --no-daemon`로 검증한다.
6. 커밋, PR 생성, CodeRabbit review gate, `finish-pr`까지 진행한다.
7. human merge 후 Deploy workflow와 oracle-was health를 다시 확인한다.

## Done Criteria
- `Verify deployed health` 단계가 단발 curl이 아니라 제한된 재시도 loop로 health를 확인한다.
- health 대기 중 compose 상태와 container health/status가 CI 로그에 남는다.
- timeout 또는 `unhealthy` 시 앱 로그 tail이 남고 workflow가 실패한다.
- 배포 계약 테스트가 새 health wait 계약을 검증한다.
- `./gradlew check build --no-daemon`이 성공한다.
- PR review gate가 latest head 기준으로 통과한다.
- merge 후 `Deploy` workflow가 green이고 `oracle-was` 내부 `/actuator/health`가 `UP`이다.

## Verification Evidence
- `bash scripts/tests/test_deployment_contracts.sh` 성공.
- `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy.yml"); puts "deploy workflow yaml ok"'` 성공.
- `bash scripts/tests/run.sh` 성공.
- `git diff --check` 성공.
- `docker compose -f deploy/docker-compose.prod.yml config` 성공. Placeholder env 사용.
- `./gradlew check build --no-daemon` 성공.
