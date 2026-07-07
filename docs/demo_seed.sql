-- ================================================================
-- StudyPot 시연용 더미 데이터 (실제 스키마/JSON 모양 검증본)
-- 기준일 2026-06-25 | 기존방=NodeJS(ACTIVE,4주차 진행중) 종료방=Python(COMPLETED)
-- 모든 id는 0191..(NodeJS) / 0192..(Python) 프리픽스 → 정리 쉬움
-- 멤버: 방장=양지훈(yjhn0410), 멤버=김개발(더미 0191..0002)
-- ================================================================

SET @u1 = (SELECT id FROM users WHERE email = 'yjhn0410@gmail.com' LIMIT 1);
SET @u2 = UNHEX('01910000000070008000000000000002');

-- NodeJS ids
SET @ng = UNHEX('01910000000070008000000000000010');
SET @nm1 = UNHEX('01910000000070008000000000000011');
SET @nm2 = UNHEX('01910000000070008000000000000012');
SET @nob1 = UNHEX('01910000000070008000000000000013');
SET @nob2 = UNHEX('01910000000070008000000000000014');
SET @nas1 = UNHEX('01910000000070008000000000000015');
SET @nc = UNHEX('01910000000070008000000000000020');
SET @nw1 = UNHEX('01910000000070008000000000000021');
SET @nw2 = UNHEX('01910000000070008000000000000022');
SET @nw3 = UNHEX('01910000000070008000000000000023');
SET @nw4 = UNHEX('01910000000070008000000000000024');
SET @nw5 = UNHEX('01910000000070008000000000000025');
SET @nw6 = UNHEX('01910000000070008000000000000026');
SET @nw7 = UNHEX('01910000000070008000000000000027');
SET @nw8 = UNHEX('01910000000070008000000000000028');
SET @nw9 = UNHEX('01910000000070008000000000000029');
-- weekly_task w1..w4 (각 4개)
SET @nt11 = UNHEX('01910000000070008000000000000031');
SET @nt12 = UNHEX('01910000000070008000000000000032');
SET @nt13 = UNHEX('01910000000070008000000000000033');
SET @nt14 = UNHEX('01910000000070008000000000000034');
SET @nt21 = UNHEX('01910000000070008000000000000035');
SET @nt22 = UNHEX('01910000000070008000000000000036');
SET @nt23 = UNHEX('01910000000070008000000000000037');
SET @nt24 = UNHEX('01910000000070008000000000000038');
SET @nt31 = UNHEX('01910000000070008000000000000039');
SET @nt32 = UNHEX('0191000000007000800000000000003a');
SET @nt33 = UNHEX('0191000000007000800000000000003b');
SET @nt34 = UNHEX('0191000000007000800000000000003c');
SET @nt41 = UNHEX('0191000000007000800000000000003d');
SET @nt42 = UNHEX('0191000000007000800000000000003e');
SET @nt43 = UNHEX('0191000000007000800000000000003f');
SET @nt44 = UNHEX('01910000000070008000000000000040');
-- member_week_progress (m1 w1..w4, m2 w1..w4)
SET @p11 = UNHEX('01910000000070008000000000000051');
SET @p12 = UNHEX('01910000000070008000000000000052');
SET @p13 = UNHEX('01910000000070008000000000000053');
SET @p14 = UNHEX('01910000000070008000000000000054');
SET @p21 = UNHEX('01910000000070008000000000000055');
SET @p22 = UNHEX('01910000000070008000000000000056');
SET @p23 = UNHEX('01910000000070008000000000000057');
SET @p24 = UNHEX('01910000000070008000000000000058');
-- 회고 / llm / 대화 / 게시판
SET @lr1 = UNHEX('01910000000070008000000000000081');
SET @lr2 = UNHEX('01910000000070008000000000000082');
SET @rt1 = UNHEX('01910000000070008000000000000083');
SET @rt2 = UNHEX('01910000000070008000000000000084');
SET @lc1 = UNHEX('01910000000070008000000000000085');
SET @cv1 = UNHEX('01910000000070008000000000000086');
SET @m1 = UNHEX('01910000000070008000000000000087');
SET @m2 = UNHEX('01910000000070008000000000000088');
SET @bN = UNHEX('01910000000070008000000000000091');
SET @bQ = UNHEX('01910000000070008000000000000092');
SET @bR = UNHEX('01910000000070008000000000000093');
SET @bT = UNHEX('01910000000070008000000000000094');
SET @bL = UNHEX('01910000000070008000000000000095');
SET @ps1 = UNHEX('01910000000070008000000000000096');
SET @ps2 = UNHEX('01910000000070008000000000000097');
SET @ps3 = UNHEX('01910000000070008000000000000098');

-- Python ids
SET @pg = UNHEX('01920000000070008000000000000010');
SET @pm1 = UNHEX('01920000000070008000000000000011');
SET @pm2 = UNHEX('01920000000070008000000000000012');
SET @pob1 = UNHEX('01920000000070008000000000000013');
SET @pob2 = UNHEX('01920000000070008000000000000014');
SET @pc = UNHEX('01920000000070008000000000000020');
SET @pw1 = UNHEX('01920000000070008000000000000021');
SET @pw2 = UNHEX('01920000000070008000000000000022');
SET @pw3 = UNHEX('01920000000070008000000000000023');
SET @pw4 = UNHEX('01920000000070008000000000000024');
SET @pp11 = UNHEX('01920000000070008000000000000031');
SET @pp12 = UNHEX('01920000000070008000000000000032');
SET @pp13 = UNHEX('01920000000070008000000000000033');
SET @pp14 = UNHEX('01920000000070008000000000000034');
SET @pp21 = UNHEX('01920000000070008000000000000035');
SET @pp22 = UNHEX('01920000000070008000000000000036');
SET @pp23 = UNHEX('01920000000070008000000000000037');
SET @pp24 = UNHEX('01920000000070008000000000000038');
SET @pbN = UNHEX('01920000000070008000000000000041');
SET @pbQ = UNHEX('01920000000070008000000000000042');
SET @pbR = UNHEX('01920000000070008000000000000043');
SET @pbT = UNHEX('01920000000070008000000000000044');
SET @pbL = UNHEX('01920000000070008000000000000045');
SET @ppost = UNHEX('01920000000070008000000000000046');
SET @prv1 = UNHEX('01920000000070008000000000000051');
SET @prv2 = UNHEX('01920000000070008000000000000052');

-- ================================================================
-- 더미 유저 (김개발)
-- ================================================================
INSERT INTO users (id, email, email_live_key, nickname, profile_image, bio, interests, skill_level, last_login_at, created_at, updated_at)
VALUES (@u2, 'kimdev.studypot@example.com', 'kimdev.studypot@example.com', '김개발', NULL, NULL,
        JSON_ARRAY('JavaScript', 'Node.js'), NULL,
        '2026-06-24 22:00:00.000000', '2026-05-20 09:00:00.000000', '2026-05-20 09:00:00.000000');

-- ================================================================
-- [기존 방] NodeJS 스터디 (ACTIVE) 2026-06-01 ~ 2026-08-01, 9주, 현재 4주차
-- ================================================================
INSERT INTO study_group (id, created_by, name, description, topic, detail_keywords, level, status, max_members, is_public, invite_code, starts_at, ends_at, onboarding_started_at, started_at, created_at, updated_at)
VALUES (@ng, @u1, 'NodeJS 스터디', 'Node.js를 기초부터 REST API 서버 개발까지 함께 학습하는 스터디입니다.', 'NodeJS',
        JSON_ARRAY('Node.js 기초','Express.js','REST API','npm','비동기 프로그래밍','모듈 시스템'),
        NULL, 'ACTIVE', 2, 0, 'NODEJS-2026-A1B2',
        '2026-06-01', '2026-08-01', '2026-05-22 10:00:00.000000', '2026-06-01 00:00:00.000000', '2026-05-20 10:00:00.000000', '2026-06-22 09:00:00.000000');

INSERT INTO group_member (id, group_id, user_id, permission, status, display_name, joined_at, activated_at, created_at, updated_at)
VALUES
 (@nm1, @ng, @u1, 'OWNER',  'ACTIVE', '양지훈', '2026-05-22 10:00:00.000000', '2026-06-01 00:00:00.000000', '2026-05-22 10:00:00.000000', '2026-06-01 00:00:00.000000'),
 (@nm2, @ng, @u2, 'MEMBER', 'ACTIVE', '김개발', '2026-05-23 11:00:00.000000', '2026-06-01 00:00:00.000000', '2026-05-23 11:00:00.000000', '2026-06-01 00:00:00.000000');

INSERT INTO group_onboarding_response (id, group_id, member_id, keyword_skill_levels, task_preferences, additional_note, status, submitted_at, created_at, updated_at)
VALUES
 (@nob1, @ng, @nm1, JSON_OBJECT('Node.js 기초',1,'Express.js',1,'REST API',1,'npm',2,'비동기 프로그래밍',1,'모듈 시스템',1), JSON_OBJECT(), NULL, 'SUBMITTED', '2026-05-22 10:30:00.000000', '2026-05-22 10:00:00.000000', '2026-05-22 10:30:00.000000'),
 (@nob2, @ng, @nm2, JSON_OBJECT('Node.js 기초',3,'Express.js',2,'REST API',2,'npm',3,'비동기 프로그래밍',2,'모듈 시스템',2), JSON_OBJECT(), NULL, 'SUBMITTED', '2026-05-23 11:30:00.000000', '2026-05-23 11:00:00.000000', '2026-05-23 11:30:00.000000');

INSERT INTO member_availability_slot (id, onboarding_response_id, member_id, day_of_week, start_time, end_time, timezone, created_at, updated_at)
VALUES (@nas1, @nob1, @nm1, 1, '10:00:00', '12:00:00', 'Asia/Seoul', '2026-05-22 10:30:00.000000', '2026-05-22 10:30:00.000000');

INSERT INTO curriculum (id, group_id, llm_usage_id, title, total_weeks, onboarding_summary, generated_by_ai, generation_prompt, status, created_at, updated_at)
VALUES (@nc, @ng, NULL, 'Node.js 기초부터 REST API 개발까지 9주 커리큘럼', 9,
        JSON_OBJECT('generatedAt','2026-05-24T00:00:00Z','taskPreferences',JSON_OBJECT(),
                    'availabilitySlots',JSON_ARRAY(JSON_OBJECT('endTime','12:00','timezone','Asia/Seoul','dayOfWeek',1,'responses',1,'startTime','10:00')),
                    'keywordSkillLevels',JSON_OBJECT('Node.js 기초',JSON_OBJECT('max',3,'min',1,'average',2.0,'responses',2)),
                    'additionalNoteCount',0,'submittedResponseCount',2),
        1, NULL, 'ACTIVE', '2026-05-24 09:00:00.000000', '2026-05-24 09:00:00.000000');

INSERT INTO curriculum_week (id, curriculum_id, week_number, title, description, sprint_goal, learning_goals, resources, status, starts_at, ends_at, created_at, updated_at)
VALUES
 (@nw1,@nc,1,'Node.js 환경 설정 & 기초','Node.js를 설치하고 기본 HTTP 서버를 만들어봅니다.','Node.js 환경을 구축하고 첫 HTTP 서버를 실행한다.',
  JSON_ARRAY('Node.js 설치 및 환경 설정','REPL 사용법','HTTP 서버 기초','npm 기초'),
  JSON_ARRAY(JSON_OBJECT('url','https://nodejs.org/docs','title','Node.js 공식 문서')),
  'COMPLETED','2026-06-01 00:00:00.000000','2026-06-07 23:59:59.000000','2026-05-24 09:00:00.000000','2026-06-07 23:59:59.000000'),
 (@nw2,@nc,2,'Node.js 모듈 시스템','CommonJS와 ESM을 이해하고 npm을 활용합니다.','모듈 시스템을 이해하고 외부 패키지를 사용할 수 있다.',
  JSON_ARRAY('CommonJS vs ESM','npm 패키지 관리','내장 모듈(fs,path)','package.json 구조'),
  JSON_ARRAY(JSON_OBJECT('url','https://nodejs.org/api/modules.html','title','Modules: CommonJS')),
  'COMPLETED','2026-06-08 00:00:00.000000','2026-06-14 23:59:59.000000','2026-05-24 09:00:00.000000','2026-06-14 23:59:59.000000'),
 (@nw3,@nc,3,'비동기 프로그래밍 & 이벤트 루프','콜백, Promise, async/await와 이벤트 루프를 학습합니다.','비동기 패턴을 이해하고 이벤트 루프 동작 원리를 설명할 수 있다.',
  JSON_ARRAY('콜백 함수','Promise 활용','async/await','이벤트 루프 원리'),
  JSON_ARRAY(JSON_OBJECT('url','https://developer.mozilla.org/ko/docs/Web/JavaScript/Guide/Using_promises','title','Promise 사용하기')),
  'COMPLETED','2026-06-15 00:00:00.000000','2026-06-21 23:59:59.000000','2026-05-24 09:00:00.000000','2026-06-21 23:59:59.000000'),
 (@nw4,@nc,4,'Express.js 기초 & REST API','Express.js로 REST API 서버를 구축합니다.','Express.js로 기본 CRUD REST API를 구현할 수 있다.',
  JSON_ARRAY('Express 기본 서버','라우터/미들웨어','REST 엔드포인트','Postman 테스트'),
  JSON_ARRAY(JSON_OBJECT('url','https://expressjs.com/ko/','title','Express 한국어 문서')),
  'IN_PROGRESS','2026-06-22 00:00:00.000000','2026-06-28 23:59:59.000000','2026-05-24 09:00:00.000000','2026-06-22 09:00:00.000000'),
 (@nw5,@nc,5,'미들웨어 & 에러 처리','미들웨어 패턴과 에러 처리를 학습합니다.',NULL,JSON_ARRAY(),JSON_ARRAY(),'PENDING','2026-06-29 00:00:00.000000','2026-07-05 23:59:59.000000',NOW(6),NOW(6)),
 (@nw6,@nc,6,'MongoDB & Mongoose','NoSQL과 Mongoose ODM을 학습합니다.',NULL,JSON_ARRAY(),JSON_ARRAY(),'PENDING','2026-07-06 00:00:00.000000','2026-07-12 23:59:59.000000',NOW(6),NOW(6)),
 (@nw7,@nc,7,'JWT 인증 & 보안','JWT 기반 인증을 구현합니다.',NULL,JSON_ARRAY(),JSON_ARRAY(),'PENDING','2026-07-13 00:00:00.000000','2026-07-19 23:59:59.000000',NOW(6),NOW(6)),
 (@nw8,@nc,8,'테스트 코드 (Jest)','Jest로 단위/통합 테스트를 학습합니다.',NULL,JSON_ARRAY(),JSON_ARRAY(),'PENDING','2026-07-20 00:00:00.000000','2026-07-26 23:59:59.000000',NOW(6),NOW(6)),
 (@nw9,@nc,9,'최종 프로젝트','미니 REST API 프로젝트를 완성합니다.',NULL,JSON_ARRAY(),JSON_ARRAY(),'PENDING','2026-07-27 00:00:00.000000','2026-08-01 23:59:59.000000',NOW(6),NOW(6));

INSERT INTO weekly_task (id, curriculum_week_id, display_order, task_type, title, description, required, due_at, generated_by_ai, created_at, updated_at)
VALUES
 (@nt11,@nw1,1,'READING','Node.js 공식 문서 읽기 (Getting Started)','시작하기 섹션을 읽고 핵심 개념을 정리하세요.',1,'2026-06-07 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt12,@nw1,2,'PRACTICE','REPL 실습: 기본 JavaScript 명령어','REPL에서 변수/함수/객체를 실습하세요.',1,'2026-06-07 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt13,@nw1,3,'PRACTICE','HTTP 서버: Hello World 응답 구현','http 모듈로 Hello World 서버를 만들어보세요.',1,'2026-06-07 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt14,@nw1,4,'ASSIGNMENT','npm 기초: package.json 생성 및 설치','npm init으로 초기화하고 패키지를 설치하세요.',1,'2026-06-07 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt21,@nw2,1,'READING','CommonJS vs ESM 비교 정리','require와 import의 차이를 정리하세요.',1,'2026-06-14 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt22,@nw2,2,'PRACTICE','내장 모듈 실습: fs 파일 읽기/쓰기','fs로 파일을 생성/읽기/수정하세요.',1,'2026-06-14 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt23,@nw2,3,'PRACTICE','axios로 공개 API 호출','axios로 공개 API 데이터를 받아 출력하세요.',1,'2026-06-14 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt24,@nw2,4,'ASSIGNMENT','npm 스크립트 + nodemon 적용','scripts 설정 후 nodemon을 적용하세요.',1,'2026-06-14 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt31,@nw3,1,'READING','이벤트 루프 MDN 문서 정독','이벤트 루프 동작 원리를 요약하세요.',1,'2026-06-21 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt32,@nw3,2,'PRACTICE','콜백 지옥 → Promise 리팩토링','중첩 콜백을 Promise 체이닝으로 변환하세요.',1,'2026-06-21 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt33,@nw3,3,'PRACTICE','async/await 비동기 파일 읽기','fs.promises로 async/await 처리하세요.',1,'2026-06-21 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt34,@nw3,4,'ASSIGNMENT','Promise.all 병렬 처리','여러 API를 동시에 호출해 합산하세요.',1,'2026-06-21 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt41,@nw4,1,'PRACTICE','Express 설치 및 기본 서버','포트 3000 기본 서버를 만들어보세요.',1,'2026-06-28 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt42,@nw4,2,'PRACTICE','라우터 분리 & 미들웨어','express.Router 분리 후 logger 미들웨어를 작성하세요.',1,'2026-06-28 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt43,@nw4,3,'ASSIGNMENT','CRUD REST API 엔드포인트','GET/POST/PUT/DELETE를 구현하세요.',1,'2026-06-28 23:59:59.000000',1,NOW(6),NOW(6)),
 (@nt44,@nw4,4,'ASSIGNMENT','Postman 테스트 & 컬렉션 저장','API를 테스트하고 컬렉션을 공유하세요.',0,'2026-06-28 23:59:59.000000',1,NOW(6),NOW(6));

INSERT INTO member_week_progress (id, curriculum_week_id, member_id, status, started_at, due_at, completed_at, created_at, updated_at)
VALUES
 (@p11,@nw1,@nm1,'COMPLETED','2026-06-01 09:00:00.000000','2026-06-07 23:59:59.000000','2026-06-06 21:00:00.000000',NOW(6),NOW(6)),
 (@p12,@nw2,@nm1,'COMPLETED','2026-06-08 09:00:00.000000','2026-06-14 23:59:59.000000','2026-06-13 20:00:00.000000',NOW(6),NOW(6)),
 (@p13,@nw3,@nm1,'COMPLETED','2026-06-15 09:00:00.000000','2026-06-21 23:59:59.000000','2026-06-20 22:00:00.000000',NOW(6),NOW(6)),
 (@p14,@nw4,@nm1,'IN_PROGRESS','2026-06-22 09:00:00.000000','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (@p21,@nw1,@nm2,'COMPLETED','2026-06-01 09:00:00.000000','2026-06-07 23:59:59.000000','2026-06-07 18:00:00.000000',NOW(6),NOW(6)),
 (@p22,@nw2,@nm2,'COMPLETED','2026-06-08 09:00:00.000000','2026-06-14 23:59:59.000000','2026-06-14 19:00:00.000000',NOW(6),NOW(6)),
 (@p23,@nw3,@nm2,'COMPLETED','2026-06-15 09:00:00.000000','2026-06-21 23:59:59.000000','2026-06-21 20:00:00.000000',NOW(6),NOW(6)),
 (@p24,@nw4,@nm2,'IN_PROGRESS','2026-06-22 09:00:00.000000','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6));

INSERT INTO task_completion (id, progress_id, weekly_task_id, member_id, status, due_at, completed_at, created_at, updated_at)
VALUES
 (UNHEX('01910000000070008000000000000061'),@p11,@nt11,@nm1,'DONE','2026-06-07 23:59:59.000000','2026-06-05 14:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000062'),@p11,@nt12,@nm1,'DONE','2026-06-07 23:59:59.000000','2026-06-05 15:30:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000063'),@p11,@nt13,@nm1,'DONE','2026-06-07 23:59:59.000000','2026-06-06 10:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000064'),@p11,@nt14,@nm1,'DONE','2026-06-07 23:59:59.000000','2026-06-06 21:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000065'),@p12,@nt21,@nm1,'DONE','2026-06-14 23:59:59.000000','2026-06-10 14:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000066'),@p12,@nt22,@nm1,'DONE','2026-06-14 23:59:59.000000','2026-06-11 16:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000067'),@p12,@nt23,@nm1,'DONE','2026-06-14 23:59:59.000000','2026-06-12 11:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000068'),@p12,@nt24,@nm1,'DONE','2026-06-14 23:59:59.000000','2026-06-13 20:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000069'),@p13,@nt31,@nm1,'DONE','2026-06-21 23:59:59.000000','2026-06-17 13:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006a'),@p13,@nt32,@nm1,'DONE','2026-06-21 23:59:59.000000','2026-06-18 15:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006b'),@p13,@nt33,@nm1,'DONE','2026-06-21 23:59:59.000000','2026-06-19 17:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006c'),@p13,@nt34,@nm1,'DONE','2026-06-21 23:59:59.000000','2026-06-20 22:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006d'),@p14,@nt41,@nm1,'DONE','2026-06-28 23:59:59.000000','2026-06-23 10:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006e'),@p14,@nt42,@nm1,'DONE','2026-06-28 23:59:59.000000','2026-06-24 14:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000006f'),@p14,@nt43,@nm1,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000070'),@p14,@nt44,@nm1,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000071'),@p21,@nt11,@nm2,'DONE','2026-06-07 23:59:59.000000','2026-06-06 11:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000072'),@p21,@nt12,@nm2,'DONE','2026-06-07 23:59:59.000000','2026-06-06 13:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000073'),@p21,@nt13,@nm2,'DONE','2026-06-07 23:59:59.000000','2026-06-07 10:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000074'),@p21,@nt14,@nm2,'DONE','2026-06-07 23:59:59.000000','2026-06-07 18:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000075'),@p22,@nt21,@nm2,'DONE','2026-06-14 23:59:59.000000','2026-06-11 10:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000076'),@p22,@nt22,@nm2,'DONE','2026-06-14 23:59:59.000000','2026-06-12 14:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000077'),@p22,@nt23,@nm2,'DONE','2026-06-14 23:59:59.000000','2026-06-13 15:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000078'),@p22,@nt24,@nm2,'DONE','2026-06-14 23:59:59.000000','2026-06-14 19:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000079'),@p23,@nt31,@nm2,'DONE','2026-06-21 23:59:59.000000','2026-06-17 10:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007a'),@p23,@nt32,@nm2,'DONE','2026-06-21 23:59:59.000000','2026-06-18 11:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007b'),@p23,@nt33,@nm2,'DONE','2026-06-21 23:59:59.000000','2026-06-20 16:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007c'),@p23,@nt34,@nm2,'DONE','2026-06-21 23:59:59.000000','2026-06-21 20:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007d'),@p24,@nt41,@nm2,'DONE','2026-06-28 23:59:59.000000','2026-06-23 11:00:00.000000',NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007e'),@p24,@nt42,@nm2,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('0191000000007000800000000000007f'),@p24,@nt43,@nm2,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6)),
 (UNHEX('01910000000070008000000000000080'),@p24,@nt44,@nm2,'TODO','2026-06-28 23:59:59.000000',NULL,NOW(6),NOW(6));

-- LLM 사용 기록 (회고 2 + 채팅 1)
INSERT INTO llm_usage (id, user_id, group_id, purpose, provider, model, input_tokens, output_tokens, total_cost_usd, latency_ms, status, created_date_utc, created_at)
VALUES
 (@lr1,@u1,@ng,'RETROSPECTIVE_FEEDBACK','OPENAI','gpt-5-nano',1200,480,0.000252,3200,'SUCCESS','2026-06-22','2026-06-22 10:00:00.000000'),
 (@lr2,@u2,@ng,'RETROSPECTIVE_FEEDBACK','OPENAI','gpt-5-nano',1100,420,0.000223,2900,'SUCCESS','2026-06-22','2026-06-22 11:00:00.000000'),
 (@lc1,@u1,@ng,'TEAM_LEAD_CHAT','OPENAI','gpt-5-mini',6300,520,0.002615,4100,'SUCCESS','2026-06-18','2026-06-18 15:30:00.000000');

-- 회고 (3주차, 두 멤버) — 실제 JSON 모양 준수
INSERT INTO retrospective (id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type, input_summary, ai_feedback, next_week_adjustment, status, requested_at, completed_at, created_at, updated_at)
VALUES
 (@rt1,@p13,@nw3,@nm1,@lr1,'MANUAL',
  JSON_OBJECT('progress',JSON_OBJECT('id','01910000-0000-7000-8000-000000000053','status','COMPLETED','startedAt','2026-06-15T00:00:00Z'),
              'taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),
              'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),
              'memberComment','진도가 너무 빠른 것 같습니다. 이벤트 루프 개념이 아직 완전히 이해되지 않았는데 다음 주로 넘어가야 할 것 같아 걱정됩니다.','generatedAt','2026-06-22T10:00:00Z'),
  JSON_OBJECT('summary','3주차 모든 태스크를 완료하셨습니다. 다만 이벤트 루프 개념이 아직 충분히 익지 않았다고 느끼시는군요. 다음 주 Express로 넘어가기 전, 핵심 개념을 한 번 더 짚고 가면 안정적입니다.',
              'strengths',JSON_ARRAY('3주 연속 모든 필수 과제를 완료했습니다.','콜백→Promise 리팩토링 같은 실습형 과제를 끝까지 수행했습니다.'),
              'risks',JSON_ARRAY('이벤트 루프 이해가 얕은 채로 Express에 진입하면 비동기 디버깅에서 막힐 수 있습니다.'),
              'actionItems',JSON_ARRAY('이벤트 루프 시각화 도구(latentflip loupe)로 Call Stack/Task Queue 흐름을 1회 실습하기','async/await 예제 2개를 직접 다시 작성해보기')),
  JSON_OBJECT('difficulty','MEDIUM',
              'memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000011','note','진도가 빠르게 느껴질 수 있어요. 4주차 시작 전에 이벤트 루프 보충 자료를 먼저 보고 진입하면 훨씬 수월합니다.')),
              'taskChanges',JSON_ARRAY('4주차 시작 전 이벤트 루프 복습 1과제를 선행으로 추가'),
              'supportMaterials',JSON_ARRAY('이벤트 루프 한눈 정리: Call Stack → Web/Node API → Task Queue → Event Loop','async/await 패턴 비교 예제 모음')),
  'COMPLETED','2026-06-22 09:50:00.000000','2026-06-22 10:00:00.000000','2026-06-22 09:50:00.000000','2026-06-22 10:00:00.000000'),
 (@rt2,@p23,@nw3,@nm2,@lr2,'MANUAL',
  JSON_OBJECT('progress',JSON_OBJECT('id','01910000-0000-7000-8000-000000000057','status','COMPLETED','startedAt','2026-06-15T00:00:00Z'),
              'taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),
              'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),
              'memberComment','용어가 너무 어렵습니다. 콜백, Promise, async/await 모두 비슷한 것 같은데 각각 언제 써야 하는지 헷갈립니다.','generatedAt','2026-06-22T11:00:00Z'),
  JSON_OBJECT('summary','비동기 용어가 아직 익숙하지 않은 건 매우 자연스러운 단계입니다. 모든 태스크를 완료하셨고, 개념은 코드를 반복해서 쓰면 빠르게 잡힙니다.',
              'strengths',JSON_ARRAY('3주차 모든 필수 과제를 완료했습니다.','Promise.all 병렬 처리까지 실습을 마쳤습니다.'),
              'risks',JSON_ARRAY('용어 혼동이 남으면 Express 미들웨어/에러 처리에서 흐름 파악이 더뎌질 수 있습니다.'),
              'actionItems',JSON_ARRAY('콜백=나중에 실행할 함수, Promise=결과를 주겠다는 약속, async/await=Promise를 읽기 쉽게 — 한 줄 정의로 메모','같은 로직을 콜백/Promise/async 3가지 방식으로 작성해 비교하기')),
  JSON_OBJECT('difficulty','EASY',
              'memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01910000-0000-7000-8000-000000000012','note','용어가 헷갈릴 땐 정의 한 줄 + 예제 1개면 충분합니다. 4주차엔 개념 설명보다 코드 작성 비중을 높여드릴게요.')),
              'taskChanges',JSON_ARRAY('4주차에 비동기 패턴 비교 예제 1과제 추가'),
              'supportMaterials',JSON_ARRAY('콜백 vs Promise vs async/await 한 장 비교표','상황별 선택 가이드: 단순 1회=async/await, 병렬=Promise.all')),
  'COMPLETED','2026-06-22 10:50:00.000000','2026-06-22 11:00:00.000000','2026-06-22 10:50:00.000000','2026-06-22 11:00:00.000000');

-- 과거 AI 팀장 대화 (이벤트 루프 질문)
INSERT INTO ai_conversation (id, group_id, member_id, curriculum_week_id, retrospective_id, conversation_type, status, summary, opened_at, closed_at, created_at, updated_at)
VALUES (@cv1,@ng,@nm1,@nw3,NULL,'TEAM_LEAD_CHAT','OPEN','이벤트 루프 동작 원리(Call Stack/Event Queue/Event Loop) 질의응답','2026-06-18 15:20:00.000000',NULL,'2026-06-18 15:20:00.000000','2026-06-18 15:35:00.000000');

INSERT INTO ai_conversation_message (id, conversation_id, llm_usage_id, sender_type, content, metadata, created_at)
VALUES
 (@m1,@cv1,NULL,'USER','이벤트 루프에 대해 알려줘',NULL,'2026-06-18 15:20:00.000000'),
 (@m2,@cv1,@lc1,'ASSISTANT','이벤트 루프(Event Loop)는 Node.js의 핵심 비동기 메커니즘이에요.\n\nJavaScript는 싱글 스레드지만, 이벤트 루프 덕분에 I/O 같은 작업을 블로킹 없이 처리합니다.\n\n핵심 3가지\n1. Call Stack: 지금 실행 중인 함수가 쌓이는 스택\n2. Task Queue: 완료된 비동기 작업의 콜백이 대기하는 큐\n3. Event Loop: Call Stack이 비면 Task Queue에서 콜백을 꺼내 실행\n\n예를 들어 setTimeout(fn, 0)을 호출해도 fn은 바로 실행되지 않고, 지금 동기 코드가 다 끝나 Call Stack이 빈 다음에야 실행돼요. 3주차에 하신 콜백→Promise 리팩토링이 바로 이 큐 동작을 더 다루기 쉽게 만드는 과정이었습니다.',NULL,'2026-06-18 15:30:00.000000');

-- 게시판 5종
INSERT INTO group_board (id, group_id, board_type, name, description, display_order, is_default, created_at, updated_at)
VALUES
 (@bN,@ng,'NOTICE','공지','스터디 공지 게시판입니다.',1,1,'2026-05-24 09:00:00.000000','2026-05-24 09:00:00.000000'),
 (@bQ,@ng,'QUESTION','질문','학습 중 궁금한 점을 질문하는 게시판입니다.',2,1,'2026-05-24 09:00:00.000000','2026-05-24 09:00:00.000000'),
 (@bR,@ng,'RESOURCE','자료 공유','유용한 자료를 공유하는 게시판입니다.',3,1,'2026-05-24 09:00:00.000000','2026-05-24 09:00:00.000000'),
 (@bT,@ng,'RETROSPECTIVE','회고','주차별 회고 게시판입니다.',4,1,'2026-05-24 09:00:00.000000','2026-05-24 09:00:00.000000'),
 (@bL,@ng,'LEADER_REPORT','AI 팀장','AI 팀장 주차 리포트 게시판입니다.',5,1,'2026-05-24 09:00:00.000000','2026-05-24 09:00:00.000000');

INSERT INTO group_board_post (id, group_id, board_id, author_member_id, title, content, is_pinned, status, created_at, updated_at)
VALUES
 (@ps1,@ng,@bN,@nm1,'NodeJS 스터디 시작을 환영합니다!','안녕하세요! 9주간의 NodeJS 스터디를 시작합니다.\n\n매주 커리큘럼에 따라 함께 학습해봐요. 모르는 건 언제든 질문 게시판에 올려주세요.\n\n1주차 Node 기초 → 2주차 모듈 → 3주차 비동기 → 4주차 Express … 화이팅입니다!',1,'PUBLISHED','2026-05-24 09:30:00.000000','2026-05-24 09:30:00.000000'),
 (@ps2,@ng,@bQ,@nm2,'async/await와 Promise의 차이가 헷갈립니다','async/await를 쓰면 Promise를 아예 안 써도 되나요? 아니면 내부적으로 Promise가 쓰이는 건가요? 둘 다 쓸 수 있는 것 같아서 언제 뭘 써야 할지 모르겠습니다.',0,'PUBLISHED','2026-06-19 14:00:00.000000','2026-06-19 14:00:00.000000'),
 (@ps3,@ng,@bL,@nm1,'3주차 학습 리포트 — 비동기 프로그래밍 & 이벤트 루프','# Week 3 주차 학습 리포트 — 비동기 프로그래밍 & 이벤트 루프\n\n## 1) 주차 목표\n- 콜백 / Promise / async-await의 차이를 이해하고\n- 이벤트 루프 동작 원리를 설명할 수 있는 것\n\n## 2) 이번 주 진행 요약\n- 참여율: **2명 / 2명**, 평균 완료율 **100%** (양지훈 4/4, 김개발 4/4)\n- 가장 많이 한 유형: PRACTICE(2), READING(1), ASSIGNMENT(1)\n\n## 3) 회고 기반 관찰\n- **양지훈**: 진도 속도에 부담 → 4주차 진입 전 이벤트 루프 보충 권장\n- **김개발**: 비동기 용어 혼동 → 정의 한 줄 + 예제 1개 방식으로 보완\n\n## 4) 다음 주 제안\n1. 4주차 시작 전 이벤트 루프 복습 과제 1개 선행\n2. 개념 설명보다 **코드 작성 비중**을 높여 진행\n3. 막히는 용어는 질문 게시판에 즉시 공유',0,'PUBLISHED','2026-06-22 12:00:00.000000','2026-06-22 12:00:00.000000');

-- ================================================================
-- [종료 방] Python 스터디 (COMPLETED) 2026-05-01 ~ 2026-05-31, 4주 전부 완료
-- ================================================================
INSERT INTO study_group (id, created_by, name, description, topic, detail_keywords, level, status, max_members, is_public, invite_code, starts_at, ends_at, onboarding_started_at, started_at, created_at, updated_at)
VALUES (@pg,@u1,'Python 스터디','파이썬 기초 문법부터 파일 I/O까지 4주간 진행한 스터디입니다.','Python',
        JSON_ARRAY('Python 기초','자료구조','함수/클래스','파일 I/O'),NULL,'COMPLETED',2,0,'PYTHON-2026-B3C4',
        '2026-05-01','2026-05-31','2026-04-25 10:00:00.000000','2026-05-01 00:00:00.000000','2026-04-22 10:00:00.000000','2026-05-31 23:59:59.000000');

INSERT INTO group_member (id, group_id, user_id, permission, status, display_name, joined_at, activated_at, created_at, updated_at)
VALUES
 (@pm1,@pg,@u1,'OWNER','ACTIVE','양지훈','2026-04-25 10:00:00.000000','2026-05-01 00:00:00.000000','2026-04-25 10:00:00.000000','2026-05-01 00:00:00.000000'),
 (@pm2,@pg,@u2,'MEMBER','ACTIVE','김개발','2026-04-25 11:00:00.000000','2026-05-01 00:00:00.000000','2026-04-25 11:00:00.000000','2026-05-01 00:00:00.000000');

INSERT INTO group_onboarding_response (id, group_id, member_id, keyword_skill_levels, task_preferences, additional_note, status, submitted_at, created_at, updated_at)
VALUES
 (@pob1,@pg,@pm1,JSON_OBJECT('Python 기초',1,'자료구조',1,'함수/클래스',1,'파일 I/O',1),JSON_OBJECT(),NULL,'SUBMITTED','2026-04-25 10:30:00.000000','2026-04-25 10:00:00.000000','2026-04-25 10:30:00.000000'),
 (@pob2,@pg,@pm2,JSON_OBJECT('Python 기초',2,'자료구조',1,'함수/클래스',1,'파일 I/O',1),JSON_OBJECT(),NULL,'SUBMITTED','2026-04-25 11:30:00.000000','2026-04-25 11:00:00.000000','2026-04-25 11:30:00.000000');

INSERT INTO curriculum (id, group_id, llm_usage_id, title, total_weeks, onboarding_summary, generated_by_ai, generation_prompt, status, created_at, updated_at)
VALUES (@pc,@pg,NULL,'Python 기초 문법 마스터 4주 커리큘럼',4,
        JSON_OBJECT('generatedAt','2026-04-26T00:00:00Z','taskPreferences',JSON_OBJECT(),'availabilitySlots',JSON_ARRAY(),
                    'keywordSkillLevels',JSON_OBJECT('Python 기초',JSON_OBJECT('max',2,'min',1,'average',1.5,'responses',2)),
                    'additionalNoteCount',0,'submittedResponseCount',2),
        1,NULL,'COMPLETED','2026-04-26 09:00:00.000000','2026-05-31 23:59:59.000000');

INSERT INTO curriculum_week (id, curriculum_id, week_number, title, description, sprint_goal, learning_goals, resources, status, starts_at, ends_at, created_at, updated_at)
VALUES
 (@pw1,@pc,1,'Python 기초 문법','변수/자료형/조건문/반복문 기초를 학습합니다.','기초 문법으로 간단한 프로그램을 작성할 수 있다.',JSON_ARRAY('변수와 자료형','조건문','반복문','리스트/딕셔너리'),JSON_ARRAY(),'COMPLETED','2026-05-01 00:00:00.000000','2026-05-07 23:59:59.000000','2026-04-26 09:00:00.000000','2026-05-07 23:59:59.000000'),
 (@pw2,@pc,2,'함수와 모듈','함수 정의와 모듈 사용을 학습합니다.','재사용 가능한 함수를 정의하고 모듈을 활용할 수 있다.',JSON_ARRAY('함수 정의/호출','매개변수/반환값','내장 함수','import 모듈'),JSON_ARRAY(),'COMPLETED','2026-05-08 00:00:00.000000','2026-05-14 23:59:59.000000','2026-04-26 09:00:00.000000','2026-05-14 23:59:59.000000'),
 (@pw3,@pc,3,'클래스와 객체지향','클래스/OOP 기초를 학습합니다.','클래스를 정의하고 객체를 생성해 사용할 수 있다.',JSON_ARRAY('클래스/인스턴스','__init__','상속/오버라이딩','캡슐화'),JSON_ARRAY(),'COMPLETED','2026-05-15 00:00:00.000000','2026-05-21 23:59:59.000000','2026-04-26 09:00:00.000000','2026-05-21 23:59:59.000000'),
 (@pw4,@pc,4,'파일 I/O & 미니 프로젝트','파일 처리와 복습 프로젝트를 진행합니다.','파일을 읽고 쓰며 간단한 CLI를 만들 수 있다.',JSON_ARRAY('파일 읽기/쓰기','with 구문','CSV 처리','메모장 CLI'),JSON_ARRAY(),'COMPLETED','2026-05-22 00:00:00.000000','2026-05-31 23:59:59.000000','2026-04-26 09:00:00.000000','2026-05-31 23:59:59.000000');

INSERT INTO member_week_progress (id, curriculum_week_id, member_id, status, started_at, due_at, completed_at, created_at, updated_at)
VALUES
 (@pp11,@pw1,@pm1,'COMPLETED','2026-05-01 09:00:00.000000','2026-05-07 23:59:59.000000','2026-05-06 20:00:00.000000',NOW(6),NOW(6)),
 (@pp12,@pw2,@pm1,'COMPLETED','2026-05-08 09:00:00.000000','2026-05-14 23:59:59.000000','2026-05-13 21:00:00.000000',NOW(6),NOW(6)),
 (@pp13,@pw3,@pm1,'COMPLETED','2026-05-15 09:00:00.000000','2026-05-21 23:59:59.000000','2026-05-20 22:00:00.000000',NOW(6),NOW(6)),
 (@pp14,@pw4,@pm1,'COMPLETED','2026-05-22 09:00:00.000000','2026-05-31 23:59:59.000000','2026-05-30 21:00:00.000000',NOW(6),NOW(6)),
 (@pp21,@pw1,@pm2,'COMPLETED','2026-05-01 09:00:00.000000','2026-05-07 23:59:59.000000','2026-05-07 18:00:00.000000',NOW(6),NOW(6)),
 (@pp22,@pw2,@pm2,'COMPLETED','2026-05-08 09:00:00.000000','2026-05-14 23:59:59.000000','2026-05-14 20:00:00.000000',NOW(6),NOW(6)),
 (@pp23,@pw3,@pm2,'COMPLETED','2026-05-15 09:00:00.000000','2026-05-21 23:59:59.000000','2026-05-21 19:00:00.000000',NOW(6),NOW(6)),
 (@pp24,@pw4,@pm2,'COMPLETED','2026-05-22 09:00:00.000000','2026-05-31 23:59:59.000000','2026-05-31 17:00:00.000000',NOW(6),NOW(6));

INSERT INTO group_board (id, group_id, board_type, name, description, display_order, is_default, created_at, updated_at)
VALUES
 (@pbN,@pg,'NOTICE','공지','스터디 공지 게시판입니다.',1,1,'2026-04-26 09:00:00.000000','2026-04-26 09:00:00.000000'),
 (@pbQ,@pg,'QUESTION','질문','질문 게시판입니다.',2,1,'2026-04-26 09:00:00.000000','2026-04-26 09:00:00.000000'),
 (@pbR,@pg,'RESOURCE','자료 공유','자료 공유 게시판입니다.',3,1,'2026-04-26 09:00:00.000000','2026-04-26 09:00:00.000000'),
 (@pbT,@pg,'RETROSPECTIVE','회고','회고 게시판입니다.',4,1,'2026-04-26 09:00:00.000000','2026-04-26 09:00:00.000000'),
 (@pbL,@pg,'LEADER_REPORT','AI 팀장','AI 팀장 리포트 게시판입니다.',5,1,'2026-04-26 09:00:00.000000','2026-04-26 09:00:00.000000');

INSERT INTO group_board_post (id, group_id, board_id, author_member_id, title, content, is_pinned, status, created_at, updated_at)
VALUES
 (@ppost,@pg,@pbL,@pm1,'수료 리포트 — Python 4주 스터디 완주','# 🎓 Python 스터디 수료 리포트\n\n4주 스터디를 완주했습니다. 그동안의 학습 여정을 정리했어요.\n\n## 여정 요약\n- 기간: 2026.05.01 ~ 2026.05.31 (4주)\n- 완료 주차: **4 / 4**\n- 그룹 평균 완료율: **100%**\n\n## 주요 성과\n- 기초 문법 → 함수/모듈 → 클래스/OOP → 파일 I/O까지 단계적으로 완성\n- 마지막 주 미니 프로젝트(메모장 CLI) 완성\n\n## 다음 학습 제안\n1. 자료구조/알고리즘 입문\n2. 가상환경 & 패키지 관리(pip, venv)\n3. 간단한 웹(Flask) 입문\n\n4주간 정말 수고 많으셨어요. 멈추지 말고 다음 여정으로 이어가 봐요! 🎉',0,'PUBLISHED','2026-05-31 22:00:00.000000','2026-05-31 22:00:00.000000');

INSERT INTO group_review (id, group_id, member_id, user_id, rating, content, created_at, updated_at)
VALUES
 (@prv1,@pg,@pm1,@u1,5,'4주 동안 파이썬 기초를 체계적으로 배울 수 있었습니다. AI 팀장의 맞춤 회고 피드백이 정말 도움이 됐어요!','2026-06-01 10:00:00.000000','2026-06-01 10:00:00.000000'),
 (@prv2,@pg,@pm2,@u2,4,'혼자 할 때보다 훨씬 효율적이었습니다. 구조가 잘 잡혀 있어서 끝까지 완주할 수 있었어요.','2026-06-01 11:00:00.000000','2026-06-01 11:00:00.000000');

-- 다음 스터디 추천 (study_recommendation: group_id PK)
INSERT INTO study_recommendation (group_id, ai_suggestions, popular_topics, created_at)
VALUES (@pg,
  JSON_ARRAY(
    JSON_OBJECT('title','자료구조/알고리즘 입문 (Python)','reason','파이썬 기초 문법을 마친 뒤 자연스러운 다음 단계로, 리스트/딕셔너리 활용과 기본 알고리즘을 다룹니다.'),
    JSON_OBJECT('title','Flask로 시작하는 웹 백엔드','reason','함수/클래스/파일 I/O 학습을 실제 웹 서버 구현으로 확장해 결과물을 만들어봅니다.'),
    JSON_OBJECT('title','파이썬 가상환경 & 패키지 관리 실무','reason','venv와 pip로 프로젝트 환경을 분리·관리하는 실무 습관을 익힙니다.')
  ),
  JSON_ARRAY(
    JSON_OBJECT('topic','알고리즘','groupCount',2,'memberCount',4),
    JSON_OBJECT('topic','NodeJS','groupCount',1,'memberCount',2),
    JSON_OBJECT('topic','Spring boot','groupCount',1,'memberCount',2)
  ),
  '2026-05-31 23:00:00.000000');
