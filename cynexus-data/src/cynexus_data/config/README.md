# config

Static configuration entrypoint.

Put non-sensitive connection declarations, source switches, table names, and write policies here. Secrets should be referenced through environment variables or a future secret resolver, not written directly into YAML.

Files:

- `settings.py`: environment-based runtime settings used by current jobs.
- `connections.yaml`: external system declarations.
- `sources.yaml`: source enablement, priority, fallback, and rate-limit policy.
- `database.yaml`: table names, database names, and write policy.
- `jobs.yaml`: Python job keys and Java-trigger parameter contracts.
