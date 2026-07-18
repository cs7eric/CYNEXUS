import { useEffect, useMemo, useState, type ReactNode } from "react"
import { Activity, AlertTriangle, Gauge, Zap } from "lucide-react"

import {
  fetchMarketBreadth,
  fetchMarketBreadthSeries,
  fetchStockKlines,
  fetchStockMeta,
} from "@/services/market/stock-chart"
import { TechnicalIndicatorPanel } from "../../stock-chart/components/technical-indicator-panel"
import {
  analyzeFullContext,
  analyzeTrend,
} from "../../stock-chart/lib/indicator-utils"
import type {
  MarketBreadth,
  MarketBreadthSeries,
  StockMeta,
  TrendAnalysis,
} from "../../stock-chart/lib/indicator-utils"
import type {
  StockAdjust,
  StockKlineBar,
  StockPeriod,
  StockTargetType,
} from "../../stock-chart/lib/types"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"
import { notification } from "@/components/ui/notification"

export interface TechnicalIndicatorTabProps {
  targetType: StockTargetType
  symbol: string
  name: string
  period?: StockPeriod
  adjust?: StockAdjust
}

const DEFAULT_PERIOD: StockPeriod = "1d"
const DEFAULT_ADJUST: StockAdjust = "qfq"

const INDEX_KLINE_SYMBOLS: Record<string, string> = {
  "上证指数": "000001",
  "上证50": "000016",
  "沪深300": "000300",
  "中证500": "000905",
  "中证1000": "000852",
  "中证2000": "932000",
  "创业板指": "399006",
  "科创50": "000688",
}

function loadContextIndexBars(params: {
  period: StockPeriod
  adjust: StockAdjust
  sector?: { symbol: string; label: string } | null
}): Promise<PromiseSettledResult<{ label: string; items: StockKlineBar[] }>[]> {
  const entries: Array<{ label: string; symbol: string; targetType: StockTargetType }> = Object.entries(INDEX_KLINE_SYMBOLS)
    .map(([label, symbol]) => ({ label, symbol, targetType: "index" }))

  if (params.sector?.symbol) {
    entries.push({
      label: params.sector.label,
      symbol: params.sector.symbol,
      targetType: /^\d+$/.test(params.sector.symbol) ? "index" : "sector",
    })
  }

  return Promise.allSettled(
    entries.map(async ({ label, symbol, targetType }) => {
      const res = await fetchStockKlines({
        targetType,
        symbol,
        name: label,
        period: params.period,
        adjust: params.adjust,
      })
      return { label, items: res.items }
    }),
  )
}

function settledBarsToMap(results: PromiseSettledResult<{ label: string; items: StockKlineBar[] }>[]) {
  const map: Record<string, StockKlineBar[]> = {}
  for (const r of results) {
    if (r.status === "fulfilled") {
      map[r.value.label] = r.value.items
    }
  }
  return map
}

function barDate(bar: StockKlineBar) {
  if (bar.trade_date) return bar.trade_date
  return new Date(bar.timestamp).toISOString().slice(0, 10)
}

function formatPct(value: number | null) {
  if (value === null || !Number.isFinite(value)) return "—"
  return `${value >= 0 ? "+" : ""}${(value * 100).toFixed(2)}%`
}

function formatPrice(value: number | null | undefined) {
  return typeof value === "number" ? value.toFixed(2) : "—"
}

function sliceBarsToDate(items: StockKlineBar[], date: string) {
  return items.filter((item) => barDate(item) <= date)
}

function sliceIndexBarsMapToDate(indexBarsMap: Record<string, StockKlineBar[]>, date: string) {
  return Object.fromEntries(
    Object.entries(indexBarsMap)
      .map(([label, items]) => [label, sliceBarsToDate(items, date)])
      .filter(([, items]) => (items as StockKlineBar[]).length > 0),
  ) as Record<string, StockKlineBar[]>
}

function breadthForDate(series: MarketBreadthSeries, fallback: MarketBreadth | null, date: string) {
  return [...series].reverse().find((item) => item.date <= date) ?? fallback
}

function scoreTextClass(score: number, reverse = false) {
  if (reverse) {
    if (score >= 70) return "text-red-600"
    if (score >= 45) return "text-amber-600"
    return "text-emerald-600"
  }
  if (score >= 70) return "text-emerald-600"
  if (score >= 45) return "text-amber-600"
  return "text-red-600"
}

function volumeLevel(value: number | null) {
  if (value === null) return "—"
  if (value >= 1.5) return "放量"
  if (value >= 0.8) return "正常"
  return "缩量"
}

function trendStateClass(value?: string) {
  if (!value) return "bg-slate-100 text-slate-600"
  if (value.includes("多头") || value.includes("偏强")) return "bg-emerald-500 text-white"
  if (value.includes("震荡")) return "bg-amber-500 text-white"
  if (value.includes("弱") || value.includes("空头")) return "bg-orange-500 text-white"
  return "bg-slate-500 text-white"
}

function MetricSummaryChip({
  icon,
  label,
  value,
  suffix,
  caption,
  tone,
}: {
  icon: ReactNode
  label: string
  value: string | number
  suffix?: string
  caption?: string
  tone?: string
}) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
      <span className="text-slate-400">{icon}</span>
      <span>{label}</span>
      <span className={cn("font-mono text-sm font-semibold leading-none", tone ?? "text-slate-700")}>{value}</span>
      {suffix ? <span className="font-mono text-xs text-slate-500">{suffix}</span> : null}
      {caption ? <span className="rounded-full border border-slate-200 bg-white px-1.5 py-0 text-[10px] text-slate-600">{caption}</span> : null}
    </span>
  )
}

function FiveDayTrendOverview({ bars }: { bars: StockKlineBar[] }) {
  const ordered = useMemo(() => [...bars].sort((left, right) => barDate(left).localeCompare(barDate(right))), [bars])
  const recent = ordered.slice(-5)
  const priorClose = ordered.length > 5 ? ordered[ordered.length - 6]?.close : ordered[0]?.close
  const latestClose = ordered.at(-1)?.close
  const totalReturn = typeof priorClose === "number" && typeof latestClose === "number" && priorClose > 0
    ? (latestClose - priorClose) / priorClose
    : null
  const changes = recent.map((bar) => {
    const index = ordered.findIndex((item) => item.timestamp === bar.timestamp)
    const prev = index > 0 ? ordered[index - 1] : null
    const pct = prev?.close ? (bar.close - prev.close) / prev.close : null
    return { date: barDate(bar), close: bar.close, pct, volume: bar.volume }
  })
  const upDays = changes.filter((item) => (item.pct ?? 0) > 0).length
  const downDays = changes.filter((item) => (item.pct ?? 0) < 0).length
  const high = recent.length ? Math.max(...recent.map((item) => item.high)) : null
  const low = recent.length ? Math.min(...recent.map((item) => item.low)) : null
  const rangePct = high != null && low != null && low > 0 ? (high - low) / low : null
  const recentAvgVolume = recent.length ? recent.reduce((sum, item) => sum + item.volume, 0) / recent.length : null
  const prevVolumeWindow = ordered.slice(Math.max(0, ordered.length - 10), Math.max(0, ordered.length - 5))
  const prevAvgVolume = prevVolumeWindow.length ? prevVolumeWindow.reduce((sum, item) => sum + item.volume, 0) / prevVolumeWindow.length : null
  const volumeRatio = recentAvgVolume != null && prevAvgVolume ? recentAvgVolume / prevAvgVolume : null
  const directionLabel = totalReturn == null ? "数据不足" : totalReturn > 0.02 ? "短线走强" : totalReturn < -0.02 ? "短线走弱" : "窄幅震荡"

  if (!recent.length) return null

  return (
    <div className="rounded-xl bg-white/70 px-3 py-2.5">
      <div className="flex flex-col gap-2 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-sm font-semibold text-slate-900">近5日趋势</span>
          <Badge variant="outline" className="rounded-full border-slate-200 bg-slate-50 px-2 py-0 text-[10px] text-slate-600">
            {directionLabel}
          </Badge>
          <span className="text-[11px] text-slate-300">·</span>
          <span className="text-[11px] text-slate-500">5D</span>
          <span className={cn("font-mono text-sm font-semibold", totalReturn != null && totalReturn >= 0 ? "text-red-600" : "text-emerald-600")}>
            {formatPct(totalReturn)}
          </span>
        </div>
        <div className="flex min-w-0 flex-wrap items-center gap-1.5">
          <span className="rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
            涨跌 <span className="font-mono font-semibold text-red-600">{upDays}</span>/<span className="font-mono font-semibold text-emerald-600">{downDays}</span>
          </span>
          <span className="rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
            振幅 <span className="font-mono font-semibold text-slate-700">{formatPct(rangePct)}</span>
          </span>
          <span className="rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
            量能 <span className="font-mono font-semibold text-slate-700">{volumeRatio == null ? "—" : `${volumeRatio.toFixed(1)}x`}</span>
          </span>
        </div>
      </div>
      <div className="mt-2 flex min-w-0 flex-wrap items-center gap-1.5">
        {changes.map((item) => (
          <span
            key={item.date}
            className={cn(
              "inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px]",
              item.pct == null
                ? "bg-slate-50 text-slate-500"
                : item.pct >= 0
                  ? "bg-red-50 text-red-600"
                  : "bg-emerald-50 text-emerald-600",
            )}
          >
            <span className="font-mono text-slate-500">{item.date.slice(5)}</span>
            <span className="font-mono font-semibold">{formatPct(item.pct)}</span>
          </span>
        ))}
      </div>
    </div>
  )
}

function TechnicalDateItem({
  date,
  bars,
  allIndexBarsMap,
  stockMeta,
  breadth,
  breadthSeries,
  latest,
}: {
  date: string
  bars: StockKlineBar[]
  allIndexBarsMap: Record<string, StockKlineBar[]>
  stockMeta: StockMeta | null
  breadth: MarketBreadth | null
  breadthSeries: MarketBreadthSeries
  latest: boolean
}) {
  const [expanded, setExpanded] = useState(latest)
  const lastBar = bars.at(-1)
  const prevBar = bars.at(-2)
  const changePct = lastBar && prevBar?.close ? (lastBar.close - prevBar.close) / prevBar.close : null
  const scopedIndexBarsMap = useMemo(() => sliceIndexBarsMapToDate(allIndexBarsMap, date), [allIndexBarsMap, date])
  const scopedBreadthSeries = useMemo(() => breadthSeries.filter((item) => item.date <= date), [breadthSeries, date])
  const scopedBreadth = useMemo(() => breadthForDate(breadthSeries, breadth, date), [breadthSeries, breadth, date])
  const analysis = useMemo<TrendAnalysis | null>(() => {
    if (!bars.length) return null
    if (Object.keys(scopedIndexBarsMap).length || scopedBreadth || stockMeta) {
      return analyzeFullContext(bars, scopedIndexBarsMap, scopedBreadth, scopedBreadthSeries, stockMeta)
    }
    return analyzeTrend(bars)
  }, [bars, scopedIndexBarsMap, scopedBreadth, scopedBreadthSeries, stockMeta])

  return (
    <div className="min-w-0 rounded-xl bg-white/70">
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="sticky top-0 z-10 flex w-full items-center rounded-xl bg-white/95 px-3 py-2.5 text-left transition hover:bg-slate-50/90"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-3 gap-y-1.5">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-mono text-sm font-semibold text-slate-900">{date}</span>
            {latest ? <Badge className="rounded-full border-slate-200 bg-slate-50 px-1.5 py-0 text-[10px] text-slate-600" variant="outline">Latest</Badge> : null}
            <span className="text-[11px] text-slate-300">·</span>
            <span className="text-[11px] text-slate-500">Close <span className="font-mono text-slate-700">{formatPrice(lastBar?.close)}</span></span>
            <span className="text-[11px] text-slate-300">·</span>
            <span className={changePct !== null && changePct >= 0 ? "font-mono text-[11px] text-red-600" : "font-mono text-[11px] text-emerald-600"}>{formatPct(changePct)}</span>
            <span className="text-[11px] text-slate-300">·</span>
            <span className="text-[11px] text-slate-500"><span className="font-mono text-slate-700">{bars.length}</span> bars</span>
          </div>
          {analysis ? (
            <div className="flex min-w-0 flex-wrap items-center gap-1.5">
              <MetricSummaryChip
                icon={<Gauge className="size-3" />}
                label="技术"
                value={analysis.technicalScore.total}
                caption={analysis.technicalScore.level}
                tone={scoreTextClass(analysis.technicalScore.total)}
              />
              <MetricSummaryChip
                icon={<AlertTriangle className="size-3" />}
                label="风险"
                value={analysis.riskScore.total}
                caption={analysis.riskScore.level}
                tone={scoreTextClass(analysis.riskScore.total, true)}
              />
              <MetricSummaryChip
                icon={<Activity className="size-3" />}
                label="综合"
                value={analysis.compositeScore.total}
                caption={analysis.compositeScore.level}
                tone={scoreTextClass(analysis.compositeScore.total)}
              />
              <MetricSummaryChip
                icon={<Zap className="size-3" />}
                label="量能"
                value={analysis.volumeRatio === null ? "—" : analysis.volumeRatio.toFixed(1)}
                suffix={analysis.volumeRatio === null ? undefined : "x"}
                caption={volumeLevel(analysis.volumeRatio)}
              />
            </div>
          ) : null}
        </div>
        {analysis?.trendState ? (
          <Badge className={cn("ml-3 shrink-0 rounded-full border-0 px-3 py-1 text-xs font-medium", trendStateClass(analysis.trendState))}>
            {analysis.trendState}
          </Badge>
        ) : null}
      </button>
      {expanded ? (
        <div className="px-3 pb-3">
          <TechnicalIndicatorPanel
            bars={bars}
            indexBarsMap={scopedIndexBarsMap}
            stockMeta={stockMeta}
            breadth={scopedBreadth}
            breadthSeries={scopedBreadthSeries}
            cardClassName="border-0 bg-transparent shadow-none"
            contentClassName="px-0"
            hideStatusOverview
            hideHeader
          />
        </div>
      ) : null}
    </div>
  )
}

function TechnicalIndicatorSkeleton({
  symbol,
  name,
}: {
  symbol: string
  name: string
}) {
  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">Technical Indicators</div>
          <div className="mt-1 text-[11px] text-slate-500">
            正在计算 {name} · <span className="font-mono">{symbol}</span> 的技术指标
          </div>
        </div>
        <Skeleton className="h-5 w-24 rounded-full bg-white/80" />
      </div>
      <Skeleton className="h-16 rounded-xl bg-white/70" />
      <div className="space-y-1.5">
        {[0, 1, 2, 3, 4].map((index) => (
          <div key={index} className="rounded-xl bg-white/70 px-3 py-2.5">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <Skeleton className="h-4 w-40" />
              <div className="flex flex-wrap gap-2">
                <Skeleton className="h-7 w-24 rounded-full" />
                <Skeleton className="h-7 w-24 rounded-full" />
                <Skeleton className="h-7 w-24 rounded-full" />
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export function TechnicalIndicatorTab({
  targetType,
  symbol,
  name,
  period = DEFAULT_PERIOD,
  adjust = DEFAULT_ADJUST,
}: TechnicalIndicatorTabProps) {
  const [bars, setBars] = useState<StockKlineBar[]>([])
  const [indexBarsMap, setIndexBarsMap] = useState<Record<string, StockKlineBar[]>>({})
  const [stockMeta, setStockMeta] = useState<StockMeta | null>(null)
  const [breadth, setBreadth] = useState<MarketBreadth | null>(null)
  const [breadthSeries, setBreadthSeries] = useState<MarketBreadthSeries>([])
  const [error, setError] = useState<string>("")
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true

    void (async () => {
      try {
        if (!active) return
        setError("")
        setLoading(true)
        setBars([])
        setIndexBarsMap({})
        setStockMeta(null)
        setBreadth(null)
        setBreadthSeries([])
        const klineResult = await fetchStockKlines({ targetType, symbol, name, period, adjust })
        if (!active) return
        setBars(klineResult.items)

        fetchStockMeta({ targetType, symbol }).then((meta) => {
          if (!active) return

          const sectorSymbol = meta.sectorIndexSymbol || meta.industry || null
          const sectorLabel = meta.sectorIndexName || sectorSymbol
          setStockMeta({
            marketCap: meta.circMarketCap || null,
            capStyle: meta.capStyle,
            sectorIndexSymbol: sectorLabel,
          })

          void loadContextIndexBars({
            period,
            adjust,
            sector: sectorSymbol ? { symbol: sectorSymbol, label: sectorLabel || sectorSymbol } : null,
          }).then((results) => {
            if (!active) return
            setIndexBarsMap(settledBarsToMap(results))
          })
        }).catch(() => {
          if (!active) return
          setStockMeta(null)

          void loadContextIndexBars({ period, adjust }).then((results) => {
            if (!active) return
            setIndexBarsMap(settledBarsToMap(results))
          })
        })

        fetchMarketBreadth().then((b) => {
          if (!active) return
          setBreadth(b)
        }).catch(() => {
          if (active) setBreadth(null)
        })

        fetchMarketBreadthSeries().then((series) => {
          if (!active) return
          setBreadthSeries(series)
        }).catch(() => {
          if (active) setBreadthSeries([])
        })
      } catch (err) {
        if (!active) return
        const msg = err instanceof Error ? err.message : "加载技术指标失败"
        setBars([])
        setIndexBarsMap({})
        setStockMeta(null)
        setBreadth(null)
        setBreadthSeries([])
        setError(msg)
        notification.danger({ title: "加载技术指标失败", description: msg })
      } finally {
        if (active) setLoading(false)
      }
    })()

    return () => {
      active = false
    }
  }, [targetType, symbol, name, period, adjust])

  const dailyItems = useMemo(() => {
    const ordered = [...bars].sort((left, right) => barDate(left).localeCompare(barDate(right)))
    const latestByDate = new Map<string, number>()
    ordered.forEach((item, index) => latestByDate.set(barDate(item), index))
    return Array.from(latestByDate.entries())
      .sort(([left], [right]) => right.localeCompare(left))
      .slice(0, 60)
      .map(([date, index]) => ({
        date,
        bars: ordered.slice(0, index + 1),
      }))
  }, [bars])
  const latestDate = dailyItems[0]?.date

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertTitle>加载技术指标失败</AlertTitle>
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    )
  }

  if (loading && bars.length === 0) {
    return <TechnicalIndicatorSkeleton symbol={symbol} name={name} />
  }

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">Technical Indicators</div>
          <div className="text-[11px] text-slate-500">Computed by trading day · newest first · {dailyItems.length} sessions</div>
        </div>
        {latestDate ? <Badge variant="outline" className="w-fit rounded-full bg-white px-2 py-0 text-[10px]">Latest {latestDate}</Badge> : null}
      </div>

      <FiveDayTrendOverview bars={bars} />

      <div className="rounded-xl bg-amber-50/70 px-3 py-2 text-[11px] leading-5 text-amber-700">
        数据边界提示：当前标的 K 线与指数 K 线已按交易日截断计算；但行业/市值等元信息仍使用当前快照，部分市场宽度在历史日期缺失时也可能使用当前兜底。等历史上下文数据整理完成后，再改为严格的 t-date ~ t 口径。
      </div>

      {dailyItems.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">
          No technical indicator data yet
        </div>
      ) : (
        <div className="space-y-1.5">
          {dailyItems.map((item) => (
            <TechnicalDateItem
              key={item.date}
              date={item.date}
              bars={item.bars}
              allIndexBarsMap={indexBarsMap}
              stockMeta={stockMeta}
              breadth={breadth}
              breadthSeries={breadthSeries}
              latest={item.date === latestDate}
            />
          ))}
        </div>
      )}
    </section>
  )
}
