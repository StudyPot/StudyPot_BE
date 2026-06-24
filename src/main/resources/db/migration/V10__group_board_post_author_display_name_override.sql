-- AI 팀장이 채팅에서 올린 질문 글을 '작성자=AI 팀장'으로 표시하기 위한 작성자명 오버라이드.
-- null 이면 기존대로 group_member/users 조인 이름을 사용한다.
alter table group_board_post
  add column author_display_name_override varchar(255) null;
