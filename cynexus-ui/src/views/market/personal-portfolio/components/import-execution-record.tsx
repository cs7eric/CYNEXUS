import { ChevronDown, CircleAlert, FileCheck2 } from "lucide-react"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
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

  if (loading) {
    return <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><div className="space-y-3 border-t border-border/60 p-4"><Skeleton className="h-14 rounded-md" /><Skeleton className="h-20 rounded-md" /><Skeleton className="h-20 rounded-md" /></div></section>
  }

  if (error) {
    return <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><div className="border-t border-border/60 p-4"><div className="flex items-start gap-2 rounded-md border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">Execution record unavailable</p><p className="mt-1 text-xs">{error}</p></div></div></div></section>
  }

  if (!history) {
    return <section className="overflow-hidden rounded-lg border border-border/60 bg-card"><RecordHeading task={task} history={history} statusLabel={statusLabel} eventCount={0} /><p className="border-t border-border/60 p-4 text-sm text-muted-foreground">This upload has not created an execution record yet.</p></section>
  }

  return <Collapsible defaultOpen className="overflow-hidden rounded-lg border border-border/60 bg-card">
    <CollapsibleTrigger asChild>
      <button type="button" className="group flex w-full items-center gap-3 px-4 py-4 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset">
        <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-muted"><FileCheck2 className="size-4 text-muted-foreground" /></div>
        <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="text-sm font-semibold">Screenshot import #{history.taskId}</h2><span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">{statusLabel}</span></div><p className="mt-1 text-xs text-muted-foreground">One record · {events.length} events · {candidateCount} candidate trades · click to {events.length ? "show or hide the event chain" : "view record details"}.</p></div>
        <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-180" />
      </button>
    </CollapsibleTrigger>
    <CollapsibleContent className="border-t border-border/60">
      <div className="grid gap-px border-b border-border/60 bg-border/60 sm:grid-cols-2 xl:grid-cols-5">
        <SummaryItem label="Record" value={`#${history.id}`} mono />
        <SummaryItem label="Created" value={formatTime(history.createdAt)} />
        <SummaryItem label="Attempts" value={String(attempts)} />
        <SummaryItem label="Candidate trades" value={String(candidateCount)} />
        <SummaryItem label="Latest activity" value={formatTime(latestEvent?.occurredAt ?? history.updatedAt)} />
      </div>
      {failure ? <div className="mx-4 mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-800"><div className="flex items-center gap-2 font-medium"><CircleAlert className="size-4" />{failure.errorCode}</div>{failure.errorMessage ? <p className="mt-1 text-xs leading-5">{failure.errorMessage}</p> : null}</div> : null}
      <div className="px-4 pb-2 pt-4"><h3 className="text-sm font-semibold">Event chain</h3><p className="mt-1 text-xs text-muted-foreground">Events are rendered in order inside this single upload record.</p></div>
      {!events.length ? <div className="px-4 pb-5 text-sm text-muted-foreground">No execution events have been recorded yet.</div> : <ol className="px-4 pb-4 pt-2">{events.map((event, index) => <TimelineEvent key={event.id} event={event} last={index === events.length - 1} />)}</ol>}
    </CollapsibleContent>
  </Collapsible>
}

function RecordHeading({ task, history, statusLabel, eventCount }: { task: PortfolioImportTask; history: PortfolioImportProcessHistory | null; statusLabel: string; eventCount: number }) {
  return <div className="flex items-center gap-3 px-4 py-4"><div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-muted"><FileCheck2 className="size-4 text-muted-foreground" /></div><div className="min-w-0 flex-1"><h2 className="text-sm font-semibold">Screenshot import #{history?.taskId ?? task.id}</h2><p className="mt-1 text-xs text-muted-foreground">{statusLabel} · {eventCount} events</p></div></div>
}

function SummaryItem({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return <div className="min-w-0 bg-card px-4 py-3"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1 truncate text-sm text-foreground ${mono ? "font-mono tabular-nums" : ""}`}>{value}</p></div>
}

function TimelineEvent({ event, last }: { event: PortfolioImportProcessHistoryEvent; last: boolean }) {
  const copy = eventCopy(event)
  const duration = formatDuration(event.durationMs)
  const metadata = [
    `Attempt ${event.attemptNo || 1}`,
    duration,
    event.providerCode ? `Provider ${event.providerCode}` : null,
    event.modelCode ? `Model ${event.modelCode}` : null,
  ].filter((item): item is string => item !== null)
  return <li className="relative flex gap-3 pb-5 last:pb-0"><div className="relative flex w-5 shrink-0 justify-center"><span className="z-10 mt-1 size-2.5 rounded-full border-2 border-background bg-muted-foreground" />{!last ? <span className="absolute left-1/2 top-3 h-[calc(100%+0.25rem)] w-px -translate-x-1/2 bg-border" /> : null}</div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-medium">{copy.title}</p>{event.toStatus ? <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">{STATUS_COPY[event.toStatus] ?? event.toStatus}</span> : null}</div><p className="mt-1 text-xs text-muted-foreground">{copy.description}</p><div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground"><span>{formatTime(event.occurredAt)}</span>{metadata.map((item) => <span key={item}>{item}</span>)}</div>{event.errorCode ? <div className="mt-2 text-xs text-rose-700"><span className="font-mono">{event.errorCode}</span>{event.errorMessage ? ` · ${event.errorMessage}` : ""}</div> : null}</div></li>
}
