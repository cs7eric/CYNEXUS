\set ON_ERROR_STOP on

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_custom_asset (
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    custom_food_id bigint NOT NULL,
    content_type text NOT NULL DEFAULT 'image/png' CHECK (content_type = 'image/png'),
    png_bytes bytea NOT NULL,
    sha256 varchar(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT pk_food_custom_asset PRIMARY KEY (owner_key, custom_food_id),
    CONSTRAINT fk_food_custom_asset_owner FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT ck_food_custom_asset_size CHECK (octet_length(png_bytes) BETWEEN 9 AND 8388608),
    CONSTRAINT ck_food_custom_asset_png CHECK (
        substring(png_bytes from 1 for 8) = decode('89504e470d0a1a0a', 'hex')
    )
);

COMMENT ON TABLE cynexus_appl_life.food_custom_asset IS
    'Owner-scoped cutout PNG artwork for custom foods. Image bytes are bounded and accessed only through the owning custom-food identity.';
COMMENT ON COLUMN cynexus_appl_life.food_custom_asset.sha256 IS
    'SHA-256 of the stored PNG bytes, also included in the custom-food idempotency fingerprint.';
