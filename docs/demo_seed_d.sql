-- ================================================================
-- StudyPot 시연 교정분 D — 과거 주차(1~3) 회고/리포트 정합성
--  ① 잘못된 제목의 정적 3주차 리포트(0098) 삭제 (앱 생성본 019eff62 유지)
--  ② 1·2주차 리포트 직접 추가 (정확한 제목 'N주차 학습 리포트')
--  ③ 1·2주차 회고를 전 멤버에 채움(제출 불가) — 누락분만
--     (이서연 3주차는 미완 2/4 유지 → 회고 잠김, 앱 3주차 리포트와 일관)
-- ================================================================
SET @ng = UNHEX('01910000000070008000000000000010');
SET @bL = UNHEX('01910000000070008000000000000095'); -- NodeJS LEADER_REPORT 게시판
SET @nw1=UNHEX('01910000000070008000000000000021'); SET @nw2=UNHEX('01910000000070008000000000000022'); SET @nw3=UNHEX('01910000000070008000000000000023');
SET @nm1=UNHEX('01910000000070008000000000000011'); -- 양지훈(owner)
SET @nm2=UNHEX('01910000000070008000000000000012'); -- 김개발
SET @nm3=UNHEX('01910000000070008000000000000016'); -- 이서연
-- member_week_progress
SET @p11=UNHEX('01910000000070008000000000000051'); SET @p12=UNHEX('01910000000070008000000000000052');
SET @p21=UNHEX('01910000000070008000000000000055'); SET @p22=UNHEX('01910000000070008000000000000056');
SET @s1=UNHEX('01910000000070008000000000000059'); SET @s2=UNHEX('0191000000007000800000000000005a'); SET @s3=UNHEX('0191000000007000800000000000005b');

-- ① 잘못된 제목의 정적 3주차 리포트 삭제(소프트) — 앱 생성본만 남김
UPDATE group_board_post SET deleted_at = NOW(6) WHERE id = UNHEX('01910000000070008000000000000098');

-- ② 1·2주차 리포트 직접 추가 (정확한 제목 → 앱 멱등키와 일치, 중복 생성 방지)
INSERT INTO group_board_post (id, group_id, board_id, author_member_id, title, content, is_pinned, status, created_at, updated_at)
VALUES
 (UNHEX('019100000000700080000000000000f8'), @ng, @bL, @nm1, '1주차 학습 리포트',
  '# 1주차 학습 리포트 — Node.js 환경 설정 & 기초\n\n## 1) 참여 현황\n- 참여율 **4명 / 4명**, 평균 완료율 **100%** (양지훈·김개발·오현우·이서연 모두 4/4)\n\n## 2) 이번 주 요약\n- Node.js 설치·REPL·HTTP 서버·npm 기초까지 전원 완수\n- 첫 주부터 실습 위주로 잘 따라옴\n\n## 3) 회고 기반 관찰\n- 전반적으로 환경 설정에서 막힘 없이 진행\n- npm 의존성 개념은 다음 주 모듈 시스템과 이어서 보강 예정\n\n## 4) 다음 주 제안\n1. CommonJS vs ESM 차이를 코드로 비교\n2. 내장 모듈(fs)부터 손에 익히기',
  0, 'PUBLISHED', '2026-06-08 09:00:00.000000', '2026-06-08 09:00:00.000000'),
 (UNHEX('019100000000700080000000000000f9'), @ng, @bL, @nm1, '2주차 학습 리포트',
  '# 2주차 학습 리포트 — Node.js 모듈 시스템\n\n## 1) 참여 현황\n- 참여율 **4명 / 4명**, 평균 완료율 **100%** (전원 4/4)\n\n## 2) 이번 주 요약\n- CommonJS/ESM, npm 패키지 관리, 내장 모듈(fs)·axios 실습까지 완수\n- nodemon 적용으로 개발 편의 개선\n\n## 3) 회고 기반 관찰\n- 모듈 import/export는 안정적으로 이해\n- 외부 API 호출에서 비동기 흐름에 대한 질문이 늘어남 → 3주차로 자연 연결\n\n## 4) 다음 주 제안\n1. 콜백 → Promise → async/await 순서로 단계 학습\n2. 이벤트 루프 개념 시각화 자료 활용',
  0, 'PUBLISHED', '2026-06-15 09:00:00.000000', '2026-06-15 09:00:00.000000');

-- ③ 누락된 과거 회고 채우기 (전 멤버 1~3주차 COMPLETED → 회고 제출 불가)
--   현재 보유: 오현우 w1·w2·w3 / 양지훈·김개발 w3.  추가: 양지훈 w1·w2, 김개발 w1·w2, 이서연 w1·w2·w3
INSERT INTO retrospective (id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type, input_summary, ai_feedback, next_week_adjustment, status, requested_at, completed_at, created_at, updated_at)
VALUES
 -- 양지훈 1주차
 (UNHEX('019100000000700080000000000000f1'), @p11, @nw1, @nm1, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','설치와 기본 서버까지는 무난했습니다. 다음 주가 살짝 기대돼요.'),
  JSON_OBJECT('summary','첫 주 과제를 모두 완료하며 좋은 출발을 했습니다.','strengths',JSON_ARRAY('환경 설정과 HTTP 서버 실습을 빠르게 완수'),'risks',JSON_ARRAY('npm 의존성 개념은 얕게 지나갈 수 있음'),'actionItems',JSON_ARRAY('package.json의 dependencies/devDependencies 차이 한 번 정리')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000011','note','좋은 페이스예요. 2주차 모듈 시스템도 같은 흐름으로 가면 됩니다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('npm 의존성 관리 핵심 정리')),
  'COMPLETED','2026-06-07 21:00:00.000000','2026-06-07 21:10:00.000000','2026-06-07 21:00:00.000000','2026-06-07 21:10:00.000000'),
 -- 양지훈 2주차
 (UNHEX('019100000000700080000000000000f2'), @p12, @nw2, @nm1, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','모듈은 이해했는데 외부 API 호출이 비동기라 살짝 헷갈렸어요.'),
  JSON_OBJECT('summary','모듈 시스템과 패키지 관리를 잘 마무리했습니다.','strengths',JSON_ARRAY('fs/axios 실습까지 완수','nodemon으로 개발 환경 개선'),'risks',JSON_ARRAY('비동기 흐름 이해가 얕으면 3주차에서 막힐 수 있음'),'actionItems',JSON_ARRAY('axios 호출을 async/await로 다시 작성해보기')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000011','note','3주차는 비동기가 핵심이에요. 이벤트 루프부터 천천히 가요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('async/await 입문 예제')),
  'COMPLETED','2026-06-14 20:00:00.000000','2026-06-14 20:10:00.000000','2026-06-14 20:00:00.000000','2026-06-14 20:10:00.000000'),
 -- 김개발 1주차
 (UNHEX('019100000000700080000000000000f3'), @p21, @nw1, @nm2, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','경험이 좀 있어서 1주차는 수월했습니다.'),
  JSON_OBJECT('summary','기초 과제를 빠르게 완료했습니다.','strengths',JSON_ARRAY('HTTP 서버·npm 기초를 능숙하게 처리'),'risks',JSON_ARRAY('쉬운 주차라 방심하면 이후 난이도에서 갭'),'actionItems',JSON_ARRAY('다음 주 모듈 시스템 심화 자료 미리 보기')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000012','note','여유 있으면 ESM까지 미리 봐도 좋아요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('ESM 심화 읽을거리')),
  'COMPLETED','2026-06-07 18:30:00.000000','2026-06-07 18:40:00.000000','2026-06-07 18:30:00.000000','2026-06-07 18:40:00.000000'),
 -- 김개발 2주차
 (UNHEX('019100000000700080000000000000f4'), @p22, @nw2, @nm2, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','모듈 시스템은 익숙했는데 Promise 쪽이 아직입니다.'),
  JSON_OBJECT('summary','모듈/패키지 과제를 안정적으로 완료했습니다.','strengths',JSON_ARRAY('패키지 관리와 스크립트 설정에 능숙'),'risks',JSON_ARRAY('Promise 개념 정리가 필요'),'actionItems',JSON_ARRAY('Promise 체이닝 예제 2개 작성')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000012','note','3주차에서 Promise를 집중적으로 다뤄요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('Promise 패턴 모음')),
  'COMPLETED','2026-06-14 19:30:00.000000','2026-06-14 19:40:00.000000','2026-06-14 19:30:00.000000','2026-06-14 19:40:00.000000'),
 -- 이서연 1주차
 (UNHEX('019100000000700080000000000000f5'), @s1, @nw1, @nm3, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','처음이라 시간이 좀 걸렸지만 끝까지 했어요.'),
  JSON_OBJECT('summary','첫 주 과제를 모두 완료했습니다. 좋은 시작이에요.','strengths',JSON_ARRAY('막히는 부분을 끝까지 마무리'),'risks',JSON_ARRAY('실습 시간이 길어지면 다음 주에 부담'),'actionItems',JSON_ARRAY('막힐 때 질문 게시판 적극 활용')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000016','note','완주가 중요해요. 모르면 바로 물어보세요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('Node 기초 보충 영상')),
  'COMPLETED','2026-06-07 21:00:00.000000','2026-06-07 21:15:00.000000','2026-06-07 21:00:00.000000','2026-06-07 21:15:00.000000'),
 -- 이서연 2주차
 (UNHEX('019100000000700080000000000000f6'), @s2, @nw2, @nm3, NULL, 'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','모듈은 했는데 axios 비동기가 어려웠어요.'),
  JSON_OBJECT('summary','2주차도 끝까지 완료했습니다.','strengths',JSON_ARRAY('내장 모듈 실습 완수'),'risks',JSON_ARRAY('비동기 개념이 약해 3주차가 고비'),'actionItems',JSON_ARRAY('콜백/Promise 정의를 한 줄로 정리')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000016','note','3주차 비동기는 천천히 가도 괜찮아요. 예제 위주로.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('비동기 입문 쉬운 예제')),
  'COMPLETED','2026-06-14 22:00:00.000000','2026-06-14 22:15:00.000000','2026-06-14 22:00:00.000000','2026-06-14 22:15:00.000000');
