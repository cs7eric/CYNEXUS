# core

Shared low-level utilities.

This layer can hold trading calendar helpers, project exceptions, constants, retry helpers, time utilities, idempotency keys, and logging wrappers. It must not depend on concrete sources, hooks, or storage clients.

`calendar.py` contains local A-share trading-day rules and pure helpers. Pipeline code is responsible for passing any runtime overrides and for persistence.

`market_symbol.py` contains provider-neutral market symbol helpers used by source fallback and domain row conversion.
