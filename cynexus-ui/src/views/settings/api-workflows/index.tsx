import { Fragment, useCallback, useEffect, useState } from "react"
import { ArrowUpRight, ChevronDown, ChevronRight, CircleAlert, FileClock, FileImage, FileVideo2, RefreshCw, RotateCcw, WalletCards } from "lucide-react"
import { Link } from "react-router-dom"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { listApiWorkflows, retryApiWorkflow, type ApiWorkflowEvent, type ApiWorkflowProcess, type ApiWorkflowStatus } from "@/services/core/api-workflows"

const PAGE_SIZE_OPTIONS = [20, 50, 100]

const statusLabels: Record<ApiWorkflowStatus, string> = {
  QUEUED: "已入队", RUNNING: "处理中", WAITING_REVIEW: "待确认", SUCCEEDED: "已完成", FAILED: "失败", CANCELLED: "已归档",
}

const typeLabels: Record<ApiWorkflowProcess["workflowType"], string> = {
  MEDIA_TRANSCRIPTION: "URL 转写", PORTFOLIO_TRADE_IMPORT: "AI Trade", FINANCE_AI_CAPTURE: "AI 入账",
}

const eventShortLabels: Record<string, string> = {
  QUEUED: "Queued", DISPATCHED: "Queued", PROCESSING_STARTED: "Started", AI_STARTED: "AI request",
  AI_SUCCEEDED: "AI completed", AI_FAILED: "AI failed", RETRY_SCHEDULED: "Retry queued",
  RESULT_READY: "Result ready", WAITING_REVIEW: "Ready", CONFIRMED: "Confirmed", FAILED: "Failed", STEP_UPDATED: "Updated",
}

const eventDescriptions: Record<string, string> = {
  QUEUED: "The request created this workflow record.", DISPATCHED: "The request was handed to the background worker.",
  PROCESSING_STARTED: "A worker started this processing attempt.", AI_STARTED: "AI extraction is being executed for this input.",
  AI_SUCCEEDED: "The AI provider returned a structured response.", AI_FAILED: "The AI provider could not produce a usable result.",
  RETRY_SCHEDULED: "The workflow was placed back in the processing queue.", RESULT_READY: "The business result is ready in its original page.",
  WAITING_REVIEW: "The result is ready for review in its original page.", CONFIRMED: "The business result was confirmed in its original page.",
  FAILED: "This workflow attempt stopped and can be retried.", STEP_UPDATED: "The workflow moved to the next processing step.",
}

function formatTime(value?: string | null) {
  if (!value) return "--"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function formatClock(value?: string | null) {
  if (!value) return "--"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })
}

function statusClass(status: ApiWorkflowStatus) {
  if (status === "FAILED" || status === "CANCELLED") return "bg-rose-50 text-rose-700"
  if (status === "WAITING_REVIEW" || status === "SUCCEEDED") return "bg-emerald-50 text-emerald-700"
  return "bg-muted text-muted-foreground"
}

function WorkflowIcon({ type }: { type: ApiWorkflowProcess["workflowType"] }) {
  if (type === "MEDIA_TRANSCRIPTION") return <FileVideo2 className="size-4 text-muted-foreground" />
  if (type === "FINANCE_AI_CAPTURE") return <WalletCards className="size-4 text-muted-foreground" />
  return <FileImage className="size-4 text-muted-foreground" />
}

function eventLabel(event: ApiWorkflowEvent) {
  return eventShortLabels[event.eventType] || event.message || event.eventType
}

function meaningfulDetails(value?: string | null) {
  return value && value !== "{}" && value !== "null" ? value : null
}

interface AttemptChain {
  attemptNo: number
  events: ApiWorkflowEvent[]
}

function groupByAttempt(events: ApiWorkflowEvent[]): AttemptChain[] {
  const grouped = new Map<number, ApiWorkflowEvent[]>()
  for (const event of events) {
    const attemptNo = event.attemptNo ?? 0
    grouped.set(attemptNo, [...(grouped.get(attemptNo) ?? []), event])
  }
  return [...grouped.entries()].map(([attemptNo, attemptEvents]) => ({ attemptNo, events: attemptEvents }))
}

function attemptLabel(attemptNo: number) {
  return attemptNo === 0 ? "Setup" : `Attempt ${attemptNo}`
}

function pageItems(page: number, totalPages: number) {
  return [...new Set([1, totalPages, page - 1, page, page + 1].filter((item) => item >= 1 && item <= totalPages))]
    .sort((left, right) => left - right)
}

function EventDetail({ event }: { event: ApiWorkflowEvent }) {
  const details = meaningfulDetails(event.details)
  const metadata = [
    formatClock(event.occurredAt),
    event.durationMs == null ? null : event.durationMs < 1_000 ? `${event.durationMs} ms` : `${(event.durationMs / 1_000).toFixed(1)} s`,
    event.providerCode ? `Provider ${event.providerCode}` : null,
    event.modelCode ? `Model ${event.modelCode}` : null,
  ].filter((value): value is string => Boolean(value))

  return <div className="py-2 first:pt-1 last:pb-1">
    <div className="flex flex-wrap items-center gap-2"><p className="text-xs font-medium text-foreground">{eventLabel(event)}</p>{event.toStatus ? <span className={`rounded-full px-2 py-0.5 text-[11px] ${statusClass(event.status)}`}>{statusLabels[event.status]}</span> : null}</div>
    <p className="mt-0.5 text-[11px] leading-4 text-muted-foreground">{eventDescriptions[event.eventType] || event.phase}</p>
    <div className="mt-1 flex flex-wrap gap-x-2 gap-y-0.5 text-[11px] text-muted-foreground">{metadata.map((value) => <span key={value}>{value}</span>)}</div>
    {event.errorCode || event.errorMessage ? <p className="mt-1 break-words text-[11px] text-rose-700"><span className="font-mono font-medium">{event.errorCode}</span>{event.errorMessage ? ` · ${event.errorMessage}` : null}</p> : null}
    {details ? <p className="mt-1 break-words text-[11px] leading-4 text-muted-foreground">{details}</p> : null}
  </div>
}

function AttemptEventChain({ chain }: { chain: AttemptChain }) {
  const [open, setOpen] = useState(false)
  const events = chain.events
  const latest = events.at(-1)
  const failure = [...events].reverse().find((event) => event.errorCode || event.errorMessage)
  const labels = events.map(eventLabel)

  return <li className="py-0.5">
    <Collapsible open={open} onOpenChange={setOpen}>
      <CollapsibleTrigger asChild>
        <button type="button" className="group flex w-full min-w-0 items-center gap-2 rounded-md px-2 py-2 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          <ChevronRight className="size-3.5 shrink-0 text-muted-foreground transition-transform duration-150 group-data-[state=open]:rotate-90" />
          <span className="w-[68px] shrink-0 font-mono text-xs font-medium tabular-nums text-foreground">{attemptLabel(chain.attemptNo)}</span>
          <span className="min-w-0 flex-1 truncate text-xs text-foreground">{labels.length ? labels.join(" → ") : "No events"}</span>
          {failure ? <span className="hidden max-w-[180px] truncate font-mono text-[11px] text-rose-700 md:inline">{failure.errorCode || "FAILED"}</span> : null}
          {latest ? <span className={`hidden rounded-full px-2 py-0.5 text-[11px] sm:inline ${statusClass(latest.status)}`}>{statusLabels[latest.status]}</span> : null}
          <span className="shrink-0 text-[11px] text-muted-foreground">{formatTime(latest?.occurredAt)}</span>
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent><div className="ml-[22px] border-l border-border/70 py-1 pl-3">{events.map((event, index) => <EventDetail key={`${event.eventType}-${event.occurredAt}-${index}`} event={event} />)}</div></CollapsibleContent>
    </Collapsible>
  </li>
}

function ApiWorkflowRecord({ item, open, onOpenChange, onRetry, retrying }: { item: ApiWorkflowProcess; open: boolean; onOpenChange: (open: boolean) => void; onRetry: () => void; retrying: boolean }) {
  const statusLabel = statusLabels[item.status]
  const failure = item.errorMessage || item.errorCode
  const attempts = Math.max(item.attemptCount, ...(item.events.map((event) => event.attemptNo ?? 0)))
  const attemptChains = groupByAttempt(item.events)

  return <Collapsible className="overflow-hidden bg-card" open={open} onOpenChange={onOpenChange}>
    <CollapsibleTrigger asChild>
      <button type="button" className="group flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted"><WorkflowIcon type={item.workflowType} /></div>
        <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-x-2 gap-y-1"><h2 className="truncate text-sm font-semibold">{item.workflowType === "PORTFOLIO_TRADE_IMPORT" ? `Import #${item.sourceRef}` : item.title || typeLabels[item.workflowType]}</h2><span className={`rounded-full px-2 py-0.5 text-xs ${statusClass(item.status)}`}>{statusLabel}</span><span className="text-xs text-muted-foreground">{attempts} attempts</span><span className="text-xs text-muted-foreground">{formatTime(item.updatedAt || item.createdAt)}</span></div>{failure ? <p className="mt-1 flex min-w-0 items-center gap-1.5 truncate text-xs text-rose-700" title={failure}><CircleAlert className="size-3.5 shrink-0" /><span className="truncate">{item.errorCode ? `${item.errorCode}: ` : null}{item.errorMessage || item.errorCode}</span></p> : null}</div>
        <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-180" />
      </button>
    </CollapsibleTrigger>
    <CollapsibleContent className="border-t border-border/60">
      {item.status === "FAILED" ? <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/60 bg-rose-50/60 px-4 py-2.5"><p className="text-xs text-rose-800">本次 workflow 执行失败，可以使用相同输入重新排队。</p><Button size="sm" variant="outline" onClick={onRetry} disabled={retrying}><RotateCcw className={`mr-1.5 size-3.5 ${retrying ? "animate-spin" : ""}`} />{retrying ? "重试中" : "重试"}</Button></div> : null}
      {item.resultRoute && (item.status === "SUCCEEDED" || item.status === "WAITING_REVIEW") ? <div className="flex items-center justify-between gap-3 border-b border-border/60 bg-muted/25 px-4 py-2.5"><p className="text-xs text-muted-foreground">业务结果仍在原有页面中查看。</p><Link to={item.resultRoute} className="shrink-0"><Button size="sm" variant="outline">查看结果<ArrowUpRight className="ml-1.5 size-3.5" /></Button></Link></div> : null}
      {!item.events.length ? <p className="px-4 py-3 text-xs text-muted-foreground">No execution events have been recorded yet.</p> : <ol className="divide-y divide-border/60 px-4 py-2">{attemptChains.map((chain) => <AttemptEventChain key={chain.attemptNo} chain={chain} />)}</ol>}
    </CollapsibleContent>
  </Collapsible>
}

export default function ApiWorkflowsPage() {
  const [items, setItems] = useState<ApiWorkflowProcess[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [pagination, setPagination] = useState<{ total: number; page: number; pageSize: number; totalPages: number; hasPrevious: boolean; hasNext: boolean } | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [openId, setOpenId] = useState<string | null>(null)
  const [retryingId, setRetryingId] = useState<string | null>(null)

  const load = useCallback(async (targetPage = page, targetPageSize = pageSize, background = false) => { if (background) setRefreshing(true); else setLoading(true); setError(null); try { const result = await listApiWorkflows(targetPage, targetPageSize); setItems(result.items); setPagination(result) } catch (cause) { setError(cause instanceof Error ? cause.message : "加载 API workflow 失败") } finally { setLoading(false); setRefreshing(false) } }, [page, pageSize])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 0); return () => window.clearTimeout(timer) }, [load])
  useEffect(() => { if (!items.some((item) => item.status === "QUEUED" || item.status === "RUNNING")) return; const timer = window.setInterval(() => void load(page, pageSize, true), 3000); return () => window.clearInterval(timer) }, [items, load, page, pageSize])
  const retry = useCallback(async (item: ApiWorkflowProcess) => { setRetryingId(item.id); try { await retryApiWorkflow(item); await load(page, pageSize, true); notification.success({ title: "已重新入队", description: "API workflow 将继续执行" }) } catch (cause) { notification.danger({ title: "重试失败", description: cause instanceof Error ? cause.message : "重试失败" }) } finally { setRetryingId(null) } }, [load, page, pageSize])

  const pages = pageItems(pagination?.page ?? page, pagination?.totalPages ?? 1)
  const start = pagination?.total ? (pagination.page - 1) * pagination.pageSize + 1 : 0
  const end = pagination ? Math.min(pagination.page * pagination.pageSize, pagination.total) : items.length

  return <WorkspaceShell sectionLabel="Settings" pageTitle="API Workflows">
    <div className="flex w-full min-w-0 flex-col gap-5">
      <section aria-labelledby="api-workflow-history-heading">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div><h1 id="api-workflow-history-heading" className="text-base font-semibold">API workflow history</h1><p className="mt-1 text-xs text-muted-foreground">Every request keeps its own processing record, including retries and result navigation.</p></div>
          <Button variant="outline" size="sm" onClick={() => void load(page, pageSize, true)} disabled={refreshing}><RefreshCw className={`mr-2 size-4 ${refreshing ? "animate-spin" : ""}`} />Refresh history</Button>
        </div>
      </section>
      {error ? <Alert variant="destructive"><AlertTitle>API workflow 加载失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
      {loading ? <div className="space-y-3"><Skeleton className="h-16 rounded-lg" /><Skeleton className="h-16 rounded-lg" /></div> : items.length ? <>
        <div className="overflow-hidden rounded-lg border border-border/60 divide-y divide-border/60">{items.map((item) => <ApiWorkflowRecord key={item.id} item={item} open={openId === item.id} onOpenChange={(open) => setOpenId(open ? item.id : null)} onRetry={() => void retry(item)} retrying={retryingId === item.id} />)}</div>
        {pagination ? <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-3"><div className="flex min-w-0 flex-wrap items-center gap-2 text-xs text-muted-foreground"><span className="font-mono tabular-nums">Showing {start}-{end} of {pagination.total}</span><span className="hidden text-border sm:inline">·</span><span className="hidden sm:inline">Rows</span><Select value={String(pageSize)} onValueChange={(value) => { const next = Number(value); setPageSize(next); setPage(1) }}><SelectTrigger size="sm" aria-label="Workflows per page" className="h-7 w-[104px] border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue /></SelectTrigger><SelectContent side="top">{PAGE_SIZE_OPTIONS.map((option) => <SelectItem key={option} value={String(option)}>{option} / page</SelectItem>)}</SelectContent></Select></div>{pagination.totalPages > 1 ? <Pagination className="mx-0 w-auto justify-end"><PaginationContent><PaginationItem><PaginationPrevious className="[&>span]:hidden" disabled={loading || !pagination.hasPrevious} onClick={() => setPage((current) => Math.max(1, current - 1))} /></PaginationItem>{pages.map((item, index) => { const previous = pages[index - 1]; return <Fragment key={item}>{previous != null && item - previous > 1 ? <PaginationItem><PaginationEllipsis /></PaginationItem> : null}<PaginationItem><PaginationLink disabled={loading} isActive={item === pagination.page} onClick={() => setPage(item)}>{item}</PaginationLink></PaginationItem></Fragment> })}<PaginationItem><PaginationNext className="[&>span]:hidden" disabled={loading || !pagination.hasNext} onClick={() => setPage((current) => Math.min(pagination.totalPages, current + 1))} /></PaginationItem></PaginationContent></Pagination> : null}</div> : null}
      </> : <div className="rounded-lg border border-dashed border-border/70 px-4 py-8 text-center"><FileClock className="mx-auto size-7 text-muted-foreground/50" /><p className="mt-2 text-sm font-medium">还没有 API workflow 记录</p><p className="mt-1 text-xs text-muted-foreground">API 请求进入队列后，记录会出现在这里。</p></div>}
    </div>
  </WorkspaceShell>
}
