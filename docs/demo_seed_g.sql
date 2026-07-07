-- ================================================================
-- StudyPot 시연 추가분 G — 종료방(Python) 회고 채우기
--  양지훈(pm1)·오현우(hpm) 1~4주차 회고 8건. (Python curriculum=COMPLETED → 스케줄러 무영향)
-- ================================================================
SET @pw1=UNHEX('01920000000070008000000000000021'); SET @pw2=UNHEX('01920000000070008000000000000022');
SET @pw3=UNHEX('01920000000070008000000000000023'); SET @pw4=UNHEX('01920000000070008000000000000024');
-- 양지훈
SET @pm1=UNHEX('01920000000070008000000000000011');
SET @pp11=UNHEX('01920000000070008000000000000031'); SET @pp12=UNHEX('01920000000070008000000000000032');
SET @pp13=UNHEX('01920000000070008000000000000033'); SET @pp14=UNHEX('01920000000070008000000000000034');
-- 오현우
SET @hpm=UNHEX('01920000000070008000000000000015');
SET @hpp1=UNHEX('01920000000070008000000000000039'); SET @hpp2=UNHEX('0192000000007000800000000000003a');
SET @hpp3=UNHEX('0192000000007000800000000000003b'); SET @hpp4=UNHEX('0192000000007000800000000000003c');

INSERT INTO retrospective (id, progress_id, curriculum_week_id, member_id, llm_usage_id, trigger_type, input_summary, ai_feedback, next_week_adjustment, status, requested_at, completed_at, created_at, updated_at)
VALUES
 -- 양지훈 1주차 (기초 문법)
 (UNHEX('01920000000070008000000000000061'),@pp11,@pw1,@pm1,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','변수/조건문/반복문까지는 익숙해서 무난했어요. 리스트 컴프리헨션이 살짝 헷갈렸습니다.'),
  JSON_OBJECT('summary','기초 문법 과제를 모두 완료하며 좋은 출발을 했습니다.','strengths',JSON_ARRAY('조건문/반복문을 능숙하게 처리','자료형 개념을 빠르게 흡수'),'risks',JSON_ARRAY('리스트 컴프리헨션 등 파이썬다운 표현은 더 연습 필요'),'actionItems',JSON_ARRAY('리스트 컴프리헨션 예제 3개 직접 작성')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000011','note','기초는 탄탄해요. 2주차 함수부터 본격적으로 가봅시다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('파이썬 리스트 컴프리헨션 정리')),
  'COMPLETED','2026-05-07 20:00:00.000000','2026-05-07 20:10:00.000000','2026-05-07 20:00:00.000000','2026-05-07 20:10:00.000000'),
 -- 양지훈 2주차 (함수/모듈)
 (UNHEX('01920000000070008000000000000062'),@pp12,@pw2,@pm1,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','함수는 괜찮은데 *args/**kwargs 개념이 처음이라 낯설었어요.'),
  JSON_OBJECT('summary','함수 정의와 모듈 사용을 잘 마무리했습니다.','strengths',JSON_ARRAY('매개변수/반환값 구조를 명확히 이해','import로 모듈 분리 실습 완료'),'risks',JSON_ARRAY('가변 인자(*args/**kwargs) 활용이 아직 얕음'),'actionItems',JSON_ARRAY('*args/**kwargs를 쓰는 함수 1개 직접 구현')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000011','note','3주차 클래스에서 self/__init__이 나와요. 함수 개념이 바탕이 됩니다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('가변 인자 패턴 예제')),
  'COMPLETED','2026-05-14 20:00:00.000000','2026-05-14 20:10:00.000000','2026-05-14 20:00:00.000000','2026-05-14 20:10:00.000000'),
 -- 양지훈 3주차 (클래스/OOP)
 (UNHEX('01920000000070008000000000000063'),@pp13,@pw3,@pm1,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','클래스 개념은 이해했는데 상속/오버라이딩에서 한 번 막혔다가 풀었어요.'),
  JSON_OBJECT('summary','객체지향 기초를 끝까지 완수했습니다. 상속까지 직접 구현한 점이 좋습니다.','strengths',JSON_ARRAY('__init__/인스턴스 개념을 정확히 적용','상속/오버라이딩을 예제로 검증'),'risks',JSON_ARRAY('캡슐화(접근 제어) 관례는 더 다뤄볼 여지'),'actionItems',JSON_ARRAY('간단한 클래스 계층(부모-자식) 1개 설계해보기')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000011','note','OOP 감 잡으셨어요. 마지막 주 파일 I/O + 미니프로젝트로 마무리해요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('파이썬 클래스/상속 핵심 정리')),
  'COMPLETED','2026-05-21 20:00:00.000000','2026-05-21 20:10:00.000000','2026-05-21 20:00:00.000000','2026-05-21 20:10:00.000000'),
 -- 양지훈 4주차 (파일 I/O & 미니프로젝트)
 (UNHEX('01920000000070008000000000000064'),@pp14,@pw4,@pm1,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','메모장 CLI까지 직접 만들어보니 그동안 배운 게 한 번에 정리됐어요. 뿌듯합니다!'),
  JSON_OBJECT('summary','4주 커리큘럼을 완주했습니다. 미니 프로젝트로 전체 개념을 통합한 점이 인상적입니다.','strengths',JSON_ARRAY('with 구문/파일 처리 안정적','배운 문법을 미니 프로젝트로 통합'),'risks',JSON_ARRAY('예외 처리(try/except)는 다음 단계에서 더 깊게'),'actionItems',JSON_ARRAY('다음 스터디로 자료구조/알고리즘 입문 추천')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000011','note','완주 축하해요! 기초가 단단하니 다음은 알고리즘으로 자연스럽게 이어집니다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('파이썬 예외 처리 가이드','자료구조 입문 로드맵')),
  'COMPLETED','2026-05-31 17:00:00.000000','2026-05-31 17:10:00.000000','2026-05-31 17:00:00.000000','2026-05-31 17:10:00.000000'),
 -- 오현우 1주차
 (UNHEX('01920000000070008000000000000065'),@hpp1,@pw1,@hpm,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','파이썬은 거의 처음이라 문법이 간결해서 신기했어요. 들여쓰기 규칙만 적응하면 될 듯.'),
  JSON_OBJECT('summary','첫 주 기초 과제를 모두 완료했습니다. 적응이 빠릅니다.','strengths',JSON_ARRAY('간결한 문법에 빠르게 적응','반복문/리스트 실습 완수'),'risks',JSON_ARRAY('들여쓰기 기반 블록에 익숙해질 시간 필요'),'actionItems',JSON_ARRAY('작은 예제를 매일 1개씩 직접 타이핑')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000015','note','시작 좋아요. 손에 익히는 게 최고예요. 2주차 함수로 갑니다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('파이썬 입문 치트시트')),
  'COMPLETED','2026-05-07 21:00:00.000000','2026-05-07 21:10:00.000000','2026-05-07 21:00:00.000000','2026-05-07 21:10:00.000000'),
 -- 오현우 2주차
 (UNHEX('01920000000070008000000000000066'),@hpp2,@pw2,@hpm,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','함수로 코드를 나누니까 확실히 깔끔해지네요. 반환값 여러 개 주는 것도 신기했어요.'),
  JSON_OBJECT('summary','함수와 모듈 과제를 끝까지 완료했습니다.','strengths',JSON_ARRAY('함수로 코드 구조화하는 감을 잡음','튜플 반환 등 파이썬 기능 활용'),'risks',JSON_ARRAY('모듈 분리/재사용은 프로젝트에서 더 체득 필요'),'actionItems',JSON_ARRAY('자주 쓰는 함수 2개를 모듈로 분리해보기')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000015','note','잘 따라오고 있어요. 3주차 클래스는 천천히 예제 위주로 가요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('모듈/패키지 기초')),
  'COMPLETED','2026-05-14 21:00:00.000000','2026-05-14 21:10:00.000000','2026-05-14 21:00:00.000000','2026-05-14 21:10:00.000000'),
 -- 오현우 3주차
 (UNHEX('01920000000070008000000000000067'),@hpp3,@pw3,@hpm,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','클래스가 제일 어려웠어요. self가 뭔지 한참 고민했는데 예제 보고 이해했습니다.'),
  JSON_OBJECT('summary','가장 어려운 주차였는데 모든 과제를 완료했습니다. 끈기가 좋아요.','strengths',JSON_ARRAY('self/__init__ 개념을 예제로 끝내 이해','객체 생성/사용 흐름 체득'),'risks',JSON_ARRAY('상속은 한 번 더 복습하면 확실해질 것'),'actionItems',JSON_ARRAY('클래스 1개를 직접 정의하고 인스턴스 2개 만들어보기')),
  JSON_OBJECT('difficulty','MEDIUM','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000015','note','제일 큰 산 넘으셨어요. 마지막 주는 파일 I/O + 미니프로젝트로 즐겁게 마무리해요.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('self/__init__ 쉽게 이해하기')),
  'COMPLETED','2026-05-21 21:00:00.000000','2026-05-21 21:10:00.000000','2026-05-21 21:00:00.000000','2026-05-21 21:10:00.000000'),
 -- 오현우 4주차
 (UNHEX('01920000000070008000000000000068'),@hpp4,@pw4,@hpm,NULL,'MANUAL',
  JSON_OBJECT('taskCompletionCounts',JSON_OBJECT('DONE',4,'TODO',0,'SKIPPED',0,'INCOMPLETE',0),'priorRetrospectives',JSON_ARRAY(),'ruleViolations',JSON_ARRAY(),'conversationSummary',JSON_OBJECT('status','NOT_AVAILABLE'),'memberComment','메모장 CLI 완성했어요! 처음 만든 프로그램이라 기억에 남을 것 같습니다. 4주 알찼어요.'),
  JSON_OBJECT('summary','4주 커리큘럼을 완주했습니다. 처음부터 끝까지 모든 과제를 완료한 성실함이 돋보입니다.','strengths',JSON_ARRAY('파일 읽기/쓰기 실습 완수','배운 문법으로 미니 프로젝트 완성'),'risks',JSON_ARRAY('코드량이 늘면 구조화/함수 분리를 더 신경 쓰면 좋음'),'actionItems',JSON_ARRAY('다음 스터디(자료구조/Flask 등)로 이어가기')),
  JSON_OBJECT('difficulty','EASY','memberNotes',JSON_ARRAY(JSON_OBJECT('memberId','01920000-0000-7000-8000-000000000015','note','완주 축하해요! 첫 프로그램 완성은 큰 한 걸음이에요. 다음 여정도 응원합니다.')),'taskChanges',JSON_ARRAY(),'supportMaterials',JSON_ARRAY('다음 단계 학습 로드맵')),
  'COMPLETED','2026-05-31 17:30:00.000000','2026-05-31 17:40:00.000000','2026-05-31 17:30:00.000000','2026-05-31 17:40:00.000000');
