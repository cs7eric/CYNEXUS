# pipelines/stock_basic

Stock metadata pipelines.

Use this folder for stock basic info, trading calendar, listing state, and other metadata sync workflows.

`sync_ths_industry_constituents.py` imports legacy per-industry THS constituent JSON or fetches explicit THS industry codes live. It keeps crawling in the source client, normalization in `domain.stock`, and persistence in the PostgreSQL repository.
