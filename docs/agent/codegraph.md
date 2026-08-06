# CodeGraph Inspection Rules

CodeGraph is the repository's local-first code intelligence index. The local `.codegraph/` database and WAL are machine-local and must not be committed.

## Required Order

- For code search, architecture questions, call-flow investigation, and change-impact assessment, query CodeGraph before repository-wide grep, rg, filesystem scans, or exploratory sub-agents.
- Use `codegraph status` first. If the index reports pending changes and the watcher has not caught up, run `codegraph sync`.
- Use `codegraph query <symbol>`, `codegraph callers <symbol>`, `codegraph callees <symbol>`, `codegraph impact <symbol>`, `codegraph files`, and `codegraph affected <files...>` as appropriate.
- Use `--json` when the result will be parsed by a script.
- If CodeGraph MCP tools are available, prefer `codegraph_explore`, `codegraph_search`, `codegraph_node`, `codegraph_callers`, `codegraph_callees`, `codegraph_impact`, `codegraph_files`, and `codegraph_status`.

## Interpretation

- Treat returned source as inspected. Read a file directly only when CodeGraph reports stale/pending content, the result is incomplete, exact live text is required, or dynamic dispatch cannot be resolved.
- Use `codegraph impact` before editing a shared symbol and inspect affected callers.
- After source changes, run `codegraph affected <files...>` or `git diff --name-only | codegraph affected --stdin` to select tests.
- CodeGraph is best-effort structural context; it does not replace compilation, tests, linting, or Release verification.

## Recovery

If `.codegraph/` is missing, initialize it with `codegraph init -i` and verify the result with `codegraph status`.
