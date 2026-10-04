CREATE TABLE ride_outbox_events (
    id UUID PRIMARY KEY,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
CREATE INDEX ride_outbox_unpublished_idx ON ride_outbox_events(created_at) WHERE published_at IS NULL;
