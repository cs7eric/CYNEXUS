# pipelines/msi

Market sentiment indicator pipelines.

Use this folder for MSI build and rebuild workflows that combine market data, domain logic, and storage output.

Factor producers are deliberately small:

- `build_ma_count` and `build_profit_effect` prepare price-strength, breadth, and profit-effect inputs.
- `build_turnover_activity`, `build_volatility_sentiment`, `build_risk_appetite`, `build_style_risk`, and `build_limit_emotion` migrate the remaining legacy MSI producers.
- `build_msi` only reads component tables or explicit request components and writes the composite index.

Keep historical percentile normalization outside source clients. Repair jobs may pass precomputed `score` or `score_overrides`; otherwise these producers persist neutral scores where a historical percentile store is not yet available.
