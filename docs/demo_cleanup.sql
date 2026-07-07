-- ================================================================
-- StudyPot 시연 더미 데이터 정리 (NodeJS + Python + 더미유저 김개발)
-- 두 그룹 ID에만 스코프 → 실데이터 안전. FK 자식→부모 순서.
-- ================================================================
SET @ng = UNHEX('01910000000070008000000000000010'); -- NodeJS
SET @pg = UNHEX('01920000000070008000000000000010'); -- Python
SET @u2 = UNHEX('01910000000070008000000000000002'); -- 김개발(더미)
SET @u3 = UNHEX('01910000000070008000000000000003'); -- 이서연(더미)

DELETE FROM notification WHERE group_id IN (@ng,@pg);
DELETE acm FROM ai_conversation_message acm JOIN ai_conversation ac ON ac.id=acm.conversation_id WHERE ac.group_id IN (@ng,@pg);
DELETE FROM ai_conversation WHERE group_id IN (@ng,@pg);
DELETE r FROM retrospective r JOIN member_week_progress p ON p.id=r.progress_id JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id IN (@ng,@pg);
DELETE tc FROM task_completion tc JOIN member_week_progress p ON p.id=tc.progress_id JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id IN (@ng,@pg);
DELETE p FROM member_week_progress p JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id IN (@ng,@pg);
DELETE wt FROM weekly_task wt JOIN curriculum_week cw ON cw.id=wt.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id IN (@ng,@pg);
DELETE cw FROM curriculum_week cw JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id IN (@ng,@pg);
DELETE FROM curriculum WHERE group_id IN (@ng,@pg);
DELETE FROM group_board_comment WHERE group_id IN (@ng,@pg);
DELETE FROM group_board_post WHERE group_id IN (@ng,@pg);
DELETE FROM group_board WHERE group_id IN (@ng,@pg);
DELETE FROM group_review WHERE group_id IN (@ng,@pg);
DELETE FROM study_recommendation WHERE group_id IN (@ng,@pg);
DELETE mas FROM member_availability_slot mas JOIN group_onboarding_response o ON o.id=mas.onboarding_response_id WHERE o.group_id IN (@ng,@pg);
DELETE FROM group_onboarding_response WHERE group_id IN (@ng,@pg);
DELETE FROM group_member WHERE group_id IN (@ng,@pg);
DELETE FROM llm_usage WHERE group_id IN (@ng,@pg);
DELETE FROM study_group WHERE id IN (@ng,@pg);
DELETE FROM users WHERE id IN (@u2,@u3);
