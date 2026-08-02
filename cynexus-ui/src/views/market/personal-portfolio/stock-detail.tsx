import { useCallback, useEffect, useMemo, useState } from "react"
import { Link, useParams, useSearchParams } from "react-router-dom"
import { ArrowLeft, BookOpen, CircleAlert, RefreshCw } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchStockKlines } from "@/services/market/stock-chart"
import { fetchPortfolioOverview, fetchPortfolioTrades } from "@/services/market/portfolio/portfolio"
import { fetchPortfolioReviews } from "@/services/market/portfolio/reviews"
import type { PortfolioOverview, PortfolioReview, PortfolioTrade } from "@/services/market/portfolio/types"
import type { StockKlineBar } from "@/views/stock-chart/lib/types"
import { PortfolioTradeChart } from "./components/portfolio-trade-chart"

function dateOnly(value: string) {
  return value.slice(0, 10)
}

function money(value: number | null | undefined) {
  return value == null ? "--" : Number(value).toFixed(2)
}

function pnlTone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

export default function PersonalPortfolioStockDetailPage() {
  const { symbol = "" } = useParams()
  const [searchParams] = useSearchParams()
  const [trades, setTrades] = useState<PortfolioTrade[]>([])
  const [bars, setBars] = useState<StockKlineBar[]>([])
  const [reviews, setReviews] = useState<PortfolioReview[]>([])
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [marketLoading, setMarketLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [marketError, setMarketError] = useState<string | null>(null)
  const requestedInstrumentType = searchParams.get("instrumentType") === "ETF" ? "ETF" : "STOCK"

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

    if (tradeResult.status === "fulfilled") setTrades(tradeResult.value)
    else setError(tradeResult.reason instanceof Error ? tradeResult.reason.message : String(tradeResult.reason))
    if (overviewResult.status === "fulfilled") setOverview(overviewResult.value)
    if (reviewResult.status === "fulfilled") setReviews(reviewResult.value)
    const instrumentType = tradeResult.status === "fulfilled"
      ? tradeResult.value[0]?.instrumentType ?? requestedInstrumentType
      : requestedInstrumentType
    const chartResult = await Promise.resolve(fetchStockKlines({
      targetType: instrumentType === "ETF" ? "etf" : "stock",
      symbol,
      period: "1d",
      adjust: "qfq",
    })).then((value) => ({ status: "fulfilled" as const, value }), (reason) => ({ status: "rejected" as const, reason }))
    if (chartResult.status === "fulfilled") setBars(chartResult.value.items ?? [])
    else setMarketError(chartResult.reason instanceof Error ? chartResult.reason.message : String(chartResult.reason))

    setLoading(false)
    setMarketLoading(false)
  }, [requestedInstrumentType, symbol])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const position = overview?.positions.find((item) => item.symbol === symbol)
  const instrumentType = position?.instrumentType ?? trades[0]?.instrumentType ?? requestedInstrumentType
  const stats = useMemo(() => trades.reduce((result, trade) => {
    const quantity = Number(trade.quantity)
    const notional = quantity * Number(trade.price)
    if (trade.side === "BUY") {
      result.buyQuantity += quantity
      result.buyNotional += notional
      result.buyCount += 1
    } else {
      result.sellQuantity += quantity
      result.sellNotional += notional
      result.sellCount += 1
    }
    return result
  }, { buyQuantity: 0, sellQuantity: 0, buyNotional: 0, sellNotional: 0, buyCount: 0, sellCount: 0 }), [trades])

  return (
    <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Position Detail">
      <div className="flex w-full min-w-0 flex-col gap-6">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div className="min-w-0">
            <Link to="/personal-portfolio" className="text-xs text-muted-foreground hover:text-foreground"><ArrowLeft className="mr-1 inline size-3" />Portfolio dashboard</Link>
            <h1 className="mt-2 font-mono text-2xl font-semibold tracking-tight">{symbol} <span className="align-middle rounded-full bg-muted px-2 py-0.5 font-sans text-xs font-medium text-muted-foreground">{instrumentType}</span></h1>
            <p className="mt-1 text-sm text-muted-foreground">Your confirmed execution points over the adjusted daily {instrumentType === "ETF" ? "ETF" : "stock"} K-line.</p>
          </div>
          <div className="flex flex-wrap gap-2"><Link to={`/personal-portfolio/reviews/new?symbol=${encodeURIComponent(symbol)}`}><Button variant="outline"><BookOpen className="mr-2 size-4" />New review</Button></Link><Button variant="outline" onClick={() => void load()} disabled={loading || marketLoading}><RefreshCw className="mr-2 size-4" />Refresh</Button></div>
        </header>

        {error ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Trade detail unavailable</p><p className="mt-1 text-xs">{error}</p></div></div> : null}

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6" aria-label="Position summary">
          <Metric label="Position" value={position ? String(position.quantity) : "--"} detail="Shares held" />
          <Metric label="Average cost" value={money(position?.averageCost)} detail="Weighted cost incl. fees" />
          <Metric label="Latest price" value={money(position?.latestPrice)} detail={position?.latestPrice == null ? "Quote unavailable" : "Latest available quote"} />
          <Metric label="Market value" value={money(position?.marketValue)} detail="Current quantity × price" />
          <Metric label="Unrealized P&L" value={money(position?.unrealizedPnl)} detail={position?.unrealizedReturnRate == null ? "Awaiting quote" : `${(position.unrealizedReturnRate * 100).toFixed(2)}% return`} valueClassName={pnlTone(position?.unrealizedPnl)} />
          <Metric label="Trade count" value={String(trades.length)} detail={`${stats.buyCount} buys · ${stats.sellCount} sells`} />
        </section>

        <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="trade-chart-heading">
          <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="trade-chart-heading" className="text-sm font-semibold">Buy / sell points</h2><p className="mt-1 text-xs text-muted-foreground">Red B markers are buys and green S markers are sells. Prices are execution prices; the chart uses qfq {instrumentType === "ETF" ? "ETF" : "stock"} market bars.</p></div><div className="flex items-center gap-3 text-xs text-muted-foreground"><span><span className="mr-1 inline-block size-2 rounded-full bg-rose-600" />Buy</span><span><span className="mr-1 inline-block size-2 rounded-full bg-emerald-600" />Sell</span><span className="rounded-full bg-muted px-2 py-0.5">{bars.length ? `${bars.length} daily bars` : "No market bars"}</span></div></div>
          {marketLoading ? <div className="p-4"><Skeleton className="h-[420px] rounded-md" /></div> : marketError ? <div className="flex items-start gap-2 px-4 py-12 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Market chart unavailable</p><p className="mt-1 text-xs">{marketError}</p><Button variant="outline" size="sm" className="mt-3" onClick={() => void load()}>Retry chart</Button></div></div> : bars.length ? <div className="p-2 sm:p-4"><PortfolioTradeChart symbol={symbol} bars={bars} trades={trades} /></div> : <div className="flex min-h-[420px] items-center justify-center px-4 py-12 text-center"><div><p className="text-sm font-medium">No market bars for this symbol</p><p className="mt-1 text-xs text-muted-foreground">The confirmed trade rows remain available below; try refreshing when the market data source is back.</p></div></div>}
        </section>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
          <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="trade-ledger-heading"><div className="border-b border-border/60 px-4 py-3"><h2 id="trade-ledger-heading" className="text-sm font-semibold">Trade ledger</h2><p className="mt-1 text-xs text-muted-foreground">Every marker below links back to a confirmed transaction.</p></div><div className="overflow-x-auto"><table className="w-full min-w-[680px] text-sm"><thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">Date</th><th className="px-4 py-3">Marker</th><th className="px-4 py-3 text-right">Qty</th><th className="px-4 py-3 text-right">Price</th><th className="px-4 py-3 text-right">Notional</th><th className="px-4 py-3" /></tr></thead><tbody className="divide-y divide-border/60">{trades.map((trade) => <tr key={trade.id} className="hover:bg-muted/35"><td className="px-4 py-3 font-mono text-xs">{dateOnly(trade.tradedAt)}</td><td className="px-4 py-3"><span className={trade.side === "BUY" ? "font-medium text-rose-600" : "font-medium text-emerald-600"}>{trade.side === "BUY" ? "B · Buy" : "S · Sell"}</span></td><td className="px-4 py-3 text-right font-mono tabular-nums">{trade.quantity}</td><td className="px-4 py-3 text-right font-mono tabular-nums">{money(trade.price)}</td><td className="px-4 py-3 text-right font-mono tabular-nums">{money(Number(trade.quantity) * Number(trade.price))}</td><td className="px-4 py-3 text-right">{trade.side === "SELL" ? <Link to={`/personal-portfolio/reviews/new?symbol=${encodeURIComponent(symbol)}&start=${dateOnly(trade.tradedAt)}&end=${dateOnly(trade.tradedAt)}`} className="text-xs text-blue-600 hover:underline">Review</Link> : null}</td></tr>)}{!trades.length ? <tr><td colSpan={6} className="px-4 py-12 text-center text-sm text-muted-foreground">No confirmed trades for this symbol.</td></tr> : null}</tbody></table></div></section>

          <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="symbol-reviews-heading"><div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 id="symbol-reviews-heading" className="text-sm font-semibold">Reviews for {symbol}</h2><p className="mt-1 text-xs text-muted-foreground">Keep the thesis next to the actual execution.</p></div><Link to={`/personal-portfolio/reviews/new?symbol=${encodeURIComponent(symbol)}`}><Button size="sm" variant="outline">Add</Button></Link></div>{reviews.length ? <div className="divide-y divide-border/60">{reviews.map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="block px-4 py-3 hover:bg-muted/35"><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">{review.periodStart} to {review.periodEnd}</span><span className={`rounded-full px-2 py-0.5 text-xs ${review.status === "COMPLETED" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{review.status ?? "DRAFT"}</span></div><p className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{review.thesis || "No thesis captured yet."}</p></Link>)}</div> : <div className="flex min-h-[260px] items-center justify-center px-4 py-10 text-center"><div><p className="text-sm font-medium">No review for this symbol</p><p className="mt-1 text-xs leading-5 text-muted-foreground">Use Add after reviewing a buy or sell sequence.</p></div></div>}</section>
        </div>
      </div>
    </WorkspaceShell>
  )
}

function Metric({ label, value, detail, valueClassName }: { label: string; value: string; detail: string; valueClassName?: string }) {
  return <div className="rounded-lg border border-border/60 bg-card p-4"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-2 font-mono text-lg font-semibold tabular-nums ${valueClassName ?? ""}`}>{value}</p><p className="mt-1 text-[11px] text-muted-foreground">{detail}</p></div>
}
