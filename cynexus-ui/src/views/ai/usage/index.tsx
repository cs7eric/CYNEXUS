import { useCallback, useEffect, useMemo, useState } from "react"
import {
  Activity,
  AlertTriangle,
  ArrowDownToLine,
  ArrowUpFromLine,
  CheckCircle2,
  Clock3,
  DollarSign,
  Gauge,
  RefreshCw,
  XCircle,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Pagination, PaginationContent, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cn } from "@/lib/utils"
import { fetchAiUsage, type AiUsageBreakdown, type AiUsageObservation, type AiUsagePage } from "@/services/ai/usage"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/ai-usage.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

const DAY_MS = 24 * 60 * 60 * 1000
const RECENT_CALLS_PAGE_SIZE = 50

function isoDate(value: Date) {
  return value.toISOString().slice(0, 10)
}

function number(value: number | null | undefined) {
  return (value || 0).toLocaleString("en-US")
}

function money(value: number | null | undefined) {
  return `$${(value || 0).toFixed(4)}`
}

function time(value?: string | null) {
  if (!value) return "--"
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime())
    ? "--"
    : parsed.toLocaleString([], { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

function capabilityLabel(value?: string | null) {
  const labels: Record<string, string> = {
    application_recent30: "AI Direction",
    market_information_event_analysis: "Market Information",
    dev_smoke: "Development Smoke",
  }
  return value ? labels[value] ?? value.replaceAll("_", " ") : "AI completion"
}

function providerLabel(value?: string | null) {
  if (!value) return "Unknown provider"
  return value === "custom" ? "Custom" : value === "deepseek" ? "DeepSeek" : value
}

function modelLabel(value?: string | null) {
  return value?.replace(/^.*\//, "") || "--"
}

function statusStyle(status?: string | null) {
  const normalized = status?.toUpperCase()
  if (normalized === "SUCCEEDED") return "border-emerald-200 bg-emerald-50 text-emerald-700"
  if (normalized === "FAILED") return "border-rose-200 bg-rose-50 text-rose-700"
  return "border-amber-200 bg-amber-50 text-amber-700"
}

function statusLabel(status?: string | null) {
  const normalized = status?.toUpperCase()
  return normalized === "SUCCEEDED" ? "Success" : normalized === "FAILED" ? "Failed" : "Pending"
}

function averageLatency(observations: AiUsageObservation[]) {
  const values = observations.map((item) => item.latencyMs || 0).filter(Boolean)
  return values.length ? Math.round(values.reduce((sum, value) => sum + value, 0) / values.length) : 0
}

function SummaryMetric({ label, value, detail, icon: Icon, tone }: { label: string; value: string; detail: string; icon: typeof Activity; tone: string }) {
  return (
    <div className="flex min-h-24 items-center gap-3 border-r border-border/60 px-4 first:pl-0 last:border-r-0 last:pr-0">
      <span className={cn("grid h-8 w-8 shrink-0 place-items-center rounded-md", tone)}><Icon className="h-4 w-4" /></span>
      <div className="min-w-0">
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        <p className="mt-1 truncate text-xl font-semibold tabular-nums tracking-tight">{value}</p>
        <p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p>
      </div>
    </div>
  )
}

function CapabilityRow({ item, maxTokens }: { item: AiUsageBreakdown; maxTokens: number }) {
  const percentage = maxTokens ? Math.max(4, Math.round((item.totalTokens / maxTokens) * 100)) : 0
  return (
    <div className="space-y-1 py-1.5 first:pt-0 last:pb-0">
      <div className="flex items-center justify-between gap-3 text-sm">
        <div className="min-w-0">
          <p className="truncate font-medium">{capabilityLabel(item.capability)}</p>
          <p className="truncate text-xs text-muted-foreground">{providerLabel(item.providerCode)} · {modelLabel(item.modelCode)}</p>
        </div>
        <div className="shrink-0 text-right"><p className="font-medium tabular-nums">{number(item.totalTokens)}</p><p className="text-[11px] text-muted-foreground">{item.requests} calls · {money(item.totalCost)}</p></div>
      </div>
      <div className="h-1 overflow-hidden rounded-full bg-muted"><div className="h-full rounded-full bg-primary/70 transition-all" style={{ width: `${percentage}%` }} /></div>
    </div>
  )
}

function ProviderHealthRow({ provider, model, requests, succeeded, failed }: { provider: string; model: string; requests: number; succeeded: number; failed: number }) {
  const successRate = requests ? Math.round((succeeded / requests) * 100) : 0
  const healthy = failed === 0
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 py-1.5 first:pt-0 last:pb-0">
      <div className="min-w-0">
        <div className="flex items-center gap-2"><span className={cn("size-2 shrink-0 rounded-full", healthy ? "bg-emerald-500" : "bg-rose-500")} /><p className="truncate text-sm font-medium">{provider}</p></div>
        <p className="mt-0.5 truncate pl-4 font-mono text-[11px] text-muted-foreground">{model}</p>
        <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-muted"><div className={cn("h-full rounded-full", healthy ? "bg-emerald-500/70" : "bg-rose-500/70")} style={{ width: `${successRate}%` }} /></div>
      </div>
      <div className="text-right text-xs"><p className={cn("font-medium tabular-nums", healthy ? "text-emerald-600" : "text-rose-600")}>{healthy ? "Healthy" : `${failed} failed`}</p><p className="mt-0.5 text-muted-foreground">{requests} calls · {successRate}%</p></div>
    </div>
  )
}

export default function AiUsageSettingsPage() {
  const [data, setData] = useState<AiUsagePage | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [to, setTo] = useState(() => isoDate(new Date()))
  const [from, setFrom] = useState(() => isoDate(new Date(Date.now() - 7 * DAY_MS)))
  const [recentPage, setRecentPage] = useState(1)
  const [recentPageSize, setRecentPageSize] = useState(RECENT_CALLS_PAGE_SIZE)

  const refresh = useCallback(async (initial = false) => {
    if (initial) setLoading(true); else setRefreshing(true)
    setError(null)
    try { setData(await fetchAiUsage({ from: `${from}T00:00:00Z`, to: `${to}T23:59:59Z` })); setRecentPage(1) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load AI usage") }
    finally { setLoading(false); setRefreshing(false) }
  }, [from, to])

  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(true), 0)
    return () => window.clearTimeout(timer)
  }, [refresh])

  const metrics = useMemo(() => {
    const requests = data?.requests || 0
    const succeeded = data?.succeeded || 0
    const failed = data?.failed || 0
    const input = data?.inputTokens || 0
    const output = data?.outputTokens || 0
    return { requests, succeeded, failed, input, output, successRate: requests ? Math.round((succeeded / requests) * 100) : 0, latency: averageLatency(data?.observations || []) }
  }, [data])

  const breakdown = useMemo(() => [...(data?.breakdown || [])].sort((a, b) => b.totalTokens - a.totalTokens), [data])
  const maxTokens = breakdown[0]?.totalTokens || 0
  const providerHealth = useMemo(() => {
    const values = new Map<string, { provider: string; model: string; requests: number; succeeded: number; failed: number }>()
    for (const item of breakdown) {
      const key = `${item.providerCode || "unknown"}:${item.modelCode || "unknown"}`
      const current = values.get(key) ?? { provider: providerLabel(item.providerCode), model: modelLabel(item.modelCode), requests: 0, succeeded: 0, failed: 0 }
      current.requests += item.requests
      current.succeeded += item.succeeded
      current.failed += item.failed
      values.set(key, current)
    }
    return [...values.values()].sort((left, right) => right.failed - left.failed || right.requests - left.requests)
  }, [breakdown])
  const visibleObservations = useMemo(() => (data?.observations || []).filter((item) => item.name !== "dev.smoke"), [data])
  const recentPageCount = Math.max(1, Math.ceil(visibleObservations.length / recentPageSize))
  const recentPageItems = useMemo(() => {
    const start = (recentPage - 1) * recentPageSize
    return visibleObservations.slice(start, start + recentPageSize)
  }, [recentPage, recentPageSize, visibleObservations])
  const recentRangeStart = visibleObservations.length ? (recentPage - 1) * recentPageSize + 1 : 0
  const recentRangeEnd = Math.min(recentPage * recentPageSize, visibleObservations.length)

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="AI Usage" contentScrollable={false}>
      <div className="flex h-full min-h-0 flex-col gap-5">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Observability</p>{data?.available ? <Badge variant="secondary" className="gap-1 text-[10px] font-normal"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />Langfuse connected</Badge> : null}</div>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">AI usage</h1>
            <p className="mt-1 text-sm text-muted-foreground">A compact view of model traffic, cost, latency and failures.</p>
          </div>
          <div className="flex flex-wrap items-end gap-2">
            <label className="grid gap-1 text-xs text-muted-foreground">From<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} className="h-9 rounded-md border bg-background px-3 text-sm text-foreground" /></label>
            <label className="grid gap-1 text-xs text-muted-foreground">To<input type="date" value={to} onChange={(event) => setTo(event.target.value)} className="h-9 rounded-md border bg-background px-3 text-sm text-foreground" /></label>
            <Button variant="secondary" size="sm" onClick={() => void refresh()} disabled={refreshing}><RefreshCw className={cn("mr-2 h-4 w-4", refreshing && "animate-spin")} />Refresh</Button>
          </div>
        </div>

        {error ? <Alert variant="destructive"><AlertTriangle className="h-4 w-4" /><AlertTitle>Usage unavailable</AlertTitle><AlertDescription>{error}</AlertDescription></Alert> : null}
        {!error && data && !data.available ? <Alert><AlertTriangle className="h-4 w-4" /><AlertTitle>Langfuse has no data</AlertTitle><AlertDescription>{data.message || "Enable the shared Langfuse stack and AI reporter to see usage."}</AlertDescription></Alert> : null}

        <Card className="gap-0 overflow-hidden py-0 shadow-none">
          <CardContent className="grid gap-0 border-b border-border/60 px-4 py-0 sm:grid-cols-2 xl:grid-cols-5">
            <SummaryMetric label="Requests" value={loading ? "--" : number(metrics.requests)} detail={`${number(metrics.succeeded)} succeeded`} icon={Activity} tone="bg-sky-50 text-sky-600" />
            <SummaryMetric label="Tokens" value={loading ? "--" : number(data?.totalTokens)} detail={`${number(metrics.input)} in · ${number(metrics.output)} out`} icon={Gauge} tone="bg-violet-50 text-violet-600" />
            <SummaryMetric label="Estimated cost" value={loading ? "--" : money(data?.totalCost)} detail="Across selected range" icon={DollarSign} tone="bg-amber-50 text-amber-600" />
            <SummaryMetric label="Success rate" value={loading ? "--" : `${metrics.successRate}%`} detail={`${number(metrics.failed)} failed`} icon={CheckCircle2} tone="bg-emerald-50 text-emerald-600" />
            <SummaryMetric label="Avg latency" value={loading ? "--" : metrics.latency ? `${number(metrics.latency)} ms` : "--"} detail="Observed completions" icon={Clock3} tone="bg-slate-100 text-slate-600" />
          </CardContent>
          <CardContent className="grid gap-0 p-0 xl:grid-cols-[1.25fr_0.75fr]">
              <section className="min-w-0 px-3 pb-1.5 pt-2 xl:border-r xl:border-border/60"><div className="mb-1 flex items-center justify-between gap-3"><h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Usage by capability</h2><span className="text-xs text-muted-foreground">{number(data?.totalTokens)} tokens</span></div><div className="divide-y divide-border/60">{breakdown.map((item) => <CapabilityRow key={item.key} item={item} maxTokens={maxTokens} />)}{!breakdown.length ? <p className="py-5 text-center text-sm text-muted-foreground">No breakdown yet</p> : null}</div></section>
              <section className="min-w-0 bg-muted/25 px-3 pb-1.5 pt-2"><div className="mb-1 flex items-center justify-between gap-3"><h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Provider health</h2><span className="text-xs text-muted-foreground">{providerHealth.length} providers</span></div><div className="divide-y divide-border/60">{providerHealth.map((item) => <ProviderHealthRow key={`${item.provider}:${item.model}`} {...item} />)}{!providerHealth.length ? <p className="py-5 text-center text-sm text-muted-foreground">No provider activity yet</p> : null}</div></section>
          </CardContent>
        </Card>

        <Card className="flex min-h-0 flex-1 flex-col overflow-hidden border-0 bg-muted/60 py-0 shadow-none">
          <CardHeader className="flex items-center justify-between gap-3 border-b border-border/50 px-4 py-3 !pb-3"><div className="min-w-0"><CardTitle className="text-sm">Recent calls</CardTitle><p className="mt-1 truncate text-xs text-muted-foreground">Operational history for the selected range</p></div><Badge variant="secondary" className="shrink-0 font-normal">{visibleObservations.length} calls</Badge></CardHeader>
          <CardContent className="flex min-h-0 flex-1 flex-col p-0"><div className="min-h-0 flex-1 overflow-auto"><Table className="table-fixed"><colgroup><col className="w-[22%]" /><col className="w-[18%]" /><col className="w-[26%]" /><col className="w-20" /><col className="w-24" /><col className="w-24" /><col className="w-32" /></colgroup><TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted"><TableRow className="hover:bg-transparent"><TableHead className="px-4">Capability</TableHead><TableHead>Provider / model</TableHead><TableHead>Status / failure reason</TableHead><TableHead className="text-right">Tokens</TableHead><TableHead className="text-right">Cost</TableHead><TableHead className="text-right">Latency</TableHead><TableHead className="pr-4 text-right">Time</TableHead></TableRow></TableHeader><TableBody>{recentPageItems.map((item) => <TableRow key={item.id}><TableCell className="max-w-0 whitespace-normal px-4 align-top"><div className="truncate font-medium leading-5">{capabilityLabel(item.name)}</div></TableCell><TableCell className="align-top"><div className="truncate font-medium">{providerLabel(item.providerCode)}</div><div className="truncate font-mono text-[11px] text-muted-foreground">{modelLabel(item.modelCode)}</div></TableCell><TableCell className="max-w-0 align-top"><Badge variant="secondary" title={item.statusMessage || undefined} className={cn("border-0 font-normal", statusStyle(item.status))}>{statusLabel(item.status)}</Badge>{item.status?.toUpperCase() === "FAILED" && item.statusMessage ? <p className="mt-1 break-words text-xs leading-4 text-rose-700" title={item.statusMessage}>{item.statusMessage}</p> : null}</TableCell><TableCell className="text-right align-top font-mono text-xs tabular-nums">{number(item.totalTokens)}</TableCell><TableCell className="text-right align-top font-mono text-xs tabular-nums text-muted-foreground">{money(item.totalCost)}</TableCell><TableCell className="text-right align-top font-mono text-xs tabular-nums text-muted-foreground">{item.latencyMs ? `${number(item.latencyMs)}ms` : "-"}</TableCell><TableCell className="pr-4 text-right align-top text-xs tabular-nums text-muted-foreground">{time(item.startedAt)}</TableCell></TableRow>)}{!visibleObservations.length ? <TableRow><TableCell colSpan={7} className="h-24 text-center text-sm text-muted-foreground">No observations in this range.</TableCell></TableRow> : null}</TableBody></Table></div>
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border/50 px-4 py-2.5"><div className="flex items-center gap-2 text-xs text-muted-foreground"><span className="tabular-nums">Showing {recentRangeStart}-{recentRangeEnd} of {visibleObservations.length}</span><span className="hidden text-border sm:inline">·</span><span className="hidden sm:inline">Rows</span><Select value={String(recentPageSize)} onValueChange={(value) => { setRecentPageSize(Number(value)); setRecentPage(1) }}><SelectTrigger size="sm" className="h-7 w-[108px] border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="10">10 / page</SelectItem><SelectItem value="20">20 / page</SelectItem><SelectItem value="50">50 / page</SelectItem><SelectItem value="100">100 / page</SelectItem></SelectContent></Select></div>{recentPageCount > 1 ? <Pagination className="mx-0 w-auto justify-end"><PaginationContent><PaginationItem><PaginationPrevious disabled={recentPage === 1} onClick={() => setRecentPage((page) => Math.max(1, page - 1))} /></PaginationItem>{Array.from({ length: recentPageCount }, (_, index) => index + 1).map((page) => <PaginationItem key={page}><PaginationLink size="icon-sm" isActive={recentPage === page} onClick={() => setRecentPage(page)}>{page}</PaginationLink></PaginationItem>)}<PaginationItem><PaginationNext disabled={recentPage === recentPageCount} onClick={() => setRecentPage((page) => Math.min(recentPageCount, page + 1))} /></PaginationItem></PaginationContent></Pagination> : null}</div>
          </CardContent>
        </Card>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted-foreground"><span className="inline-flex items-center gap-1"><Clock3 className="h-3.5 w-3.5" />{from} to {to}</span><span className="inline-flex items-center gap-1"><ArrowDownToLine className="h-3.5 w-3.5" />Input {number(metrics.input)}</span><span className="inline-flex items-center gap-1"><ArrowUpFromLine className="h-3.5 w-3.5" />Output {number(metrics.output)}</span><span className="inline-flex items-center gap-1"><XCircle className="h-3.5 w-3.5" />Failed {number(metrics.failed)}</span><span>Source: {data?.source || "Langfuse"}</span></div>
      </div>
    </WorkspaceShell>
  )
}
