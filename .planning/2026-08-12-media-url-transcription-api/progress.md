# Progress

## 2026-08-12

- 已通过 CodeGraph 检查 URL 解析、媒体转写进程和调用方。
- 已定位需要复用的自动化 process 链路，以及 UI/Core Downloader 媒体地址选择差异。
- 新增 `POST /api/core/media-transcription/tasks/parse-url`，复用自动化 process 调度链路。
- Core Resolver 现在读取 `originDownloadAudioUrl` / `originDownloadVideoUrl`，并由领域策略按 UI 同样的优先级选择最终媒体 URL。
- 新增 `MediaTranscriptionProcessDomainPolicyTest` 覆盖媒体地址优先级和回退。
- 自动化 process 的 audio metadata 也改为使用原始音频直链优先级。
- 新增 URL API 文档，记录提交、轮询和幂等键。
- 验证通过：领域测试 2/2、Core Controller 编译、Core Infra 编译、UI 定向 ESLint、UI `pnpm build`。
- 版本保持 Maven/UI `1.1.398-SNAPSHOT`。
- 未纳入 Git 的 `artifacts/` Chrome 运行数据保持原样。
- URL API 现在返回 `accepted/message/processId/status`，不会等待后台处理。
- 新增 Settings / API Workflows 页面，复用 process 查询接口并对 queued/running 任务自动轮询。
