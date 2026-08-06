CREATE SCHEMA IF NOT EXISTS {schema};

CREATE TABLE IF NOT EXISTS {schema}.social_creator_monitor (
    id BIGINT PRIMARY KEY,
    scope_key VARCHAR(128) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    creator_key VARCHAR(256) NOT NULL,
    profile_url TEXT NOT NULL,
    display_name VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (scope_key, platform, creator_key)
);

CREATE TABLE IF NOT EXISTS {schema}.social_post (
    id BIGINT PRIMARY KEY,
    scope_key VARCHAR(128) NOT NULL,
    creator_id BIGINT NOT NULL REFERENCES {schema}.social_creator_monitor(id),
    platform VARCHAR(32) NOT NULL,
    post_key VARCHAR(256) NOT NULL,
    url TEXT NOT NULL,
    title TEXT,
    published_at TIMESTAMPTZ,
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_comment_sync_at TIMESTAMPTZ,
    availability_status VARCHAR(32) NOT NULL DEFAULT 'available',
    availability_checked_at TIMESTAMPTZ,
    availability_reason TEXT,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (scope_key, platform, post_key)
);

ALTER TABLE {schema}.social_post
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(32) NOT NULL DEFAULT 'available',
    ADD COLUMN IF NOT EXISTS availability_checked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS availability_reason TEXT;

CREATE TABLE IF NOT EXISTS {schema}.social_comment (
    id BIGINT PRIMARY KEY,
    scope_key VARCHAR(128) NOT NULL,
    post_id BIGINT NOT NULL REFERENCES {schema}.social_post(id),
    platform VARCHAR(32) NOT NULL,
    comment_key VARCHAR(256) NOT NULL,
    parent_comment_key VARCHAR(256),
    content TEXT NOT NULL,
    published_at TIMESTAMPTZ,
    like_count BIGINT,
    reply_count BIGINT,
    author_platform_id_hash CHAR(64),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_fetched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (scope_key, platform, comment_key)
);

CREATE INDEX IF NOT EXISTS idx_social_post_active
    ON {schema}.social_post (scope_key, platform, discovered_at DESC);

CREATE INDEX IF NOT EXISTS idx_social_comment_post
    ON {schema}.social_comment (scope_key, post_id, published_at DESC);
