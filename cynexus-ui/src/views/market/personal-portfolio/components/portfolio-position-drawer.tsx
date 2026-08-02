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
    const [tradeResult, overviewResult, reviewResult] = await Promise.allSettled([
      fetchPortfolioTrades(symbol),
      fetchPortfolioOverview(),
      fetchPortfolioReviews(symbol),
    ])
    const nextTrades = tradeResult.status === "fulfilled" ? tradeResult.value : []
    setTrades(nextTrades)
    if (tradeResult.status === "rejected") setError(tradeResult.reason instanceof Error ? tradeResult.reason.message : String(tradeResult.reason))
    if (overviewResult.status === "fulfilled") setOverview(overviewResult.value)
    if (reviewResult.status === "fulfilled") setReviews(reviewResult.value)

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
      setMarketError(cause instanceof Error ? cause.message : String(cause))
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
      <SheetContent side="right" className="flex h-full w-full flex-col gap-0 overflow-hidden p-0 md:w-[min(760px,72vw)] md:max-w-none">
        <SheetHeader className="shrink-0 border-b border-border/60 px-5 py-4 pr-14">
          <div className="flex min-w-0 items-start justify-between gap-4">
            <div className="min-w-0">
              <SheetTitle className="truncate text-lg"><span className="font-mono">{symbol ?? "持仓详情"}</span><span className="ml-2 text-muted-foreground">{securityName}</span></SheetTitle>
              <SheetDescription className="mt-1">{effectiveInstrumentType === "ETF" ? "ETF" : "股票"} · 买卖点、持仓盈亏与复盘记录</SheetDescription>
            </div>
            <span className="shrink-0 rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">{trades.length} 笔交易</span>
          </div>
        </SheetHeader>

        <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-5">
          {error ? <div className="mb-4 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">交易数据加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div></div> : null}

          <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-muted-foreground">日 K 线使用市场数据源，红色为买入，绿色为卖出。</p>
            <div className="flex gap-2"><Link to={`/personal-portfolio/reviews/new?symbol=${encodeURIComponent(symbol ?? "")}&instrumentType=${effectiveInstrumentType}`}><Button size="sm" variant="outline"><BookOpen className="mr-2 size-4" />新建复盘</Button></Link><Button size="sm" variant="outline" onClick={() => void load()} disabled={loading || marketLoading}><RefreshCw className={`mr-2 size-4 ${(loading || marketLoading) ? "animate-spin" : ""}`} />刷新</Button></div>
          </div>

          <section className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3" aria-label="持仓摘要">
            <Metric label="持仓数量" value={position ? String(position.quantity) : "--"} detail="当前持有数量" />
            <Metric label="平均成本" value={money(position?.averageCost)} detail="含手续费加权成本" />
            <Metric label="最新价" value={money(position?.latestPrice)} detail={position?.latestPrice == null ? "行情暂不可用" : "最新市场价"} />
            <Metric label="持仓市值" value={money(position?.marketValue)} detail="持仓数量 × 最新价" />
            <Metric label="持仓浮动盈亏" value={money(position?.unrealizedPnl)} detail={position?.unrealizedReturnRate == null ? "等待行情" : `${(position.unrealizedReturnRate * 100).toFixed(2)}%`} valueClassName={pnlTone(position?.unrealizedPnl)} />
            <Metric label="已实现盈亏" value={money(position?.realizedPnl)} detail="已卖出部分的盈亏" valueClassName={pnlTone(position?.realizedPnl)} />
          </section>

          <section className="mb-5 overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-trade-chart-heading">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="portfolio-trade-chart-heading" className="text-sm font-semibold">买卖点与日 K 线</h2><p className="mt-1 text-xs text-muted-foreground">执行价格来自你的确认交易，K 线来自市场行情数据源。</p></div><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{bars.length ? `${bars.length} 根日 K` : "暂无 K 线"}</span></div>
            {marketLoading ? <div className="p-4"><Skeleton className="h-[320px] rounded-md" /></div> : marketError ? <div className="flex items-start gap-2 px-4 py-10 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">行情加载失败</p><p className="mt-1 break-words text-xs">{marketError}</p><Button variant="outline" size="sm" className="mt-3" onClick={() => void load()}>重试</Button></div></div> : bars.length ? <div className="p-2 sm:p-3"><PortfolioTradeChart symbol={symbol ?? ""} bars={bars} trades={trades} /></div> : <div className="flex min-h-[260px] items-center justify-center px-4 py-10 text-center text-xs text-muted-foreground">暂无该标的 K 线数据</div>}
          </section>

          <section className="mb-5 overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-trade-ledger-heading">
            <div className="border-b border-border/60 px-4 py-3"><h2 id="portfolio-trade-ledger-heading" className="text-sm font-semibold">交易明细</h2><p className="mt-1 text-xs text-muted-foreground">每一笔交易都对应图表中的一个买卖点。</p></div>
            <div className="max-h-[260px] overflow-auto"><table className="w-full min-w-[560px] text-sm"><thead className="sticky top-0 bg-muted/90 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-2.5">日期</th><th className="px-4 py-2.5">方向</th><th className="px-4 py-2.5 text-right">数量</th><th className="px-4 py-2.5 text-right">价格</th><th className="px-4 py-2.5 text-right">成交额</th></tr></thead><tbody className="divide-y divide-border/60">{trades.map((trade) => <tr key={trade.id} className="hover:bg-muted/35"><td className="px-4 py-2.5 font-mono text-xs">{dateOnly(trade.tradedAt)}</td><td className={`px-4 py-2.5 font-medium ${trade.side === "BUY" ? "text-rose-600" : "text-emerald-600"}`}>{trade.side === "BUY" ? "买入" : "卖出"}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{trade.quantity}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{money(trade.price)}</td><td className="px-4 py-2.5 text-right font-mono tabular-nums">{money(Number(trade.quantity) * Number(trade.price))}</td></tr>)}{!trades.length ? <tr><td colSpan={5} className="px-4 py-10 text-center text-xs text-muted-foreground">暂无已确认交易</td></tr> : null}</tbody></table></div>
          </section>

          <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="portfolio-symbol-reviews-heading">
            <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="portfolio-symbol-reviews-heading" className="text-sm font-semibold">复盘记录</h2><p className="mt-1 text-xs text-muted-foreground">把交易计划、执行和结果放在一起。</p></div><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{stats.buyCount} 买 · {stats.sellCount} 卖</span></div>
            {reviews.length ? <div className="divide-y divide-border/60">{reviews.slice(0, 5).map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="block px-4 py-3 hover:bg-muted/35"><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">{review.periodStart} 至 {review.periodEnd}</span><span className={`rounded-full px-2 py-0.5 text-xs ${review.status === "COMPLETED" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{review.status === "COMPLETED" ? "已完成" : "草稿"}</span></div><p className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{review.thesis || "尚未填写交易逻辑。"}</p></Link>)}</div> : <div className="flex min-h-[150px] items-center justify-center px-4 py-8 text-center"><div><p className="text-sm font-medium">暂无复盘记录</p><p className="mt-1 text-xs text-muted-foreground">点击右上角“新建复盘”记录这笔交易。</p></div></div>}
          </section>
        </div>
      </SheetContent>
    </Sheet>
  )
}

function Metric({ label, value, detail, valueClassName }: { label: string; value: string; detail: string; valueClassName?: string }) {
  return <div className="rounded-md border border-border/60 bg-muted/20 p-3"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1.5 font-mono text-base font-semibold tabular-nums ${valueClassName ?? ""}`}>{value}</p><p className="mt-1 text-[11px] text-muted-foreground">{detail}</p></div>
}
