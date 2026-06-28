# sources/tencent

Tencent source adapter.

Use this folder for Tencent client calls, parsers, schemas, and adapters. Keep fallback decisions in pipeline/config rather than inside low-level client code.

- K-line reads use `web.ifzq.gtimg.cn`.
- Stock fund-flow snapshots use `qt.gtimg.cn` quote rows plus `s_pk` disk-ratio rows.

Source modules fetch and parse only; persistence belongs in pipelines/storage.
