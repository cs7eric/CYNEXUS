# Layering

The project follows this dependency direction:

```text
jobs -> pipelines -> sources/storage/domain/quality -> hooks -> connections -> config
```

Business data flow:

```text
source client -> parser/adapter -> raw dataset -> staging dataset -> domain calculation -> mart dataset -> storage writer
```

Rules:

- Jobs are thin entrypoints.
- Pipelines orchestrate work and receive runtime context.
- Sources adapt provider data but do not write storage.
- Storage writes tables but does not fetch external market data.
- Hooks create clients from connections.
- Connections describe external systems.
- Config holds non-sensitive declarations and env references.

