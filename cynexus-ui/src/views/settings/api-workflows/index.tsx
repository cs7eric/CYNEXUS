import { useCallback, useEffect, useMemo, useState } from "react"
import { Check, ChevronDown, CircleAlert, Clock3, FileVideo2, Loader2, RefreshCw, RotateCcw } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Input } from "@/components/ui/input"
import { Progress } from "@/components/ui/progress"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  getMediaProcess,
  listMediaProcesses,
  retryMediaProcess,
  type MediaProcessItem,
} from "@/services/core/mp4-process-history"

const PHASES = [
  { id: "queued", label: "已入队", description: "请求已接受，等待后台调度。" },
  { id: "starting", label: "启动处理", description: "后台 worker 已开始处理本次 workflow。" },
  { id: "downloading", label: "解析链接并获取媒体", description: "Downloader 正在解析原始页面并获取可转写媒体。" },
  { id: "transcribing", label: "转写", description: "媒体已交给转写服务。" },
  { id: "polishing", label: "AI 润色", description: "正在整理转写文本。" },
  { id: "summarizing", label: "生成摘要", description: "正在生成内容摘要。" },
  { id: "metadata", label: "整理元数据", description: "正在合并标题、平台和媒体元数据。" },
  { id: "persisting", label: "写入历史", description: "正在保存最终结果到历史记录。" },
  { id: "completed", label: "处理完成", description: "workflow 已完成，结果可以查看。" },
] as const

const PHASE_INDEX = new Map(PHASES.map((phase, index) => [phase.id, index]))
type WorkflowPhase = typeof PHASES[number]["id"]

function formatDate(value?: string | null) {
  if (!value) return "--"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false })
}

function statusLabel(status: string) {
  if (status === "succeeded") return "已完成"
  if (status === "running") return "处理中"
  if (status === "failed") return "失败"
  return "已入队"
}

function statusVariant(status: string): "secondary" | "destructive" | "outline" {
  if (status === "failed") return "destructive"
  if (status === "succeeded") return "secondary"
  return "outline"
}

function currentPhaseIndex(item: MediaProcessItem) {
  if (item.status === "succeeded") return PHASES.length - 1
  const phase = item.phase as WorkflowPhase
  if (item.status === "failed") return Math.max(0, PHASE_INDEX.get(phase) ?? 0)
  return PHASE_INDEX.get(phase) ?? 0
}

function StepIcon({ state }: { state: "pending" | "running" | "completed" | "failed" }) {
  if (state === "completed") return <span className="flex size-6 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-700"><Check className="size-3.5" /></span>
  if (state === "failed") return <span className="flex size-6 items-center justify-center rounded-full bg-destructive/10 text-destructive"><CircleAlert className="size-3.5" /></span>
  if (state === "running") return <span className="flex size-6 items-center justify-center rounded-full bg-primary/10 text-primary"><Loader2 className="size-3.5 animate-spin" /></span>
  return <span className="flex size-6 items-center justify-center rounded-full border border-border bg-background text-muted-foreground"><Clock3 className="size-3" /></span>
}

function WorkflowSteps({ item }: { item: MediaProcessItem }) {
  const activeIndex = currentPhaseIndex(item)
  return (
    <ol className="divide-y divide-border/60">
      {PHASES.map((phase, index) => {
        const failed = item.status === "failed" && index === activeIndex
        const completed = item.status === "succeeded" ? index <= activeIndex : index < activeIndex
        const running = item.status !== "failed" && item.status !== "succeeded" && index === activeIndex
        const state = failed ? "failed" : completed ? "completed" : running ? "running" : "pending"
        return (
          <li key={phase.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
            <div className="flex shrink-0 flex-col items-center">
              <StepIcon state={state} />
              {index < PHASES.length - 1 ? <span className={`mt-1 h-full min-h-4 w-px ${completed ? "bg-emerald-500/40" : "bg-border"}`} /> : null}
            </div>
            <div className="min-w-0 flex-1 pb-1">
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                <span className={`text-sm font-medium ${state === "pending" ? "text-muted-foreground" : "text-foreground"}`}>{phase.label}</span>
                {state === "running" ? <Badge variant="outline" className="text-[10px]">处理中</Badge> : null}
                {state === "failed" ? <Badge variant="destructive" className="text-[10px]">失败</Badge> : null}
              </div>
              <p className="mt-1 text-xs leading-5 text-muted-foreground">{phase.description}</p>
              {state === "failed" && item.errorMessage ? <p className="mt-1 break-words text-xs text-destructive">{item.errorMessage}</p> : null}
            </div>
          </li>
        )
      })}
    </ol>
  )
}

function WorkflowRecord({ item, open, onOpenChange, onRetry, retrying }: {
  item: MediaProcessItem
  open: boolean
  onOpenChange: (open: boolean) => void
  onRetry: () => void
  retrying: boolean
}) {
  return (
    <Collapsible open={open} onOpenChange={onOpenChange} className="overflow-hidden border-b border-border/60 last:border-b-0">
      <CollapsibleTrigger asChild>
        <button type="button" className="group flex w-full min-w-0 items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/35 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset sm:px-5">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground"><FileVideo2 className="size-4" /></span>
          <span className="min-w-0 flex-1">
            <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
              <span className="truncate text-sm font-medium text-foreground">{item.title || item.fileName || "URL workflow"}</span>
              <Badge variant={statusVariant(item.status)} className="shrink-0 text-[10px]">{statusLabel(item.status)}</Badge>
              <span className="shrink-0 text-xs text-muted-foreground">{item.progress ?? 0}%</span>
            </span>
            <span className="mt-1 block truncate text-xs text-muted-foreground" title={item.sourceUrl}>{item.sourceUrl}</span>
          </span>
          <span className="hidden shrink-0 text-xs text-muted-foreground sm:block">{formatDate(item.createdAt)}</span>
          <ChevronDown className="size-4 shrink-0 text-muted-foreground transition-transform duration-150 group-data-[state=open]:rotate-180" />
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent className="border-t border-border/60 bg-muted/15 px-4 py-4 sm:px-5">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(300px,0.8fr)]">
          <section className="min-w-0">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <div><h3 className="text-sm font-semibold">Process history</h3><p className="mt-1 text-xs text-muted-foreground">当前状态会随后台 workflow 自动更新。</p></div>
              {item.status === "failed" ? <Button size="sm" variant="outline" onClick={onRetry} disabled={retrying}><RotateCcw className={`size-3.5 ${retrying ? "animate-spin" : ""}`} />{retrying ? "重试中" : "重试"}</Button> : null}
            </div>
            <WorkflowSteps item={item} />
          </section>
          <section className="min-w-0 space-y-3">
            <div className="rounded-md bg-background p-3">
              <div className="flex items-center justify-between gap-3 text-xs"><span className="font-medium text-foreground">{item.phase || "queued"}</span><span className="font-mono tabular-nums text-muted-foreground">{item.progress ?? 0}%</span></div>
              <Progress value={item.progress ?? 0} className="mt-2 h-1.5" />
              <div className="mt-3 grid grid-cols-2 gap-3 text-xs text-muted-foreground"><div>创建<div className="mt-1 text-foreground">{formatDate(item.createdAt)}</div></div><div>完成<div className="mt-1 text-foreground">{formatDate(item.completedAt)}</div></div></div>
            </div>
            <div className="space-y-2 rounded-md border border-border/60 bg-background p-3 text-xs">
              <div><span className="text-muted-foreground">原始 URL</span><div className="mt-1 break-all text-foreground">{item.sourceUrl}</div></div>
              {item.downloadUrl ? <div><span className="text-muted-foreground">实际媒体地址</span><div className="mt-1 break-all text-foreground">{item.downloadUrl}</div></div> : null}
              <div className="grid grid-cols-2 gap-3"><div><span className="text-muted-foreground">平台</span><div className="mt-1 text-foreground">{item.platform || "--"}</div></div><div><span className="text-muted-foreground">尝试次数</span><div className="mt-1 font-mono text-foreground">{item.attemptCount ?? 0}</div></div></div>
              <div><span className="text-muted-foreground">Process ID</span><div className="mt-1 break-all font-mono text-foreground">{item.id}</div></div>
            </div>
            {item.summary || item.polished || item.transcript ? <div className="space-y-3 rounded-md border border-border/60 bg-background p-3 text-sm"><h3 className="text-sm font-semibold">结果</h3>{item.summary ? <div><div className="text-xs text-muted-foreground">摘要</div><p className="mt-1 whitespace-pre-wrap break-words leading-6">{item.summary}</p></div> : null}{item.polished ? <div><div className="text-xs text-muted-foreground">AI 润色</div><p className="mt-1 line-clamp-6 whitespace-pre-wrap break-words leading-6">{item.polished}</p></div> : null}{item.transcript ? <div><div className="text-xs text-muted-foreground">转写文本</div><p className="mt-1 line-clamp-8 whitespace-pre-wrap break-words leading-6">{item.transcript}</p></div> : null}</div> : null}
            {item.errorMessage ? <div className="break-words rounded-md bg-destructive/10 p-3 text-xs leading-5 text-destructive"><div className="font-medium">错误信息</div><div className="mt-1">{item.errorMessage}</div></div> : null}
          </section>
        </div>
      </CollapsibleContent>
    </Collapsible>
  )
}

export default function ApiWorkflowsPage() {
  const [items, setItems] = useState<MediaProcessItem[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [openId, setOpenId] = useState<string | null>(null)
  const [retryingId, setRetryingId] = useState<string | null>(null)

  const load = useCallback(async (background = false) => {
    if (background) setRefreshing(true)
    else setLoading(true)
    setError(null)
    try {
      const next = await listMediaProcesses()
      setItems(next)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "加载 API workflow 失败")
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])
  useEffect(() => {
    if (!items.some((item) => item.status === "queued" || item.status === "running")) return
    const timer = window.setInterval(() => { void load(true) }, 3000)
    return () => window.clearInterval(timer)
  }, [items, load])

  const visibleItems = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    if (!keyword) return items
    return items.filter((item) => [item.title, item.fileName, item.sourceUrl, item.status, item.phase].filter(Boolean).join(" ").toLowerCase().includes(keyword))
  }, [items, query])
  const stats = useMemo(() => ({ active: items.filter((item) => item.status === "queued" || item.status === "running").length, succeeded: items.filter((item) => item.status === "succeeded").length, failed: items.filter((item) => item.status === "failed").length }), [items])

  const openRecord = useCallback(async (id: string, open: boolean) => {
    setOpenId(open ? id : null)
    if (!open) return
    try {
      const detail = await getMediaProcess(id)
      setItems((current) => current.map((item) => item.id === id ? detail : item))
    } catch { /* list data remains available */ }
  }, [])

  const retry = useCallback(async (id: string) => {
    setRetryingId(id)
    try {
      const next = await retryMediaProcess(id)
      setItems((current) => current.map((item) => item.id === id ? next : item))
      notification.success({ title: "已重新入队", description: "API workflow 将继续执行" })
    } catch (cause) {
      notification.danger({ title: "重试失败", description: cause instanceof Error ? cause.message : "重试失败" })
    } finally {
      setRetryingId(null)
    }
  }, [])

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="API Workflows">
      <div className="flex w-full flex-col gap-5">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0"><div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><FileVideo2 className="size-4" />API 触发的统一处理流程</div><p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">查看通过 API 进入队列的 URL workflow，包括链接解析、转写、AI 加工和历史写入。提交接口会立即返回“已入队”。</p></div>
          <Button variant="outline" onClick={() => void load(true)} disabled={refreshing}><RefreshCw className={`size-4 ${refreshing ? "animate-spin" : ""}`} />刷新</Button>
        </header>
        <div className="grid gap-3 sm:grid-cols-3">{[{ label: "处理中", value: stats.active }, { label: "已完成", value: stats.succeeded }, { label: "失败", value: stats.failed }].map((stat) => <div key={stat.label} className="rounded-md bg-muted/60 px-4 py-3"><div className="text-xs font-medium text-muted-foreground">{stat.label}</div><div className="mt-2 font-mono text-2xl font-semibold tabular-nums text-foreground">{stat.value}</div></div>)}</div>
        <div className="flex flex-wrap items-center gap-3"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索标题、URL、状态" className="w-full sm:max-w-sm" /><span className="text-xs text-muted-foreground">共 {visibleItems.length} / {items.length} 条</span></div>
        {error ? <Alert variant="destructive"><AlertTitle>API workflow 加载失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
        <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-label="API workflow history">
          {loading ? <div className="space-y-3 p-4"><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /></div> : visibleItems.length ? visibleItems.map((item) => <WorkflowRecord key={item.id} item={item} open={openId === item.id} onOpenChange={(open) => void openRecord(item.id, open)} onRetry={() => void retry(item.id)} retrying={retryingId === item.id} />) : <div className="flex min-h-48 flex-col items-center justify-center px-6 text-center"><FileVideo2 className="size-7 text-muted-foreground/50" /><p className="mt-2 text-sm font-medium">还没有 API workflow 记录</p><p className="mt-1 text-xs text-muted-foreground">调用 URL 入队接口后，任务会出现在这里。</p></div>}
        </section>
      </div>
    </WorkspaceShell>
  )
}
