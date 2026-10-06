create table api_keys (
    id                    bigint generated always as identity primary key,
    user_id               bigint       not null references users (id) on delete cascade,
    key_hash              varchar(64)  not null,
    label                 varchar(50)  not null,
    rate_limit_per_minute int          not null default 60,
    created_at            timestamptz  not null,
    constraint uq_api_keys_hash unique (key_hash)
);

create index idx_api_keys_user on api_keys (user_id);
