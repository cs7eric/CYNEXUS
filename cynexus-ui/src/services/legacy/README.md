# Legacy Python API Surfaces

这些 service 仍然指向 `VITE_API_BASE` 对应的旧 Python 接口，统一收口在 `src/services/legacy/*`。

当前页面依赖关系：

- `/stock-overview`
  - `market-overview.ts`
  - 旧市场情景组合接口，尚未迁到 Java
- `StockDetailDialog`
  - F10 已迁到 `src/services/market/f10.ts`
- `/mp4-to-word`
  - `mp4-workspace.ts`
  - 转写、SSE、导出历史、远程解析接管仍走旧接口
- `/mp4-to-word/history`
  - `mp4-history.ts`
  - 历史记录列表/详情/问答仍走旧接口
- `/downloader`
  - `downloader.ts`
  - 链接解析仍走独立旧服务

迁移约束：

- 页面代码不要再从 `@/lib/api` 直接取实现。
- 新接口优先放 `src/services/<domain>/*`。
- `src/lib/api.ts` 只保留兼容出口，不再承载实现。
