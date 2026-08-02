import { useCallback, useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { ArrowUpRight, ClipboardCheck, CircleAlert, RefreshCw, Upload, WalletCards } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { fetchPortfolioOverview } from "@/services/market/portfolio/portfolio"
import { fetchPortfolioReviews } from "@/services/market/portfolio/reviews"
import type { PortfolioOverview, PortfolioPosition, PortfolioReview } from "@/services/market/portfolio/types"
import { PortfolioReturnBreakdown } from "./components/portfolio-return-breakdown"
import { PortfolioImportDrawer } from "./components/portfolio-import-drawer"

const money = (value: number | null | undefined) => value == null ? "--" : value.toFixed(2)
const percent = (value: number | null | undefined) => value == null ? "--" : `${(value * 100).toFixed(2)}%`

function pnlTone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function positionValue(position: PortfolioPosition, key: "marketValue" | "costBasis") {
  return Number(position[key] ?? 0)
}

export default function PersonalPortfolioPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [reviews, setReviews] = useState<PortfolioReview[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const importDrawerOpen = searchParams.get("imports") === "1"

  function setImportDrawerOpen(open: boolean) {
    const next = new URLSearchParams(searchParams)
    if (open) next.set("imports", "1")
    else next.delete("imports")
    setSearchParams(next, { replace: true })
  }

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [nextOverview, nextReviews] = await Promise.all([fetchPortfolioOverview(), fetchPortfolioReviews()])
      setOverview(nextOverview)
      setReviews(nextReviews)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "Portfolio load failed", description: message })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const positions = useMemo(() => overview?.positions ?? [], [overview])
  const costBasis = overview?.costBasis ?? positions.reduce((sum, position) => sum + positionValue(position, "costBasis"), 0)
  const marketValue = overview?.marketValue ?? positions.reduce((sum, position) => sum + positionValue(position, "marketValue"), 0)
  const totalPnl = overview?.totalPnl ?? 0
  const reviewDue = reviews.filter((review) => (review.status ?? "DRAFT") !== "COMPLETED").length
  const largestPosition = useMemo(() => positions.reduce<PortfolioPosition | null>((largest, position) => {
    if (!largest || positionValue(position, "marketValue") > positionValue(largest, "marketValue")) return position
    return largest
  }, null), [positions])

  return (
    <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Personal Portfolio">
      <div className="flex w-full min-w-0 flex-col gap-6">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><WalletCards className="size-4" />Personal portfolio</div>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">Holdings dashboard</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">Track confirmed positions, execution P&L, buy/sell context, and the reviews that explain each decision.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => setImportDrawerOpen(true)}><Upload className="mr-2 size-4" />Import trades</Button>
            <Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className="mr-2 size-4" />Refresh</Button>
          </div>
        </header>

        {error && !loading ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Portfolio data unavailable</p><p className="mt-1 text-xs">{error}</p></div><Button variant="outline" size="sm" className="ml-auto" onClick={() => void load()}>Retry</Button></div> : null}

        {loading ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">{Array.from({ length: 5 }, (_, index) => <Skeleton key={index} className="h-24 rounded-lg" />)}</div> : (
          <>
            <section aria-labelledby="holdings-summary" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              <h2 id="holdings-summary" className="sr-only">Holdings summary</h2>
              <Metric label="Market value" value={money(marketValue)} detail={overview?.valuationStatus === "LATEST_PRICE_UNAVAILABLE" ? "Latest prices unavailable" : "Latest available quote"} />
              <Metric label="Cost basis" value={money(costBasis)} detail="Confirmed buy cost + fees" />
              <Metric label="Total P&L" value={money(totalPnl)} detail="Realized + unrealized" valueClassName={pnlTone(totalPnl)} />
              <Metric label="Open positions" value={String(positions.filter((position) => Number(position.quantity) > 0).length)} detail={`${positions.length} symbols in ledger`} />
              <Metric label="Reviews to finish" value={String(reviewDue)} detail={reviewDue ? "Open the review center" : "No open review"} valueClassName={reviewDue ? "text-amber-600" : undefined} />
            </section>

            <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="positions-heading">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
                <div><h2 id="positions-heading" className="text-sm font-semibold">Current positions</h2><p className="mt-1 text-xs text-muted-foreground">Holdings are derived only from trades you confirmed. Market values use the latest available quote.</p></div>
                <div className="text-right text-xs text-muted-foreground">{largestPosition ? `Largest: ${largestPosition.symbol}` : "No confirmed holdings"}</div>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-sm">
                  <thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">Symbol</th><th className="px-4 py-3 text-right">Qty</th><th className="px-4 py-3 text-right">Avg cost</th><th className="px-4 py-3 text-right">Latest</th><th className="px-4 py-3 text-right">Market value</th><th className="px-4 py-3 text-right">Unrealized P&L</th><th className="px-4 py-3 text-right">Return</th><th className="px-4 py-3" /></tr></thead>
                  <tbody className="divide-y divide-border/60">
                    {positions.map((position) => <tr key={position.symbol} className="hover:bg-muted/35">
                      <td className="px-4 py-3"><Link className="font-mono font-medium text-blue-600 hover:underline" to={`/personal-portfolio/stocks/${position.symbol}?instrumentType=${position.instrumentType}`}>{position.symbol}</Link><span className="ml-2 rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground">{position.instrumentType}</span></td>
                      <td className="px-4 py-3 text-right font-mono tabular-nums">{position.quantity}</td>
                      <td className="px-4 py-3 text-right font-mono tabular-nums">{money(position.averageCost)}</td>
                      <td className="px-4 py-3 text-right font-mono tabular-nums">{money(position.latestPrice)}</td>
                      <td className="px-4 py-3 text-right font-mono tabular-nums">{money(position.marketValue)}</td>
                      <td className={`px-4 py-3 text-right font-mono tabular-nums ${pnlTone(position.unrealizedPnl)}`}>{money(position.unrealizedPnl)}</td>
                      <td className={`px-4 py-3 text-right font-mono tabular-nums ${pnlTone(position.unrealizedReturnRate)}`}>{percent(position.unrealizedReturnRate)}</td>
                      <td className="px-4 py-3 text-right"><Link className="text-xs text-blue-600 hover:underline" to={`/personal-portfolio/stocks/${position.symbol}?instrumentType=${position.instrumentType}`}>Chart <ArrowUpRight className="inline size-3" /></Link></td>
                    </tr>)}
                    {!positions.length ? <tr><td colSpan={8} className="px-4 py-14 text-center"><p className="text-sm font-medium">No confirmed positions yet</p><p className="mt-1 text-xs text-muted-foreground">Import a screenshot, correct the candidates, and confirm the trades to populate this dashboard.</p></td></tr> : null}
                  </tbody>
                </table>
              </div>
            </section>

            <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(300px,0.65fr)]">
              <PortfolioReturnBreakdown overview={overview} />
              <ReviewCenter reviews={reviews} />
            </div>
          </>
        )}
        <PortfolioImportDrawer open={importDrawerOpen} onOpenChange={setImportDrawerOpen} />
      </div>
    </WorkspaceShell>
  )
}

function Metric({ label, value, detail, valueClassName }: { label: string; value: string; detail: string; valueClassName?: string }) {
  return <div className="rounded-lg border border-border/60 bg-card p-4"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-2 font-mono text-xl font-semibold tabular-nums ${valueClassName ?? ""}`}>{value}</p><p className="mt-1 text-[11px] text-muted-foreground">{detail}</p></div>
}

function ReviewCenter({ reviews }: { reviews: PortfolioReview[] }) {
  const openReviews = reviews.filter((review) => (review.status ?? "DRAFT") !== "COMPLETED")
  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="review-center-heading">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><div className="flex items-center gap-2"><ClipboardCheck className="size-4 text-muted-foreground" /><h2 id="review-center-heading" className="text-sm font-semibold">Review center</h2></div><p className="mt-1 text-xs text-muted-foreground">Connect your trading plan, execution, and lessons.</p></div><Link to="/personal-portfolio/reviews/new"><Button size="sm" variant="outline">New review</Button></Link></div>
    {reviews.length ? <div className="divide-y divide-border/60">{reviews.slice(0, 6).map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-muted/35"><div className="min-w-0"><p className="truncate text-sm"><span className="font-mono font-medium">{review.symbol}</span><span className="ml-2 text-xs text-muted-foreground">{review.periodStart} to {review.periodEnd}</span></p><p className="mt-1 line-clamp-1 text-xs text-muted-foreground">{review.thesis || "No thesis captured yet."}</p></div><span className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${review.status === "COMPLETED" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{review.status ?? "DRAFT"}</span></Link>)}</div> : <div className="flex min-h-[220px] items-center justify-center px-4 py-10 text-center"><div><p className="text-sm font-medium">No reviews yet</p><p className="mt-1 text-xs leading-5 text-muted-foreground">Open a stock chart after confirming a trade, then capture the reason and result while the context is fresh.</p></div></div>}
    {openReviews.length > 6 ? <div className="border-t border-border/60 px-4 py-2 text-right text-xs text-muted-foreground">{openReviews.length} open reviews</div> : null}
  </section>
}
