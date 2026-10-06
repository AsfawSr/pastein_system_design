alter table pastes
    add column owner_id bigint references users (id) on delete set null;

create index idx_pastes_owner_recent
    on pastes (owner_id, created_at desc)
    where owner_id is not null;
