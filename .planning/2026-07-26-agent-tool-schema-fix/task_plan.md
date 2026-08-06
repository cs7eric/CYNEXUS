# Agent Tool Schema Fix

## Goal

Diagnose and fix invalid Agent tool arguments, especially `market.stock_research`, across the registered tool set.

## Phases

- [completed] Inspect tool registration, generated schemas, and handler argument parsing.
- [completed] Fix schema/argument contract mismatches and add regression coverage.
- [completed] Synchronize versions, rebuild, restart Release stack, and verify.
- [pending] Synchronize versions, rebuild, restart Release stack, and verify.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Planning skill path under repository was missing | 1 | Read the installed skill from `C:\\Users\\Cs7er\\.agents\\skills\\planning-with-files` |
| PowerShell stripped the `-D` prefix from an unquoted Maven property | 1 | Quote Maven `-D...` arguments before retrying |
