# Cynexus 前端 UI 实现 Guide

这份 guide 是基于当前 `cynexus-ui` 源码整理的实现规范。目标不是重新设计 UI，而是让新增页面看起来像同一个 Cynexus：克制、直接、专业、信息密度高，但不牺牲扫描效率。

设计系统的机器可读 token 在 [`DESIGN.md`](./DESIGN.md)，本文件负责解释如何在 React 页面中使用它们。

## 1. 先理解当前 UI

### 1.1 主设计系统

普通业务页的基准是：

- React 19 + Vite 8 + Tailwind CSS v4。
- shadcn New York 风格，Radix UI 提供交互行为。
- `src/index.css` 提供 CSS variables、Tailwind theme mapping、全局 reset 和字体声明。
- `src/components/ui` 是唯一优先复用的基础组件目录。
- `src/layout/workspace-shell.tsx` 是普通页面的统一应用壳层。
- 图标使用 `lucide-react`；别手画常规按钮图标。

### 1.2 三类视觉区域

项目不是完全同质化的单一页面，新增代码必须先判断属于哪一类：

1. **工作台主系统**：Finance、Market、Information、AI、Settings、Knowledge Base、Dashboard。使用中性色 token、`WorkspaceShell`、紧凑 panel、表格和明确状态。
2. **Application Hub**：`src/views/home.tsx` 使用小面积渐变 tile 和手绘 SVG 插画，作用是应用入口导航。只能用于入口页，不要传播到业务页。
3. **MP4 to Word 特例**：`src/views/mp4-to-word/styles.ts` 及 `src/index.css` 中的 `.result-box`、`.summary-box`、`.summary-panel` 属于转写/摘要工作流的旧玻璃拟态画布。它是 feature-local signature，不是全局设计语言。

新页面默认属于第 1 类。

## 2. 视觉基线

### 2.1 颜色

优先使用语义 class，不要直接写 hex：

```tsx
<section className="rounded-lg border border-border/60 bg-card text-card-foreground">
  <p className="text-sm text-foreground">主内容</p>
  <p className="text-xs text-muted-foreground">辅助信息</p>
</section>
```

核心 token：

| 用途 | class | 当前基线 |
| --- | --- | --- |
| 页面背景 | `bg-background` | 白色 |
| 主文字 | `text-foreground` | 近黑色 |
| 卡片 | `bg-card` | 白色 |
| 次级面 | `bg-muted` / `bg-muted/*` | 极浅灰 |
| 次级文字 | `text-muted-foreground` | 中灰 |
| 边框 | `border-border` | 低对比浅灰 |
| 主按钮 | `bg-primary text-primary-foreground` | 中性深色 |
| 失败/危险 | `destructive` / `red` / `rose` | 系统负向 |
| 成功/健康 | `emerald` | 系统正向 |
| 提醒/待处理 | `amber` | 警示 |
| 信息/市场蓝 | `blue` / `sky` | 信息或行情辅助色 |

A 股行情有一个必须保留的领域约定：**红色表示上涨，绿色表示下跌**。这和系统层面的 success/error 不同。行情方向必须同时显示数值正负、箭头、文字或标签，不允许只靠颜色表达。

新建共享模式不得使用 `bg-[#f9f9f9]`、`bg-[#f6f7f9]`、大量 `text-slate-*` 或散落的自定义 hex。若语义 token 不够，先判断是否真的需要新增全局 token；只有跨页面复用时才扩展 `src/index.css`。

### 2.2 字体与数字

- 正文和 UI 默认使用 Montserrat，回退到系统字体以及 `PingFang SC` / `Microsoft YaHei`。
- `src/index.css` 已声明 Montserrat 400/700，但当前仓库 `public/fonts/montserrat` 只有 README，字体文件缺失时会回退。发布前不要假设自托管字体一定存在。
- 页面标题通常 `text-2xl font-semibold tracking-tight`，约 24px。
- 区块标题一般 14–16px；正文 14px；辅助信息 12px；密集表头/徽标 10–11px。
- 股票代码、ID、金额、百分比、时间、耗时、token 数量使用 `font-mono tabular-nums`，数值右对齐。
- 业务产品页不要再引入展示型 serif、超大字号或 `clamp()` 标题。全局 `h1` 的旧规则属于历史兼容代码，新页面用显式 Tailwind class。

### 2.3 圆角、间距、深度

- 控件：`rounded-md`，约 8px。
- 普通 panel：`rounded-lg`，约 10px。
- 大面积入口 tile：`rounded-2xl` 只限 Home 或已有特例。
- Badge / 状态 chip：`rounded-full`。
- 页面顶层区块间距通常 `gap-6`；区块内部 `gap-4`；紧凑行 `gap-2`。
- 普通 panel 使用 `p-4` / `p-5`；紧凑数据行使用 `px-3 py-2` 或 `px-4 py-3`。
- 默认先使用边框和灰阶层次，阴影最多 `shadow-sm`。不要用重阴影制造“浮起来的卡片墙”。

## 3. 页面实现模板

### 3.1 壳层是必选项

普通路由必须从 `WorkspaceShell` 开始：

```tsx
import { WorkspaceShell } from "@/layout/workspace-shell"

export default function ExamplePage() {
  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="Example">
      {/* page content */}
    </WorkspaceShell>
  )
}
```

壳层当前行为：

- 桌面侧栏宽 16rem，收起为 3rem；移动端用 18rem Sheet。
- 顶部 header 高度 `h-16`，包含侧栏触发、Breadcrumb、全局命令入口。
- 默认内容 padding 为 `p-3 md:p-6`，顶层内容纵向 `gap-6`。
- `contentScrollable={false}` 只用于编辑器、双栏工作区、固定高度表格等由页面自己管理滚动的场景。
- `fullBleed` 只用于真正需要全屏画布的页面，例如 Application Analysis；普通业务页不要使用。
- 侧栏支持 `Ctrl+B` / `Cmd+B`，状态通过 cookie 保存；菜单组状态通过 localStorage 保存。不要在页面内另造一套侧栏状态。

新增页面时必须同步：

1. `src/router/index.tsx` 路由。
2. `src/config/sidebar.ts` 菜单入口。
3. Breadcrumb 的 section/page label。
4. 如果是 AI 能力，同时更新 AI Provider 页的 Capability routing 列表、runtime entry、prompt key、group、description。

### 3.2 推荐页面骨架

```tsx
<WorkspaceShell sectionLabel="Finance" pageTitle="Overview">
  <div className="flex w-full flex-col gap-6">
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <WalletCards className="size-4" />
          Finance
        </div>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Overview</h1>
        <p className="mt-1 max-w-2xl text-sm leading-6 text-muted-foreground">
          说明页面任务、数据范围和刷新语义。
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="secondary" size="sm">Refresh</Button>
        <Button variant="default" size="sm">Create</Button>
      </div>
    </header>

    <section className="grid gap-4 xl:grid-cols-2">
      {/* decision-relevant content first */}
    </section>
  </div>
</WorkspaceShell>
```

页面第一屏优先顺序：页面任务和数据时间点 → 主操作 → 筛选 → 最需要决策的数据 → 完整列表/详情。不要先放大面积装饰、营销式 hero 或无意义的 KPI 墙。

### 3.3 Panel 组合

优先用单层 section：

```tsx
<section className="overflow-hidden rounded-lg border border-border/60 bg-card">
  <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
    <div>
      <h2 className="text-sm font-semibold">Recent transactions</h2>
      <p className="mt-1 text-xs text-muted-foreground">Selected date range</p>
    </div>
    <Badge variant="secondary">24</Badge>
  </div>
  <div className="p-4">...</div>
</section>
```

如果 panel 内需要分组，用 `bg-muted/45` 的 inset row、分割线或 `divide-y`，不要再套完整 `Card`。`Card` 适用于确实需要独立 framed unit 的复用组件；默认 `Card` 自带 `rounded-xl border bg-card shadow-sm py-6`，密集页面通常需要明确 override。

## 4. 组件选型规则

### Button

使用 [`src/components/ui/button.tsx`](./src/components/ui/button.tsx)。**能用 `secondary` 就用 `secondary`。** 刷新、筛选、取消、重试、次要操作、工具栏文字按钮都写 `variant="secondary"`，不要省略 variant，也不要默认写成 `outline` 或 primary。

- `secondary`：默认选择。几乎所有非主提交按钮都用它。
- `default`：仅页面唯一的主要提交/确认动作，必须显式写 `variant="default"`。
- `ghost`：行操作、紧凑 toolbar、icon-only action。
- `destructive`：不可逆或高风险删除。
- `outline` / `link`：只有明确需要描边或行内文字链时才用；先考虑 secondary。
- `sm` / `icon-sm` 用于紧凑工具栏；`default` size 用于常规表单动作。
- icon-only 必须有 `aria-label`；不熟悉的图标补 `title` 或 Tooltip。
- loading 时保留按钮尺寸，只替换图标为 `Loader2` / `RefreshCw animate-spin`，不要让布局跳动。

### Input / Select / Tabs / Badge

- `Input` 默认 `h-9 rounded-md border-0 bg-muted/60 px-3`，焦点使用 ring。
- 筛选 Select 常用 `h-7` / `h-8 border-0 bg-muted/45 text-xs`。
- 统一使用 `Tabs`；默认是 muted segmented control，`line` 是带 active underline 的页面内导航。
- `Badge` 只承载短状态、数量、分类或来源，不放长句。
- 状态类文案只用英文，包括徽章、筛选器和 status chip。workflow / job 状态用 `Queued`、`Running`、`Waiting review`、`Success`、`Failed`、`Cancelled`。不要写 `已入队`、`处理中`、`待确认`、`已完成`、`成功`、`失败`、`已归档`。
- 筛选变化要同步重置分页；按钮/Select 的文字要明确表达结果，而不是技术实现。

### AI route selection

每个允许用户触发 AI capability 的页面或 panel，必须在对应操作旁或局部标题区域展示共享的 `AiBindingProviderSelect`。它同时提供 Provider 与 Model，并写入该 capability 的 `ai_usage_bindings` 路由；禁止再实现页面私有 selector、硬编码模型，或只展示不可编辑的路由名称。

- 传入精确的稳定 capability code，例如 `stock_selection_ai_screening` 或 `auction_analysis`。
- 每个可独立执行的 AI capability 都应拥有自己的 route selector；同一工作流且路由必须一致的多个 capability 可传入对齐的 capability 列表。
- 必须在执行前可见，并保留组件提供的 loading、保存失败状态；执行结果可用时继续展示实际返回的 provider/model。
- Provider 与 Model 仅管理运行路由；Prompt、fallback、timeout、reasoning 等治理参数继续在 AI Provider 设置中管理，除非 capability 显式支持用户级覆盖。

### Table / Chart / Drawer

- 统一使用 `Table`，它已有横向滚动容器；宽表保留可读的 `min-w-*`，不要强行压缩列。
- 表头小号、中等字重、muted；行用低对比 divider 和 `hover:bg-muted/50`。
- 数值列右对齐，使用 `font-mono tabular-nums`；身份列可 sticky，但 sticky 背景必须与表格一致。
- 图表首选使用 `src/components/ui/chart.tsx` 提供的 shadcn chart 组合：`ChartContainer` + `ChartConfig` + `ChartTooltipContent`，底层复用 Recharts 图元（`AreaChart`、`LineChart`、`BarChart`）。业务页面不要直接使用裸 `ResponsiveContainer`、`Tooltip` 或 `Legend`。
- 时间序列优先用 `AreaChart` / `LineChart`，分类比较优先用 `BarChart`；多条业务序列必须通过 `ChartConfig` 命名，并使用 `ChartTooltip`、`ChartLegendContent` 保持统一的 tooltip 和图例样式。
- 当时间序列需要保留全部日期类目、密集日期标签、双 Y 轴或数据缩放时，可以使用项目已安装的按需 ECharts 组合；必须复用 `ResizeObserver`、组件卸载时 `dispose`，并明确处理空数据、tooltip、图例和日期标签，不要全量引入 ECharts。
- 图表所在 panel 必须有标题、单位、日期范围、数据源/刷新语义，以及 loading/empty/error 状态。日期范围切换在桌面可用 `ToggleGroup`，窄屏使用 `Select` 或可横向滚动的紧凑 tabs。
- 查看行详情、成分股、历史记录优先 Drawer；创建/编辑/确认才用 Dialog。Dialog 必须有标题、稳定 footer、loading 和关闭路径。
- 任何 portal/dropdown 不要放在会裁剪内容的 `overflow-hidden` 容器内手工定位，复用 Radix 组件的 portal。

### 特殊双栏工作区

Knowledge Base 的左列表 + 右阅读区、AI Usage 的固定表格属于合法的 `contentScrollable={false}` 场景：外层 `h-full min-h-0`，每个 pane 自己 `overflow-auto`。这类页面必须处理小屏退化，不要把两个固定 pane 硬塞进手机宽度。

## 5. 数据状态和交互反馈

每个数据模块都必须有完整状态，不允许只实现 happy path：

| 状态 | 实现 | 要求 |
| --- | --- | --- |
| Loading | `Skeleton` | 保持最终布局几何；spinner 只放在正在执行的按钮内 |
| Error | `Alert` 或 `notification.danger` | 说明哪个模块失败，并提供 Refresh/Retry 或恢复动作 |
| Empty | 空状态 block | 说明为什么为空、当前范围、下一步怎么做 |
| Stale | badge/辅助文本 | 明确 snapshot、manual、provider unavailable、最后更新时间 |
| Saving | disabled + loading icon | 保持按钮宽度，保存结果用 inline 或 notification |
| Success | `notification.success` / `Saved` | 用与按钮相同的动词，例如 Save → Saved |
| Partial | 行级/字段级状态 | 不要把可用数据全部隐藏；标出未映射/未知/部分失败 |

动画只服务状态变化。常规 transition 控制在 150–250ms，遵守 `prefers-reduced-motion`；禁止页面加载 choreography、无意义的漂浮、弹跳和循环装饰动画。

## 6. 响应式和可访问性

- `md` 是桌面侧栏切换的重要边界；`sm` 处理按钮/筛选换行；`xl` 处理宽表、图表和多列分析区。
- 用 Grid 组织二维分析区，用 Flex 组织一维工具栏；工具栏用 `flex-wrap`，不要让按钮在窄屏互相覆盖。
- 动态文本的 flex/grid 子项加 `min-w-0`，标题用 `truncate` 或 `line-clamp-2`，长 ID 用 `break-all`。
- 宽表横向滚动，身份列可 sticky；不要为手机删除关键数据字段而不提供详情入口。
- 所有交互控件必须有 `focus-visible`；优先使用真正的 `<button>`、`<a>`、`<input>`，不要用 `div role="button"` 替代。
- icon-only control 需要 `aria-label`，form field 需要 label 或可关联的 `htmlFor`。
- 状态不可只靠颜色：为方向/健康/错误增加文字、图标、数值符号或结构位置。
- placeholder 不能承担唯一说明；错误文案说明原因和修复动作。

## 7. 代码组织与审查清单

### 代码位置

- 页面入口：`src/views/<feature>/index.tsx`；单文件简单页面可以直接使用已有约定。
- 页面私有 UI：`src/views/<feature>/components/`。
- 页面私有格式化、状态映射、纯函数：`src/views/<feature>/lib/`。
- 可跨页面复用的业务组件：`src/components/app/` 或 `src/components/cynexus/`。
- 通用交互 primitive：`src/components/ui/`。
- API/service 不要从 view 内重复拼接；复用 `src/services` 的领域服务。
- class 组合使用 `cn`；不要靠多层 selector 或 inline style 解决普通状态。

### 提交前检查

- [ ] 页面使用了 `WorkspaceShell`，并正确设置 section/page breadcrumb。
- [ ] 新路由和侧栏入口已同步。
- [ ] 使用了语义 token，而不是无理由的独立 hex、`slate` 或 `bg-[#...]`。
- [ ] 标题、辅助信息、来源、更新时间和主操作层级清楚。
- [ ] Loading / Error / Empty / Stale / Success 状态齐全。
- [ ] 表格数值、日期、ID 的对齐和字体稳定。
- [ ] 移动端没有溢出、重叠、按钮挤压或不可滚动区域。
- [ ] 键盘焦点、Tooltip、aria label 和非颜色状态表达齐全。
- [ ] 没有复制 MP4 玻璃拟态或 Home 渐变 tile 到普通业务页。
- [ ] 没有新增孤立的全局 CSS；必要 CSS 放在 feature 目录。
- [ ] 新增 AI 能力已加入 AI Provider Capability routing，并与 backend capability code 的 runtime、prompt、group、description 对齐。

## 8. 一句话判断

如果一个新页面去掉颜色和阴影后仍然能通过导航、标题、间距、表格结构、状态文字和操作位置被快速理解，它就符合 Cynexus 的 UI 习惯；如果它依赖渐变、卡片堆叠、超大数字或用户猜测自定义控件才能成立，就需要回到本 guide 重做信息结构。
