# 02 요구사항

## 문서 상태
- Jira: `SPT-4`
- Lock status: `LOCKED_FOR_IMPLEMENTATION`
- Source of truth: `docs/specs/requirements-v1.md`
- Supporting sources: `docs/specs/prd-v1.md`, `docs/specs/user-journeys-v1.md`, `docs/specs/notification-contract-v1.md`
- Approved changes: `CR-20260430-onboarding-mysql8-mvp`, `CR-20260504-no-discord-inapp-notification`, `CR-20260506-auth-api-entrypoints`, `CR-20260520-onboarding-simplification-auto-merge`
- 변경 규칙: 제품 범위, API, DB, AI, 알림, 권한, QA 의미 변경은 Change Request + ADR 필요

## MVP 골든패스
`그룹 생성 -> 초대 링크 공유 -> 멤버별 온보딩 -> 호스트 시작 -> AI 커리큘럼 생성 -> 주차 todo 수행 -> AI 팀장 회고/피드백 -> 다음 주 조정 -> 인앱 알림`

## P0 기능 요구사항
| Feature ID | Req ID | 요구사항 | 수용 기준 |
| --- | --- | --- | --- |
| `identity-core` | `REQ-ID-001` | 사용자는 인증하고 앱 프로필 데이터를 가진다. | Google OAuth code exchange가 `users`, `oauth_account`, application token record를 생성/갱신한다. |
| `identity-core` | `REQ-ID-002` | 사용자는 애플리케이션 세션을 갱신하고 폐기할 수 있다. | `refresh_token`은 해시 토큰, 만료, 폐기 상태를 저장하고 rotation 및 logout/logout-all revocation을 지원한다. |
| `study-group-core` | `REQ-GRP-001` | 호스트는 스터디 그룹을 만들 수 있다. | 필수 입력은 이름, 주제, 상세 키워드, 최대 인원, 시작/종료일이다. |
| `study-group-core` | `REQ-GRP-002` | 생성된 그룹은 온보딩 플로우에 들어간다. | `study_group.status = ONBOARDING`, owner member는 `PENDING_ONBOARDING`이다. |
| `study-group-core` | `REQ-INV-001` | 호스트는 초대 링크/코드를 공유할 수 있다. | 초대 코드는 유니크하고, 참여 시 pending member record를 만든다. |
| `group-onboarding` | `REQ-ONB-001` | 호스트와 멤버는 그룹별 온보딩을 제출한다. | 공개 응답은 전체 실력, 노트, 가능 시간, 제출 시각을 저장한다. 호스트 제출은 owner membership이 active가 된 뒤 그룹을 `READY_TO_START`로 전환한다. 백엔드는 커리큘럼 컨텍스트용 내부 키워드 점수로 투영할 수 있다. |
| `group-onboarding` | `REQ-ONB-002` | 온보딩은 반복 가능 시간대를 저장한다. | 슬롯은 요일, 시작 시각, 종료 시각, timezone을 포함한다. |
| `group-onboarding` | `REQ-ONB-003` | 온보딩 실력 점수는 1-5 범위를 사용한다. | 범위를 벗어난 값은 거절된다. |
| `curriculum-core` | `REQ-CUR-001` | 호스트는 호스트 온보딩이 완료된 뒤 스터디를 시작할 수 있다. | 시작은 `study_group.status = READY_TO_START`를 요구하며, 모든 멤버가 온보딩을 완료하지 않아도 가능하다. |
| `curriculum-core` | `REQ-CUR-002` | AI 커리큘럼은 제출된 온보딩 응답을 사용한다. | `curriculum.onboarding_summary`에 생성 컨텍스트를 저장하고, 생성 주차 수는 스터디 기간에서 산출한 1주 단위 스프린트 수와 일치해야 한다. |
| `weekly-todo` | `REQ-TODO-001` | 커리큘럼 주차는 weekly task를 가진다. | task는 type, order, title, required flag, sprint window 기반 due timestamp를 가진다. |
| `weekly-todo` | `REQ-TODO-002` | 멤버는 마감 전 todo를 완료할 수 있다. | 완료 시각과 노트가 저장된다. |
| `weekly-todo` | `REQ-TODO-003` | 멤버는 마감 후 미완료 사유를 제출해야 한다. | 미완료 사유와 제출 시각이 저장된다. |
| `retrospective-feedback` | `REQ-RETRO-001` | 주차 진행 현황에서 회고가 생성된다. | 트리거는 주차 종료, 미완료 모달, 사용자 채팅, 수동 요청이 될 수 있다. |
| `retrospective-feedback` | `REQ-RETRO-002` | AI 피드백은 다음 주 조정을 제안할 수 있다. | 피드백과 조정안은 JSON으로 저장된다. |
| `ai-team-leader` | `REQ-AI-001` | AI는 상세 키워드를 제안할 수 있다. | 사용자가 선택하지 않은 제안은 후보로 저장하지 않는다. |
| `ai-team-leader` | `REQ-AI-002` | AI 채팅은 메시지와 요약을 저장한다. | 메시지는 conversation 및 LLM usage와 연결된다. |
| `notification` | `REQ-NOTI-001` | 시스템은 온보딩, weekly todo, 미완료 사유, 회고, AI 피드백 이벤트에 대해 인앱 알림을 만든다. | 알림은 recipient, title/body, payload, related resource IDs, idempotency key, status, delivered timestamp, read timestamp를 저장한다. |
| `notification` | `REQ-NOTI-002` | 멤버는 자기 알림을 조회하고 읽음 처리할 수 있다. | recipient user 기준으로 unread/read 상태 조회가 가능하다. |

## P1 요구사항
| Feature ID | Req ID | 요구사항 |
| --- | --- | --- |
| `study-group-rules` | `REQ-RULE-001` | 그룹 규칙은 과제 마감과 회고 정책을 정의할 수 있다. |
| `notification` | `REQ-NOTI-003` | 인앱 알림이 안정화된 뒤 외부 전달 채널을 추가할 수 있다. |
| `ai-team-leader` | `REQ-AI-003` | LLM 사용량은 그룹, 사용자, 목적, UTC date 기준으로 집계할 수 있다. |

## P2 / 제외 범위
| 영역 | 제외 이유 |
| --- | --- |
| Live meeting assistant | MVP 범위 밖이며 실시간 UX/latency 계약이 필요하다. |
| Voice transcription | MVP 범위 밖이며 별도 개인정보/저장 정책이 필요하다. |
| Heavy synchronous meeting automation | MVP는 비동기 온보딩/todo/피드백 중심이다. |
| Automatic full curriculum regeneration for late joiners | 늦게 합류한 멤버의 컨텍스트는 향후 주차 조정에만 반영한다. |
| Discord integration and bot delivery | MVP 범위 밖이며 첫 알림 표면은 서비스 내부 `IN_APP` 알림이다. |

## 주요 수용 기준
- 그룹은 `ONBOARDING` 상태로 생성되고 호스트 온보딩 완료 후 `READY_TO_START` 상태에서 시작을 기다리며, 초대 코드/링크를 통해 멤버가 합류한다.
- 호스트와 멤버는 그룹별 온보딩을 제출하며, 전체 실력 점수 입력은 1-5 범위로 검증한다.
- 호스트 시작은 모든 초대자의 온보딩 완료를 요구하지 않는다.
- 초기 커리큘럼 생성은 시작 시점까지 제출된 온보딩 응답만 사용한다.
- weekly todo는 완료, 스킵, 미완료 및 미완료 사유 제출을 감사 가능하게 저장한다.
- 현재 sprint unit은 1주 고정이며, 사용자 선택형 sprint 기간은 후속 Change Request로 다룬다.
- AI 팀장은 회고와 다음 주 조정안을 저장하고 LLM usage와 연결한다.
- MVP 알림 채널은 `IN_APP`이며, Discord/Email/Push/Kakao는 추후 CR/ADR 이후 확장한다.

## 추적 링크
- Repo requirements: `docs/specs/requirements-v1.md`
- PRD: `docs/specs/prd-v1.md`
- User journeys: `docs/specs/user-journeys-v1.md`
- Notification contract: `docs/specs/notification-contract-v1.md`
- Change control: `docs/specs/change-control-v1.md`
- Jira: `SPT-4`
