# hooks/storage

Hooks for storage systems.

Examples:

- `ClickHouseHook`
- `PostgresHook`
- `DuckDBHook`
- `RedisHook`

Storage readers and writers should receive clients from hooks instead of creating connections internally.

