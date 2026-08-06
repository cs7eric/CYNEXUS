# 进度日志

## 2026-08-01

- 初始化本任务计划，准备先复用既有 AI 接入案例。
- 完成已有 `application_recent30` 与 `market_information_*` 链路调研。
- 新增期货 AI request/DTO、domain service、completion port、application adapter/controller、Agent capability handler，以及 prompt/binding migration。
- 前端已接入 IF/IH/IM/IC 白名单入口，专项 TypeScript 与 ESLint 检查通过。
- `pnpm build` 通过；Release 脚本已纳入期货 prompt migration，数据库中的 prompt 与 usage binding 已成功写入并可重复执行。
- Maven/前端版本已同步为 `1.1.391-SNAPSHOT`；首次标准 Release 构建在 Docker 基础镜像拉取阶段被本机代理 `127.0.0.1:7890` 阻断。
- 后续已使用本机已有 Release 基础镜像完成 `1.1.391-260801-RELEASE` 的 market、ai、ui 镜像构建并重启 Release 容器；AI capability 已在数据库注册，市场查询和 UI 页面均可访问。
- AI 真实调用链已验证到 Provider：IPv4 代理可连通，但当前可用 Provider 返回上游权限/服务错误（`custom-yancc` 返回 403 分组无权访问，`custom` 返回 403 分组停用，`xhhai` 返回 502），因此不是本功能链路或输入校验失败。
- 为避免不支持的品种被 Spring 当作 500，controller 已将 domain 的非法品种输入映射为 400；`XX` 实测返回 400。版本同步到 `1.1.392-SNAPSHOT`，AI/UI 运行镜像已更新。
- 已确认 xtyooo 对应 Provider 是 `custom`，并将期货 capability 的 usage binding 切换为 `custom / gpt-5.4-mini`；AI Provider 前端静态 capability routing 也已补齐，版本同步到 `1.1.394-SNAPSHOT`，AI 镜像已更新。
- 修复了 Provider 异常被包装成通用 HTTP 500 的问题：AI provider 错误现在返回 `AI_PROVIDER_ERROR`，页面会显示真实原因。当前 `custom/xtyooo` 的数据库 API key 实测返回 `GROUP_DISABLED`，所以调用仍需更新该 Provider 的有效 key/分组；HTTP 200 失败结果已验证可读。
