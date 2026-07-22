# Agent Task Persistence Release Verification

Date: 2026-07-23
Release: `1.1.183-260723-RELEASE`

## Scope

This verification covers only Agent task persistence: the `AgentTaskPO`/`AgentTaskMapper` contract, PostgreSQL task migrations, dead-letter transitions, expired-lease recovery, and owner-scoped state changes. Notification, UI, Provider, and dynamic capability behavior were not changed or included in this task.

## Changes Verified

- `ai_agent_task.last_error` is `text` in the idempotent task migration, and the migration upgrades an existing `varchar(1000)` column with `ALTER COLUMN last_error TYPE text`.
- `claimDue` first moves expired `RUNNING` rows at or above `max_attempts` to `DEAD_LETTER`, then claims only queued or expired-running rows whose `attempt_count` is below `max_attempts`.
- `complete`, `retry`, and `deadLetter` require both `owner_key`/`task_no` and the current `lease_owner`; deleted rows are excluded.
- The Release migration bundle includes the task and owner-constraint repair migrations.

## Test Evidence

Focused Maven test command:

```text
mvn -pl cynexus-ai/cynexus-ai-infra -am "-Dtest=AgentTaskPersistenceContractTest,AgentPersistenceSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: `8` tests, `0` failures, `0` errors.

The focused contract tests cover the `text` migration, Release registration, max-attempt dead-letter branch, eligible lease recovery branch, and owner/lease predicates for lifecycle updates.

## Release Evidence

Release was built and started with:

```text
deploy/release/start-release.ps1 -Version 1.1.183-260723-RELEASE -Build
```

The six application containers were running with the matching versioned images:

```text
cynexus-release-ui            cynexus-ui:1.1.183-260723-RELEASE
cynexus-release-ai            cynexus-ai:1.1.183-260723-RELEASE
cynexus-release-market        cynexus-market:1.1.183-260723-RELEASE
cynexus-release-knowledgebase cynexus-knowledgebase:1.1.183-260723-RELEASE
cynexus-release-core          cynexus-core:1.1.183-260723-RELEASE
cynexus-release-gateway       cynexus-gateway:1.1.183-260723-RELEASE
```

Health probes returned HTTP `200` for ports `18080`, `18081`, `18082`, `18000`, `18084`, and UI port `5173` while the full `1.1.183-260723-RELEASE` stack was starting.

Against the Release PostgreSQL instance, a rollback-wrapped SQL scenario produced:

```text
REL2-DEAD-LETTER    DEAD_LETTER  last_error_length=26  attempt_count=3
REL2-LEASE-RECOVERY RUNNING      lease_owner=worker-recovery attempt_count=2
REL2-LONG-ERROR     RUNNING      last_error_length=5000 lease_owner=worker-recovery attempt_count=2
wrong-owner update  UPDATE 0
correct-owner update UPDATE 1, status=CANCELLED
```

The transaction ended with `ROLLBACK`; no verification rows were retained.

After the final AI-only image rebuild, the new Agent capability runtime attempted to read a pre-existing `registration_mode` column that is absent from the current capability schema. This is outside task persistence and was intentionally not changed. The AI container was therefore restored to the previously healthy versioned `1.1.180-260722-RELEASE` image; the other Release containers remain on `1.1.183-260723-RELEASE`. The task persistence verification itself is independent of this dynamic-capability startup mismatch and passed against the Release PostgreSQL instance.

## Remaining Risk

The verification exercises mapper-equivalent SQL directly against PostgreSQL and the versioned Release runtime. It does not invoke an external AI Provider, and it does not claim notification delivery or UI workflow coverage. A follow-up dynamic-capability migration is required before the AI `1.1.183` image can replace the restored `1.1.180` runtime.
