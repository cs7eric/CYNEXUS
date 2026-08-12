# Findings

- `PortfolioImportConfirmationDomainService.toTrade` passes AI `fee` directly into `PortfolioTradingFeePolicy.resolve`.
- `PortfolioTradingFeePolicy.resolve` preserves any positive declared fee, so a model value equal to the gross amount is persisted as commission.
- `PortfolioImportProcessingDomainService.normalize` copies `fee` without normalization, so the bad value is also visible in the review screen.
- The screenshot symptom is consistent with gross amounts 436.00, 320.80, 599.90 and 596.40 being copied into the fee column.
- The configured fallback schedule is ETF minimum 0.10, stock minimum 0.50, or 0.854 per 10,000 above 10,000 gross amount.
- Prompt v6 requires scalar fields but does not explicitly forbid copying the 成交额 column into `fee` or define a visible-fee-only rule.
- Existing changed/untracked files belong to parallel work and must be preserved.
