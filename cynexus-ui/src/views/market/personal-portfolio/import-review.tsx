import { useCallback, useEffect, useMemo, useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { Check, CircleAlert, Loader2, Pencil, RefreshCw, Save } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import {
  confirmPortfolioImport,
  fetchPortfolioImport,
  fetchPortfolioImportHistory,
  fetchPortfolioImportReview,
  updatePortfolioCandidate,
  type PortfolioCandidateEdit,
} from "@/services/market/portfolio/imports"
import type { PortfolioImportProcessHistory, PortfolioImportReview } from "@/services/market/portfolio/types"
import { ImportExecutionRecord } from "./components/import-execution-record"

const terminal = new Set(["READY_FOR_REVIEW", "CONFIRMED", "FAILED", "CANCELLED"])
type Draft = PortfolioCandidateEdit

function toDraft(payload: string, version: number): Draft {
  let data: Record<string, unknown> = {}
  try { data = JSON.parse(payload) } catch { /* validation is handled by the API */ }
  return {
    version,
    symbol: String(data.symbol ?? ""),
    securityName: String(data.securityName ?? ""),
    side: data.side === "SELL" ? "SELL" : "BUY",
    tradedAt: String(data.tradedAt ?? ""),
    quantity: String(data.quantity ?? ""),
    price: String(data.price ?? ""),
    totalFee: String(data.fee ?? data.totalFee ?? "0"),
    externalTradeNo: String(data.externalTradeNo ?? ""),
  }
}

export default function PersonalPortfolioImportReviewPage() {
  const { taskId } = useParams(); const navigate = useNavigate(); const id = Number(taskId)
  const [review, setReview] = useState<PortfolioImportReview | null>(null)
  const [history, setHistory] = useState<PortfolioImportProcessHistory | null>(null)
  const [drafts, setDrafts] = useState<Record<number, Draft>>({})
  const [loading, setLoading] = useState(true); const [historyLoading, setHistoryLoading] = useState(true)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [savingId, setSavingId] = useState<number | null>(null); const [confirming, setConfirming] = useState(false)
  const [selected, setSelected] = useState<number[]>([]); const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const item = await fetchPortfolioImportReview(id)
      setReview(item)
      setSelected(item.candidates.filter((candidate) => candidate.reviewStatus === "PENDING").map((candidate) => candidate.id))
      setDrafts(Object.fromEntries(item.candidates.map((candidate) => [candidate.id, toDraft(candidate.normalizedPayload, candidate.version)])))
    } catch (cause) { setError(cause instanceof Error ? cause.message : String(cause)) } finally { setLoading(false) }
  }, [id])

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true); setHistoryError(null)
    try { setHistory(await fetchPortfolioImportHistory(id)) } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setHistoryError(message)
      notification.danger({ title: "Process history unavailable", description: message })
    } finally { setHistoryLoading(false) }
  }, [id])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); void loadHistory() }, 0)
    return () => window.clearTimeout(timer)
  }, [load, loadHistory])
  const status = review?.task.status
  useEffect(() => {
    if (!status || terminal.has(status)) return
    const timer = window.setInterval(() => {
      void fetchPortfolioImport(id).then((task) => { setReview((current) => current ? ({ ...current, task }) : current); void loadHistory() }).catch(() => undefined)
    }, 2500)
    return () => window.clearInterval(timer)
  }, [id, status, loadHistory])

  const pending = useMemo(() => review?.candidates.filter((candidate) => candidate.reviewStatus === "PENDING") ?? [], [review])
  const updateDraft = (candidateId: number, key: keyof Draft, value: string) => setDrafts((current) => ({ ...current, [candidateId]: { ...current[candidateId], [key]: value } }))
  async function saveCandidate(candidateId: number) {
    const draft = drafts[candidateId]; if (!draft) return
    setSavingId(candidateId)
    try {
      const saved = await updatePortfolioCandidate(id, candidateId, draft)
      setReview((current) => current ? ({ ...current, candidates: current.candidates.map((candidate) => candidate.id === candidateId ? saved : candidate) }) : current)
      setDrafts((current) => ({ ...current, [candidateId]: { ...current[candidateId], version: saved.version } }))
      await loadHistory(); notification.success({ title: "Candidate saved", description: `Row ${saved.rowNo} is ready for confirmation.` })
    } catch (cause) { notification.danger({ title: "Candidate save failed", description: cause instanceof Error ? cause.message : String(cause) }) } finally { setSavingId(null) }
  }
  async function confirm() {
    if (!review) return; setConfirming(true)
    try { await confirmPortfolioImport(id, review.task.version, selected); notification.success({ title: "Trades confirmed", description: "The confirmed candidates are now in your portfolio ledger." }); navigate("/personal-portfolio") }
    catch (cause) { notification.danger({ title: "Confirmation failed", description: cause instanceof Error ? cause.message : String(cause) }) } finally { setConfirming(false) }
  }

  return <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Import Review">
    <div className="flex w-full min-w-0 flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0"><p className="text-xs text-muted-foreground">Import #{taskId}</p><h1 className="mt-1 text-2xl font-semibold tracking-tight">Verify trade candidates</h1><p className="mt-1 text-sm text-muted-foreground">Review and correct AI output before it affects your portfolio ledger.</p></div>
        <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => { void load(); void loadHistory() }} disabled={loading}><RefreshCw className="mr-2 size-4" />Refresh</Button><Button onClick={() => void confirm()} disabled={confirming || !pending.length || review?.task.status !== "READY_FOR_REVIEW"}><Check className="mr-2 size-4" />{confirming ? "Confirming..." : `Confirm ${selected.length} trades`}</Button></div>
      </header>
      {loading ? <Skeleton className="h-72 rounded-lg" /> : error ? <div className="flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="size-4" />{error}<Button variant="outline" size="sm" className="ml-auto" onClick={() => void load()}>Retry</Button></div> : <>
        <div className="flex items-center gap-3 rounded-lg border border-border/60 bg-muted/35 px-4 py-3 text-sm"><span className="font-medium">Status</span><span className="rounded-full bg-background px-2.5 py-1 text-xs">{review?.task.status}</span>{!terminal.has(review?.task.status || "") ? <Loader2 className="size-4 animate-spin text-muted-foreground" /> : null}</div>
        <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><div className="border-b border-border/60 px-4 py-3"><div className="flex items-center gap-2"><Pencil className="size-4 text-muted-foreground" /><h2 className="text-sm font-semibold">Candidate trades</h2></div><p className="mt-1 text-xs text-muted-foreground">Edit a pending row and save it. The AI payload remains preserved for audit.</p></div><div className="overflow-x-auto"><table className="w-full min-w-[1180px] text-sm"><thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">Use</th><th className="px-4 py-3">Row</th><th className="px-4 py-3">Symbol</th><th className="px-4 py-3">Side</th><th className="px-4 py-3">Date / time</th><th className="px-4 py-3">Quantity</th><th className="px-4 py-3">Price</th><th className="px-4 py-3">Fee</th><th className="px-4 py-3">Action</th></tr></thead><tbody className="divide-y divide-border/60">{review?.candidates.map((candidate) => { const draft = drafts[candidate.id]; const editable = candidate.reviewStatus === "PENDING" && review.task.status === "READY_FOR_REVIEW"; const checked = selected.includes(candidate.id); return <tr key={candidate.id} className="align-top hover:bg-muted/35"><td className="px-4 py-3"><input type="checkbox" checked={checked} disabled={!editable} onChange={() => setSelected((current) => checked ? current.filter((value) => value !== candidate.id) : [...current, candidate.id])} aria-label={`Select candidate ${candidate.rowNo}`} /></td><td className="px-4 py-3 font-mono">{candidate.rowNo}</td><td className="px-4 py-3"><Input className="w-24" value={draft?.symbol ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "symbol", event.target.value)} aria-label={`Symbol row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={draft?.side ?? "BUY"} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "side", event.target.value)} aria-label={`Side row ${candidate.rowNo}`}><option value="BUY">BUY</option><option value="SELL">SELL</option></select></td><td className="px-4 py-3"><Input className="w-44" value={draft?.tradedAt ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "tradedAt", event.target.value)} aria-label={`Trade time row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><Input className="w-28 font-mono" value={draft?.quantity ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "quantity", event.target.value)} aria-label={`Quantity row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><Input className="w-28 font-mono" value={draft?.price ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "price", event.target.value)} aria-label={`Price row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><Input className="w-24 font-mono" value={draft?.totalFee ?? "0"} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "totalFee", event.target.value)} aria-label={`Fee row ${candidate.rowNo}`} /></td><td className="px-4 py-3">{editable ? <Button size="sm" variant="outline" onClick={() => void saveCandidate(candidate.id)} disabled={savingId === candidate.id}><Save className="mr-2 size-4" />{savingId === candidate.id ? "Saving..." : "Save"}</Button> : <span className="text-xs text-muted-foreground">{candidate.reviewStatus}</span>}</td></tr> })}</tbody></table></div></section>
        {review ? <ImportExecutionRecord task={review.task} history={history} candidateCount={review.candidates.length} loading={historyLoading} error={historyError} /> : null}
      </>}
    </div>
  </WorkspaceShell>
}
