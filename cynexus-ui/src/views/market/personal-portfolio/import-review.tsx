import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { Check, CircleAlert, Loader2, Pencil, RefreshCw, Save } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { DatePicker } from "@/components/ui/date-picker"
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
import { toLocalIso, todayLocal } from "@/lib/date-utils"
import { ImportExecutionRecord } from "./components/import-execution-record"

const terminal = new Set(["READY_FOR_REVIEW", "CONFIRMED", "FAILED", "CANCELLED"])
type Draft = PortfolioCandidateEdit

function sameTask(left: PortfolioImportReview["task"], right: PortfolioImportReview["task"]) {
  return left.id === right.id
    && left.status === right.status
    && left.version === right.version
    && left.attemptCount === right.attemptCount
    && left.assetId === right.assetId
    && left.queuedAt === right.queuedAt
    && left.startedAt === right.startedAt
    && left.completedAt === right.completedAt
    && left.confirmedAt === right.confirmedAt
    && left.lastErrorCode === right.lastErrorCode
    && left.lastErrorMessage === right.lastErrorMessage
}

function sameHistory(left: PortfolioImportProcessHistory | null, right: PortfolioImportProcessHistory) {
  if (!left || left.id !== right.id || left.taskId !== right.taskId || left.updatedAt !== right.updatedAt
    || left.events.length !== right.events.length) return false
  return left.events.every((event, index) => event.id === right.events[index]?.id)
}

function sameReview(left: PortfolioImportReview | null, right: PortfolioImportReview) {
  if (!left || !sameTask(left.task, right.task) || left.candidates.length !== right.candidates.length) return false
  return left.candidates.every((candidate, index) => {
    const next = right.candidates[index]
    if (!next) return false
    return candidate.id === next.id && candidate.version === next.version
      && candidate.reviewStatus === next.reviewStatus && candidate.normalizedPayload === next.normalizedPayload
  })
}

function toDraft(payload: string, version: number): Draft {
  let data: Record<string, unknown> = {}
  try { data = JSON.parse(payload) } catch { /* validation is handled by the API */ }
  const extractedTradeTime = String(data.tradedAt ?? "").trim()
  const defaultTradeDate = toLocalIso(todayLocal())
  return {
    version,
    symbol: String(data.symbol ?? ""),
    securityName: String(data.securityName ?? ""),
    instrumentType: data.instrumentType === "ETF" ? "ETF" : "STOCK",
    side: data.side === "SELL" ? "SELL" : "BUY",
    tradedAt: extractedTradeTime || defaultTradeDate,
    quantity: String(data.quantity ?? ""),
    price: String(data.price ?? ""),
    totalFee: String(data.fee ?? data.totalFee ?? "0"),
    externalTradeNo: String(data.externalTradeNo ?? ""),
  }
}

function splitTradeDateTime(value: string) {
  const text = value.trim()
  if (!/^\d{4}-\d{2}-\d{2}/.test(text)) return { date: "", time: "" }
  const date = text.slice(0, 10)
  const timeMatch = text.match(/[T ](\d{2}:\d{2})/)
  return { date, time: timeMatch?.[1] ?? "" }
}

function mergeTradeDateTime(date: string, time: string) {
  if (!date) return ""
  return time ? `${date}T${time}` : date
}

function localDateString(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, "0")
  const day = String(value.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function TradeDateTimeEditor({
  value,
  editable,
  onChange,
  rowNo,
}: {
  value: string
  editable: boolean
  onChange: (value: string) => void
  rowNo: number
}) {
  const parts = splitTradeDateTime(value)
  return <div className="space-y-1.5">
    <div className="flex flex-wrap items-center gap-1.5">
      <DatePicker
        value={parts.date}
        onChange={(date) => onChange(mergeTradeDateTime(date ? localDateString(date) : "", parts.time))}
        disableWeekends={false}
        clearable
        disabled={!editable}
        placeholder="选择日期"
        triggerClassName="w-[136px]"
        aria-label={`Trade date row ${rowNo}`}
      />
      <Input
        className="h-8 w-[100px] bg-background text-xs"
        type="time"
        value={parts.time}
        disabled={!editable}
        onChange={(event) => onChange(mergeTradeDateTime(parts.date, event.target.value))}
        aria-label={`Trade time row ${rowNo}`}
      />
    </div>
  </div>
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
  const pollInFlight = useRef(false)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const item = await fetchPortfolioImportReview(id)
      setReview(item)
      setSelected(item.candidates.filter((candidate) => candidate.reviewStatus === "PENDING").map((candidate) => candidate.id))
      setDrafts(Object.fromEntries(item.candidates.map((candidate) => [candidate.id, toDraft(candidate.normalizedPayload, candidate.version)])))
    } catch (cause) { setError(cause instanceof Error ? cause.message : String(cause)) } finally { setLoading(false) }
  }, [id])

  const loadHistory = useCallback(async (showLoading = false) => {
    if (showLoading) setHistoryLoading(true)
    setHistoryError(null)
    try { setHistory(await fetchPortfolioImportHistory(id)) } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setHistoryError(message)
      notification.danger({ title: "Process history unavailable", description: message })
    } finally {
      if (showLoading) setHistoryLoading(false)
    }
  }, [id])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); void loadHistory(true) }, 0)
    return () => window.clearTimeout(timer)
  }, [load, loadHistory])
  const status = review?.task.status

  const refreshProgress = useCallback(async () => {
    if (pollInFlight.current) return
    pollInFlight.current = true
    try {
      const task = await fetchPortfolioImport(id)
      const nextHistory = await fetchPortfolioImportHistory(id)
      setReview((current) => current && sameTask(current.task, task) ? current : current ? ({ ...current, task }) : current)
      setHistory((current) => sameHistory(current, nextHistory) ? current : nextHistory)

      if (task.status === "READY_FOR_REVIEW") {
        const nextReview = await fetchPortfolioImportReview(id)
        setReview((current) => sameReview(current, nextReview) ? current : nextReview)
        setSelected((current) => current.length ? current : nextReview.candidates
          .filter((candidate) => candidate.reviewStatus === "PENDING").map((candidate) => candidate.id))
        setDrafts((current) => Object.keys(current).length ? current : Object.fromEntries(
          nextReview.candidates.map((candidate) => [candidate.id, toDraft(candidate.normalizedPayload, candidate.version)]),
        ))
      }
    } catch {
      // Keep the last successful event chain on screen during a transient poll failure.
    } finally {
      pollInFlight.current = false
    }
  }, [id])

  useEffect(() => {
    if (!status || terminal.has(status)) return
    const timer = window.setInterval(() => { void refreshProgress() }, 1500)
    return () => window.clearInterval(timer)
  }, [status, refreshProgress])

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
    if (!review) return
    const selectedCandidates = review.candidates.filter((candidate) => selected.includes(candidate.id) && candidate.reviewStatus === "PENDING")
    if (!selectedCandidates.length) {
      notification.warn({ title: "未选择候选交易", description: "请至少选择一条候选交易后再确认。" })
      return
    }
    const requiredFields: Array<[keyof Draft, string]> = [["symbol", "证券代码"], ["side", "买卖方向"], ["tradedAt", "成交日期/时间"], ["quantity", "成交数量"], ["price", "成交价格"]]
    const invalid = selectedCandidates.map((candidate) => {
      const draft = drafts[candidate.id]
      const missing = requiredFields.find(([field]) => !String(draft?.[field] ?? "").trim())
      return missing ? { candidate, field: missing[1] } : null
    }).find(Boolean)
    if (invalid) {
      notification.warn({ title: `第 ${invalid.candidate.rowNo} 行信息不完整`, description: `请补充${invalid.field}后再确认。修改后点击保存，或直接再次点击确认自动保存。` })
      return
    }
    setConfirming(true)
    try {
      // Persist the current draft before confirming so editing a row does not silently lose changes.
      for (const candidate of selectedCandidates) {
        const draft = drafts[candidate.id]
        if (!draft) continue
        const saved = await updatePortfolioCandidate(id, candidate.id, draft)
        setReview((current) => current ? ({ ...current, candidates: current.candidates.map((item) => item.id === candidate.id ? saved : item) }) : current)
        setDrafts((current) => ({ ...current, [candidate.id]: { ...current[candidate.id], version: saved.version } }))
      }
      await confirmPortfolioImport(id, review.task.version, selected)
      notification.success({ title: "Trades confirmed", description: "The confirmed candidates are now in your portfolio ledger." }); navigate("/personal-portfolio")
    }
    catch (cause) { notification.danger({ title: "Confirmation failed", description: cause instanceof Error ? cause.message : String(cause) }) } finally { setConfirming(false) }
  }

  return <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Import Review">
    <div className="flex w-full min-w-0 flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0"><p className="text-xs text-muted-foreground">Import #{taskId}</p><h1 className="mt-1 text-2xl font-semibold tracking-tight">Verify trade candidates</h1><p className="mt-1 text-sm text-muted-foreground">Review and correct AI output before it affects your portfolio ledger.</p></div>
        <div className="flex flex-wrap gap-2"><Link to="/personal-portfolio?imports=1"><Button variant="outline">Back to imports</Button></Link><Button variant="outline" onClick={() => { void load(); void loadHistory(true) }} disabled={loading}><RefreshCw className="mr-2 size-4" />Refresh</Button><Button onClick={() => void confirm()} disabled={confirming || !pending.length || !selected.length || review?.task.status !== "READY_FOR_REVIEW"}><Check className="mr-2 size-4" />{confirming ? "Confirming..." : `Confirm ${selected.length} trades`}</Button></div>
      </header>
      {loading ? <Skeleton className="h-72 rounded-lg" /> : error ? <div className="flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="size-4" />{error}<Button variant="outline" size="sm" className="ml-auto" onClick={() => void load()}>Retry</Button></div> : <>
        <div className="flex items-center gap-3 rounded-lg border border-border/60 bg-muted/35 px-4 py-3 text-sm"><span className="font-medium">Status</span><span className="rounded-full bg-background px-2.5 py-1 text-xs">{review?.task.status}</span>{!terminal.has(review?.task.status || "") ? <Loader2 className="size-4 animate-spin text-muted-foreground" /> : null}</div>
        {review?.task.lastErrorMessage ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">Latest processing error <span className="font-mono text-xs">{review.task.lastErrorCode}</span></p><p className="mt-1 break-words text-xs leading-5">{review.task.lastErrorMessage}</p></div></div> : null}
        <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><div className="border-b border-border/60 px-4 py-3"><div className="flex items-center gap-2"><Pencil className="size-4 text-muted-foreground" /><h2 className="text-sm font-semibold">Candidate trades</h2></div><p className="mt-1 text-xs text-muted-foreground">Verify the instrument type before confirmation so the correct stock or ETF quote is used. Any warning below must be resolved before confirming that row.</p></div><div className="overflow-x-auto"><table className="w-full min-w-[1280px] text-sm"><thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">Use</th><th className="px-4 py-3">Row</th><th className="px-4 py-3">Symbol</th><th className="px-4 py-3">Type</th><th className="px-4 py-3">Side</th><th className="px-4 py-3">Trade date / time</th><th className="px-4 py-3">Quantity</th><th className="px-4 py-3">Price</th><th className="px-4 py-3">Fee</th><th className="px-4 py-3">Action</th></tr></thead><tbody className="divide-y divide-border/60">{review?.candidates.map((candidate) => { const draft = drafts[candidate.id]; const editable = candidate.reviewStatus === "PENDING" && review.task.status === "READY_FOR_REVIEW"; const checked = selected.includes(candidate.id); return <tr key={candidate.id} className="align-top hover:bg-muted/35"><td className="px-4 py-3"><input type="checkbox" checked={checked} disabled={!editable} onChange={() => setSelected((current) => checked ? current.filter((value) => value !== candidate.id) : [...current, candidate.id])} aria-label={`Select candidate ${candidate.rowNo}`} /></td><td className="px-4 py-3 font-mono">{candidate.rowNo}</td><td className="px-4 py-3"><Input className="w-24" value={draft?.symbol ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "symbol", event.target.value)} aria-label={`Symbol row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={draft?.instrumentType ?? "STOCK"} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "instrumentType", event.target.value)} aria-label={`Instrument type row ${candidate.rowNo}`}><option value="STOCK">Stock</option><option value="ETF">ETF</option></select></td><td className="px-4 py-3"><select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={draft?.side ?? "BUY"} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "side", event.target.value)} aria-label={`Side row ${candidate.rowNo}`}><option value="BUY">BUY</option><option value="SELL">SELL</option></select></td><td className="px-4 py-3"><TradeDateTimeEditor value={draft?.tradedAt ?? ""} editable={editable} onChange={(value) => updateDraft(candidate.id, "tradedAt", value)} rowNo={candidate.rowNo} />{candidate.warnings?.length ? <div className="mt-1 max-w-52 text-[11px] leading-4 text-amber-700">{candidate.warnings.map((warning) => <p key={warning}>{warning}</p>)}</div> : null}</td><td className="px-4 py-3"><Input className="w-28 font-mono" value={draft?.quantity ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "quantity", event.target.value)} aria-label={`Quantity row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><Input className="w-28 font-mono" value={draft?.price ?? ""} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "price", event.target.value)} aria-label={`Price row ${candidate.rowNo}`} /></td><td className="px-4 py-3"><Input className="w-24 font-mono" value={draft?.totalFee ?? "0"} disabled={!editable} onChange={(event) => updateDraft(candidate.id, "totalFee", event.target.value)} aria-label={`Fee row ${candidate.rowNo}`} /></td><td className="px-4 py-3">{editable ? <Button size="sm" variant="outline" onClick={() => void saveCandidate(candidate.id)} disabled={savingId === candidate.id}><Save className="mr-2 size-4" />{savingId === candidate.id ? "Saving..." : "Save"}</Button> : <span className="text-xs text-muted-foreground">{candidate.reviewStatus}</span>}</td></tr> })}</tbody></table></div></section>
        {review ? <ImportExecutionRecord task={review.task} history={history} candidateCount={review.candidates.length} loading={historyLoading} error={historyError} /> : null}
      </>}
    </div>
  </WorkspaceShell>
}
