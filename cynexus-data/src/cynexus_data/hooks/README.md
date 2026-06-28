# hooks

Hook layer.

Hooks turn a `conn_id` into a usable client or session. Pipelines should receive hooks from runtime context instead of reading environment variables or creating raw clients directly.

Rules:

- Hooks may depend on `connections` and `config`.
- Hooks may create clients, sessions, engines, readers, or filesystem handles.
- Hooks should not implement source-specific business parsing or storage table logic.

