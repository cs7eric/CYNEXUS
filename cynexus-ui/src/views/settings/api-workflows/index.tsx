import { useCallback, useEffect, useMemo, useState } from "react"
import { ArrowUpRight, Check, CircleAlert, Clock3, FileClock, FileImage, FileVideo2, Loader2, RefreshCw, RotateCcw, WalletCards } from "lucide-react"
import { Link } from "react-router-dom"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Input } from "@/components/ui/input"
import { Progress } from "@/components/ui/progress"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { listApiWorkflows, retryApiWorkflow, type ApiWorkflowProcess, type ApiWorkflowStatus } from "@/services/core/api-workflows"

const statusLabels: Record<ApiWorkflowStatus, string> = {
  QUEUED: "已入队", RUNNING: "处理中", WAITING_REVIEW: "待确认", SUCCEEDED: "已完成", FAILED: "失败", CANCELLED: "已归档",
}
const typeLabels = { MEDIA_TRANSCRIPTION: "URL 转写", PORTFOLIO_TRADE_IMPORT: "AI Trade", FINANCE_AI_CAPTURE: "AI 入账" } as const

function formatDate(value?: string | null) {
  if (!value) return "--"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false })
}

function statusVariant(status: ApiWorkflowStatus): "secondary" | "destructive" | "outline" {
  if (status === "FAILED") return "destructive"
  if (status === "SUCCEEDED") return "secondary"
  return "outline"
}

function WorkflowIcon({ type }: { type: ApiWorkflowProcess["workflowType"] }) {
  if (type === "MEDIA_TRANSCRIPTION") return <FileVideo2 className="size-4" />
  if (type === "FINANCE_AI_CAPTURE") return <WalletCards className="size-4" />
  return <FileImage className="size-4" />
}

function StepIcon({ event, active }: { event: ApiWorkflowProcess["events"][number]; active: boolean }) {
  if (event.status === "FAILED") return <span className="flex size-6 items-center justify-center rounded-full bg-destructive/10 text-destructive"><CircleAlert className="size-3.5" /></span>
  if (active) return <span className="flex size-6 items-center justify-center rounded-full bg-primary/10 text-primary"><Loader2 className="size-3.5 animate-spin" /></span>
  if (event.status === "SUCCEEDED" || event.status === "WAITING_REVIEW") return <span className="flex size-6 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-700"><Check className="size-3.5" /></span>
  return <span className="flex size-6 items-center justify-center rounded-full border border-border bg-background text-muted-foreground"><Clock3 className="size-3" /></span>
}

function WorkflowRecord({ item, open, onOpenChange, onRetry, retrying }: { item: ApiWorkflowProcess; open: boolean; onOpenChange: (open: boolean) => void; onRetry: () => void; retrying: boolean }) {
  return <Collapsible open={open} onOpenChange={onOpenChange} className="overflow-hidden border-b border-border/60 last:border-b-0">
    <CollapsibleTrigger asChild>
      <button type="button" className="group flex w-full min-w-0 items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset sm:px-5">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground"><WorkflowIcon type={item.workflowType} /></span>
        <span className="min-w-0 flex-1"><span className="flex flex-wrap items-center gap-x-2 gap-y-1"><span className="truncate text-sm font-medium">{item.title || typeLabels[item.workflowType]}</span><Badge variant={statusVariant(item.status)} className="shrink-0 text-[10px]">{statusLabels[item.status]}</Badge><span className="shrink-0 text-xs text-muted-foreground">{typeLabels[item.workflowType]}</span></span><span className="mt-1 block truncate text-xs text-muted-foreground">{item.workflowKey} · {item.sourceRef}</span></span>
        <span className="hidden shrink-0 text-xs text-muted-foreground sm:block">{formatDate(item.createdAt)}</span><span className="shrink-0 font-mono text-xs text-muted-foreground">{item.progress}%</span><ArrowUpRight className="size-4 shrink-0 text-muted-foreground transition-transform group-data-[state=open]:rotate-45" />
      </button>
    </CollapsibleTrigger>
    <CollapsibleContent className="border-t border-border/60 bg-muted/15 px-4 py-4 sm:px-5"><div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(280px,0.65fr)]">
      <section className="min-w-0"><div className="mb-3 flex flex-wrap items-center justify-between gap-2"><div><h2 className="text-sm font-semibold">Process history</h2><p className="mt-1 text-xs text-muted-foreground">这里只记录 API workflow 的执行过程。</p></div>{item.status === "FAILED" ? <Button size="sm" variant="outline" onClick={onRetry} disabled={retrying}><RotateCcw className={`size-3.5 ${retrying ? "animate-spin" : ""}`} />{retrying ? "重试中" : "重试"}</Button> : null}</div><ol className="divide-y divide-border/60">{item.events.map((event, index) => <li key={`${event.eventType}-${event.occurredAt}-${index}`} className="flex gap-3 py-3 first:pt-0 last:pb-0"><div className="flex shrink-0 flex-col items-center"><StepIcon event={event} active={item.status === "RUNNING" && index === item.events.length - 1} />{index < item.events.length - 1 ? <span className="mt-1 h-full min-h-4 w-px bg-border" /> : null}</div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><span className="text-sm font-medium">{event.message || event.eventType}</span><Badge variant="outline" className="text-[10px]">{event.phase}</Badge><span className="text-xs text-muted-foreground">{formatDate(event.occurredAt)}</span></div>{event.errorMessage ? <p className="mt-1 break-words text-xs text-destructive">{event.errorMessage}</p> : null}</div></li>)}</ol></section>
      <section className="min-w-0 space-y-3"><div className="rounded-md bg-background p-3"><div className="flex items-center justify-between gap-3 text-xs"><span className="font-medium">{item.phase}</span><span className="font-mono tabular-nums text-muted-foreground">{item.progress}%</span></div><Progress value={item.progress} className="mt-2 h-1.5" /><div className="mt-3 grid grid-cols-2 gap-3 text-xs text-muted-foreground"><div>创建<div className="mt-1 text-foreground">{formatDate(item.createdAt)}</div></div><div>更新<div className="mt-1 text-foreground">{formatDate(item.updatedAt)}</div></div></div></div><div className="space-y-2 rounded-md border border-border/60 bg-background p-3 text-xs"><div><span className="text-muted-foreground">Workflow ID</span><div className="mt-1 break-all font-mono text-foreground">{item.id}</div></div><div><span className="text-muted-foreground">尝试次数</span><div className="mt-1 font-mono text-foreground">{item.attemptCount}</div></div>{item.errorMessage ? <div className="break-words text-destructive"><span className="font-medium">错误信息</span><div className="mt-1">{item.errorMessage}</div></div> : null}</div>{item.resultRoute ? <Link to={item.resultRoute} className="flex items-center justify-between gap-3 rounded-md border border-border/60 bg-background px-3 py-2 text-sm font-medium hover:bg-muted/40"><span>查看业务结果</span><ArrowUpRight className="size-4" /></Link> : null}</section>
    </div></CollapsibleContent>
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
  return <WorkspaceShell sectionLabel="Settings" pageTitle="API Workflows"><div className="flex w-full flex-col gap-5"><header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div className="min-w-0"><div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><FileClock className="size-4" />统一 API workflow process history</div><p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">统一记录 URL 转写、AI Trade 和 AI 入账的队列、步骤、进度与错误；业务产出仍在各自页面查看。</p></div><Button variant="outline" onClick={() => void load(true)} disabled={refreshing}><RefreshCw className={`size-4 ${refreshing ? "animate-spin" : ""}`} />刷新</Button></header><div className="flex flex-wrap items-center gap-3"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索 workflow、类型、状态" className="w-full sm:max-w-sm" /><span className="text-xs text-muted-foreground">共 {visibleItems.length} / {items.length} 条</span></div>{error ? <Alert variant="destructive"><AlertTitle>API workflow 加载失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}<section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-label="API workflow process history">{loading ? <div className="space-y-3 p-4"><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /></div> : visibleItems.length ? visibleItems.map((item) => <WorkflowRecord key={item.id} item={item} open={openId === item.id} onOpenChange={(open) => setOpenId(open ? item.id : null)} onRetry={() => void retry(item)} retrying={retryingId === item.id} />) : <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center"><FileClock className="size-7 text-muted-foreground/50" /><p className="mt-2 text-sm font-medium">还没有 API workflow 记录</p><p className="mt-1 text-xs text-muted-foreground">API 请求进入队列后，记录会出现在这里。</p></div>}</section></div></WorkspaceShell>
}
