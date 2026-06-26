# Cynexus Local Docker

Use `up-latest.ps1` when you want Docker services to run the newest source code.

```powershell
.\deploy\local\up-latest.ps1
```

It runs `mvn clean package -DskipTests`, then rebuilds and recreates `core`, `market`, `ai`, and `gateway` from the current workspace.

The local gateway is the single backend entrypoint:

```powershell
curl http://127.0.0.1:18000/api/core/info
curl http://127.0.0.1:18000/api/ai/config/providers
curl http://127.0.0.1:18000/api/market/data/overview-snapshots
```

For a full rebuild without Docker layer cache:

```powershell
.\deploy\local\up-latest.ps1 -NoCache
```
