create table users (
    id            bigint generated always as identity primary key,
    username      varchar(50)  not null,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null,
    constraint uq_users_username unique (username)
);
