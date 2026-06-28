# storage/postgres

Postgres storage layer.

Use this folder for UI-serving repositories, metadata tables, task states, and upsert logic. Do not store large high-frequency raw records here.

Repositories own DDL application and small business-result snapshots. High-volume market facts stay in ClickHouse.

THS industry constituents are stored as lightweight snapshot/membership rows in `ths_industry_constituent_*`; the pipeline replaces only requested industry codes for a date/source so partial refreshes are safe.
