# AI Prompt and Capability Management

## Source of Truth

Never hardcode production prompt text only in Java. New or changed prompts must be managed in `cynexus_appl_ai.ai_prompt` and editable from the AI Prompt UI. Java text is permitted only as a behavior-preserving runtime fallback when the DB row is missing.

Use one stable key whenever possible:

```text
backend capability code
  = ai_prompt.prompt_key
  = ai_usage_bindings.capability
  = AI Provider routing promptKey
```

## Required Objects

Every new AI feature needs:

1. An `ai_prompt` row with `prompt_key`, `prompt_version`, `status=active`, `system_prompt`, optional `user_prompt_template`, `variables`, `output_schema`, `tags`, `extra`, and `remark`.
2. An `ai_usage_bindings` row with capability, provider, optional model override, `is_enabled=true`, `prompt_id` pointing to the prompt, and runtime params in `params` JSON.
3. AI Provider Capability routing registration with runtime entry, prompt key, group, and description.

## Migration Rules

- Seed through an idempotent SQL migration under `deploy/local/postgres/migrations/YYYYMMDD_*.sql`.
- Use `INSERT ... ON CONFLICT (prompt_key, prompt_version) ... DO UPDATE`.
- Set `status = 'active'` and declare template placeholders in `variables` JSON.
- Update `ai_usage_bindings.prompt_id` from the inserted prompt row.
- If Agent-related and applied by Release scripts, list the migration in `deploy/release/apply-agent-migrations.ps1`.
- Do not invent another prompt table. Version through `prompt_version` and bind the intended version by `prompt_id`.

## Runtime Resolution

Resolve in this order:

1. `ai_usage_bindings.prompt_id` -> active `ai_prompt` row.
2. Active `ai_prompt` by `prompt_key`.
3. Domain/service hardcoded fallback template.

Dynamic values belong in `{{var}}` placeholders and are rendered by domain policy/service. Do not concatenate business prompt sentences in Application Service. When wording changes, update the SQL seed/active row first and keep fallback behavior synchronized.

## Completion Checklist

- [ ] Capability code and UI routing agree.
- [ ] Prompt migration is present, idempotent, active, and applied.
- [ ] Usage binding has provider and `prompt_id`.
- [ ] Runtime loads binding -> key -> fallback.
- [ ] Prompt UI can edit/publish wording without redeploy.
- [ ] Focused domain tests cover resolution and fallback.
- [ ] Maven and UI versions are synchronized.
