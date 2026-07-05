export const defaultMarkdown = `# AI 研报摘要

> 这个页面用于验证 Markdown 渲染、GFM 表格、代码块和内容排版。

## 今日结论

- 市场强度继续修复，主线集中在 **AI 应用** 和 **算力基础设施**。
- 两融余额需要结合行业排名观察，避免只看单日增量。
- Prompt 输出建议走结构化 JSON，再落到 Markdown 展示层。

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| ChatClient | Wiring | 接入多模型路由 |
| Prompt | Ready | 已有 CRUD 和版本字段 |
| RAG | Planned | 等待知识库索引链路 |

\`\`\`json
{
  "scene": "market_review",
  "confidence": 0.82,
  "sections": ["summary", "risk", "watchlist"]
}
\`\`\`

### 下一步

1. 接入流式输出。
2. 支持复制渲染结果。
3. 补充 prompt 变量高亮。`

export const markdownSamples = [
  {
    label: "AI 研报",
    value: defaultMarkdown,
  },
  {
    label: "任务说明",
    value: `# Scheduler 任务说明

## 任务

- job code: \`margin_sync_daily\`
- 数据源: AkShare
- 写入: PostgreSQL

## 注意事项

如果 \`rows_written = 0\`，优先检查交易日期、接口返回和幂等逻辑。

> POC 页面只负责渲染，不在这里做业务 API 编排。`,
  },
  {
    label: "表格",
    value: `# 行业观察

| 行业 | 两融余额 | 融资买入 | 匹配股票 |
| --- | ---: | ---: | ---: |
| 半导体 | 128.42 亿 | 18.90 亿 | 94 |
| 软件开发 | 91.73 亿 | 12.41 亿 | 126 |
| 证券 | 78.66 亿 | 10.18 亿 | 51 |

**说明:** 表格走 GFM 渲染，右对齐列需要保留。`,
  },
]
