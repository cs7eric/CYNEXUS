# sources

Business source adapters.

Sources adapt raw providers such as TDX, EastMoney, AkShare, Sina, Tencent, and local files into project-level records. A source may parse and normalize provider fields, but it should not write directly to ClickHouse or Postgres.

Data flow:

```text
provider client -> parser -> adapter -> raw/staging dataset
```

`provider_fallback.py` is the source-layer facade for ordered provider attempts. It returns rows plus metadata, while pipelines own persistence and job reporting.
