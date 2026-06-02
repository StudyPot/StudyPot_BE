# 01 MVP 골든패스

## Flow
1. Host creates study group.
2. System stores topic and final detail keywords.
3. System creates owner member as `PENDING_ONBOARDING`.
4. Host shares invite link.
5. Host and members submit onboarding.
6. Owner onboarding completion moves the group to `READY_TO_START`.
7. Host starts study.
8. AI creates curriculum from submitted onboarding responses.
9. Members execute weekly todos.
10. Incomplete work requires reason capture.
11. AI team leader provides retrospective feedback and next-week adjustment.
12. In-app notifications keep members aware of onboarding, due dates, incomplete reasons, and feedback readiness.

## Key Policies
- Host can start before every invitee completes onboarding.
- Initial curriculum uses only submitted onboarding responses at start time.
- Late joiners join from current week after onboarding.
- AI team leader adjusts future weeks as a recurring operator.
- Discord integration is not P0.
- Meetings are not P0.
