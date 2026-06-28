# cynexus_data

Main package for Cynexus data ingestion and market-data processing.

Dependency direction:

```text
jobs -> pipelines -> sources/storage/domain/quality -> hooks -> connections -> config
```

Keep modules small and layer-specific. A lower layer should not import a higher layer.

