import { useCallback, useEffect, useMemo, useState } from "react"
import { BookOpen, CircleAlert, RefreshCw } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchStockKlines } from "@/services/market/stock-chart"
import { fetchPortfolioOverview, fetchPortfolioTrades } from "@/services/market/portfolio/portfolio"
import { fetchPortfolioReviews } from "@/services/market/portfolio/reviews"
import type { PortfolioOverview, PortfolioReview, PortfolioTrade } from "@/services/market/portfolio/types"
import type { StockKlineBar } from "@/views/stock-chart/lib/types"
import { PortfolioTradeChart } from "./portfolio-trade-chart"

interface PortfolioPositionDrawerProps {
  open: boolean
  symbol: string | null
  instrumentType: "ETF" | "STOCK"
  onOpenChange: (open: boolean) => void
}

function money(value: number | null | undefined) {
  return value == null ? "--" : Number(value).toFixed(2)
}

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  const amount = Number(value)
  return `${amount > 0 ? "+" : ""}${amount.toFixed(2)}`
}

function pnlTone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function dateOnly(value: string) {
  return value.slice(0, 10)
}

export function PortfolioPositionDrawer({ open, symbol, instrumentType: requestedInstrumentType, onOpenChange }: PortfolioPositionDrawerProps) {
  const [trades, setTrades] = useState<PortfolioTrade[]>([])
  const [bars, setBars] = useState<StockKlineBar[]>([])
  const [reviews, setReviews] = useState<PortfolioReview[]>([])
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [loading, setLoading] = useState(false)
  const [marketLoading, setMarketLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [marketError, setMarketError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!symbol) return
    setLoading(true)
    setMarketLoading(true)
    setError(null)
    setMarketError(null)
    setTrades([])
    setBars([])
    setReviews([])
    setOverview(null)

    const [tradeResult, overviewResult, reviewResult] = await Promise.allSettled([
      fetchPortfolioTrades(symbol),
      fetchPortfolioOverview(),
      fetchPortfolioReviews(symbol),
    ])
    const failures: string[] = []
    const nextTrades = tradeResult.status === "fulfilled" ? tradeResult.value : []
    setTrades(nextTrades)
    if (tradeResult.status === "rejected") failures.push(`交易明细：${messageOf(tradeResult.reason)}`)
    if (overviewResult.status === "fulfilled") setOverview(overviewResult.value)
    else failures.push(`持仓摘要：${messageOf(overviewResult.reason)}`)
    if (reviewResult.status === "fulfilled") setReviews(reviewResult.value)
    else failures.push(`复盘记录：${messageOf(reviewResult.reason)}`)
    setError(failures.length ? failures.join("；") : null)

    const effectiveInstrumentType = nextTrades[0]?.instrumentType ?? requestedInstrumentType
    try {
      const chart = await fetchStockKlines({
        targetType: effectiveInstrumentType === "ETF" ? "etf" : "stock",
        symbol,
        period: "1d",
        adjust: "qfq",
      })
      setBars(chart.items ?? [])
    } catch (cause) {
      setMarketError(messageOf(cause))
    } finally {
      setLoading(false)
      setMarketLoading(false)
    }
  }, [requestedInstrumentType, symbol])

  useEffect(() => {
    if (!open || !symbol) return
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load, open, symbol])

  const position = overview?.positions.find((item) => item.symbol === symbol)
  const effectiveInstrumentType = position?.instrumentType ?? trades[0]?.instrumentType ?? requestedInstrumentType
  const securityName = position?.securityName ?? trades.find((trade) => trade.securityName)?.securityName ?? symbol ?? "持仓详情"
  const stats = useMemo(() => trades.reduce((result, trade) => {
    if (trade.side === "BUY") result.buyCount += 1
    else result.sellCount += 1
    return result
  }, { buyCount: 0, sellCount: 0 }), [trades])

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex h-full w-full flex-col gap-0 overflow-hidden p-0 md:w-[min(820px,76vw)] md:max-w-none">
        <SheetHeader className="shrink-0 border-b border-border/60 px-5 py-4 pr-14">
          <div className="flex min-w-0 items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <SheetTitle className="truncate text-xl tracking-tight"><span className="font-mono">{symbol ?? "持仓详情"}</span><span className="ml-2 text-muted-foreground">{securityName}</span></SheetTitle>
                <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-foreground">{effectiveInstrumentType === "ETF" ? "ETF" : "股票"}</span>
              </div>
              <SheetDescription className="mt-1">持仓状态、买卖点和交易复盘</SheetDescription>
            </div>
            <span className="shrink-0 rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">{trades.length} 笔交易</span>
          </div>
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-y-auto bg-muted/20 p-4 sm:p-5">
          {error ? <div className="mb-4 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">部分持仓数据加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div></div> : null}

          <section className="mb-4 overflow-hidden rounded-lg border border-border/60 bg-card" aria-label="持仓概览">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/60 px-4 py-3">
              <div><p className="text-xs font-medium text-muted-foreground">当前持仓盈亏</p><p className={`mt-1 font-mono text-2xl font-semibold tabular-nums ${pnlTone(position?.unrealizedPnl)}`}>{signedMoney(position?.unrealizedPnl)}</p></div>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground"><span>收益率 <strong className={`font-mono tabular-nums ${pnlTone(position?.unrealizedReturnRate)}`}>{position?.unrealizedReturnRate == null ? "--" : `${(position.unrealizedReturnRate * 100).toFixed(2)}%`}</strong></span><span>已实现 <strong className={`font-mono tabular-nums ${pnlTone(position?.realizedPnl)}`}>{signedMoney(position?.realizedPnl)}</strong></span></div>
            </div>
            <div className="grid grid-cols-2 divide-x divide-y divide-border/60 sm:grid-cols-4 sm:divide-y-0">
              <SummaryValue label="持仓数量" value={position ? String(position.quantity) : "--"} detail="当前持有" />
              <SummaryValue label="持仓市值" value={money(position?.marketValue)} detail="数量 × 最新价" />
              <SummaryValue label="最新价" value={money(position?.latestPrice)} detail="市场行情" />
              <SummaryValue label="平均成本" value={money(position?.averageCost)} detail="含手续费" />
            </div>
          </section>

          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-muted-foreground">K 线来自市场数据源；红色标记为买入，绿色标记为卖出。</p>
            <div className="flex gap-2"><Link to={`/personal-portfolio/reviews/new?symbol=${encodeURIComponent(symbol ?? "")}&instrumentType=${effectiveInstrumentType}`}><Button size="sm" variant="outline"><BookOpen className="mr-2 size-4" />新建复盘</Button></Link><Button size="sm" variant="outline" onClick={() => void load()} disabled={loading || marketLoading}><RefreshCw className={`mr-2 size-4 ${(loading || marketLoading) ? "animate-spin" : ""}`} />刷新</Button></div>
          </div>

          <section className="mb-4 overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-trade-chart-heading">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="portfolio-trade-chart-heading" className="text-base font-semibold">价格走势与买卖点</h2><p className="mt-1 text-xs text-muted-foreground">确认交易显示在对应的日 K 上，便于回看入场和退出位置。</p></div><span className="rounded-full bg-muted px-2 py-1 text-[11px] text-muted-foreground">{bars.length ? `${bars.length} 根日 K` : "暂无 K 线"}</span></div>
            {marketLoading ? <div className="p-4"><Skeleton className="h-[340px] rounded-md" /></div> : marketError ? <div className="flex items-start gap-2 px-4 py-10 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">行情加载失败</p><p className="mt-1 break-words text-xs">{marketError}</p><Button variant="outline" size="sm" className="mt-3" onClick={() => void load()}>重试行情</Button></div></div> : bars.length ? <div className="p-2 sm:p-3"><PortfolioTradeChart symbol={symbol ?? ""} bars={bars} trades={trades} /></div> : <div className="flex min-h-[260px] items-center justify-center px-4 py-10 text-center text-xs text-muted-foreground">暂无该标的 K 线数据</div>}
          </section>

          <div className="grid gap-4 lg:grid-cols-2">
            <TradeLedger trades={trades} />
            <ReviewList reviews={reviews} stats={stats} symbol={symbol} />
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function SummaryValue({ label, value, detail }: { label: string; value: string; detail: string }) {
  return <div className="min-w-0 px-4 py-3"><p className="text-xs text-muted-foreground">{label}</p><p className="mt-1 font-mono text-base font-semibold tabular-nums text-foreground">{value}</p><p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p></div>
}

function TradeLedger({ trades }: { trades: PortfolioTrade[] }) {
  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-trade-ledger-heading">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="portfolio-trade-ledger-heading" className="text-sm font-semibold">交易明细</h2><p className="mt-1 text-xs text-muted-foreground">共 {trades.length} 笔确认交易</p></div><span className="rounded-full bg-muted px-2 py-1 text-[11px] text-muted-foreground">买卖记录</span></div>
    <div className="max-h-[255px] overflow-auto"><table className="w-full min-w-[420px] text-sm"><thead className="sticky top-0 bg-muted/95 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-2.5">日期</th><th className="px-4 py-2.5">方向</th><th className="px-4 py-2.5 text-right">数量</th><th className="px-4 py-2.5 text-right">价格</th><th className="px-4 py-2.5 text-right">成交额</th></tr></thead><tbody className="divide-y divide-border/60">{trades.map((trade) => <tr key={trade.id} className="hover:bg-muted/35"><td className="px-4 py-2.5 font-mono text-xs">{dateOnly(trade.tradedAt)}</td><td className={`px-4 py-2.5 font-medium ${trade.side === "BUY" ? "text-rose-600" : "text-emerald-600"}`}>{trade.side === "BUY" ? "买入" : "卖出"}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{trade.quantity}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{money(trade.price)}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{money(Number(trade.quantity) * Number(trade.price))}</td></tr>)}{!trades.length ? <tr><td colSpan={5} className="px-4 py-10 text-center text-xs text-muted-foreground">暂无已确认交易</td></tr> : null}</tbody></table></div>
  </section>
}

function ReviewList({ reviews, stats, symbol }: { reviews: PortfolioReview[]; stats: { buyCount: number; sellCount: number }; symbol: string | null }) {
  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-symbol-reviews-heading">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="portfolio-symbol-reviews-heading" className="text-sm font-semibold">复盘记录</h2><p className="mt-1 text-xs text-muted-foreground">把交易计划和执行结果放在一起。</p></div><span className="rounded-full bg-muted px-2 py-1 text-[11px] text-muted-foreground">{stats.buyCount} 买 · {stats.sellCount} 卖</span></div>
    {reviews.length ? <div className="max-h-[255px] divide-y divide-border/60 overflow-auto">{reviews.slice(0, 5).map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="block px-4 py-3 hover:bg-muted/35"><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">{review.periodStart} 至 {review.periodEnd}</span><span className={`rounded-full px-2 py-0.5 text-xs ${review.status === "COMPLETED" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{review.status === "COMPLETED" ? "已完成" : "草稿"}</span></div><p className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{review.thesis || "尚未填写交易逻辑。"}</p></Link>)}</div> : <div className="flex min-h-[190px] items-center justify-center px-4 py-8 text-center"><div><p className="text-sm font-medium">暂无复盘记录</p><p className="mt-1 text-xs text-muted-foreground">点击上方“新建复盘”，记录 {symbol ?? "该标的"} 的交易决策。</p></div></div>}
  </section>
}

function messageOf(cause: unknown) {
  return cause instanceof Error ? cause.message : String(cause)
}
