\set ON_ERROR_STOP on

ALTER TABLE cynexus_appl_core.data_source_health_check_item
    ADD COLUMN IF NOT EXISTS description text;

COMMENT ON COLUMN cynexus_appl_core.data_source_health_check_item.description IS
    'Human-readable data source capability description shown in Settings health-check UI.';
