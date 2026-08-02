import { CircleAlert, Clock3, FileCheck2 } from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioImportProcessHistory, PortfolioImportProcessHistoryEvent, PortfolioImportTask } from "@/services/market/portfolio/types"

interface ImportExecutionRecordProps {
  task: PortfolioImportTask
  history: PortfolioImportProcessHistory | null
  candidateCount: number
  loading: boolean
  error: string | null
}

const EVENT_COPY: Record<string, { title: string; description: string }> = {
  TASK_CREATED: { title: "Screenshot accepted", description: "The upload created this import record." },
  OUTBOX_CREATED: { title: "Queued for processing", description: "The screenshot was handed to the background worker." },
  PROCESSING_STARTED: { title: "Processing started", description: "A worker started this parsing attempt." },
  AI_REQUEST_STARTED: { title: "AI extraction started", description: "The transaction facts are being extracted from the screenshot." },
  AI_REQUEST_SUCCEEDED: { title: "AI extraction completed", description: "The model returned a structured response." },
  AI_REQUEST_FAILED: { title: "AI extraction failed", description: "This attempt could not produce a usable result." },
  RETRY_SCHEDULED: { title: "Retry scheduled", description: "The task was placed back in the delayed queue." },
  CANDIDATES_SAVED: { title: "Candidate trades saved", description: "Extracted trades are ready for review." },
  CANDIDATE_EDITED: { title: "Candidate trade edited", description: "A reviewer corrected an extracted trade." },
  TASK_READY_FOR_REVIEW: { title: "Ready for review", description: "Confirm or edit the candidate trades before posting them to the ledger." },
  TASK_CONFIRMED: { title: "Trades confirmed", description: "The selected trades were posted to the portfolio ledger." },
  TASK_FAILED: { title: "Import failed", description: "This import chain stopped and needs attention before a new upload." },
}

const STATUS_COPY: Record<string, string> = {
  PENDING_DISPATCH: "Waiting to queue",
  QUEUED: "Queued",
  PROCESSING: "Processing",
  RETRY_WAITING: "Retry scheduled",
  READY_FOR_REVIEW: "Ready for review",
  CONFIRMED: "Confirmed",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
}

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : "--"
}

function formatDuration(value?: number | null) {
  if (value == null) return null
  return value < 1_000 ? `${value} ms` : `${(value / 1_000).toFixed(1)} s`
}

function eventCopy(event: PortfolioImportProcessHistoryEvent) {
  return EVENT_COPY[event.eventType] ?? { title: event.eventType, description: "Import record event." }
}

export function ImportExecutionRecord({ task, history, candidateCount, loading, error }: ImportExecutionRecordProps) {
  const events = history?.events ?? []
  const attempts = Math.max(task.attemptCount, ...events.map((event) => event.attemptNo))
  const latestEvent = events.at(-1)
  const failure = [...events].reverse().find((event) => event.errorCode)
  const statusLabel = STATUS_COPY[task.status] ?? task.status

  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card">
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div className="min-w-0">
        <div className="flex items-center gap-2"><FileCheck2 className="size-4 text-muted-foreground" /><h2 className="text-sm font-semibold">Import execution record</h2></div>
        <p className="mt-1 text-xs text-muted-foreground">One screenshot upload, its complete execution chain, and the resulting review state.</p>
      </div>
      <span className="rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">{statusLabel}</span>
    </div>
    {loading ? <div className="space-y-3 p-4"><Skeleton className="h-14 rounded-md" /><Skeleton className="h-20 rounded-md" /><Skeleton className="h-20 rounded-md" /></div> : error ? <div className="m-4 flex items-start gap-2 rounded-md border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Execution record unavailable</p><p className="mt-1 text-xs">{error}</p></div></div> : !history ? <div className="p-4 text-sm text-muted-foreground">This upload has not created an execution record yet.</div> : <>
      <div className="grid gap-px border-b border-border/60 bg-border/60 sm:grid-cols-2 xl:grid-cols-5">
        <SummaryItem label="Upload" value={`#${history.taskId}`} mono />
        <SummaryItem label="Created" value={formatTime(history.createdAt)} />
        <SummaryItem label="Attempts" value={String(attempts)} />
        <SummaryItem label="Candidate trades" value={String(candidateCount)} />
        <SummaryItem label="Latest activity" value={formatTime(latestEvent?.occurredAt ?? history.updatedAt)} />
      </div>
      {failure ? <div className="mx-4 mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-800"><div className="flex items-center gap-2 font-medium"><CircleAlert className="size-4" />{failure.errorCode}</div>{failure.errorMessage ? <p className="mt-1 text-xs leading-5">{failure.errorMessage}</p> : null}</div> : null}
      <div className="px-4 pb-2 pt-4"><h3 className="text-sm font-semibold">Execution timeline</h3><p className="mt-1 text-xs text-muted-foreground">{events.length} recorded events from upload through the current outcome.</p></div>
      {!events.length ? <div className="px-4 pb-5 text-sm text-muted-foreground">No execution events have been recorded yet.</div> : <ol className="divide-y divide-border/60">{events.map((event) => <TimelineEvent key={event.id} event={event} />)}</ol>}
    </>}
  </section>
}

function SummaryItem({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return <div className="min-w-0 bg-card px-4 py-3"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1 truncate text-sm text-foreground ${mono ? "font-mono tabular-nums" : ""}`}>{value}</p></div>
}

function TimelineEvent({ event }: { event: PortfolioImportProcessHistoryEvent }) {
  const copy = eventCopy(event)
  const duration = formatDuration(event.durationMs)
  const metadata = [
    `Attempt ${event.attemptNo || 1}`,
    duration,
    event.providerCode ? `Provider ${event.providerCode}` : null,
    event.modelCode ? `Model ${event.modelCode}` : null,
  ].filter(Boolean)
  return <li className="flex gap-3 px-4 py-3"><Clock3 className="mt-0.5 size-4 shrink-0 text-muted-foreground" /><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-medium">{copy.title}</p>{event.toStatus ? <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">{STATUS_COPY[event.toStatus] ?? event.toStatus}</span> : null}</div><p className="mt-1 text-xs text-muted-foreground">{copy.description}</p><div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground"><span>{formatTime(event.occurredAt)}</span>{metadata.map((item) => <span key={item}>{item}</span>)}</div>{event.errorCode ? <div className="mt-2 text-xs text-rose-700"><span className="font-mono">{event.errorCode}</span>{event.errorMessage ? ` · ${event.errorMessage}` : ""}</div> : null}</div></li>
}
