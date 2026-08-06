# Release and Version Rules

## Version Sync Rules

One version must be used everywhere:

- Root `pom.xml` project `<version>`.
- Parent `<version>` in every Cynexus module `pom.xml`.
- `cynexus-ui/package.json` `version`.

Source and Maven versions remain `x.y.z-SNAPSHOT`. The matching Release image/version is `x.y.z-YYMMDD-RELEASE`.

Never bump only the root POM or UI package. Third-party dependency versions must not be rewritten during project version synchronization.

For a deliberate version bump, update the root and every module parent/project version carefully, then update `cynexus-ui/package.json`. A PowerShell pattern for inspection is:

```powershell
$ver = "x.y.z-SNAPSHOT"
Get-ChildItem -Recurse -Filter pom.xml |
  Where-Object { $_.FullName -notmatch "\\target\\|\\node_modules\\" } |
  ForEach-Object {
    (Get-Content $_.FullName -Raw) -replace "<version>1\\.1\\.\\d+-SNAPSHOT</version>", "<version>$ver</version>" |
      Set-Content $_.FullName -NoNewline
  }
```

Prefer a structured or carefully scoped replacement limited to Cynexus project/parent versions. Do not rewrite third-party dependency versions.

Verify all Cynexus SNAPSHOT versions are identical, the UI package matches, and `mvn -DskipTests compile` succeeds.

## Runtime

- Do not start `deploy/dev/start-dev.ps1`, local Java processes, `cmd.exe`, Vite, or source-tree application runtimes.
- Release runs only from versioned Docker images and the built UI image.
- PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor are shared infrastructure and remain running.
- Only the Release application stack may be online.
- Use `deploy/release/start-release.ps1 -Version <version>` for every application restart.
- Use `deploy/release/stop-release.ps1` to stop the application stack.
- Rebuild/recreate and verify the Release stack after every functional, configuration, migration, or UI change; compilation and tests alone are insufficient.
- Deploy every service that could be affected, including dependent services. Use a full Release deployment when the impact boundary is uncertain; a narrow `-Services` or `-Changed` deployment is valid only when the complete affected-service set has been established.
- The normal completion command for a functional change is `deploy/release/start-release.ps1 -Build -Version <release-version>`. A running container does not contain source changes until its image is rebuilt and the container is recreated.

## Verification

Run focused checks first, then compile/build affected modules, synchronize versions, rebuild the appropriate Release images, start the versioned Release stack, and verify health and changed behavior. Report any check that could not be run.
