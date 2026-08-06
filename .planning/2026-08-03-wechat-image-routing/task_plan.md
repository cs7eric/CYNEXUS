# Task Plan: WeChat Image Routing and History

## Goal

Add an extensible AI-backed image classification capability for WeChat image messages, route future image types through a registry-style policy, and expose WeChat event history under Settings in the UI. Reuse the existing AI quality-control/review flows; do not add a separate task-number confirmation protocol.

## Phases

- [completed] Phase 1: Inspect existing WeChat, AI vision, persistence, and Settings route contracts
- [completed] Phase 2: Implement classification capability, prompt, binding, validation, and WeChat orchestration contract
- [completed] Phase 3: Add WeChat event history API and UI route under Settings
- [completed] Phase 4: Add focused tests and documentation
- [completed] Phase 5: Run Maven/UI/version and Release verification

## Decisions

- Classification is a standalone capability: `wechat_image_route`.
- Classification output is an extensible route code plus confidence/signals; it does not parse or write finance/portfolio data.
- Existing accounting and portfolio AI QC/review flows remain the confirmation boundary.
- WeChat history is a read-only operational view; outbound delivery remains an external side effect and is not exposed as an autonomous Agent tool.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| `codegraph search` is not a supported CLI command | 1 | Use `codegraph query` after checking CLI help |
