-- Series navigation for thematically related posts (SWR-040)
ALTER TABLE posts ADD COLUMN series_previous_post_id UUID;
ALTER TABLE posts ADD COLUMN series_next_post_id UUID;
