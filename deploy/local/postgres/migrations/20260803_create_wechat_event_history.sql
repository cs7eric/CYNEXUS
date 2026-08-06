\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_wechat;

CREATE TABLE IF NOT EXISTS cynexus_appl_wechat.wechat_event_history (
    id bigint PRIMARY KEY,
    message_id varchar(128),
    direction varchar(16) NOT NULL DEFAULT 'INBOUND',
    message_type varchar(32),
    event_type varchar(64),
    from_user_name varchar(128),
    to_user_name varchar(128),
    content text,
    media_id varchar(256),
    pic_url text,
    storage_path text,
    image_content_type varchar(128),
    image_size bigint,
    processing_status varchar(32) NOT NULL DEFAULT 'RECEIVED',
    route_code varchar(128),
    route_confidence numeric(6,5),
    route_reason varchar(2000),
    route_signals jsonb NOT NULL DEFAULT '[]'::jsonb,
    route_warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    reply_content text,
    error_message varchar(2000),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wechat_event_history_message_id
    ON cynexus_appl_wechat.wechat_event_history (message_id)
    WHERE message_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_wechat_event_history_occurred_at
    ON cynexus_appl_wechat.wechat_event_history (occurred_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_wechat_event_history_routing_queue
    ON cynexus_appl_wechat.wechat_event_history (processing_status, occurred_at, id)
    WHERE deleted_at IS NULL AND message_type = 'image';

COMMENT ON TABLE cynexus_appl_wechat.wechat_event_history IS
    'WeChat inbound event audit and image route classification history; raw user identifiers are not returned by the UI API.';
