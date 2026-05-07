-- SPT-23 DB core live-row uniqueness policy.
-- MySQL unique indexes allow multiple NULL values, so generated columns
-- evaluate to a natural key only for rows where deleted_at is null.
-- Rollout note: adding STORED generated columns and unique indexes may rebuild
-- or lock populated InnoDB tables. This migration is intended for the MVP schema
-- before production data; populated environments should schedule maintenance or
-- split/online-DDL the changes according to the target MySQL version.

alter table study_group
  drop index study_group_invite_code_uidx,
  add column invite_code_live_key varchar(80) generated always as (case when deleted_at is null then invite_code else null end) stored,
  add unique key study_group_invite_code_live_uidx (invite_code_live_key);

alter table group_member
  drop index group_member_group_user_uidx,
  add column group_user_live_key varbinary(32) generated always as (case when deleted_at is null then concat(cast(group_id as binary(16)), cast(user_id as binary(16))) else null end) stored,
  add unique key group_member_group_user_live_uidx (group_user_live_key);

alter table group_onboarding_response
  drop index onboarding_member_uidx,
  add column member_live_key binary(16) generated always as (case when deleted_at is null then member_id else null end) stored,
  add unique key onboarding_member_live_uidx (member_live_key);

alter table curriculum
  drop index curriculum_group_uidx,
  add column group_live_key binary(16) generated always as (case when deleted_at is null then group_id else null end) stored,
  add unique key curriculum_group_live_uidx (group_live_key);
