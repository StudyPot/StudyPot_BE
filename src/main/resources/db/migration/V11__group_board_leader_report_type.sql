-- LEADER_REPORT 보드 타입이 group_board.board_type CHECK 제약에서 누락되어 있었다.
-- (LEADER_REPORT 기능 추가 시 enum/코드는 추가됐으나 DB 제약 마이그레이션이 빠짐)
-- INSERT IGNORE 로 인해 LEADER_REPORT 보드 삽입이 조용히 실패해 팀장 리포트 게시가 동작하지 않았다.
-- CHECK 제약을 재정의해 LEADER_REPORT 를 허용한다.
alter table group_board drop check group_board_type_check;
alter table group_board
  add constraint group_board_type_check
  check (board_type in ('NOTICE', 'QUESTION', 'RESOURCE', 'RETROSPECTIVE', 'LEADER_REPORT'));
