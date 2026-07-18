# Cynexus Release Runtime

Release is Docker-managed and uses the default `application.yml` from each starter. It runs on the stable ports used by the tunnel:

Starting Release automatically stops the Dev application processes first. Shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor containers remain running.

- Gateway: `18000`
- Core: `18080`
- AI: `18081`
- Market: `18082`
- UI: `5173`

Start the current Release Docker stack:

```powershell
.\deploy\release\start-release.ps1
```

Run a specific developed version as a Release build, for example `1.1.1-ALPHA-260716-RELEASE`:

```powershell
.\deploy\release\start-release.ps1 -Version 1.1.1-ALPHA-260716-RELEASE
```

Build the Maven artifacts and Docker images first:

```powershell
.\deploy\release\start-release.ps1 -Build
```

`-Version 1.1.1-ALPHA-260716-RELEASE` uses the matching `1.1.1-ALPHA-260716-SNAPSHOT` development artifacts and tags all Docker images as `1.1.1-ALPHA-260716-RELEASE`. When `-Version` is omitted, the script uses the current Maven base version and adds today's `YYMMDD` date when needed. Release is always Docker-managed. Development remains `1.1.1-ALPHA-260716-SNAPSHOT`.

Stop the Release Docker stack:

```powershell
.\deploy\release\stop-release.ps1
```

PostgreSQL, Redis, ClickHouse, Nacos, and the Python Data Executor are shared with the development environment through the external `cynexus-dev_default` Docker network. They must be running before the Release stack starts.
