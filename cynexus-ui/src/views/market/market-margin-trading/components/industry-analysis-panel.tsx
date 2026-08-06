import { ArrowDownRight, ArrowUpRight, BarChart3, Info, Layers3, ShieldAlert } from "lucide-react"

import type { MarginIndustrySummary } from "@/services/market/margin"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"
import { formatYuanAsYi } from "../lib/format"

interface IndustryAnalysisPanelProps {
  rows: MarginIndustrySummary[]
  loading?: boolean
  selectedIndustryCode?: string | null
  onIndustrySelect: (row: MarginIndustrySummary) => void
}

function netFinancingBuy(row: MarginIndustrySummary) {
  return (row.financingBuyAmount ?? 0) - (row.financingRepayAmount ?? 0)
}

function marginBalance(row: MarginIndustrySummary) {
  return row.marginBalance ?? 0
}

function lendingPressure(row: MarginIndustrySummary) {
  const balance = marginBalance(row)
  return balance > 0 ? ((row.securitiesLendingBalanceAmount ?? 0) / balance) * 100 : 0
}

function coverage(row: MarginIndustrySummary) {
  return row.constituentCount > 0 ? (row.matchedStockCount / row.constituentCount) * 100 : 0
}

function industryName(row: MarginIndustrySummary) {
  return row.industryName ?? row.tdxName ?? row.industryCode
}

function signedYi(value: number) {
  return `${value >= 0 ? "+" : ""}${formatYuanAsYi(value, 1)}`
}

function AnalysisStat({ label, value, detail, icon: Icon, tone = "neutral" }: {
  label: string
  value: string
  detail: string
  icon: typeof BarChart3
  tone?: "neutral" | "up" | "down"
}) {
  return (
    <div className="min-w-0 py-1">
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className="size-3.5" />
        <span>{label}</span>
      </div>
      <div className={cn(
        "mt-1 truncate text-lg font-semibold tabular-nums",
        tone === "up" && "text-red-600 dark:text-red-400",
        tone === "down" && "text-emerald-600 dark:text-emerald-400",
      )}>{value}</div>
      <div className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</div>
    </div>
  )
}

function FlowRow({ row, selected, onSelect }: {
  row: MarginIndustrySummary
  selected: boolean
  onSelect: () => void
}) {
  const net = netFinancingBuy(row)
  const positive = net >= 0
  return (
    <button
      type="button"
      className={cn(
        "flex w-full items-center justify-between gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-muted/55 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        selected && "bg-information/10",
      )}
      aria-pressed={selected}
      onClick={onSelect}
    >
      <span className="min-w-0">
        <span className="block truncate text-sm font-medium">{industryName(row)}</span>
        <span className="mt-0.5 block truncate text-[11px] text-muted-foreground">
          余额 {formatYuanAsYi(row.marginBalance, 1)} · 覆盖 {coverage(row).toFixed(0)}%
        </span>
      </span>
      <span className={cn(
        "inline-flex shrink-0 items-center gap-1 font-mono text-xs font-medium tabular-nums",
        positive ? "text-red-600 dark:text-red-400" : "text-emerald-600 dark:text-emerald-400",
      )}>
        {positive ? <ArrowUpRight className="size-3.5" /> : <ArrowDownRight className="size-3.5" />}
        {signedYi(net)}
      </span>
    </button>
  )
}

function LoadingRows() {
  return <div className="space-y-2">{[1, 2, 3, 4].map((item) => <Skeleton key={item} className="h-11 w-full" />)}</div>
}

export function IndustryAnalysisPanel({ rows, loading = false, selectedIndustryCode, onIndustrySelect }: IndustryAnalysisPanelProps) {
  const sortedByNet = [...rows].sort((left, right) => netFinancingBuy(right) - netFinancingBuy(left))
  const netBuyRows = sortedByNet
  const netSellRows = [...sortedByNet].reverse()
  const totalBalance = rows.reduce((sum, row) => sum + marginBalance(row), 0)
  const topFiveBalance = sortedByNet.slice(0, 5).reduce((sum, row) => sum + marginBalance(row), 0)
  const totalConstituents = rows.reduce((sum, row) => sum + (row.constituentCount ?? 0), 0)
  const matchedConstituents = rows.reduce((sum, row) => sum + (row.matchedStockCount ?? 0), 0)
  const aggregateNet = rows.reduce((sum, row) => sum + netFinancingBuy(row), 0)
  const highestLendingPressure = [...rows].sort((left, right) => lendingPressure(right) - lendingPressure(left))[0]

  return (
    <section className="flex h-full min-h-0 min-w-0 flex-col overflow-hidden rounded-lg bg-muted/35 p-4" aria-label="行业两融分析">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-base font-semibold">行业两融分析</h2>
          <p className="text-xs text-muted-foreground">按融资净买入、集中度和融券压力，定位资金偏好的板块。</p>
        </div>
        <span className="shrink-0 text-xs tabular-nums text-muted-foreground">{loading ? "分析中" : `${rows.length} 个行业`}</span>
      </div>
      <div className="grid grid-cols-1 gap-3 border-b border-border/60 pb-3 sm:grid-cols-3">
        {loading ? (
          <div className="col-span-full grid gap-3 sm:grid-cols-3"><Skeleton className="h-14" /><Skeleton className="h-14" /><Skeleton className="h-14" /></div>
        ) : (
          <>
            <AnalysisStat label="行业净融资" value={signedYi(aggregateNet)} detail="融资买入 - 融资偿还" icon={BarChart3} tone={aggregateNet >= 0 ? "up" : "down"} />
            <AnalysisStat label="前五行业集中度" value={totalBalance > 0 ? `${((topFiveBalance / totalBalance) * 100).toFixed(1)}%` : "-"} detail="按两融余额计算" icon={Layers3} />
            <AnalysisStat label="行业数据覆盖" value={totalConstituents > 0 ? `${((matchedConstituents / totalConstituents) * 100).toFixed(1)}%` : "-"} detail={highestLendingPressure ? `融券压力最高：${industryName(highestLendingPressure)}` : "成分匹配情况"} icon={ShieldAlert} />
          </>
        )}
      </div>
      <div className="mt-3 flex items-start gap-2 rounded-md bg-background/55 px-3 py-2 text-[11px] leading-5 text-muted-foreground">
        <Info className="mt-0.5 size-3.5 shrink-0 text-information" />
        <p>
          <span className="font-medium text-foreground">融资买入</span> 是借钱买股，代表杠杆资金增加；
          <span className="font-medium text-foreground">融资偿还</span> 是归还融资负债，代表杠杆资金收缩。
          净融资买入 = 融资买入 - 融资偿还，正值偏扩张、负值偏收缩；普通卖出不等于融资偿还，不等同于全市场买卖额。
        </p>
      </div>
      <div className="grid min-h-0 flex-1 gap-4 pt-3 lg:grid-cols-2">
        <div className="min-h-0 flex min-w-0 flex-col">
          <div className="mb-1 flex shrink-0 items-center justify-between px-2">
            <h3 className="text-xs font-semibold">融资净买入靠前</h3>
            <span className="text-[11px] text-muted-foreground">全量 {netBuyRows.length} · 红色为增加</span>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1">
            {loading ? <LoadingRows /> : netBuyRows.map((row) => <FlowRow key={row.industryCode} row={row} selected={selectedIndustryCode === row.industryCode} onSelect={() => onIndustrySelect(row)} />)}
          </div>
        </div>
        <div className="min-h-0 flex min-w-0 flex-col">
          <div className="mb-1 flex shrink-0 items-center justify-between px-2">
            <h3 className="text-xs font-semibold">融资净偿还靠前</h3>
            <span className="text-[11px] text-muted-foreground">全量 {netSellRows.length} · 绿色为收缩</span>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1">
            {loading ? <LoadingRows /> : netSellRows.map((row) => <FlowRow key={row.industryCode} row={row} selected={selectedIndustryCode === row.industryCode} onSelect={() => onIndustrySelect(row)} />)}
          </div>
        </div>
      </div>
      {!rows.length && !loading ? <div className="px-4 py-8 text-center text-sm text-muted-foreground">暂无行业分析数据</div> : null}
      <div className="mt-auto border-t border-border/60 pt-2 text-[11px] text-muted-foreground">点击行业打开详情抽屉，查看行业分析与成分股两融排名。</div>
    </section>
  )
}
