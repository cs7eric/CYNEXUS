# Findings

- `cynexus-ui/src/services/ai/provider.ts` is the existing client for providers and `ai_usage_bindings`.
- Business-page provider changes must call that same client; request-local provider overrides would not synchronize with the AI Provider page.
- Existing PUT payloads need a full binding merge because the backend DTO update is not a partial-update contract.
- The Agent page already provides the desired compact, transparent Select styling.
