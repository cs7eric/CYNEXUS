import { useCallback, useEffect, useMemo, useState } from "react"
import { ArrowUpRight, ChevronDown, ChevronRight, CircleAlert, FileClock, FileImage, FileVideo2, RefreshCw, RotateCcw, WalletCards } from "lucide-react"
import { Link } from "react-router-dom"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { listApiWorkflows, retryApiWorkflow, type ApiWorkflowEvent, type ApiWorkflowProcess, type ApiWorkflowStatus } from "@/services/core/api-workflows"

const statusLabels: Record<ApiWorkflowStatus, string> = {
  QUEUED: "已入队", RUNNING: "处理中", WAITING_REVIEW: "待确认", SUCCEEDED: "已完成", FAILED: "失败", CANCELLED: "已归档",
}

const typeLabels: Record<ApiWorkflowProcess["workflowType"], string> = {
  MEDIA_TRANSCRIPTION: "URL 转写", PORTFOLIO_TRADE_IMPORT: "AI Trade", FINANCE_AI_CAPTURE: "AI 入账",
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
  return event.message || event.eventType
}

function EventDetail({ event }: { event: ApiWorkflowEvent }) {
  return <div className="py-2 first:pt-1 last:pb-1">
    <div className="flex flex-wrap items-center gap-2"><p className="text-xs font-medium text-foreground">{eventLabel(event)}</p><span className={`rounded-full px-2 py-0.5 text-[11px] ${statusClass(event.status)}`}>{statusLabels[event.status]}</span></div>
    <p className="mt-0.5 text-[11px] leading-4 text-muted-foreground">{event.phase} · {event.progress}% · {formatClock(event.occurredAt)}</p>
    <p className="mt-1 break-words text-[11px] text-muted-foreground">{event.eventType}</p>
    {event.errorCode || event.errorMessage ? <p className="mt-1 break-words text-[11px] text-rose-700"><span className="font-mono font-medium">{event.errorCode}</span>{event.errorMessage ? ` · ${event.errorMessage}` : null}</p> : null}
  </div>
}

function AttemptEventChain({ item }: { item: ApiWorkflowProcess }) {
  const [open, setOpen] = useState(false)
  const events = item.events
  const latest = events.at(-1)
  const failure = [...events].reverse().find((event) => event.errorCode || event.errorMessage)
  const labels = events.map(eventLabel)

  return <li className="py-0.5">
    <Collapsible open={open} onOpenChange={setOpen}>
      <CollapsibleTrigger asChild>
        <button type="button" className="group flex w-full min-w-0 items-center gap-2 rounded-md px-2 py-2 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          <ChevronRight className="size-3.5 shrink-0 text-muted-foreground transition-transform duration-150 group-data-[state=open]:rotate-90" />
          <span className="w-[68px] shrink-0 font-mono text-xs font-medium tabular-nums text-foreground">Execution</span>
          <span className="min-w-0 flex-1 truncate text-xs text-foreground">{labels.length ? labels.join(" → ") : "No events"}</span>
          {failure ? <span className="hidden max-w-[180px] truncate font-mono text-[11px] text-rose-700 md:inline">{failure.errorCode || "FAILED"}</span> : null}
          {latest ? <span className={`hidden rounded-full px-2 py-0.5 text-[11px] sm:inline ${statusClass(latest.status)}`}>{statusLabels[latest.status]}</span> : null}
          <span className="shrink-0 text-[11px] text-muted-foreground">{formatTime(latest?.occurredAt || item.updatedAt)}</span>
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent><div className="ml-[22px] border-l border-border/70 py-1 pl-3">{events.map((event, index) => <EventDetail key={`${event.eventType}-${event.occurredAt}-${index}`} event={event} />)}</div></CollapsibleContent>
    </Collapsible>
  </li>
}

function ApiWorkflowRecord({ item, open, onOpenChange, onRetry, retrying }: { item: ApiWorkflowProcess; open: boolean; onOpenChange: (open: boolean) => void; onRetry: () => void; retrying: boolean }) {
  const statusLabel = statusLabels[item.status]
  const failure = item.errorMessage || item.errorCode

  return <Collapsible className="overflow-hidden bg-card" open={open} onOpenChange={onOpenChange}>
    <CollapsibleTrigger asChild>
      <button type="button" className="group flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted"><WorkflowIcon type={item.workflowType} /></div>
        <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-x-2 gap-y-1"><h2 className="truncate text-sm font-semibold">{item.title || typeLabels[item.workflowType]}</h2><span className={`rounded-full px-2 py-0.5 text-xs ${statusClass(item.status)}`}>{statusLabel}</span><span className="text-xs text-muted-foreground">{typeLabels[item.workflowType]}</span><span className="text-xs text-muted-foreground">{item.attemptCount} attempts</span><span className="text-xs text-muted-foreground">{formatTime(item.updatedAt || item.createdAt)}</span></div><p className="mt-1 truncate text-xs text-muted-foreground">{item.workflowKey} · {item.sourceRef}</p>{failure ? <p className="mt-1 flex min-w-0 items-center gap-1.5 truncate text-xs text-rose-700" title={failure}><CircleAlert className="size-3.5 shrink-0" /><span className="truncate">{item.errorCode ? `${item.errorCode}: ` : null}{item.errorMessage || item.errorCode}</span></p> : null}</div>
        <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-180" />
      </button>
    </CollapsibleTrigger>
    <CollapsibleContent className="border-t border-border/60">
      {item.status === "FAILED" ? <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/60 bg-rose-50/60 px-4 py-2.5"><p className="text-xs text-rose-800">本次 workflow 执行失败，可以使用相同输入重新排队。</p><Button size="sm" variant="outline" onClick={onRetry} disabled={retrying}><RotateCcw className={`mr-1.5 size-3.5 ${retrying ? "animate-spin" : ""}`} />{retrying ? "重试中" : "重试"}</Button></div> : null}
      {item.resultRoute && (item.status === "SUCCEEDED" || item.status === "WAITING_REVIEW") ? <div className="flex items-center justify-between gap-3 border-b border-border/60 bg-muted/25 px-4 py-2.5"><p className="text-xs text-muted-foreground">业务结果仍在原有页面中查看。</p><Link to={item.resultRoute} className="shrink-0"><Button size="sm" variant="outline">查看结果<ArrowUpRight className="ml-1.5 size-3.5" /></Button></Link></div> : null}
      {!item.events.length ? <p className="px-4 py-3 text-xs text-muted-foreground">No execution events have been recorded yet.</p> : <ol className="divide-y divide-border/60 px-4 py-2"><AttemptEventChain item={item} /></ol>}
    </CollapsibleContent>
  </Collapsible>
}

export default function ApiWorkflowsPage() {
  const [items, setItems] = useState<ApiWorkflowProcess[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [openId, setOpenId] = useState<string | null>(null)
  const [retryingId, setRetryingId] = useState<string | null>(null)

  const load = useCallback(async (background = false) => { if (background) setRefreshing(true); else setLoading(true); setError(null); try { setItems(await listApiWorkflows()) } catch (cause) { setError(cause instanceof Error ? cause.message : "加载 API workflow 失败") } finally { setLoading(false); setRefreshing(false) } }, [])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 0); return () => window.clearTimeout(timer) }, [load])
  useEffect(() => { if (!items.some((item) => item.status === "QUEUED" || item.status === "RUNNING")) return; const timer = window.setInterval(() => void load(true), 3000); return () => window.clearInterval(timer) }, [items, load])
  const visibleItems = useMemo(() => { const keyword = query.trim().toLowerCase(); return keyword ? items.filter((item) => [item.title, item.workflowType, item.workflowKey, item.status, item.phase, item.sourceRef].join(" ").toLowerCase().includes(keyword)) : items }, [items, query])
  const retry = useCallback(async (item: ApiWorkflowProcess) => { setRetryingId(item.id); try { await retryApiWorkflow(item); await load(true); notification.success({ title: "已重新入队", description: "API workflow 将继续执行" }) } catch (cause) { notification.danger({ title: "重试失败", description: cause instanceof Error ? cause.message : "重试失败" }) } finally { setRetryingId(null) } }, [load])

  return <WorkspaceShell sectionLabel="Settings" pageTitle="API Workflows">
    <div className="flex w-full min-w-0 flex-col gap-5">
      <section aria-labelledby="api-workflow-history-heading">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div><h1 id="api-workflow-history-heading" className="text-base font-semibold">API workflow history</h1><p className="mt-1 text-xs text-muted-foreground">Every API request keeps its own processing record, including retries and result navigation.</p></div>
          <Button variant="outline" size="sm" onClick={() => void load(true)} disabled={refreshing}><RefreshCw className={`mr-2 size-4 ${refreshing ? "animate-spin" : ""}`} />Refresh history</Button>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-3"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索 workflow、类型、状态" className="w-full sm:max-w-sm" /><span className="text-xs text-muted-foreground">{visibleItems.length} / {items.length} records</span></div>
      </section>
      {error ? <Alert variant="destructive"><AlertTitle>API workflow 加载失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
      {loading ? <div className="space-y-3"><Skeleton className="h-16 rounded-lg" /><Skeleton className="h-16 rounded-lg" /></div> : visibleItems.length ? <div className="overflow-hidden rounded-lg border border-border/60 divide-y divide-border/60">{visibleItems.map((item) => <ApiWorkflowRecord key={item.id} item={item} open={openId === item.id} onOpenChange={(open) => setOpenId(open ? item.id : null)} onRetry={() => void retry(item)} retrying={retryingId === item.id} />)}</div> : <div className="rounded-lg border border-dashed border-border/70 px-4 py-8 text-center"><FileClock className="mx-auto size-7 text-muted-foreground/50" /><p className="mt-2 text-sm font-medium">{query ? "没有匹配的处理记录" : "还没有 API workflow 记录"}</p><p className="mt-1 text-xs text-muted-foreground">API 请求进入队列后，记录会出现在这里。</p></div>}
    </div>
  </WorkspaceShell>
}
