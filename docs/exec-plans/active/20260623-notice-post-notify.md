# EXEC_PLAN: [feat] 공지(NOTICE) 게시물 작성 시 전체 멤버 알림

- Task slug: `notice-post-notify`
- Base branch: `develop`
- Feature branch: `codex/notice-post-notify`
- Jira issue: `SPT-150`
- Jira URL: https://studypot.atlassian.net/browse/SPT-150
- Jira summary: [feat] 공지(NOTICE) 게시물 작성 시 전체 멤버 알림
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/notification-contract (NotificationType/이벤트 발행 패턴)

## Related Feature IDs
- [x] notification
- [x] studygroup (board)

## Doc Notes
- 스키마/마이그레이션 변경 없음. notification 도메인 enum 1개 추가뿐. db-schema-coverage 영향 없음.
- 알림 발행 패턴: in-process(NotificationService)와 Queued(RabbitMQ) 양쪽을 반드시 구현(누락 시 prod noop 버그).

## Goal
NOTICE("공지") 보드에 글이 작성되면 작성자를 제외한 활성 멤버 전원에게 알림을 보낸다.

## Approach
- NotificationType.NOTICE_POSTED 추가.
- NotificationEventPublisher 에 publishNoticePosted(groupId, actorUserId, postId, title) 추가(인터페이스 + noop + NotificationService + QueuedNotificationEventPublisher 양쪽 구현). 수신자는 findActiveGroupRecipientUserIds 로 해석하고 actorUserId 는 제외. 멱등키 post+recipient.
- NotificationCommandFactory.noticePosted: 제목 "새 공지가 등록됐어요", 본문=글 제목, deepLink /groups/{groupId}/posts/{postId}, payload(groupId/postId/title).
- GroupBoardService: NotificationEventPublisher 주입(noop 기본), createPost 에서 board.boardType()==NOTICE 이면 publishNoticePosted 호출. 보드 설정에 ObjectProvider 주입.
- 테스트: 보드 서비스 NOTICE/비NOTICE 분기 검증 + 각 서비스 테스트 페이크에 신규 메서드 구현.

## Step Plan
1. enum/publisher/factory/service 배선.
2. GroupBoardService + 설정 주입.
3. 테스트(보드 분기 2종 + 페이크 5개 보강).
4. `./gradlew check build` 그린.

## Done Criteria
- NOTICE 보드 글 작성 시 작성자 제외 전원 알림, 다른 보드는 알림 없음.
- in-process/Queued 양쪽 구현, 전체 테스트 통과.
