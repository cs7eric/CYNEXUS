# Agent Reasoning Override

## Goal
让 AI Provider binding 的 reasoning 配置真正进入 Agent 模型请求；未配置时统一默认 `low`，并将 reasoning 相关常量放在合理的领域/适配器位置。

## Current Phase
Phase 5

## Phases

### Phase 1: Discovery
- [x] 读取项目规则、设计系统和既有会话状态
- [x] 定位 Agent UI、请求 DTO、领域模型解析链路
- **Status:** complete

### Phase 2: Implementation
- [x] 增加 UI reasoning 选择器与请求字段
- [x] 增加领域层运行时 reasoning 覆盖
- [x] 补充 focused test
- **Status:** complete

### Phase 3: Verification
- [x] 同步版本号
- [x] 运行 UI lint/build 与 Maven compile/test
- [x] 检查 diff 与工作区临时文件
- **Status:** complete

### Phase 4: Binding Runtime Integration
- [x] 确认 binding params 与 provider extra 的配置来源和优先级
- [x] 在 domain 层解析 binding reasoning，未配置默认 `low`
- [x] 在 chat/responses 两条适配器路径发送 reasoning 参数
- **Status:** complete

### Phase 5: Focused Verification
- [x] 补充 binding/default/request payload 测试
- [x] 同步版本并运行 UI/Maven 验证
- [x] 重启 Release 栈并验证 Agent 页面
- **Status:** complete

## Decisions Made
| Decision | Rationale |
|---|---|
| reasoning 作为本次 Agent run 的覆盖值 | Provider 默认值仍由 Provider 页面维护，Agent 选择不污染持久化配置 |
| 选择项为 Provider default / low / medium / high | 对齐既有 Provider 配置和 Responses API adapter 支持的值 |
| binding reasoning 优先于 provider 默认值，缺失时默认 `low` | 让路由配置可控且避免模型请求缺少明确 reasoning 参数 |

## Errors Encountered
| Error | Attempt | Resolution |
|---|---:|---|
| Impeccable script path from generic instructions did not exist in repo | 1 | Used installed skill path under `C:\Users\Cs7er\.codex\skills\impeccable` |
| Planning catchup path under `.claude` did not exist | 1 | Located installed script under `C:\Users\Cs7er\.agents\skills\planning-with-files` |
