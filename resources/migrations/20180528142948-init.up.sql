create table if not exists users (
  id serial,
  username varchar(100) primary key,
  email varchar(100) unique not null,
  password varchar(150) not null,
  verified boolean not null default false,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp
);
--;;
create table if not exists email_verifications (
  id serial,
  token varchar(32) primary key,
  email varchar(100) unique references users (email) on delete cascade,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp
);
