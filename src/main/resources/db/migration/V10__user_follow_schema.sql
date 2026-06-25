create table user_follow (
  id binary(16) not null,
  follower_user_id binary(16) not null,
  followee_user_id binary(16) not null,
  created_at timestamp(6) not null default current_timestamp(6),
  primary key (id),
  unique key user_follow_pair_uidx (follower_user_id, followee_user_id),
  key user_follow_followee_idx (followee_user_id),
  constraint user_follow_follower_fk foreign key (follower_user_id) references users (id),
  constraint user_follow_followee_fk foreign key (followee_user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
