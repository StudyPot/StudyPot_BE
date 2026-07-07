-- ================================================================
-- StudyPot 시연 교정분 F — AI 팀장 리포트 작성자명 정상화
--  ① Python 중복 수료 리포트(잘못된 제목) 소프트 삭제 → 앱 생성 '수료 리포트' 유지
--  ② 모든 LEADER_REPORT 글의 작성자 표시명을 'AI 팀장'으로 (author_display_name_override)
--     ※ 코드 버그(WeeklyReportScheduler가 override 없이 게시) 데모용 데이터 우회.
-- ================================================================
SET @ng = UNHEX('01910000000070008000000000000010');
SET @pg = UNHEX('01920000000070008000000000000010');

-- ① 잘못된 제목의 정적 수료 리포트 삭제(앱 생성 '수료 리포트'만 유지)
UPDATE group_board_post SET deleted_at = NOW(6)
WHERE id = UNHEX('01920000000070008000000000000046');

-- ② AI 팀장 리포트 작성자 표시명 덮어쓰기
UPDATE group_board_post p
JOIN group_board b ON b.id = p.board_id
SET p.author_display_name_override = 'AI 팀장'
WHERE p.group_id IN (@ng, @pg)
  AND b.board_type = 'LEADER_REPORT'
  AND p.deleted_at IS NULL;
