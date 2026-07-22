# Agent Persistence Contract Audit

This change hardens the PostgreSQL persistence boundary for the Agent workflow in
`cynexus_appl_ai`. It does not change the Maven or UI version.

## Owner boundary

- Every Agent `owner_key` column is `varchar(128) NOT NULL`.
- Transcript children (`ai_agent_message` and `ai_agent_run_step`) now carry
  `owner_key`, are backfilled from their parent, and use owner-qualified
  foreign keys.
- Message, step, event, task, and usage ordering/lookup indexes include the
  owner boundary where the lookup is owner-scoped.
- The repair migration drops the original unqualified child foreign keys before
  recreating them, so an upgrade cannot silently retain the weaker contract.

## Lease and idempotency

- A task can be `RUNNING` only while both lease fields are present; terminal and
  queued states cannot retain a lease.
- Claiming a task requires `attempt_count < max_attempts`.
- Expired running tasks at the attempt limit are atomically moved to
  `DEAD_LETTER`; an expired lease cannot be claimed for an extra attempt.
- Completion, retry, dead-letter, and run-link updates include `owner_key` and
  the lease owner.
- Run idempotency remains unique per owner through the partial unique index on
  `(owner_key, idempotency_key)`.

## Retention

The existing run retention flow now also archives terminal tasks and write
commands and redacts old write audit payloads. Existing run steps, transcript
messages, and public events remain owner-qualified while their sensitive payloads
are replaced by the retention marker. Feedback retention remains soft deletion
through its owner-scoped repository job.

## Verification evidence

- Focused Maven contract test: `AgentPersistenceSchemaContractTest`, 3 tests,
  0 failures.
- Live PostgreSQL migration: `20260722_repair_ai_agent_capability_runtime_state.sql`
  applied with `psql -v ON_ERROR_STOP=1`.
- Live contract script: `db/agent-persistence-contract-check.sql` passed.
- Live PostgreSQL 18.4 inspection confirmed 18 owner/tenant/principal columns at
  the expected lengths, owner-qualified child foreign keys, transcript unique
  constraints, lease/attempt checks, and owner-qualified indexes.
