# GitHub Actions Review Gate

이 문서는 CodeRabbit 대신 사용하는 무료 PR 품질 게이트 계약이다.

## 구성
- `.github/workflows/pr-quality.yml`
  - `harness-tests`: `bash scripts/tests/run.sh`
  - `shellcheck-reviewdog`: shell script ShellCheck feedback을 reviewdog으로 PR에 남김
  - `workflow-lint`: GitHub Actions workflow를 actionlint로 검사
  - `openapi-parse`: `docs/specs/openapi.yaml` 문법과 최소 필수 필드 확인
  - `backend-check`: Spring/Gradle scaffold가 생기면 `./gradlew check --no-daemon`
  - `review-gate-pass`: 선행 job 통과 후 PASS marker comment 작성
- `.github/workflows/codeql.yml`
  - Java/Kotlin source가 생기면 CodeQL 분석 실행
  - 구현 전 문서/하네스 상태에서는 pass 상태로 skip
- `.github/dependabot.yml`
  - GitHub Actions dependency update를 weekly로 확인

## Required Checks
`verify-pr-ready.sh`의 기본 required check 목록:

```text
harness-tests shellcheck-reviewdog workflow-lint openapi-parse backend-check codeql-scan review-gate-pass
```

모든 required check는 `SUCCESS`, `SKIPPED`, `NEUTRAL` 중 하나여야 한다.

## PASS Marker
`review-gate-pass` job은 PR comment에 아래 marker를 남긴다.

```text
GitHub Actions Review Gate: PASS
Head: <current_pr_head_sha>
```

`finish-pr.sh`는 이 marker가 최신 PR head SHA와 일치해야 merge를 진행한다.

## 실패 복구
- workflow 실패: GitHub Actions log에서 실패 job을 확인하고 코드/스크립트/문서를 수정한다.
- reviewdog comment: ShellCheck 또는 actionlint feedback을 반영하고 thread를 resolve한다.
- OpenAPI 실패: `docs/specs/openapi.yaml`이 YAML로 파싱되고 `openapi`, `info.title`, `paths`가 있는지 확인한다.
- backend-check 실패: scaffold 이후 `./gradlew` 실행 권한과 `./gradlew check --no-daemon` 결과를 확인한다.
- marker 누락: `review-gate-pass` job이 성공했는지 확인하고 PR head가 marker의 `Head`와 같은지 확인한다.

## Branch Policy
- 작업 PR target은 `develop`이다.
- `develop`에는 GitHub repository settings에서 PR merge, required status checks, conversation resolution을 활성화한다.
- `main`은 릴리즈/보존 브랜치로 두고 직접 구현 작업을 하지 않는다.
