create table group_review (
  id binary(16) not null,
  group_id binary(16) not null,
  member_id binary(16) not null,
  user_id binary(16) not null,
  rating tinyint not null,
  content text null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  primary key (id),
  unique key group_review_group_user_uidx (group_id, user_id),
  key group_review_group_idx (group_id),
  constraint group_review_group_fk foreign key (group_id) references study_group (id),
  constraint group_review_member_fk foreign key (member_id) references group_member (id),
  constraint group_review_user_fk foreign key (user_id) references users (id),
  constraint group_review_rating_check check (rating between 1 and 5)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
