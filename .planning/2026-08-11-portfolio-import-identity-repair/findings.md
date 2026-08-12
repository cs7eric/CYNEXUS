# Findings

## Evidence

- Latest import `1786425423280` has `securityName` values `红星发展` and `中京电子`, but each normalized payload stores `symbol: null`.
- The parser warnings state that the screenshot does not display a security code; the active prompt correctly forbids guessing a missing code.
- The market search endpoint returns one active exact match for each name: `红星发展 -> 600367`, `中京电子 -> 002579`.
- `PortfolioCandidateEditRequest` already carries `securityName`, but `import-review.tsx` renders only the symbol input, so the extracted name is hidden.

## Design

- Preserve source payload values; enrich the review DTO with a code suggestion only when the name is an exact unique active stock-master result.
- Make the UI show an editable name below/alongside the code and visually label a suggested code until saved.
- Do not add an Agent tool: this review data is tied to an in-progress, user-confirmed write workflow and is not a safe autonomous read contract.

## Implemented

- `PortfolioSecuritySymbolResolver` resolves at most two exact active records from `sec_stock_basic` or `market_etf_catalog`; a suggestion is returned only when one distinct code remains.
- The review DTO exposes the suggestion separately from `normalizedPayload`, preserving the AI-extracted payload until the user saves or confirms.
- The import-review UI pre-fills and labels the suggestion, and now renders an editable security-name input beside the code.
