# Cynexus AI Agent Guide

AI changes cross Java domain code, provider routing, prompt data, migrations, and the UI Capability routing list. Treat them as one capability change.

Before editing AI code, read:

- `docs/agent/ai-capability.md`
- `docs/agent/backend-ddd.md`
- `docs/agent/agent-prompt-cache.md` for autonomous-agent or prompt-cache work
- `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md` when the Provider page changes

Every new or changed capability must keep these identifiers synchronized:

```text
capability code = ai_prompt.prompt_key = ai_usage_bindings.capability = UI promptKey
```

The change must include an idempotent migration seed, a binding with `prompt_id`, runtime resolution through binding -> prompt key -> fallback, and AI Provider routing metadata. Production prompt text must be DB-managed; Java text is only a behavior-preserving fallback.

Keep business policies, prompt resolution, parsing, routing, normalization, and lifecycle rules in `domain`, not AppService. Use `$cynexus-ai` for capability changes.

When a project data read/query method should be callable by the autonomous Agent, use `$cynexus-agent-tools`. Register only bounded read-only handlers in the autonomous tool registry. Mutations must use the separate preview -> explicit confirmation -> execute -> audit workflow; never expose repositories, mappers, arbitrary SQL, or generic CRUD directly to the model. The current write panel is user-initiated, not a model-visible Tool; a model-proposed write requires an explicit `NEEDS_CONFIRMATION` proposal state before it can be described as Agent-callable.
