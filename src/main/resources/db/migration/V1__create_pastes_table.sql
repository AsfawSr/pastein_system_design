create table pastes (
    id         varchar(16) primary key,
    title      varchar(120),
    content    text        not null,
    created_at timestamptz not null
);
