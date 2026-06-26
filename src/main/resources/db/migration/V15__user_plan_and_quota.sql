-- 사용자 플랜(FREE/PREMIUM). 프리미엄 구독으로 호스트 스터디 동시 개수 제한을 푼다.
-- 기존 사용자는 default 'FREE' 로 채워진다. 한도 강제는 애플리케이션 로직에서 수행.
alter table users
  add column plan varchar(40) not null default 'FREE' after skill_level,
  add constraint users_plan_check check (plan in ('FREE', 'PREMIUM'));
