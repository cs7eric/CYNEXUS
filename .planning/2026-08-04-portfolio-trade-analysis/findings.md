# Findings

## Initial state

- The repository already contains a financial image analysis upload flow and a separate AI transaction recognition/import flow.
- The requested feature should connect those two existing capabilities, not introduce a separate image transport or persistence path.

## Existing contracts

- Financial image upload: `POST /api/core/finance/ai-captures` accepts multipart field `image`, persists a capture, and routes through `POST /api/ai/executions/vision` with capability `accounting_receipt_parse`.
- Trade image upload: `POST /api/market/portfolio/imports` already accepts multipart field `file`, persists an asynchronous import task, and the market vision gateway routes through the same AI vision endpoint with capability `portfolio_trade_screenshot_parse`.
- The trade route already has review/confirm persistence and duplicate protection. The missing piece is an API shape equivalent to the financial upload API, if the caller needs a direct image-analysis result rather than the asynchronous portfolio task.
- The trade capability, prompt, and binding already exist, so this change should reuse `portfolio_trade_screenshot_parse` rather than add a second AI capability.
