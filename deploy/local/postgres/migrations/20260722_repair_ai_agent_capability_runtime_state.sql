-- Runtime discovery must not permanently disable a capability through the governance flag.
-- Restore rows affected by the old synchronization behavior, while preserving an explicit
-- Provider-page disablement recorded after the latest live handler observation.
UPDATE cynexus_appl_ai.ai_agent_capability
SET is_enabled = true,
    availability = 'AVAILABLE',
    stale_at = now() + interval '2 hours',
    updated_at = now()
WHERE deleted_at IS NULL
  AND governance_availability = 'AVAILABLE'
  AND is_enabled = false
  AND availability IN ('AVAILABLE', 'STALE')
  AND NOT EXISTS (
      SELECT 1
      FROM cynexus_appl_ai.ai_agent_capability_audit governed
      WHERE governed.capability_code = ai_agent_capability.capability_code
        AND governed.action = 'GOVERNANCE_UPDATED'
        AND governed.occurred_at > COALESCE((
            SELECT max(observed.occurred_at)
            FROM cynexus_appl_ai.ai_agent_capability_audit observed
            WHERE observed.capability_code = ai_agent_capability.capability_code
              AND observed.version = ai_agent_capability.version
              AND observed.action = 'REGISTERED'
        ), '-infinity'::timestamptz)
  );
