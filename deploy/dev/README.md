# Cynexus Development Runtime

The existing `5173/18000/18080-18083` endpoints remain the runtime environment used by the tunnel. Release defaults live in each starter's `src/main/resources/application.yml`; development overrides live beside them in `application-dev.yml` and are activated with the `dev` profile. The dev environment uses separate application ports while sharing the existing PostgreSQL, Redis, ClickHouse, Nacos, and Python data executor instances.

Starting Dev automatically stops the Release Docker application stack first. Shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor containers remain running.

Start after a source change:

```powershell
.\deploy\dev\start-dev.ps1
```

Skip Maven packaging when the four starter JARs are already current:

```powershell
.\deploy\dev\start-dev.ps1 -SkipBuild
```

Stop dev services:

```powershell
.\deploy\dev\stop-dev.ps1
```

Dev endpoints:

- UI: `http://localhost:5174`
- Gateway: `http://127.0.0.1:28000`
- Core: `28080`
- AI: `28081`
- Market: `28082`

The dev gateway uses fixed localhost routes and does not register with Nacos, so it cannot receive runtime traffic. Because the databases are shared, avoid destructive migrations and use the existing schemas deliberately.
