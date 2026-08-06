---
name: cynexus-version-sync
description: Use when changing project versions, preparing a Release image, or checking Maven and cynexus-ui package version consistency.
---

# Cynexus Version Synchronization

1. Read `deploy/AGENTS.md` and `docs/agent/release.md`.
2. Update the root project version, every Cynexus module parent/project version, and `cynexus-ui/package.json` together.
3. Keep source versions `x.y.z-SNAPSHOT` and do not rewrite third-party dependency versions.
4. Run `ci/verify-version-sync.ps1`.
5. Run `mvn -DskipTests compile` when the change is functional.
