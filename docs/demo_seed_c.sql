-- ================================================================
-- StudyPot 시연 추가분 C — 오현우(본인 계정) 데이터 채우기
-- 대상: NodeJS(@ng) + Python(@pg). 오현우는 실계정 → 멤버십/진행만 추가.
-- ================================================================
SET @hw = (SELECT id FROM users WHERE email = 'hw62459930@gmail.com' LIMIT 1);
SET @ng = UNHEX('01910000000070008000000000000010');
SET @pg = UNHEX('01920000000070008000000000000010');
-- NodeJS weeks/tasks
SET @nw1=UNHEX('01910000000070008000000000000021'); SET @nw2=UNHEX('01910000000070008000000000000022');
SET @nw3=UNHEX('01910000000070008000000000000023'); SET @nw4=UNHEX('01910000000070008000000000000024');
SET @nt11=UNHEX('01910000000070008000000000000031'); SET @nt12=UNHEX('01910000000070008000000000000032');
SET @nt13=UNHEX('01910000000070008000000000000033'); SET @nt14=UNHEX('01910000000070008000000000000034');
SET @nt21=UNHEX('01910000000070008000000000000035'); SET @nt22=UNHEX('01910000000070008000000000000036');
SET @nt23=UNHEX('01910000000070008000000000000037'); SET @nt24=UNHEX('01910000000070008000000000000038');
SET @nt31=UNHEX('01910000000070008000000000000039'); SET @nt32=UNHEX('0191000000007000800000000000003a');
SET @nt33=UNHEX('0191000000007000800000000000003b'); SET @nt34=UNHEX('0191000000007000800000000000003c');
SET @nt41=UNHEX('0191000000007000800000000000003d'); SET @nt42=UNHEX('0191000000007000800000000000003e');
SET @nt43=UNHEX('0191000000007000800000000000003f'); SET @nt44=UNHEX('01910000000070008000000000000040');
-- Python weeks
SET @pw1=UNHEX('01920000000070008000000000000021'); SET @pw2=UNHEX('01920000000070008000000000000022');
SET @pw3=UNHEX('01920000000070008000000000000023'); SET @pw4=UNHEX('01920000000070008000000000000024');
-- 오현우 ids
SET @hnm = UNHEX('01910000000070008000000000000018'); -- NodeJS membership
SET @hob = UNHEX('0191000000007000800000000000001a'); -- NodeJS onboarding
SET @hp1=UNHEX('019100000000700080000000000000d1'); SET @hp2=UNHEX('019100000000700080000000000000d2');
SET @hp3=UNHEX('019100000000700080000000000000d3'); SET @hp4=UNHEX('019100000000700080000000000000d4');
SET @hretro=UNHEX('019100000000700080000000000000e5'); SET @hllm=UNHEX('019100000000700080000000000000e6');
SET @hpm = UNHEX('01920000000070008000000000000015'); -- Python membership
SET @hpob = UNHEX('01920000000070008000000000000016'); -- Python onboarding
SET @hpp1=UNHEX('01920000000070008000000000000039'); SET @hpp2=UNHEX('0192000000007000800000000000003a');
SET @hpp3=UNHEX('0192000000007000800000000000003b'); SET @hpp4=UNHEX('0192000000007000800000000000003c');
SET @hprev=UNHEX('01920000000070008000000000000053');

-- 인원 한도 상향 (NodeJS 4명, Python 3명 수용)
UPDATE study_group SET max_members = 5 WHERE id = @ng;
UPDATE study_group SET max_members = 4 WHERE id = @pg;

-- ----------------------------------------------------------------
-- NodeJS: 오현우 멤버 + 진행(주1~3 완료, 주4 1/4) + 3주차 회고 + 알림
-- ----------------------------------------------------------------
INSERT INTO group_member (id, group_id, user_id, permission, status, display_name, joined_at, activated_at, created_at, updated_at)
VALUES (@hnm, @ng, @hw, 'MEMBER', 'ACTIVE', '오현우', '2026-05-23 09:00:00.000000', '2026-06-01 00:00:00.000000', '2026-05-23 09:00:00.000000', '2026-06-01 00:00:00.000000');

INSERT INTO group_onboarding_response (id, group_id, member_id, keyword_skill_levels, task_preferences, additional_note, status, submitted_at, created_at, updated_at)
VALUES (@hob, @ng, @hnm, JSON_OBJECT('Node.js 기초',2,'Express.js',2,'REST API',2,'npm',2,'비동기 프로그래밍',2,'모듈 시스템',2), JSON_OBJECT(), NULL, 'SUBMITTED', '2026-05-23 09:30:00.000000', '2026-05-23 09:00:00.000000', '2026-05-23 09:30:00.000000');

INSERT INTO member_week_progress (id, curriculum_week_id, member_id, status, started_at, due_at, completed_at, created_at, updated_at)
VALUES
 (@hp1,@nw1,@hnm,'COMPLETED','2026-06-01 09:00:00.000000','2026-06-07 23:59:59.000000','2026-06-06 19:30:00.000000',NOW(6),NOW(6)),
 (@hp2,@nw2,@hnm,'COMPLETED','2026-06-08 09:00:00.000000','2026-06-14 23:59:59.000000','2026-06-13 19:30:00.000000',NOW(6),NOW(6)),
 (@hp3,@nw3,@hnm,'COMPLETED','2026-06-15 09:00:00.000000','2026-06-21 23:59:59.000000','2026-06-20 21:30:00.000000',NOW(6),NOW(6)),
 (@hp4,@nw4,@hnm,'IN_PROGRESS','2026-06-22 09:00:00.000000','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6));

INSERT INTO task_completion (id, progress_id, weekly_task_id, member_id, status, due_at, completed_at, created_at, updated_at)
VALUES
 (UNHEX('019100000000700080000000000000d5'),@hp1,@nt11,@hnm,'DONE','2026-06-07 23:59:59.000000','2026-06-05 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000d6'),@hp1,@nt12,@hnm,'DONE','2026-06-07 23:59:59.000000','2026-06-05 19:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000d7'),@hp1,@nt13,@hnm,'DONE','2026-06-07 23:59:59.000000','2026-06-06 14:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000d8'),@hp1,@nt14,@hnm,'DONE','2026-06-07 23:59:59.000000','2026-06-06 19:30:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000d9'),@hp2,@nt21,@hnm,'DONE','2026-06-14 23:59:59.000000','2026-06-11 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000da'),@hp2,@nt22,@hnm,'DONE','2026-06-14 23:59:59.000000','2026-06-12 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000db'),@hp2,@nt23,@hnm,'DONE','2026-06-14 23:59:59.000000','2026-06-13 15:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000dc'),@hp2,@nt24,@hnm,'DONE','2026-06-14 23:59:59.000000','2026-06-13 19:30:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000dd'),@hp3,@nt31,@hnm,'DONE','2026-06-21 23:59:59.000000','2026-06-18 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000de'),@hp3,@nt32,@hnm,'DONE','2026-06-21 23:59:59.000000','2026-06-19 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000df'),@hp3,@nt33,@hnm,'DONE','2026-06-21 23:59:59.000000','2026-06-20 16:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000e0'),@hp3,@nt34,@hnm,'DONE','2026-06-21 23:59:59.000000','2026-06-20 21:30:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000e1'),@hp4,@nt41,@hnm,'DONE','2026-06-28 23:59:59.000000','2026-06-24 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000e2'),@hp4,@nt42,@hnm,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000e3'),@hp4,@nt43,@hnm,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('019100000000700080000000000000e4'),@hp4,@nt44,@hnm,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6));

-- 오현우 3주차 회고 (실제 JSON 모양)
INSERT INTO llm_usage (id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens, total_cost_usd, latency_ms, status, created_date_utc, created_at)
VALUES (@hllm,@hw,@ng,'RETROSPECTIVE_FEEDBACK','OPENAI','gpt-5-nano',1150,450,0.000238,3000,'SUCCESS','2026-06-22','2026-06-22 09:30:00.000000');

INSERT INTO retrospective (id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type, input_summary, ai_feedback, next_week_adjustment, status, requested_at, completed_at, created_at, updated_at)
VALUES (@hretro,@hp3,@nw3,@hnm,@hllm,'MANUAL',
  JSON_OBJECT('progress',JSON_OBJECT('id','01910000-0000-7000-8000-0000000000d3','status','COMPLETED','startedAt','2026-06-15T00:00:00Z'),
              'taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),
              'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),
              'memberComment','실습 과제가 도움이 많이 됐습니다. 다만 Promise.all 병렬 처리 부분이 조금 헷갈렸어요.','generatedAt','2026-06-22T09:30:00Z'),
  JSON_OBJECT('summary','3주차 모든 과제를 끝까지 완료하셨고, 실습 위주로 잘 따라오셨습니다. Promise.all 병렬 처리만 한 번 더 짚으면 4주차 Express에서 비동기 흐름이 훨씬 매끄러워집니다.',
              'strengths',JSON_ARRAY('3주 연속 모든 필수 과제 완료','콜백→Promise→async/await 실습을 끝까지 수행'),
              'risks',JSON_ARRAY('Promise.all 같은 동시 처리 개념이 얕으면 Express에서 외부 API 다중 호출 시 막힐 수 있음'),
              'actionItems',JSON_ARRAY('Promise.all 예제를 직접 2개 작성해 결과 순서/에러 처리 확인','4주차 진입 전 async 에러 핸들링(try/catch) 패턴 한 번 정리')),
  JSON_OBJECT('difficulty','MEDIUM',
              'memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000018','note','실습을 끝까지 해주셔서 기초는 탄탄합니다. 4주차엔 동시 처리/에러 핸들링 예제를 조금 더 넣어드릴게요.')),
              'taskChanges',JSON_ARRAY('4주차에 async 에러 핸들링 미니 실습 1개 추가'),
              'supportMaterials',JSON_ARRAY('Promise.all 결과 순서/실패 처리 예제','Express 비동기 라우트의 try/catch 패턴 예제')),
  'COMPLETED','2026-06-22 09:20:00.000000','2026-06-22 09:30:00.000000','2026-06-22 09:20:00.000000','2026-06-22 09:30:00.000000');

-- 오현우 알림 3건 (안읽음)
INSERT INTO notification (id, group_id, recipient_user_id, related_week_id, related_retrospective_id, notification_type, channel, idempotency_key, title, body, payload, status, delivered_at, read_at, created_at)
VALUES
 (UNHEX('019100000000700080000000000000a6'), @ng, @hw, @nw4, NULL, 'RETROSPECTIVE_REMINDER', 'IN_APP', 'demo-nt-hw-1',
  '이번 주 회고를 작성해 주세요', '4주차 마감이 다가와요. AI 팀장과 함께 회고를 시작해 보세요.',
  JSON_OBJECT('groupId','01910000-0000-7000-8000-000000000010','deepLink','/groups/01910000-0000-7000-8000-000000000010'),
  'DELIVERED', '2026-06-26 08:00:00.000000', NULL, '2026-06-26 08:00:00.000000'),
 (UNHEX('019100000000700080000000000000a7'), @ng, @hw, @nw3, NULL, 'LEADER_REPORT_POSTED', 'IN_APP', 'demo-nt-hw-2',
  '새 팀장 리포트가 올라왔어요', '3주차 학습 리포트',
  JSON_OBJECT('groupId','01910000-0000-7000-8000-000000000010','deepLink','/groups/01910000-0000-7000-8000-000000000010'),
  'DELIVERED', '2026-06-22 12:00:00.000000', NULL, '2026-06-22 12:00:00.000000'),
 (UNHEX('019100000000700080000000000000a8'), @ng, @hw, @nw4, NULL, 'WEEK_STARTED', 'IN_APP', 'demo-nt-hw-3',
  '4주차 학습이 시작됐어요', 'Week 4: Express.js 기초 & REST API · TODO를 확인해 주세요.',
  JSON_OBJECT('groupId','01910000-0000-7000-8000-000000000010','deepLink','/groups/01910000-0000-7000-8000-000000000010'),
  'DELIVERED', '2026-06-22 00:00:40.000000', NULL, '2026-06-22 00:00:40.000000');

-- 3주차 리포트 글 4명 기준으로 갱신
UPDATE group_board_post SET content =
'# Week 3 주차 학습 리포트 — 비동기 프로그래밍 & 이벤트 루프\n\n## 1) 주차 목표\n- 콜백 / Promise / async-await의 차이를 이해하고\n- 이벤트 루프 동작 원리를 설명할 수 있는 것\n\n## 2) 이번 주 진행 요약\n- 참여율: **4명 / 4명**, 평균 완료율 **88%** (양지훈 4/4, 김개발 4/4, 오현우 4/4, 이서연 2/4)\n- 가장 많이 한 유형: PRACTICE(2), READING(1), ASSIGNMENT(1)\n\n## 3) 회고 기반 관찰\n- **양지훈**: 진도 속도에 부담 → 4주차 진입 전 이벤트 루프 보충 권장\n- **김개발**: 비동기 용어 혼동 → 정의 한 줄 + 예제 1개 방식으로 보완\n- **오현우**: Promise.all 병렬 처리 보강 필요 → 동시 처리/에러 핸들링 예제 추가\n- **이서연**: 3주차 과제 2건 미완 + 회고 미제출 → 다음 주 리마인더와 난이도 완화 필요\n\n## 4) 다음 주 제안\n1. 4주차 시작 전 이벤트 루프 복습 과제 1개 선행\n2. 개념 설명보다 **코드 작성 비중**을 높여 진행\n3. 뒤처진 멤버는 핵심 과제부터 우선 정리'
WHERE id = UNHEX('01910000000070008000000000000098');

-- ----------------------------------------------------------------
-- Python(종료): 오현우 멤버 + 전 주차 완료 + 리뷰
-- ----------------------------------------------------------------
INSERT INTO group_member (id, group_id, user_id, permission, status, display_name, joined_at, activated_at, created_at, updated_at)
VALUES (@hpm, @pg, @hw, 'MEMBER', 'ACTIVE', '오현우', '2026-04-25 09:00:00.000000', '2026-05-01 00:00:00.000000', '2026-04-25 09:00:00.000000', '2026-05-01 00:00:00.000000');

INSERT INTO group_onboarding_response (id, group_id, member_id, keyword_skill_levels, task_preferences, additional_note, status, submitted_at, created_at, updated_at)
VALUES (@hpob, @pg, @hpm, JSON_OBJECT('Python 기초',2,'자료구조',1,'함수/클래스',1,'파일 I/O',1), JSON_OBJECT(), NULL, 'SUBMITTED', '2026-04-25 09:30:00.000000', '2026-04-25 09:00:00.000000', '2026-04-25 09:30:00.000000');

INSERT INTO member_week_progress (id, curriculum_week_id, member_id, status, started_at, due_at, completed_at, created_at, updated_at)
VALUES
 (@hpp1,@pw1,@hpm,'COMPLETED','2026-05-01 09:00:00.000000','2026-05-07 23:59:59.000000','2026-05-06 19:00:00.000000',NOW(6),NOW(6)),
 (@hpp2,@pw2,@hpm,'COMPLETED','2026-05-08 09:00:00.000000','2026-05-14 23:59:59.000000','2026-05-13 19:00:00.000000',NOW(6),NOW(6)),
 (@hpp3,@pw3,@hpm,'COMPLETED','2026-05-15 09:00:00.000000','2026-05-21 23:59:59.000000','2026-05-20 19:00:00.000000',NOW(6),NOW(6)),
 (@hpp4,@pw4,@hpm,'COMPLETED','2026-05-22 09:00:00.000000','2026-05-31 23:59:59.000000','2026-05-30 18:00:00.000000',NOW(6),NOW(6));

INSERT INTO group_review (id, group_id, member_id, user_id, rating, content, created_at, updated_at)
VALUES (@hprev,@pg,@hpm,@hw,5,'커리큘럼이 단계적으로 잘 짜여 있어서 기초를 탄탄하게 다질 수 있었어요. AI 팀장 리포트 덕분에 매주 뭘 보완할지 명확했습니다.','2026-06-01 12:00:00.000000','2026-06-01 12:00:00.000000');
