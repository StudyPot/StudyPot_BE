alter table users
  add column password_hash varchar(100) null after email_live_key;
