alter table pastes
    add column search_vector tsvector
        generated always as (
            setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
            setweight(to_tsvector('english', content), 'B')
        ) stored;

create index idx_pastes_search on pastes using gin (search_vector);
