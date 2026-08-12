# Trade Review Daily Replay Read Model

The Trade Review panel reads only post-close materialized data.  It never
calls TDX, THS, ELTDX, or another quote provider while a user moves a replay
cursor.

## Read path

`Redis day bundle -> PostgreSQL projection -> unavailable/partial response`

The authoritative replay date is the active daily K-line cursor.  All daily
history, pools, industry factors, and trend phases are filtered to that date
or earlier.  Range backfills run raw-1m trading dates in ascending order so a
later day cannot influence an earlier phase.

## Agent exposure decision

No autonomous Agent tool is exposed for this read model at present.  The
endpoint can return a full-market, 60-day payload containing large pools and
many industry rows; it is designed for the authenticated interactive Panel,
not bounded model invocation.  Until a separate request DTO defines strict
date/window/pool limits and a compact response schema, this data is explicitly
excluded from Agent tools.  The existing HTTP endpoint remains read-only.
