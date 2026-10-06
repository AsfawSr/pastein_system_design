alter table pastes
    add column visibility varchar(16) not null default 'UNLISTED';

-- partial index: only PUBLIC rows, matching the public-list query
create index idx_pastes_public_recent
    on pastes (created_at desc)
    where visibility = 'PUBLIC';
