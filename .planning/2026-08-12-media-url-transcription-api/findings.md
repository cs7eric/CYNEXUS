# Findings

- 现有 `POST /api/core/media-transcription/processes` 已经能接收原始 URL，并由定时调度器异步执行 Downloader 解析、Worker 转写、AI 润色、摘要和历史入库。
- UI 手动解析使用 `originDownloadAudioUrl -> originDownloadVideoUrl -> downloadVideoUrl -> downloadAudioUrl` 的媒体地址优先级。
- Core 的 `HttpMediaTranscriptionLinkResolver` 目前只读取 `downloadVideoUrl` 和 `downloadAudioUrl`，因此自动化 API 与 UI 的最终媒体地址可能不同。
- 新增 `/api/core/media-transcription/tasks/parse-url` 作为清晰的外部 URL-only 入口，复用 `MediaTranscriptionProcessAppService`，只返回入队确认；完整过程通过 `/processes` 查询。
- Settings 新增 `/settings/api-workflows`，按交易截图识别的执行记录模式展示任务列表、步骤、进度、结果、错误和重试。
