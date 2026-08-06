import { useEffect, useMemo, useState } from "react"
import { ArrowDownRight, ArrowUpRight, BarChart3, Layers3, Minus, ShieldAlert, Table2 } from "lucide-react"

import type {
  MarginExchange,
  MarginIndustrySummary,
  MarginRankingSort,
  MarginSortDirection,
  MarginStockDetail,
  MarginStocksResponse,
} from "@/services/market/margin"
import { fetchMarginIndustryHistory } from "@/services/market/margin"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { cn } from "@/lib/utils"
import { formatDate, formatYuanAsYi } from "../lib/format"
import { MarginSentimentSummary, type MarginInsightPoint } from "./margin-insights"
import { MarginOverviewCharts } from "./margin-overview-charts"
import { StockRankingTable } from "./stock-ranking-table"
import { isUsableMarginHistoryPoint } from "../lib/margin-history"

interface IndustryMarginDrawerProps {
  industry: MarginIndustrySummary | null
  stocks: MarginStocksResponse | null
  stocksLoading?: boolean
  stockPage: number
  stockPageSize: number
  exchange: "ALL" | MarginExchange
  sort: MarginRankingSort
  sortDir: MarginSortDirection
  onClose: () => void
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  onExchangeChange: (value: "ALL" | MarginExchange) => void
  onSortChange: (value: MarginRankingSort, direction?: MarginSortDirection) => void
  onStockSelect: (row: MarginStockDetail) => void
  onRefresh: () => void
}

function finite(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(value)
}

function industryName(industry: MarginIndustrySummary) {
  return industry.industryName ?? industry.tdxName ?? industry.industryCode
}

function netFinancingBuy(industry: MarginIndustrySummary) {
  if (!finite(industry.financingBuyAmount) || !finite(industry.financingRepayAmount)) return null
  return industry.financingBuyAmount - industry.financingRepayAmount
}

function percent(value: number | null) {
  return finite(value) ? `${value.toFixed(1)}%` : "-"
}

function signedYuan(value: number | null) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${formatYuanAsYi(value, 1)}`
}

const INDUSTRY_HISTORY_LIMIT = 60
const INDUSTRY_PERIODS = [5, 10, 30, 60] as const

type IndustryPeriod = (typeof INDUSTRY_PERIODS)[number]

function signedPercent(value: number | null) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${value.toFixed(1)}%`
}

function historyChange(items: MarginIndustrySummary[]) {
  const values = items.filter((item) => finite(item.marginBalance))
  if (values.length < 2) return null
  const first = values[0].marginBalance
  const latest = values.at(-1)?.marginBalance
  if (!finite(first) || !finite(latest)) return null
  return {
    value: latest - first,
    percent: first === 0 ? null : ((latest - first) / Math.abs(first)) * 100,
  }
}

function trendTone(value: number | null) {
  return value == null ? "neutral" : value > 0 ? "up" : value < 0 ? "down" : "neutral"
}

function toIndustryInsightPoints(items: MarginIndustrySummary[], fallback?: MarginIndustrySummary): MarginInsightPoint[] {
  const source = [...items]
  if (fallback && !source.some((item) => item.tradeDate === fallback.tradeDate)) source.push(fallback)
  return source
    .sort((left, right) => left.tradeDate.localeCompare(right.tradeDate))
    .map((item) => ({
      tradeDate: item.tradeDate,
      constituentCount: item.constituentCount,
      matchedStockCount: item.matchedStockCount,
      financingBalance: item.financingBalance,
      marginBalance: item.marginBalance,
      financingBuyAmount: item.financingBuyAmount,
      financingRepayAmount: item.financingRepayAmount,
      securitiesLendingBalanceAmount: item.securitiesLendingBalanceAmount,
      turnoverAmount: item.turnoverAmount,
    }))
}

function useIndustryHistory(industry: MarginIndustrySummary | null) {
  const [history, setHistory] = useState<MarginIndustrySummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const industryCode = industry?.industryCode
  const tradeDate = industry?.tradeDate

  useEffect(() => {
    let active = true

    void (async () => {
      if (!industryCode || !tradeDate) return
      setHistory([])
      setError(null)
      setLoading(true)
      try {
        const payload = await fetchMarginIndustryHistory(industryCode, {
          tradeDate,
          limit: INDUSTRY_HISTORY_LIMIT,
        })
        if (!payload.ok) throw new Error(payload.error ?? "暂无行业历史两融数据")
        if (active) setHistory(payload.items ?? [])
      } catch (reason) {
        if (!active) return
        setError(reason instanceof Error ? reason.message : String(reason))
      } finally {
        if (active) setLoading(false)
      }
    })()

    return () => {
      active = false
    }
  }, [industryCode, tradeDate])

  return { history, loading, error }
}

function IndustryPeriodCard({
  period,
  items,
  className,
}: {
  period: IndustryPeriod
  items: MarginIndustrySummary[]
  className?: string
}) {
  const latest = items.at(-1)
  const change = historyChange(items)
  const tone = trendTone(change?.value ?? null)
  const Icon = tone === "up" ? ArrowUpRight : tone === "down" ? ArrowDownRight : Minus
  return (
    <div className={cn("min-w-0 rounded-md border border-border/60 bg-background/60 px-3 py-2.5", className)}>
      <div className="text-xs text-muted-foreground">近 {period} 日</div>
      <div className="mt-1 truncate text-sm font-semibold tabular-nums">
        {signedYuan(change?.value ?? null)}
      </div>
      <div className={cn(
        "mt-1 inline-flex items-center gap-1 text-xs tabular-nums",
        tone === "up" && "text-red-600 dark:text-red-400",
        tone === "down" && "text-emerald-600 dark:text-emerald-400",
        tone === "neutral" && "text-muted-foreground",
      )}>
        <Icon className="size-3.5" />
        {change == null
          ? "数据不足"
          : `${signedPercent(change.percent)} · 期末 ${formatYuanAsYi(latest?.marginBalance, 2)}`}
      </div>
    </div>
  )
}

function IndustryHistorySection({
  history,
  error,
}: {
  history: MarginIndustrySummary[]
  error: string | null
}) {
  const orderedHistory = useMemo(
    () => [...history]
      .filter((item) => isUsableMarginHistoryPoint(item))
      .sort((left, right) => left.tradeDate.localeCompare(right.tradeDate)),
    [history],
  )
  const insightPoints = toIndustryInsightPoints(orderedHistory)

  if (error && !history.length) {
    return <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-xs text-destructive">{error}</div>
  }

  return (
    <MarginOverviewCharts points={insightPoints} bare />
  )
}

function IndustryMetric({
  label,
  value,
  detail,
  icon: Icon,
  tone = "neutral",
}: {
  label: string
  value: string
  detail: string
  icon: typeof BarChart3
  tone?: "neutral" | "up" | "down"
}) {
  return (
    <div className="min-w-0 bg-background px-3 py-3">
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className="size-3.5" />
        <span className="truncate">{label}</span>
      </div>
      <div className={cn(
        "mt-1 truncate text-base font-semibold tabular-nums",
        tone === "up" && "text-red-600 dark:text-red-400",
        tone === "down" && "text-emerald-600 dark:text-emerald-400",
      )}>{value}</div>
      <div className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</div>
    </div>
  )
}

function IndustryAnalysis({ industry, history }: { industry: MarginIndustrySummary; history: MarginIndustrySummary[] }) {
  const net = netFinancingBuy(industry)
  const balance = industry.marginBalance
  const financingShare = finite(balance) && balance > 0 && finite(industry.financingBalance)
    ? (industry.financingBalance / balance) * 100
    : null
  const lendingPressure = finite(balance) && balance > 0 && finite(industry.securitiesLendingBalanceAmount)
    ? (industry.securitiesLendingBalanceAmount / balance) * 100
    : null
  const coverage = industry.constituentCount > 0
    ? (industry.matchedStockCount / industry.constituentCount) * 100
    : null
  const netTone = net == null ? "neutral" : net >= 0 ? "up" : "down"
  const direction = net == null ? "暂无方向判断" : net >= 0 ? "融资资金净流入" : "融资资金净偿还"
  return (
    <section className="border-t border-border/60 pt-5">
      <div className="overflow-hidden rounded-lg border border-border/60 bg-border/60">
        <div className="grid grid-cols-2 gap-px sm:grid-cols-3">
          <IndustryMetric label="两融余额" value={formatYuanAsYi(industry.marginBalance, 2)} detail="融资 + 融券" icon={Layers3} />
          <IndustryMetric label="融资净买入" value={signedYuan(net)} detail="融资买入 - 融资偿还" icon={netTone === "down" ? ArrowDownRight : ArrowUpRight} tone={netTone} />
          <IndustryMetric label="融资占比" value={percent(financingShare)} detail="融资余额 / 两融余额" icon={BarChart3} />
          <IndustryMetric label="融券压力" value={percent(lendingPressure)} detail="融券余额 / 两融余额" icon={ShieldAlert} tone={lendingPressure != null && lendingPressure >= 5 ? "down" : "neutral"} />
          <IndustryMetric label="成分覆盖" value={percent(coverage)} detail={`${industry.matchedStockCount} / ${industry.constituentCount} 只`} icon={Layers3} />
          <IndustryMetric label="融资方向" value={direction} detail={`数据日 ${formatDate(industry.tradeDate)}`} icon={netTone === "down" ? ArrowDownRight : ArrowUpRight} tone={netTone} />
        </div>
        <div className="grid grid-cols-2 gap-px border-t border-border/60 bg-border/60 sm:grid-cols-4">
          {INDUSTRY_PERIODS.map((value) => (
            <IndustryPeriodCard key={value} period={value} items={history.slice(-value)} className="rounded-none border-0 bg-background px-3 py-3" />
          ))}
        </div>
      </div>

      <div className="mt-3 rounded-md bg-muted/35 px-3 py-2.5 text-xs leading-5 text-muted-foreground">
        {net == null
          ? "当前行业缺少融资买入或融资偿还数据，暂不推断资金方向。"
          : net >= 0
            ? "融资净买入为正，表示当日新增融资资金高于偿还资金；结合成分覆盖率判断该信号是否具有代表性。"
             : "融资净买入为负，表示当日融资偿还高于买入，板块杠杆资金出现收缩。"}
      </div>
    </section>
  )
}

export function IndustryMarginDrawer({
  industry,
  stocks,
  stocksLoading = false,
  stockPage,
  stockPageSize,
  exchange,
  sort,
  sortDir,
  onClose,
  onPageChange,
  onPageSizeChange,
  onExchangeChange,
  onSortChange,
  onStockSelect,
  onRefresh,
}: IndustryMarginDrawerProps) {
  const [rankingIndustryCode, setRankingIndustryCode] = useState<string | null>(null)
  const name = industry ? industryName(industry) : "行业两融"
  const { history, error: historyError } = useIndustryHistory(industry)
  const usableHistory = useMemo(
    () => history.filter((item) => isUsableMarginHistoryPoint(item)),
    [history],
  )
  const insightPoints = industry
    ? toIndustryInsightPoints(usableHistory, isUsableMarginHistoryPoint(industry) ? industry : undefined)
    : []
  const showStockRanking = rankingIndustryCode === industry?.industryCode

  const closeDrawer = () => {
    setRankingIndustryCode(null)
    onClose()
  }

  const stockRankingTable = industry ? (
    <StockRankingTable
      rows={stocks?.items ?? []}
      count={stocks?.count ?? industry.matchedStockCount ?? 0}
      loading={stocksLoading}
      page={stockPage}
      pageSize={stockPageSize}
      exchange={exchange}
      selectedIndustryCode={industry.industryCode}
      selectedIndustryName={name}
      sort={sort}
      sortDir={sortDir}
      onPageChange={onPageChange}
      onPageSizeChange={onPageSizeChange}
      onExchangeChange={onExchangeChange}
      onSortChange={onSortChange}
      onIndustryClear={closeDrawer}
      onStockSelect={onStockSelect}
      onRefresh={onRefresh}
    />
  ) : null

  return (
    <Sheet open={industry != null} onOpenChange={(open) => { if (!open) closeDrawer() }}>
      <SheetContent
        side="right"
        className={cn(
          "flex h-full w-full flex-col gap-0 overflow-hidden p-0",
          showStockRanking ? "sm:max-w-[96vw] xl:max-w-[1600px]" : "sm:max-w-5xl xl:max-w-6xl",
        )}
      >
        <SheetHeader className="shrink-0 border-b border-border/70 pr-12 sm:flex-row sm:items-start sm:justify-between sm:gap-5">
          <div className="min-w-0">
            <SheetTitle className="truncate">{name} · 两融研究</SheetTitle>
            <SheetDescription>
              {industry ? (
                <>
                  THS <span className="font-mono">{industry.industryCode}</span>
                  {industry.tdxCode ? <> · TDX <span className="font-mono">{industry.tdxCode}</span></> : null}
                  <span className="mx-1">·</span>
                  数据日 {formatDate(industry.tradeDate)}
                </>
              ) : "选择一个行业查看详情"}
            </SheetDescription>
          </div>
          {industry ? (
            <div className="flex shrink-0 flex-wrap items-start justify-end gap-2">
              <Button
                variant={showStockRanking ? "secondary" : "outline"}
                size="sm"
                onClick={() => setRankingIndustryCode(showStockRanking ? null : industry.industryCode)}
              >
                <Table2 className="size-3.5" />
                {showStockRanking ? "收起个股排名" : "个股两融排名"}
              </Button>
              <MarginSentimentSummary points={insightPoints} />
            </div>
          ) : null}
        </SheetHeader>

        {industry ? (
          showStockRanking ? (
            <div className="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
              <div className="order-1 min-h-0 overflow-hidden border-t border-border/60 lg:border-r lg:border-t-0">
                {stockRankingTable}
              </div>
              <div className="order-2 min-h-0 overflow-y-auto">
                <div className="space-y-6 border-border/60 bg-muted/10 p-4">
                  <IndustryHistorySection history={usableHistory} error={historyError} />
                  <IndustryAnalysis industry={industry} history={usableHistory} />
                </div>
              </div>
            </div>
          ) : (
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
              <div className="shrink-0 space-y-6 border-b border-border/60 bg-muted/10 p-4">
                <IndustryHistorySection history={usableHistory} error={historyError} />
                <IndustryAnalysis industry={industry} history={usableHistory} />
              </div>
              <div className="min-h-0 flex-1 p-4 pt-3">
                {stockRankingTable}
              </div>
            </div>
          )
        ) : null}
      </SheetContent>
    </Sheet>
  )
}
