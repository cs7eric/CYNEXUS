import { useCallback, useEffect, useRef, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { ArrowUpRight, Camera, RefreshCw, Upload, WalletCards } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { fetchPortfolioOverview } from "@/services/market/portfolio/portfolio"
import { uploadPortfolioScreenshot } from "@/services/market/portfolio/imports"
import { fetchPortfolioReviews } from "@/services/market/portfolio/reviews"
import type { PortfolioOverview, PortfolioReview } from "@/services/market/portfolio/types"

const money = (value: number | null | undefined) => value == null ? "--" : value.toFixed(2)

export default function PersonalPortfolioPage() {
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [reviews, setReviews] = useState<PortfolioReview[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const [nextOverview, nextReviews] = await Promise.all([fetchPortfolioOverview(), fetchPortfolioReviews()])
      setOverview(nextOverview); setReviews(nextReviews)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message); notification.danger({ title: "Portfolio load failed", description: message })
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 0); return () => window.clearTimeout(timer) }, [load])

  async function onFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""
    if (!file) return
    if (!/^image\/(png|jpeg|webp)$/.test(file.type)) { notification.warn({ title: "Unsupported screenshot", description: "Use PNG, JPEG, or WebP." }); return }
    setUploading(true)
    try {
      const task = await uploadPortfolioScreenshot(file, `ui-${crypto.randomUUID()}`)
      notification.success({ title: "Screenshot queued", description: "AI parsing is running in the background." })
      navigate(`/personal-portfolio/imports/${task.id}`)
    } catch (cause) {
      notification.danger({ title: "Upload failed", description: cause instanceof Error ? cause.message : String(cause) })
    } finally { setUploading(false) }
  }

  return <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Personal Portfolio">
    <div className="flex w-full min-w-0 flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0"><div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><WalletCards className="size-4" />Personal portfolio</div><h1 className="mt-1 text-2xl font-semibold tracking-tight">Holdings & trade journal</h1><p className="mt-1 text-sm text-muted-foreground">Upload one trading screenshot, verify AI candidates, then confirm them into your ledger.</p></div>
        <div className="flex flex-wrap gap-2"><input ref={inputRef} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={onFileChange} /><Button onClick={() => inputRef.current?.click()} disabled={uploading}><Upload className="mr-2 size-4" />{uploading ? "Uploading..." : "Import screenshot"}</Button><Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className="mr-2 size-4" />Refresh</Button></div>
      </header>
      {error && !loading ? <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div> : null}
      {loading ? <div className="grid gap-4 md:grid-cols-4"><Skeleton className="h-24 rounded-lg" /><Skeleton className="h-24 rounded-lg" /><Skeleton className="h-24 rounded-lg" /><Skeleton className="h-24 rounded-lg" /></div> : <>
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[['Total P&L', overview?.totalPnl], ['Realized P&L', overview?.realizedPnl], ['Unrealized P&L', overview?.unrealizedPnl], ['Confirmed trades', overview?.tradeCount]].map(([label, value]) => <div key={String(label)} className="rounded-lg border border-border/60 bg-card p-4"><p className="text-xs text-muted-foreground">{label}</p><p className="mt-2 font-mono text-xl font-semibold tabular-nums">{typeof value === "number" && label !== "Confirmed trades" ? money(value) : value ?? "0"}</p></div>)}
        </section>
        <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">Current positions</h2><p className="mt-1 text-xs text-muted-foreground">{overview?.valuationStatus === "LATEST_PRICE_UNAVAILABLE" ? "Latest market prices are not available; unrealized P&L is provisional." : "Latest valuation"}</p></div><span className="text-xs text-muted-foreground">{overview?.positions.length ?? 0} symbols</span></div><div className="overflow-x-auto"><table className="w-full min-w-[680px] text-sm"><thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">Symbol</th><th className="px-4 py-3 text-right">Qty</th><th className="px-4 py-3 text-right">Avg cost</th><th className="px-4 py-3 text-right">Market value</th><th className="px-4 py-3 text-right">Unrealized</th><th className="px-4 py-3" /></tr></thead><tbody className="divide-y divide-border/60">{overview?.positions.map((position) => <tr key={position.symbol} className="hover:bg-muted/35"><td className="px-4 py-3 font-mono">{position.symbol}</td><td className="px-4 py-3 text-right font-mono">{position.quantity}</td><td className="px-4 py-3 text-right font-mono">{money(position.averageCost)}</td><td className="px-4 py-3 text-right font-mono">{money(position.marketValue)}</td><td className="px-4 py-3 text-right font-mono">{money(position.unrealizedPnl)}</td><td className="px-4 py-3 text-right"><Link className="text-xs text-blue-600 hover:underline" to={`/personal-portfolio/stocks/${position.symbol}`}>Details <ArrowUpRight className="inline size-3" /></Link></td></tr>)}{!overview?.positions.length ? <tr><td colSpan={6} className="px-4 py-12 text-center text-sm text-muted-foreground">No confirmed trades yet. Import a screenshot to start the ledger.</td></tr> : null}</tbody></table></div></section>
        <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">Trade reviews</h2><p className="mt-1 text-xs text-muted-foreground">Keep a decision record for each symbol and period.</p></div><Link to="/personal-portfolio/reviews/new"><Button size="sm" variant="outline"><Camera className="mr-2 size-4" />New review</Button></Link></div><div className="divide-y divide-border/60">{reviews.slice(0, 5).map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="flex items-center justify-between px-4 py-3 text-sm hover:bg-muted/35"><span><span className="font-mono">{review.symbol}</span><span className="ml-3 text-xs text-muted-foreground">{review.periodStart} to {review.periodEnd}</span></span><span className="text-xs text-muted-foreground">{review.status || "DRAFT"}</span></Link>)}{!reviews.length ? <div className="px-4 py-8 text-sm text-muted-foreground">No reviews yet.</div> : null}</div></section>
      </>}
    </div>
  </WorkspaceShell>
}
