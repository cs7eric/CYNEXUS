import { useCallback, useEffect, useMemo, useState } from "react"
import { createPortal } from "react-dom"
import { useNavigate } from "react-router-dom"
import {
  Activity,
  Compass,
  Database,
  FileText,
  History,
  ListFilter,
  LayoutDashboard,
  LineChart,
  MessageSquareText,
  Newspaper,
  Search,
  Server,
  Sparkles,
  Star,
  TrendingUp,
  WalletCards,
  Workflow,
} from "lucide-react"

import { cn } from "@/lib/utils"
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import {
  fetchSelfSelectedGroups,
  fetchSelfSelectedItems,
  type SelfSelectedGroup,
  type SelfSelectedItem,
} from "@/services/market/self-selected"
import {
  fetchApplicationAnalysisTargets,
  type ApplicationAnalysisTarget,
} from "@/services/ai/application-analysis"
import {
  listMP4History,
} from "@/services/core/mp4-history"
import type { MP4HistoryListItem } from "@/lib/history-types"
import { fetchPortfolioTrades } from "@/services/market/portfolio/portfolio"
import type { PortfolioTrade } from "@/services/market/portfolio/types"
import { searchStockChart } from "@/services/market/stock-chart"
import type { StockSearchItem } from "@/views/stock-chart/lib/types"

interface PaletteItem {
  id: string
  label: string
  hint?: string
  group: string
  icon: React.ComponentType<{ className?: string }>
  to: string
  keywords: string
}

const STATIC_NAV_ITEMS: PaletteItem[] = [
  { id: "page-home", label: "Applications · 首页", group: "Core", icon: LayoutDashboard, to: "/", keywords: "home applications app launcher 首页 应用" },
  { id: "page-dashboard", label: "Dashboard", group: "Core", icon: LayoutDashboard, to: "/dashboard", keywords: "dashboard overview metrics home 总览 仪表盘" },
  { id: "page-agent", label: "Cynexus Agent", group: "Core", icon: Sparkles, to: "/cynexus-agent", keywords: "agent autonomous assistant ai 智能体 助手" },
  { id: "page-trade-review", label: "Trade View", group: "Core", icon: LineChart, to: "/trade-review", keywords: "trade view review trading 交易 复盘" },
  { id: "page-screener", label: "Cynexus Screener", group: "Core", icon: ListFilter, to: "/cynexus-screener", keywords: "screener screen stocks filter 选股 筛选" },
  { id: "page-finance", label: "Finance · Overview", group: "Finance", icon: WalletCards, to: "/finance/overview", keywords: "finance overview accounting 财务 总览" },
  { id: "page-finance-ledger", label: "Finance · Ledger", group: "Finance", icon: WalletCards, to: "/finance/ledger", keywords: "finance ledger account 财务 台账" },
  { id: "page-finance-transactions", label: "Finance · Transactions", group: "Finance", icon: WalletCards, to: "/finance/transactions", keywords: "finance transactions expense income 财务 交易 收支" },
  { id: "page-finance-ai-capture", label: "Finance · AI Capture", group: "Finance", icon: Sparkles, to: "/finance/ai-capture", keywords: "finance ai capture receipt extraction 财务 AI 录入 发票" },
  { id: "page-downloader", label: "Downloader", group: "Tools", icon: Sparkles, to: "/downloader", keywords: "downloader media video audio download 下载 媒体" },
  { id: "page-mp4", label: "MP4 to Word · Workspace", group: "Tools", icon: History, to: "/mp4-to-word", keywords: "mp4 word transcript transcription parse video 转写 文稿" },
  { id: "page-mp4-history", label: "MP4 · History", group: "Tools", icon: History, to: "/mp4-to-word/history", keywords: "mp4 history archive transcript 历史 归档" },
  { id: "page-mp4-process-history", label: "MP4 · Process History", group: "Tools", icon: History, to: "/mp4-to-word/process-history", keywords: "mp4 process history jobs tasks 处理历史 任务" },
  { id: "page-scheduler", label: "Settings · Scheduler", group: "Tools", icon: Sparkles, to: "/settings/scheduler", keywords: "scheduler settings cron job 调度 任务 设置" },
  { id: "page-self-selected", label: "Self-Selected · 自选", group: "Stock", icon: Star, to: "/stock-overview/self-selected", keywords: "self selected watchlist favorites 自选 分组" },
  { id: "page-stock-review", label: "Stock Review", group: "Stock", icon: LineChart, to: "/stock-review", keywords: "stock review 复盘 股票 评审" },
  { id: "page-stock-overview", label: "Stock Overview · 市场情景", group: "Market", icon: LineChart, to: "/stock-overview", keywords: "market regime overview 大盘 市场概览 情景" },
  { id: "page-stock-overview-market", label: "Stock Overview · Market Pulse", group: "Market", icon: LineChart, to: "/stock-overview/market", keywords: "market pulse overview 大盘 市场脉搏" },
  { id: "page-market-overview", label: "Market · Overview", group: "Market", icon: LineChart, to: "/market/overview", keywords: "market overview 大盘 市场总览" },
  { id: "page-market-pulse", label: "Market Pulse · 市场脉搏", group: "Market", icon: TrendingUp, to: "/market/pulse", keywords: "market pulse breadth industry fund flow 涨跌 行业 资金 市场脉搏" },
  { id: "page-market-sentiment", label: "Market Sentiment · MSI", group: "Market", icon: LineChart, to: "/market/sentiment", keywords: "market sentiment msi emotion risk appetite 市场情绪 情绪指数" },
  { id: "page-market-margin-trading", label: "Market Margin Trading · 两融", group: "Market", icon: LineChart, to: "/market/margin-trading", keywords: "margin trading financing securities lending 两融 融资融券 余额" },
  { id: "page-market-etf", label: "Market ETF · ETF 资金方向", group: "Market", icon: TrendingUp, to: "/market/etf", keywords: "etf fund flow share subscription redemption premium iopv rotation ETF 资金 申赎 折溢价 轮动" },
  { id: "page-market-futures", label: "Market · Futures Position", group: "Market", icon: TrendingUp, to: "/market/futures-position", keywords: "futures position open interest market 期货 持仓" },
  { id: "page-personal-portfolio", label: "Personal Portfolio", group: "Market", icon: WalletCards, to: "/personal-portfolio", keywords: "personal portfolio holdings positions watchlist 个人投资组合 持仓" },
  { id: "page-market-indicator", label: "Data Collector · Market Indicator · 盘中异动", group: "Data Collector", icon: Activity, to: "/data-collector/market-indicator", keywords: "data collector market indicator realtime live limit up limit down rapid rise fall 盘中 异动 涨停 跌停 拉升 打压" },
  { id: "page-limit-board-monitor", label: "Data Collector · Limit Board Monitor · 封板监控", group: "Data Collector", icon: Activity, to: "/data-collector/limit-board-monitor", keywords: "data collector limit board monitor sealed amount queue trend morning afternoon 涨停 跌停 封板额 封单 趋势" },
  { id: "page-information", label: "Information · Feed", group: "Information", icon: Newspaper, to: "/information", keywords: "information feed news market news 资讯 新闻" },
  { id: "page-information-analyzed", label: "Information · AI analyzed", group: "Information", icon: Sparkles, to: "/information/analyzed", keywords: "information analyzed ai news 资讯 AI 分析" },
  { id: "page-social-monitoring", label: "Information · Social Monitoring", group: "Information", icon: MessageSquareText, to: "/information/social-monitoring", keywords: "social monitoring sentiment mentions 社交监控" },
  { id: "page-app-analysis", label: "Application Analysis", group: "Market", icon: Compass, to: "/stock-overview/application-analysis", keywords: "application analysis theme 题材 120 日 四段 分析" },
  { id: "page-industry-application", label: "Industry / Concept · 行业概念", group: "Market", icon: Compass, to: "/stock-overview/industry-application", keywords: "industry concept sector heatmap 行业 概念 板块 sh8803 sh8804 eltdx 热力图" },
  { id: "page-knowledge-base", label: "Knowledge Base", group: "Knowledge", icon: Database, to: "/knowledge-base/view", keywords: "knowledge base documents notes 知识库 文档" },
  { id: "page-knowledge-writing", label: "Knowledge Base · Writing", group: "Knowledge", icon: FileText, to: "/knowledge-base/writing", keywords: "knowledge writing editor documents 知识库 写作" },
  { id: "page-ai-provider", label: "AI · Provider", group: "AI", icon: Sparkles, to: "/ai/provider", keywords: "ai provider model routing AI 提供商 模型" },
  { id: "page-ai-prompt", label: "AI · Prompt", group: "AI", icon: Sparkles, to: "/ai/prompt", keywords: "ai prompt template AI 提示词" },
  { id: "page-ai-usage", label: "AI · Usage", group: "AI", icon: Activity, to: "/ai/usage", keywords: "ai usage tokens cost AI 用量 token" },
  { id: "page-settings-data-source", label: "Settings · Data Source", group: "Settings", icon: Database, to: "/settings/data-source", keywords: "settings data source connection 数据源 设置" },
  { id: "page-settings-provider", label: "Settings · Provider", group: "Settings", icon: Server, to: "/settings/provider", keywords: "settings provider integration 服务提供商 设置" },
  { id: "page-settings-wechat", label: "Settings · 微信历史事件", group: "Settings", icon: MessageSquareText, to: "/settings/wechat-history", keywords: "wechat history events 微信 历史 事件" },
  { id: "page-settings-api-workflows", label: "Settings · API Workflows", group: "Settings", icon: Workflow, to: "/settings/api-workflows", keywords: "api workflows automation settings API 工作流 自动化" },
  { id: "page-poc", label: "POC Lab", group: "Dev", icon: Sparkles, to: "/poc", keywords: "poc lab prototype demo 实验" },
  { id: "page-heatmap-demo", label: "Heatmap Demo", group: "Dev", icon: LineChart, to: "/heatmap-demo", keywords: "heatmap demo treemap 热力图 demo" },
  { id: "page-heatmap-debug", label: "Heatmap Data Debug", group: "Dev", icon: LineChart, to: "/heatmap-data-debug", keywords: "heatmap data debug treemap 热力图 数据 调试" },
]

const PALETTE_CACHE_TTL_MS = 30_000

// Common short aliases are intentionally kept close to the command palette so
// users can jump to a holding without first remembering its numeric code.
const STOCK_NAME_ALIASES: Record<string, string[]> = {
  "华森药业": ["hsyy"],
  "运盛医疗": ["ysyl", "ysly"],
}

interface PaletteData {
  selfSelected: SelfSelectedItem[]
  groups: SelfSelectedGroup[]
  analysis: ApplicationAnalysisTarget[]
  recentMp4: MP4HistoryListItem[]
  portfolioTrades: PortfolioTrade[]
  fetchedAt: number
}

function normalizeSelfSelectedTarget(item: SelfSelectedItem) {
  const symbol = item.symbol.trim().toUpperCase()
  const targetType =
    item.target_type ?? (item.market?.toUpperCase() === "HK" ? "hk_stock" : "stock")
  const market = (item.market || targetType).trim().toUpperCase()
  return {
    key: `${targetType}:${market}:${symbol}`,
    symbol,
    targetType,
  }
}

function formatGroupHint(groupNames: string[]) {
  const names = Array.from(new Set(groupNames.filter(Boolean)))
  if (names.length === 0) return undefined
  if (names.length === 1) return names[0]
  return `${names[0]} +${names.length - 1}`
}

// ---- 模块级 store（singleton open 状态 + 跨组件触发） ----
type Listener = (open: boolean) => void
let openState = false
const listeners = new Set<Listener>()
const emit = () => listeners.forEach((l) => l(openState))
const setOpenState = (next: boolean) => {
  if (next === openState) return
  openState = next
  emit()
}

export function openGlobalCommand() {
  setOpenState(true)
}
export function closeGlobalCommand() {
  setOpenState(false)
}
export function toggleGlobalCommand() {
  setOpenState(!openState)
}

export function GlobalCommandTrigger({ className }: { className?: string }) {
  return (
    <button
      type="button"
      onClick={openGlobalCommand}
      className={cn(
        "inline-flex h-9 items-center gap-2 rounded-md border border-border/70 bg-muted/40 px-2 text-sm text-muted-foreground transition-colors hover:bg-muted/80 hover:text-foreground sm:px-3",
        className,
      )}
    >
      <Search className="size-4" />
      <span className="hidden sm:inline">搜索</span>
      <span className="ml-0 hidden items-center gap-0.5 rounded border border-border/70 bg-background/80 px-1 py-0.5 font-mono text-[10px] sm:inline-flex sm:ml-1">
        Alt K
      </span>
    </button>
  )
}

export function GlobalCommandPalette() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(openState)
  const [data, setData] = useState<PaletteData | null>(null)
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [stockSearchResults, setStockSearchResults] = useState<StockSearchItem[]>([])

  // 订阅 singleton 状态
  useEffect(() => {
    const l: Listener = (next) => setOpen(next)
    listeners.add(l)
    return () => {
      listeners.delete(l)
    }
  }, [])

  // 全局快捷键：Alt+K
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (
        e.altKey &&
        !e.metaKey &&
        !e.ctrlKey &&
        !e.shiftKey &&
        e.key.toLowerCase() === "k"
      ) {
        e.preventDefault()
        toggleGlobalCommand()
      }
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [])

  const ensureData = useCallback(async () => {
    if (data && Date.now() - data.fetchedAt < PALETTE_CACHE_TTL_MS) return
    setLoading(true)
    try {
      const [groupsRes, itemsRes, analysisRes, historyRes, portfolioTradesRes] = await Promise.allSettled([
        fetchSelfSelectedGroups(),
        fetchSelfSelectedItems(),
        fetchApplicationAnalysisTargets(),
        listMP4History(),
        fetchPortfolioTrades(),
      ])

      setData({
        groups:
          groupsRes.status === "fulfilled" && groupsRes.value.ok
            ? groupsRes.value.items
            : [],
        selfSelected:
          itemsRes.status === "fulfilled" && itemsRes.value.ok
            ? itemsRes.value.items
            : [],
        analysis:
          analysisRes.status === "fulfilled" ? analysisRes.value.items || [] : [],
        recentMp4:
          historyRes.status === "fulfilled" ? historyRes.value.slice(0, 8) : [],
        portfolioTrades:
          portfolioTradesRes.status === "fulfilled" ? portfolioTradesRes.value : [],
        fetchedAt: Date.now(),
      })
    } catch {
      setData({
        groups: [],
        selfSelected: [],
        analysis: [],
        recentMp4: [],
        portfolioTrades: [],
        fetchedAt: Date.now(),
      })
    } finally {
      setLoading(false)
    }
  }, [data])

  useEffect(() => {
    if (open) void ensureData()
  }, [open, ensureData])

  useEffect(() => {
    const query = searchQuery.trim()
    if (!open || query.length < 2) {
      setStockSearchResults([])
      return
    }
    const aliasName = Object.entries(STOCK_NAME_ALIASES).find(([, aliases]) =>
      aliases.some((alias) => alias === query.toLowerCase()),
    )?.[0]
    let active = true
    const timer = window.setTimeout(() => {
      void searchStockChart(aliasName ?? query)
        .then((items) => {
          if (active) setStockSearchResults(items.filter((item) => item.target_type === "stock" || item.target_type === "etf"))
        })
        .catch(() => {
          if (active) setStockSearchResults([])
        })
    }, 180)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [open, searchQuery])

  const handleSelect = useCallback(
    (to: string) => {
      closeGlobalCommand()
      navigate(to)
    },
    [navigate],
  )

  const handleOpenChange = useCallback((next: boolean) => {
    setOpenState(next)
  }, [])

  const items = useMemo<PaletteItem[]>(() => {
    if (!data) return STATIC_NAV_ITEMS
    const groupNameById = new Map(data.groups.map((g) => [g.id, g.name]))

    const uniqueSelfSelected = new Map<
      string,
      { item: SelfSelectedItem; groupNames: string[] }
    >()
    for (const item of data.selfSelected) {
      const target = normalizeSelfSelectedTarget(item)
      const groupName = groupNameById.get(item.group_id) ?? ""
      const existing = uniqueSelfSelected.get(target.key)
      if (existing) {
        existing.groupNames.push(groupName)
      } else {
        uniqueSelfSelected.set(target.key, { item, groupNames: [groupName] })
      }
    }

    const selfSelected: PaletteItem[] = Array.from(uniqueSelfSelected.values()).map(({ item, groupNames }) => {
      const target = normalizeSelfSelectedTarget(item)
      const market = (item.market || target.targetType).trim().toUpperCase()
      const hint = formatGroupHint(groupNames)
      return {
        id: `self-${target.key}`,
        label: `${item.name || target.symbol} · ${target.symbol}`,
        hint,
        group: "自选",
        icon: Star,
        to: `/stock-overview/application-analysis?target_type=${encodeURIComponent(target.targetType)}&symbol=${encodeURIComponent(target.symbol)}&name=${encodeURIComponent(item.name || target.symbol)}&market=${encodeURIComponent(market)}`,
        keywords: `${target.symbol} ${item.name ?? ""} ${groupNames.join(" ")} watchlist 自选`,
      }
    })

    const analysis: PaletteItem[] = data.analysis.map((it) => ({
      id: `analysis-${it.id}`,
      label: `${it.name} · ${it.symbol}`,
      hint: it.target_type === "index" ? "指数" : "个股",
      group: "Application Analysis",
      icon: Compass,
      to: `/stock-overview/application-analysis?target=${encodeURIComponent(it.id)}`,
      keywords: `${it.symbol} ${it.name} analysis 题材 120 日`,
    }))

    const mp4: PaletteItem[] = data.recentMp4.map((it) => ({
      id: `mp4-${it.id}`,
      label: it.title || it.task_id,
      hint: it.status,
      group: "MP4 历史",
      icon: History,
      to: `/mp4-to-word/history/${encodeURIComponent(it.id)}`,
      keywords: `${it.title ?? ""} ${it.task_id} mp4 history`,
    }))

    const latestTradeDateBySymbol = new Map<string, string>()
    const tradeNameBySymbol = new Map<string, string>()
    for (const trade of data.portfolioTrades) {
      const current = latestTradeDateBySymbol.get(trade.symbol)
      const date = trade.tradedAt.slice(0, 10)
      if (!current || date > current) latestTradeDateBySymbol.set(trade.symbol, date)
      if (trade.securityName?.trim()) tradeNameBySymbol.set(trade.symbol, trade.securityName.trim())
    }
    const latestPortfolioDate = Array.from(latestTradeDateBySymbol.values()).sort().at(-1) ?? ""
    const tradeReview: PaletteItem[] = Array.from(latestTradeDateBySymbol, ([tradeSymbol, date]) => {
      const name = tradeNameBySymbol.get(tradeSymbol) ?? tradeSymbol
      const aliases = STOCK_NAME_ALIASES[name] ?? []
      return {
        id: `trade-review-${tradeSymbol}`,
        label: `${name} · ${tradeSymbol}`,
        hint: `${date} · Trade Review`,
        group: "Trade Review",
        icon: LineChart,
        to: `/trade-review?symbol=${encodeURIComponent(tradeSymbol)}&date=${encodeURIComponent(date)}&open=1`,
        keywords: `${tradeSymbol} ${name} ${aliases.join(" ")} trade review 交易复盘 最新交易日`,
      }
    })

    const searchedTradeReview: PaletteItem[] = stockSearchResults.map((item) => {
      const targetType = item.target_type === "etf" ? "ETF" : "STOCK"
      const date = latestTradeDateBySymbol.get(item.symbol) ?? latestPortfolioDate
      const aliases = STOCK_NAME_ALIASES[item.name] ?? []
      return {
        id: `trade-review-search-${item.target_type}-${item.symbol}`,
        label: `${item.name} · ${item.symbol}`,
        hint: date ? `${date} · Trade Review` : "Trade Review",
        group: "Trade Review",
        icon: LineChart,
        to: `/trade-review?symbol=${encodeURIComponent(item.symbol)}&type=${targetType}&${date ? `date=${encodeURIComponent(date)}&` : ""}open=1`,
        keywords: `${item.symbol} ${item.name} ${aliases.join(" ")} trade review 交易复盘 最新交易日`,
      }
    })

    // Keep the existing navigation order when idle; promote Trade Review only
    // while the user is actively searching for a stock.
    const mergedTradeReview = [...searchedTradeReview, ...tradeReview].filter(
      (item, index, list) => list.findIndex((candidate) => candidate.id === item.id) === index,
    )
    const rest = [...STATIC_NAV_ITEMS, ...selfSelected, ...analysis, ...mp4]
    return searchQuery.trim()
      ? [...mergedTradeReview, ...rest]
      : [...STATIC_NAV_ITEMS, ...selfSelected, ...analysis, ...tradeReview, ...mp4]
  }, [data, searchQuery, stockSearchResults])

  const groups = useMemo(() => {
    const map = new Map<string, PaletteItem[]>()
    for (const it of items) {
      const arr = map.get(it.group) ?? []
      arr.push(it)
      map.set(it.group, arr)
    }
    return Array.from(map.entries())
  }, [items])

  const visibleGroups = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    if (!query) return groups
    return groups
      .map(([groupName, groupItems]) => [
        groupName,
        groupItems.filter((item) =>
          `${item.label} ${item.hint ?? ""} ${item.keywords}`.toLowerCase().includes(query),
        ),
      ] as [string, PaletteItem[]])
      .filter(([, groupItems]) => groupItems.length > 0)
  }, [groups, searchQuery])

  if (typeof document === "undefined") return null

  return createPortal(
    <CommandDialog
      open={open}
      onOpenChange={handleOpenChange}
      title="全局搜索"
      description="Alt + K 唤起，搜索页面 / 自选 / 分析标的 / MP4 历史"
      className="w-[calc(100vw-1rem)] border-0 shadow-2xl sm:max-w-[840px] lg:max-w-[960px]"
    >
      <CommandInput
        autoFocus
        onValueChange={setSearchQuery}
        placeholder="搜索页面、股票、代码、拼音... (Alt + K)"
        className="border-0"
      />
      <CommandList className="max-h-[min(72vh,640px)]">
        <CommandEmpty>
          {loading ? "正在拉取数据..." : "未找到结果"}
        </CommandEmpty>
        {visibleGroups.map(([groupName, groupItems]) => (
          <CommandGroup key={groupName} heading={groupName}>
            {groupItems.map((it) => {
              const Icon = it.icon
              return (
                <CommandItem
                  key={it.id}
                  value={`${it.label} ${it.hint ?? ""} ${it.keywords}`}
                  onSelect={() => handleSelect(it.to)}
                >
                  <Icon className="mr-2 size-4 text-slate-500" />
                  <span className="flex-1 truncate">{it.label}</span>
                  {it.hint ? (
                    <span className="ml-2 text-xs text-slate-400">{it.hint}</span>
                  ) : null}
                </CommandItem>
              )
            })}
          </CommandGroup>
        ))}
      </CommandList>
      <div className="flex items-center gap-3 border-t border-slate-200 px-3 py-1.5 text-[10px] tracking-wide text-slate-400">
        <span>↑↓ 选择</span>
        <span>↵ 打开</span>
        <span>esc 关闭</span>
        <span className="ml-auto rounded border border-border/70 bg-background/80 px-1 py-0.5 font-mono">
          Alt K
        </span>
      </div>
    </CommandDialog>,
    document.body,
  )
}
