SELECT '=== groups ===' AS v;
SELECT name, status, starts_at, ends_at, max_members FROM study_group
 WHERE id IN (UNHEX('01910000000070008000000000000010'), UNHEX('01920000000070008000000000000010'));
SELECT '=== NodeJS counts (members/weeks/taskComps/posts/retros/convs) ===' AS v;
SELECT
 (SELECT COUNT(*) FROM group_member WHERE group_id=UNHEX('01910000000070008000000000000010')) members,
 (SELECT COUNT(*) FROM curriculum_week cw JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id=UNHEX('01910000000070008000000000000010')) weeks,
 (SELECT COUNT(*) FROM task_completion tc JOIN member_week_progress p ON p.id=tc.progress_id JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id=UNHEX('01910000000070008000000000000010')) task_comps,
 (SELECT COUNT(*) FROM group_board_post WHERE group_id=UNHEX('01910000000070008000000000000010')) posts,
 (SELECT COUNT(*) FROM retrospective r JOIN member_week_progress p ON p.id=r.progress_id JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id=UNHEX('01910000000070008000000000000010')) retros,
 (SELECT COUNT(*) FROM ai_conversation WHERE group_id=UNHEX('01910000000070008000000000000010')) convs;
SELECT '=== NodeJS 4주차 TODO 상태 (절반 진행) ===' AS v;
SELECT gm.display_name, SUM(tc.status='DONE') AS done, SUM(tc.status='TODO') AS todo
 FROM task_completion tc JOIN weekly_task wt ON wt.id=tc.weekly_task_id JOIN group_member gm ON gm.id=tc.member_id
 WHERE wt.curriculum_week_id=UNHEX('01910000000070008000000000000024') GROUP BY gm.display_name;
SELECT '=== Python (recs/reviews/weeksCompleted) ===' AS v;
SELECT
 (SELECT COUNT(*) FROM study_recommendation WHERE group_id=UNHEX('01920000000070008000000000000010')) recs,
 (SELECT COUNT(*) FROM group_review WHERE group_id=UNHEX('01920000000070008000000000000010')) reviews,
 (SELECT COUNT(*) FROM member_week_progress p JOIN curriculum_week cw ON cw.id=p.curriculum_week_id JOIN curriculum c ON c.id=cw.curriculum_id WHERE c.group_id=UNHEX('01920000000070008000000000000010') AND p.status='COMPLETED') weeks_completed;
SELECT '=== 회고 JSON 키 확인 ===' AS v;
SELECT BIN_TO_UUID(member_id) AS mid, JSON_KEYS(ai_feedback) AS feedback_keys, JSON_KEYS(next_week_adjustment) AS adjust_keys
 FROM retrospective WHERE curriculum_week_id=UNHEX('01910000000070008000000000000023');
