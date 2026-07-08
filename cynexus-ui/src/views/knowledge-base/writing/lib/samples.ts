export const defaultMarkdown = `# Knowledge Base 文档

> 这里用于撰写知识库 Markdown 文档，支持表格、代码块、引用和基础排版。

## 文档要点

- 文档内容存储到 PostgreSQL。
- 后续可以接 ES 做全文检索。
- Markdown 编辑器保留 Typora 风格的实时编辑体验。

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| Document | Ready | title / description / content |
| Tag | Ready | 支持多标签映射 |
| Category | Ready | 支持分类映射 |

\`\`\`json
{
  "scene": "knowledge_base",
  "confidence": 0.82,
  "sections": ["summary", "details", "next_steps"]
}
\`\`\`

### 下一步

1. 接入保存能力。
2. 支持版本管理。
3. 接入搜索和召回。`

export const markdownSamples = [
  {
    label: "知识文档",
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

> Writing 页面当前只负责编辑器体验，后续再接保存链路。`,
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
