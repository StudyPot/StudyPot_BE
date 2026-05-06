# Obsidian Error Ledger

Obsidian mirror는 구현 진실이 아니라 session continuity와 failure memory를 보존하는 공간이다. 실행 가능한 계약은 항상 repo docs와 scripts에 둔다.

Repo-tracked source of truth: [Error Ledger](./error-ledger.md)

## 위치
- Vault project: `/Users/hyunwoo/Library/Mobile Documents/iCloud~md~obsidian/Documents/Project/AI Study Leader`
- Current state: `05 Handoffs/Current State.md`
- Error ledger mirror: `04 Errors/Error Ledger.md`

## 기록 대상
실제 실패만 기록한다. 형식 채우기를 위해 가짜 오류를 만들지 않는다.

기록 대상 예시:

- local verification과 CI 결과가 다름
- Docker/package/deploy 실패
- PR review gate blocker
- unresolved review thread
- hook failure
- worktree cleanup failure
- submodule initialization failure
- 외부 dependency outage가 CI 또는 검증에 영향을 줌

## 항목 형식
각 항목은 다음 내용을 포함한다.

- Date
- Work / feature id
- Symptom
- Cause
- Fix
- Prevent next time
- 다음부터의 체크포인트

## 업데이트 순서
1. repo docs와 코드에 실행 가능한 수정 사항을 먼저 반영한다.
2. 검증 결과를 확보한다.
3. 실제 실패가 있었다면 repo [Error Ledger](./error-ledger.md)에 원인과 재발 방지 체크포인트를 남긴다.
4. Obsidian이 응답 가능하면 `04 Errors/Error Ledger.md`에 repo ledger 내용을 mirror한다.
5. `05 Handoffs/Current State.md`에 현재 branch/worktree/다음 작업을 갱신한다.

## iCloud 주의
- Obsidian vault가 iCloud Drive 아래에 있어 shell read가 멈출 수 있다.
- iCloud 파일 접근이 멈추면 repo ledger를 먼저 갱신하고, Obsidian mirror는 나중에 동기화한다.
- iCloud read hang 자체도 실제 실패로 간주해 repo Error Ledger에 기록한다.
