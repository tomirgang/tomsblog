-- Full-text search index for posts (SWR-038)
-- Uses PostgreSQL tsvector with 'simple' config for language-independent search
CREATE INDEX idx_posts_fulltext ON posts
    USING GIN (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '')));
