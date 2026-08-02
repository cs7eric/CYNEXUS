import { ChevronDown, ChevronRight, CircleAlert, FileCheck2, Pencil } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioImportProcessHistory, PortfolioImportProcessHistoryEvent, PortfolioImportTask } from "@/services/market/portfolio/types"

interface ImportExecutionRecordProps {
  task: PortfolioImportTask
  history: PortfolioImportProcessHistory | null
  candidateCount?: number | null
  loading: boolean
  error: string | null
  open?: boolean
  onOpenChange?: (open: boolean) => void
  reviewHref?: string
  embedded?: boolean
}

const EVENT_COPY: Record<string, { title: string; description: string }> = {
  TASK_CREATED: { title: "Screenshot accepted", description: "The upload created this import record." },
  OUTBOX_CREATED: { title: "Queued for processing", description: "The screenshot was handed to the background worker." },
  PROCESSING_STARTED: { title: "Processing started", description: "A worker started this parsing attempt." },
  AI_REQUEST_STARTED: { title: "AI extraction started", description: "Transaction facts are being extracted from the screenshot." },
  AI_REQUEST_SUCCEEDED: { title: "AI extraction completed", description: "The model returned a structured response." },
  AI_REQUEST_FAILED: { title: "AI extraction failed", description: "This attempt could not produce a usable result." },
  RETRY_SCHEDULED: { title: "Retry scheduled", description: "The task was placed back in the delayed queue." },
  CANDIDATES_SAVED: { title: "Candidate trades saved", description: "Extracted trades are ready for review." },
  CANDIDATE_EDITED: { title: "Candidate trade edited", description: "A reviewer corrected an extracted trade." },
  TASK_READY_FOR_REVIEW: { title: "Ready for review", description: "Confirm or edit the candidate trades before posting them to the ledger." },
  TASK_CONFIRMED: { title: "Trades confirmed", description: "The selected trades were posted to the portfolio ledger." },
  TASK_FAILED: { title: "Import failed", description: "This import chain stopped and needs attention before a new upload." },
}

const EVENT_SHORT_LABEL: Record<string, string> = {
  TASK_CREATED: "Accepted",
  OUTBOX_CREATED: "Queued",
  PROCESSING_STARTED: "Started",
  AI_REQUEST_STARTED: "AI request",
  AI_REQUEST_SUCCEEDED: "AI completed",
  AI_REQUEST_FAILED: "AI failed",
  RETRY_SCHEDULED: "Retry queued",
  CANDIDATES_SAVED: "Trades saved",
  CANDIDATE_EDITED: "Edited",
  TASK_READY_FOR_REVIEW: "Ready",
  TASK_CONFIRMED: "Confirmed",
  TASK_FAILED: "Failed",
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

function formatClock(value?: string | null) {
  return value ? new Date(value).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "--"
}

function formatDuration(value?: number | null) {
  if (value == null) return null
  return value < 1_000 ? `${value} ms` : `${(value / 1_000).toFixed(1)} s`
}

function eventCopy(event: PortfolioImportProcessHistoryEvent) {
  return EVENT_COPY[event.eventType] ?? { title: event.eventType, description: "Import record event." }
}

function meaningfulDetails(value?: string | null) {
  if (!value || value === "{}" || value === "null") return null
  return value
}

interface AttemptChain {
  attemptNo: number
  events: PortfolioImportProcessHistoryEvent[]
}

function groupByAttempt(events: PortfolioImportProcessHistoryEvent[]): AttemptChain[] {
  const grouped = new Map<number, PortfolioImportProcessHistoryEvent[]>()
  for (const event of events) {
    const attemptNo = event.attemptNo ?? 0
    grouped.set(attemptNo, [...(grouped.get(attemptNo) ?? []), event])
  }
  return [...grouped.entries()].map(([attemptNo, attemptEvents]) => ({ attemptNo, events: attemptEvents }))
}

function attemptLabel(attemptNo: number) {
  return attemptNo === 0 ? "Setup" : `Attempt ${attemptNo}`
}

function statusClass(status: string) {
  if (status === "FAILED" || status === "CANCELLED") return "bg-rose-50 text-rose-700"
  if (status === "READY_FOR_REVIEW" || status === "CONFIRMED") return "bg-emerald-50 text-emerald-700"
  return "bg-muted text-muted-foreground"
}

export function ImportExecutionRecord({ task, history, candidateCount, loading, error, open, onOpenChange, reviewHref, embedded = false }: ImportExecutionRecordProps) {
  const events = history?.events ?? []
  const attempts = Math.max(task.attemptCount, ...events.map((event) => event.attemptNo))
  const latestEvent = events.at(-1)
  const failure = [...events].reverse().find((event) => event.errorCode)
  const failureCode = failure?.errorCode ?? task.lastErrorCode
  const failureMessage = failure?.errorMessage ?? task.lastErrorMessage ?? failureCode
  const statusLabel = STATUS_COPY[task.status] ?? task.status
  const attemptChains = groupByAttempt(events)
  const recordSurface = embedded ? "overflow-hidden bg-card" : "overflow-hidden rounded-lg border border-border/60 bg-card"

  if (loading) {
    return <section className={recordSurface}><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><div className="space-y-3 border-t border-border/60 p-4"><Skeleton className="h-14 rounded-md" /><Skeleton className="h-20 rounded-md" /><Skeleton className="h-20 rounded-md" /></div></section>
  }

  if (error) {
    return <section className={recordSurface}><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><div className="border-t border-border/60 p-4"><div className="flex items-start gap-2 rounded-md border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Execution record unavailable</p><p className="mt-1 text-xs">{error}</p></div></div></div></section>
  }

  if (!history) {
    return <section className={recordSurface}><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><p className="border-t border-border/60 p-4 text-sm text-muted-foreground">This upload has not created an execution record yet.</p></section>
  }

  return <Collapsible className={recordSurface} open={open} onOpenChange={onOpenChange}>
    <CollapsibleTrigger asChild>
      <button type="button" className="group flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted"><FileCheck2 className="size-4 text-muted-foreground" /></div>
        <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-x-2 gap-y-1"><h2 className="text-sm font-semibold">Import #{history.taskId}</h2><span className={`rounded-full px-2 py-0.5 text-xs ${statusClass(task.status)}`}>{statusLabel}</span><span className="text-xs text-muted-foreground">{attempts} attempts</span>{candidateCount != null ? <span className="text-xs text-muted-foreground">{candidateCount} trades</span> : null}<span className="text-xs text-muted-foreground">{formatTime(latestEvent?.occurredAt ?? history.updatedAt)}</span></div>{failureMessage ? <p className="mt-1 flex min-w-0 items-center gap-1.5 truncate text-xs text-rose-700" title={failureCode ? `${failureCode}: ${failureMessage}` : failureMessage}><CircleAlert className="size-3.5 shrink-0" /><span className="truncate">{failureCode ? `${failureCode}: ` : null}{failureMessage}</span></p> : null}</div>
        <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-180" />
      </button>
    </CollapsibleTrigger>
    <CollapsibleContent className="border-t border-border/60">
      {reviewHref && task.status === "READY_FOR_REVIEW" ? <div className="flex items-center justify-between gap-3 border-b border-border/60 bg-muted/25 px-4 py-2.5">
        <p className="text-xs text-muted-foreground">Candidates are ready. Review and confirm them before updating holdings.</p>
        <Link to={reviewHref} className="shrink-0"><Button size="sm" variant="outline"><Pencil className="mr-1.5 size-3.5" />Review candidates</Button></Link>
      </div> : null}
      {!events.length ? <p className="px-4 py-3 text-xs text-muted-foreground">No execution events have been recorded yet.</p> : <ol className="divide-y divide-border/60 px-4 py-2">{attemptChains.map((chain) => <AttemptEventChain key={chain.attemptNo} chain={chain} />)}</ol>}
    </CollapsibleContent>
  </Collapsible>
}

function RecordHeading({ task, history, statusLabel, eventCount }: { task: PortfolioImportTask; history: PortfolioImportProcessHistory | null; statusLabel: string; eventCount: number }) {
  return <div className="flex items-center gap-3 px-4 py-4"><div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-muted"><FileCheck2 className="size-4 text-muted-foreground" /></div><div className="min-w-0 flex-1"><h2 className="text-sm font-semibold">Screenshot import #{history?.taskId ?? task.id}</h2><p className="mt-1 text-xs text-muted-foreground">{statusLabel} · {eventCount} events</p></div></div>
}

function AttemptEventChain({ chain }: { chain: AttemptChain }) {
  const latest = chain.events.at(-1)
  const failure = [...chain.events].reverse().find((event) => event.errorCode)
  const duration = chain.events.map((event) => event.durationMs ?? 0).reduce((sum, value) => sum + value, 0)
  const labels = chain.events.map((event) => EVENT_SHORT_LABEL[event.eventType] ?? eventCopy(event).title)
  const latestStatus = latest?.toStatus ? STATUS_COPY[latest.toStatus] ?? latest.toStatus : null

  return <li className="py-0.5">
    <Collapsible>
      <CollapsibleTrigger asChild>
        <button type="button" className="group flex w-full min-w-0 items-center gap-2 rounded-md px-2 py-2 text-left hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          <ChevronRight className="size-3.5 shrink-0 text-muted-foreground transition-transform duration-150 group-data-[state=open]:rotate-90" />
          <span className="w-[68px] shrink-0 font-mono text-xs font-medium tabular-nums text-foreground">{attemptLabel(chain.attemptNo)}</span>
          <span className="min-w-0 flex-1 truncate text-xs text-foreground">{labels.join(" → ")}</span>
          {failure ? <span className="hidden max-w-[180px] truncate font-mono text-[11px] text-rose-700 md:inline">{failure.errorCode}</span> : null}
          {latestStatus ? <span className={`hidden rounded-full px-2 py-0.5 text-[11px] sm:inline ${statusClass(latest?.toStatus ?? "")}`}>{latestStatus}</span> : null}
          <span className="shrink-0 text-[11px] text-muted-foreground">{formatTime(latest?.occurredAt)}</span>
          {duration ? <span className="hidden shrink-0 text-[11px] text-muted-foreground lg:inline">{formatDuration(duration)}</span> : null}
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="ml-[22px] border-l border-border/70 py-1 pl-3">
          {chain.events.map((event) => <EventDetail key={event.id} event={event} />)}
        </div>
      </CollapsibleContent>
    </Collapsible>
  </li>
}

function EventDetail({ event }: { event: PortfolioImportProcessHistoryEvent }) {
  const copy = eventCopy(event)
  const details = meaningfulDetails(event.details)
  const metadata = [
    formatClock(event.occurredAt),
    event.durationMs == null ? null : formatDuration(event.durationMs),
    event.providerCode ? `Provider ${event.providerCode}` : null,
    event.modelCode ? `Model ${event.modelCode}` : null,
  ].filter((item): item is string => Boolean(item))

  return <div className="py-2 first:pt-1 last:pb-1">
    <div className="flex flex-wrap items-center gap-2"><p className="text-xs font-medium text-foreground">{copy.title}</p>{event.toStatus ? <span className={`rounded-full px-2 py-0.5 text-[11px] ${statusClass(event.toStatus)}`}>{STATUS_COPY[event.toStatus] ?? event.toStatus}</span> : null}</div>
    <p className="mt-0.5 text-[11px] leading-4 text-muted-foreground">{copy.description}</p>
    <div className="mt-1 flex flex-wrap gap-x-2 gap-y-0.5 text-[11px] text-muted-foreground">{metadata.map((item) => <span key={item}>{item}</span>)}</div>
    {event.errorCode ? <p className="mt-1 break-words text-[11px] text-rose-700"><span className="font-mono font-medium">{event.errorCode}</span>{event.errorMessage ? ` · ${event.errorMessage}` : null}</p> : null}
    {details ? <p className="mt-1 break-words text-[11px] leading-4 text-muted-foreground">{details}</p> : null}
  </div>
}
