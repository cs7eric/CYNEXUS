# Connections And Hooks

Connections describe external systems. Hooks create usable clients from those descriptions.

Do not read secrets directly inside pipelines. Use the runtime context to access hooks, and let hooks resolve clients from `conn_id`.

Recommended call shape:

```python
def sync_minute(context):
    source = context.hooks.eastmoney.get_client()
    writer = context.hooks.clickhouse.get_client()
    records = source.fetch_minute_kline(...)
    writer.write_kline_1m(records)
```

