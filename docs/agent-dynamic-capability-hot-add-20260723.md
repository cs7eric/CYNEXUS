# Dynamic Agent Capability Hot-Add

## Scope

The Agent runtime now accepts a typed capability descriptor through a protected
administrator API. Registration updates the persisted governance catalog and the
in-process tool catalog in one application workflow; subsequent
Agent runs see the new descriptor without restarting `cynexus-ai`.

## API

Both endpoints require the existing signed identity headers:

- `X-Cynexus-Principal`
- `X-Cynexus-Tenant`
- `X-Cynexus-Identity-Signature`

The principal must be present in
`cynexus.ai.agent.governance-principals` (default `workspace-user`).

Register:

```http
POST /api/ai/agent/capabilities
```

Update an existing admin-owned descriptor:

```http
PUT /api/ai/agent/capabilities/{capabilityCode}
```

The JSON body contains the typed fields `capabilityCode`, `version`, `name`,
`module`, `description`, `inputSchemaJson`, `outputSchemaJson`, `readOnly`,
`requiresConfirmation`, `tenantScope`, `source`, `sourceUrl`, `freshness`,
`availability`, `governanceAvailability`, `enabled`, `priority`, `promptKey`,
`runtimeEntry`, and `routeGroup`.

`runtimeEntry` must identify an already registered `AgentToolHandler` bean. The
runtime validates that the handler is read-only, does not require confirmation,
and has exactly matching input and output JSON schemas. The descriptor is then
bound to that handler, so the API cannot introduce arbitrary Java classes,
URLs, SQL, MVC routes, or executable code.

## Lifecycle

1. The controller verifies the signed administrator identity.
2. The application service validates the descriptor and asks the runtime catalog
   to bind it to the handler allow-list.
3. The infrastructure repository upserts the descriptor with
   `registration_mode = ADMIN` and writes an audit event.
4. The runtime catalog replaces its immutable snapshot, so the next Agent turn
   immediately sees the new tool code.
5. Admin-owned descriptors are restored from PostgreSQL on the next process
   start; handler synchronization only retires `registration_mode = HANDLER`
   rows.

Use the existing governance endpoint to disable or mark a capability unavailable:

```http
POST /api/ai/agent/capabilities/{capabilityCode}/governance
```

Hot-add registration accepts only an active (`AVAILABLE`, enabled) descriptor.
This prevents a registration request from bypassing the governance catalog.

## Limitations

- This is descriptor hot-add, not arbitrary runtime code loading. A new data
  ability still needs a deployed `AgentToolHandler` implementation and must be
  included in the handler allow-list.
- Runtime handler schema changes require a new deployed handler version and a
  new descriptor version.
- The API intentionally has no delete endpoint; disable through governance and
  retain the audit trail.
- Provider/UI routing metadata synchronization remains an administrative
  responsibility; this change does not modify the UI.
