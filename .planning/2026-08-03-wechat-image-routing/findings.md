# Findings: WeChat Image Routing and History

## Existing WeChat slice

- `cynexus-wechat` already parses image metadata, downloads media, stores a local copy, and replies with an image acknowledgment.
- The callback currently performs image storage synchronously and does not persist a durable event/history record.
- `WechatInteractionDomainService` owns inbound reply decisions; `WechatCallbackAppService` currently invokes image storage.

## Existing AI slice

- Existing vision capabilities are `accounting_receipt_parse` and `portfolio_trade_screenshot_parse`.
- `ScenarioExecutionDomainService` supports vision preparation, prompt resolution, model routing, and JSON validation.
- The new classifier must align capability code, prompt key, usage binding, execution log, and `cynexus-ui/src/services/ai/provider.ts` metadata.

## Existing UI slice

- Settings routes and sidebar entries are separate files and must be updated together.
- New page should use `WorkspaceShell`, semantic tokens, shared table/state primitives, and explicit loading/error/empty states.

## Constraints

- The worktree contains extensive unrelated user changes. Only files required for this task may be edited.
- Existing AI QC/review flows are the confirmation mechanism; the WeChat layer should report results and surface existing review links/state rather than inventing task-number confirmation.

## Implementation shape

- The AI service already exposes `/api/ai/executions/vision` but has no shared Feign contract. Add one in `cynexus-ai-api` and enable it only in the WeChat starter.
- `ScenarioExecutionDomainServiceImpl` currently maps unknown vision scenarios to accounting; it must explicitly support `wechat_image_route` and provide a DB-prompt-backed fallback.
- WeChat currently has no datasource. Add `cynexus-common-db`, a qualified `cynexus_appl_wechat` table, a durable polling worker, and a read-only `/api/wechat/history` endpoint.
- The worker should update route metadata only. It must not create finance/portfolio review or confirmation tasks in this slice.
- UI service prefixes currently have no WeChat entry; add one and place the page at `/settings/wechat-history` under the Settings sidebar group.
