create table study_group_catalog (
  id binary(16) not null,
  name varchar(120) not null,
  topic varchar(120) not null,
  status varchar(40) not null,
  starts_at date not null,
  ends_at date not null,
  member_count int not null default 1,
  average_rating decimal(3, 2) not null default 0.00,
  favorite tinyint(1) not null default 0,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  primary key (id),
  key study_group_catalog_status_idx (status),
  key study_group_catalog_name_idx (name),
  key study_group_catalog_favorite_start_idx (favorite, starts_at, id),
  constraint study_group_catalog_period_check check (ends_at >= starts_at),
  constraint study_group_catalog_member_count_check check (member_count >= 0),
  constraint study_group_catalog_rating_check check (average_rating between 0 and 5)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table study_group_review (
  id binary(16) not null,
  group_id binary(16) not null,
  author_id binary(16) not null,
  rating int not null,
  content text not null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  group_author_live_key varbinary(32) generated always as (
    case when deleted_at is null then concat(cast(group_id as binary(16)), cast(author_id as binary(16))) else null end
  ) stored,
  primary key (id),
  unique key study_group_review_group_author_live_uidx (group_author_live_key),
  key study_group_review_group_created_idx (group_id, created_at, id),
  key study_group_review_author_idx (author_id, created_at),
  constraint study_group_review_rating_check check (rating between 1 and 5)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
