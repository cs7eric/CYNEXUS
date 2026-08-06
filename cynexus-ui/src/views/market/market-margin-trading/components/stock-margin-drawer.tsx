import { useEffect, useMemo, useState } from "react"
import { AlertCircle, ArrowDownRight, ArrowUpRight, Minus, TrendingUp } from "lucide-react"

import {
  backfillMarginStockHistory,
  fetchMarginStockHistory,
  type MarginStockDetail,
} from "@/services/market/margin"
import { fetchStockFundFlow, fetchStockMeta, type StockFundFlowPoint } from "@/services/market/stock-chart"
import { Badge } from "@/components/ui/badge"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { formatDate, formatDateTime, formatNumber, formatYuanAsYi } from "../lib/format"
import { cn } from "@/lib/utils"
import {
  FinancingBalanceInsight,
  FinancingBuyRatioInsight,
  FinancingExposureInsight,
  MarginSentimentSummary,
  type MarginInsightPoint,
} from "./margin-insights"
import { MarginOverviewCharts } from "./margin-overview-charts"

interface StockMarginDrawerProps {
  stock: MarginStockDetail | null
  onClose: () => void
}

interface ChangeValue {
  value: number
  percent: number | null
  gap: number
}

const DAY_MS = 86_400_000
const HISTORY_TARGET = 120

function finite(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(value)
}

function latestChange(items: MarginStockDetail[], read: (item: MarginStockDetail) => number | null): ChangeValue | null {
  const values = items
    .filter((item) => finite(read(item)))
    .sort((left, right) => left.tradeDate.localeCompare(right.tradeDate))
  if (values.length < 2) return null

  const current = read(values[values.length - 1])
  const previous = read(values[values.length - 2])
  if (!finite(current) || !finite(previous)) return null

  const gap = (Date.parse(values[values.length - 1].tradeDate) - Date.parse(values[values.length - 2].tradeDate)) / DAY_MS
  if (!Number.isFinite(gap) || gap > 10) return null

  return {
    value: current - previous,
    percent: previous === 0 ? null : ((current - previous) / Math.abs(previous)) * 100,
    gap,
  }
}

function signedYuan(value: number | null | undefined) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${formatYuanAsYi(value, 2)}`
}

function Metric({ label, value, detail }: { label: string; value: React.ReactNode; detail?: string }) {
  return (
    <div className="min-w-0 rounded-md border border-border/60 bg-background/55 px-3 py-2.5">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 truncate text-sm font-semibold tabular-nums">{value}</div>
      {detail ? <div className="mt-1 truncate text-[11px] text-muted-foreground">{detail}</div> : null}
    </div>
  )
}

export function StockMarginDrawer({ stock, onClose }: StockMarginDrawerProps) {
  const [history, setHistory] = useState<MarginStockDetail[]>([])
  const [fundFlowHistory, setFundFlowHistory] = useState<StockFundFlowPoint[]>([])
  const [circulatingMarketCap, setCirculatingMarketCap] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!stock) {
      return
    }

    let active = true

    void (async () => {
      await Promise.resolve()
      if (!active) return
      setLoading(true)
      setError(null)
      setHistory([])
      setFundFlowHistory([])
      setCirculatingMarketCap(null)
    })()

    void (async () => {
      try {
        const payload = await fetchStockFundFlow(stock.stockCode)
        if (active) setFundFlowHistory(payload.history ?? [])
      } catch {
        // Fund-flow data is an optional enrichment for the margin drawer.
      }
    })()

    void (async () => {
      try {
        const payload = await fetchStockMeta({ targetType: "stock", symbol: stock.stockCode })
        if (active) setCirculatingMarketCap(payload.circMarketCap)
      } catch {
        // The margin history remains useful when stock metadata is unavailable.
      }
    })()

    void (async () => {
      let initialItems: MarginStockDetail[] = []
      try {
        const payload = await fetchMarginStockHistory(stock.stockCode, HISTORY_TARGET)
        initialItems = payload.items ?? []
        if (!payload.ok) throw new Error(payload.error ?? "暂无历史两融数据")

        // The daily detail job only keeps the latest snapshot. Complete a
        // selected stock's history on demand before rendering the trend.
        if (initialItems.length < HISTORY_TARGET) {
          await backfillMarginStockHistory(stock.stockCode)
          const refreshed = await fetchMarginStockHistory(stock.stockCode, HISTORY_TARGET)
          if (!refreshed.ok) throw new Error(refreshed.error ?? "暂无历史两融数据")
          initialItems = refreshed.items ?? initialItems
        }
        if (active) setHistory(initialItems)
      } catch (reason) {
        if (!active) return
        setHistory(initialItems)
        setError(reason instanceof Error ? reason.message : String(reason))
      } finally {
        if (active) setLoading(false)
      }
    })()

    return () => {
      active = false
    }
  }, [stock])

  const orderedHistory = useMemo(
    () => [...history].sort((left, right) => left.tradeDate.localeCompare(right.tradeDate)),
    [history],
  )
  const current = orderedHistory.at(-1) ?? stock
  const financingChange = useMemo(() => latestChange(history, (item) => item.financingBalance), [history])
  const netFinancingBuy = finite(current?.financingBuyAmount) && finite(current?.financingRepayAmount)
    ? current.financingBuyAmount - current.financingRepayAmount
    : null
  const fundFlowByDate = useMemo(
    () => new Map(fundFlowHistory.map((item) => [item.trade_date, item])),
    [fundFlowHistory],
  )
  const insightPoints = useMemo<MarginInsightPoint[]>(
    () => orderedHistory.map((item) => {
      const flow = fundFlowByDate.get(item.tradeDate)
      return {
        tradeDate: item.tradeDate,
        financingBalance: item.financingBalance,
        marginBalance: item.marginBalance,
        financingBuyAmount: item.financingBuyAmount,
        financingRepayAmount: item.financingRepayAmount,
        securitiesLendingBalanceAmount: item.securitiesLendingBalanceAmount,
        turnoverAmount: finite(flow?.amountWan) ? flow.amountWan * 10000 : null,
      }
    }),
    [fundFlowByDate, orderedHistory],
  )
  const historicalExposureRatios = useMemo(
    () => circulatingMarketCap != null && circulatingMarketCap > 0
      ? orderedHistory
        .map((item) => finite(item.financingBalance) ? (item.financingBalance / circulatingMarketCap) * 100 : null)
        .filter((value): value is number => finite(value))
      : [],
    [circulatingMarketCap, orderedHistory],
  )
  const sentiment = financingChange == null
    ? { label: "数据不足", description: "历史样本间隔过大，暂不判断杠杆资金方向", tone: "muted" }
    : financingChange.value > 0
      ? { label: "偏多", description: "融资余额较近一交易日增加，杠杆资金趋于入场", tone: "up" }
      : financingChange.value < 0
        ? { label: "偏谨慎", description: "融资余额较近一交易日减少，杠杆资金趋于收缩", tone: "down" }
        : { label: "中性", description: "融资余额较近一交易日基本持平", tone: "muted" }

  const overviewFallback: MarginInsightPoint | undefined = current ? {
    tradeDate: current.tradeDate,
    financingBalance: current.financingBalance,
    marginBalance: current.marginBalance,
    financingBuyAmount: current.financingBuyAmount,
    financingRepayAmount: current.financingRepayAmount,
    securitiesLendingBalanceAmount: current.securitiesLendingBalanceAmount,
  } : undefined

  return (
    <Sheet open={stock != null} onOpenChange={(open) => { if (!open) onClose() }}>
      <SheetContent side="right" className="flex h-full w-full flex-col gap-0 p-0 sm:max-w-5xl xl:max-w-6xl">
        <SheetHeader className="border-b border-border/70 pr-12 sm:flex-row sm:items-start sm:justify-between sm:gap-5">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <SheetTitle className="truncate">{current?.stockName ?? stock?.stockName ?? stock?.stockCode ?? "个股两融"}</SheetTitle>
              {current?.exchange ? <Badge variant="outline">{current.exchange}</Badge> : null}
            </div>
            <SheetDescription>
              <span className="font-mono">{current?.stockCode ?? stock?.stockCode ?? "-"}</span>
              <span className="mx-1">·</span>
              数据日 {formatDate(current?.tradeDate)}
              {current?.updatedAt ? ` · 更新于 ${formatDateTime(current.updatedAt)}` : ""}
            </SheetDescription>
          </div>
          <MarginSentimentSummary points={insightPoints} />
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-y-auto p-4">
          {loading && !history.length ? (
            <div className="flex h-40 items-center justify-center text-sm text-muted-foreground">正在加载两融历史...</div>
          ) : error && !history.length ? (
            <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              <span>{error}</span>
            </div>
          ) : !current ? (
            <div className="flex h-40 items-center justify-center text-sm text-muted-foreground">暂无个股两融数据</div>
          ) : (
            <div className="space-y-4">
              <section className="rounded-lg bg-muted/35 p-3.5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-xs font-medium text-muted-foreground">杠杆资金观察</div>
                    <div className={cn(
                      "mt-1 flex items-center gap-1.5 text-lg font-semibold",
                      sentiment.tone === "up" && "text-red-600 dark:text-red-400",
                      sentiment.tone === "down" && "text-emerald-600 dark:text-emerald-400",
                    )}>
                      {sentiment.tone === "up" ? <ArrowUpRight className="size-5" /> : sentiment.tone === "down" ? <ArrowDownRight className="size-5" /> : <Minus className="size-5" />}
                      {sentiment.label}
                    </div>
                  </div>
                  <TrendingUp className="size-5 text-muted-foreground" />
                </div>
                <p className="mt-2 text-xs leading-5 text-muted-foreground">{sentiment.description}。余额是存量，方向判断优先看变化量。</p>
              </section>

              <MarginOverviewCharts points={insightPoints} fallback={overviewFallback} title="两融市场概览" />

              <div className="grid gap-3 lg:grid-cols-2">
                <section className="rounded-lg border border-border/70 bg-background p-3.5">
                  <div className="mb-2 text-xs font-medium text-muted-foreground">当前规模</div>
                  <div className="grid grid-cols-2 gap-2">
                    <Metric label="两融余额" value={formatYuanAsYi(current.marginBalance, 2)} detail="融资 + 融券" />
                    <Metric label="融资余额" value={formatYuanAsYi(current.financingBalance, 2)} detail="未偿还融资借款" />
                    <Metric label="融券余额" value={formatYuanAsYi(current.securitiesLendingBalanceAmount, 2)} detail="借入股票的市值" />
                    <Metric label="融券余量" value={formatNumber(current.securitiesLendingBalance)} detail="未归还股票数量" />
                  </div>
                </section>
                <section className="rounded-lg border border-border/70 bg-background p-3.5">
                  <div className="mb-2 text-xs font-medium text-muted-foreground">当日行为</div>
                  <div className="grid grid-cols-2 gap-2">
                    <Metric label="融资买入" value={formatYuanAsYi(current.financingBuyAmount, 2)} detail="今天借钱买入" />
                    <Metric label="融资偿还" value={formatYuanAsYi(current.financingRepayAmount, 2)} detail="今天归还借款" />
                    <Metric label="净融资买入" value={netFinancingBuy == null ? "数据不足" : signedYuan(netFinancingBuy)} detail="融资买入 - 融资偿还" />
                    <Metric label="融券卖出量" value={formatNumber(current.securitiesLendingSellVolume)} detail="今天借股卖出" />
                  </div>
                </section>
              </div>

              <div className="space-y-3">
                <FinancingBalanceInsight points={insightPoints} />
                <FinancingExposureInsight
                  financingBalance={current.financingBalance}
                  circulatingMarketCap={circulatingMarketCap}
                  historicalRatios={historicalExposureRatios}
                />
                <FinancingBuyRatioInsight points={insightPoints} />
              </div>

              <section className="border-t border-border/60 pt-3 text-xs leading-5 text-muted-foreground">
                <div>融资余额上升通常代表看多杠杆增加，下降代表杠杆资金收缩；融资买入与融资偿还用于观察当日进出。</div>
                <div className="mt-1">融券余额上升通常代表做空仓位增加。当前接口缺少有效偿还字段时，页面会标记为“数据不足”，不做推算。</div>
                {error ? <div className="mt-2 text-amber-700 dark:text-amber-400">历史接口提示：{error}</div> : null}
              </section>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
