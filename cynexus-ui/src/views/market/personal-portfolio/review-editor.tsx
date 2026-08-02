import { useEffect, useState } from "react"
import { useNavigate, useParams, useSearchParams } from "react-router-dom"
import { ArrowLeft, CheckCircle2, Save } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { fetchPortfolioReview, savePortfolioReview } from "@/services/market/portfolio/reviews"
import type { PortfolioReview } from "@/services/market/portfolio/types"

function defaultReview(symbol: string) {
  const today = new Date().toISOString().slice(0, 10)
  return { symbol, periodStart: today, periodEnd: today, status: "DRAFT" } satisfies PortfolioReview
}

export default function PersonalPortfolioReviewEditorPage() {
  const { reviewId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [review, setReview] = useState<PortfolioReview>(() => defaultReview(searchParams.get("symbol") ?? ""))
  const [loading, setLoading] = useState(Boolean(reviewId && reviewId !== "new"))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!reviewId || reviewId === "new") return
    const id = Number(reviewId)
    if (!Number.isFinite(id)) return
    void fetchPortfolioReview(id).then(setReview).catch((cause) => {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "Review load failed", description: message })
    }).finally(() => setLoading(false))
  }, [reviewId])

  function set(key: keyof PortfolioReview, value: string | number | null) {
    setReview((current) => ({ ...current, [key]: value }))
  }

  async function save(status = review.status ?? "DRAFT") {
    if (!review.symbol.trim()) {
      notification.warn({ title: "Symbol required", description: "Attach the review to a stock symbol before saving." })
      return
    }
    setSaving(true)
    try {
      const saved = await savePortfolioReview({ ...review, symbol: review.symbol.trim().toUpperCase(), status })
      setReview(saved)
      notification.success({ title: status === "COMPLETED" ? "Review completed" : "Review saved", description: "The decision journal is stored with your portfolio." })
      if (status === "COMPLETED") navigate(`/personal-portfolio/stocks/${encodeURIComponent(saved.symbol)}`)
      else if (reviewId === "new") navigate(`/personal-portfolio/reviews/${saved.id}`)
    } catch (cause) {
      notification.danger({ title: "Review save failed", description: cause instanceof Error ? cause.message : String(cause) })
    } finally {
      setSaving(false)
    }
  }

  return <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Portfolio Review">
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4"><div className="min-w-0"><Button variant="ghost" size="sm" className="-ml-2 mb-2" onClick={() => navigate(-1)}><ArrowLeft className="mr-2 size-4" />Back</Button><p className="text-xs text-muted-foreground">Decision journal</p><h1 className="mt-1 text-2xl font-semibold tracking-tight">Review a trading period</h1><p className="mt-1 text-sm text-muted-foreground">Record the thesis, plan, execution gap, result, and next action while the trade context is still available.</p></div><div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => void save("DRAFT")} disabled={saving}><Save className="mr-2 size-4" />{saving ? "Saving..." : "Save draft"}</Button><Button onClick={() => void save("COMPLETED")} disabled={saving}><CheckCircle2 className="mr-2 size-4" />Complete review</Button></div></header>
      {error ? <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div> : null}
      {loading ? <Skeleton className="h-[520px] rounded-lg" /> : <section className="grid gap-4 rounded-lg border border-border/60 bg-card p-4 sm:grid-cols-2">
        <label className="space-y-1 text-xs text-muted-foreground">Symbol<Input value={review.symbol} onChange={(event) => set("symbol", event.target.value)} placeholder="600000" /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Status<select className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground" value={review.status ?? "DRAFT"} onChange={(event) => set("status", event.target.value)}><option value="DRAFT">Draft</option><option value="COMPLETED">Completed</option></select></label>
        <label className="space-y-1 text-xs text-muted-foreground">Start date<Input type="date" value={review.periodStart} onChange={(event) => set("periodStart", event.target.value)} /></label>
        <label className="space-y-1 text-xs text-muted-foreground">End date<Input type="date" value={review.periodEnd} onChange={(event) => set("periodEnd", event.target.value)} /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Rating (1-5)<Input type="number" min={1} max={5} value={review.rating ?? ""} onChange={(event) => set("rating", event.target.value ? Number(event.target.value) : null)} /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Tags<Input value={review.tags ?? ""} onChange={(event) => set("tags", event.target.value)} placeholder="breakout, follow-through" /></label>
        <label className="space-y-1 text-xs text-muted-foreground sm:col-span-2">Thesis<Textarea value={review.thesis ?? ""} onChange={(event) => set("thesis", event.target.value)} rows={4} placeholder="Why did you enter or exit this position?" /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Plan<Textarea value={review.plan ?? ""} onChange={(event) => set("plan", event.target.value)} rows={5} placeholder="What was the invalidation and exit plan?" /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Execution<Textarea value={review.execution ?? ""} onChange={(event) => set("execution", event.target.value)} rows={5} placeholder="What actually happened?" /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Lessons<Textarea value={review.lessons ?? ""} onChange={(event) => set("lessons", event.target.value)} rows={5} placeholder="What should be repeated or avoided?" /></label>
        <label className="space-y-1 text-xs text-muted-foreground">Next action<Textarea value={review.nextAction ?? ""} onChange={(event) => set("nextAction", event.target.value)} rows={5} placeholder="What will you do next?" /></label>
        <div className="sm:col-span-2 rounded-md bg-muted/35 px-3 py-3 text-xs leading-5 text-muted-foreground">Objective result snapshot is kept separate from your notes. It is based on confirmed portfolio trades and market data, not on text entered here.</div>
      </section>}
    </div>
  </WorkspaceShell>
}
