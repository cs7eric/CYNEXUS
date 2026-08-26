import { useCallback, useEffect, useMemo, useState } from "react"
import {
  Activity,
  AlertTriangle,
  ArrowDownToLine,
  ArrowUpFromLine,
  Building2,
  CheckCircle2,
  Clock3,
  DollarSign,
  Gauge,
  Globe2,
  Loader2,
  Plus,
  RefreshCw,
  Save,
  Trash2,
  UserRound,
  XCircle,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { notification } from "@/components/ui/notification"
import { Pagination, PaginationContent, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cn } from "@/lib/utils"
import { createAgentQuotaPolicy, deleteAgentQuotaPolicy, fetchAgentQuotaPolicies, fetchAiUsage, updateAgentQuotaPolicy, type AgentQuotaPolicy, type AiUsageBreakdown, type AiUsageObservation, type AiUsagePage } from "@/services/ai/usage"

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

type QuotaPolicyScope = "global" | "tenant" | "principal"

type QuotaPolicyForm = {
  id?: string
  scope: QuotaPolicyScope
  tenantKey: string
  principalKey: string
  dailyRequestLimit: string
  dailyTokenLimit: string
  enabled: boolean
}

const quotaScopeCopy: Record<QuotaPolicyScope, { label: string; description: string }> = {
  global: { label: "全局默认", description: "未命中更具体策略时，对所有调用生效" },
  tenant: { label: "租户策略", description: "仅对指定租户内的调用生效" },
  principal: { label: "用户策略", description: "仅对指定租户内的指定用户生效" },
}

function quotaScopeOf(policy: Pick<AgentQuotaPolicy, "tenantKey" | "principalKey">): QuotaPolicyScope {
  if (policy.principalKey) return "principal"
  if (policy.tenantKey) return "tenant"
  return "global"
}

function quotaFormFromPolicy(policy?: AgentQuotaPolicy): QuotaPolicyForm {
  return {
    id: policy?.id,
    scope: policy ? quotaScopeOf(policy) : "tenant",
    tenantKey: policy?.tenantKey || "",
    principalKey: policy?.principalKey || "",
    dailyRequestLimit: String(policy?.dailyRequestLimit ?? 200),
    dailyTokenLimit: String(policy?.dailyTokenLimit ?? 100000),
    enabled: policy?.enabled ?? true,
  }
}

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
  const [quotaPolicies, setQuotaPolicies] = useState<AgentQuotaPolicy[]>([])
  const [quotaLoading, setQuotaLoading] = useState(true)
  const [quotaSaving, setQuotaSaving] = useState(false)
  const [quotaError, setQuotaError] = useState<string | null>(null)
  const [quotaDialogOpen, setQuotaDialogOpen] = useState(false)
  const [quotaForm, setQuotaForm] = useState<QuotaPolicyForm>(() => quotaFormFromPolicy())
  const [quotaDeletingId, setQuotaDeletingId] = useState("")

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

  const refreshQuotaPolicies = useCallback(async () => {
    setQuotaLoading(true); setQuotaError(null)
    try { setQuotaPolicies(await fetchAgentQuotaPolicies()) }
    catch (cause) { setQuotaError(cause instanceof Error ? cause.message : "Unable to load quota policies") }
    finally { setQuotaLoading(false) }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => void refreshQuotaPolicies(), 0)
    return () => window.clearTimeout(timer)
  }, [refreshQuotaPolicies])

  const openQuotaCreate = useCallback(() => {
    setQuotaError(null)
    setQuotaForm(quotaFormFromPolicy())
    setQuotaDialogOpen(true)
  }, [])

  const openQuotaEdit = useCallback((policy: AgentQuotaPolicy) => {
    setQuotaError(null)
    setQuotaForm(quotaFormFromPolicy(policy))
    setQuotaDialogOpen(true)
  }, [])

  const saveQuota = useCallback(async () => {
    const tenantKey = quotaForm.scope === "global" ? null : quotaForm.tenantKey.trim() || null
    const principalKey = quotaForm.scope === "principal" ? quotaForm.principalKey.trim() || null : null
    const dailyRequestLimit = Number(quotaForm.dailyRequestLimit)
    const dailyTokenLimit = Number(quotaForm.dailyTokenLimit)
    const scopeLabel = quotaScopeCopy[quotaForm.scope].label

    if ((quotaForm.scope === "tenant" || quotaForm.scope === "principal") && !tenantKey) {
      notification.danger({ title: "策略未保存", description: "请填写租户标识" })
      return
    }
    if (quotaForm.scope === "principal" && !principalKey) {
      notification.danger({ title: "策略未保存", description: "请填写用户标识" })
      return
    }
    if (!Number.isInteger(dailyRequestLimit) || dailyRequestLimit < 1) {
      notification.danger({ title: "策略未保存", description: "每日请求上限必须是大于 0 的整数" })
      return
    }
    if (!Number.isInteger(dailyTokenLimit) || dailyTokenLimit < 1024) {
      notification.danger({ title: "策略未保存", description: "每日 Token 上限至少为 1024" })
      return
    }

    setQuotaSaving(true); setQuotaError(null)
    try {
      const duplicate = quotaPolicies.some((policy) => !quotaForm.id
        && (policy.tenantKey || null) === tenantKey
        && (policy.principalKey || null) === principalKey)
      if (duplicate) throw new Error(`该${scopeLabel}已存在，请直接编辑现有策略`)

      const payload = { tenantKey, principalKey, dailyRequestLimit, dailyTokenLimit, enabled: quotaForm.enabled }
      if (quotaForm.id) await updateAgentQuotaPolicy({ id: quotaForm.id, ...payload })
      else await createAgentQuotaPolicy(payload)
      await refreshQuotaPolicies()
      notification.success({ title: quotaForm.id ? "配额策略已保存" : "配额策略已创建", description: scopeLabel })
      setQuotaDialogOpen(false)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "配额策略保存失败"
      setQuotaError(message)
      notification.danger({ title: "配额策略保存失败", description: message })
    } finally { setQuotaSaving(false) }
  }, [quotaForm, quotaPolicies, refreshQuotaPolicies])

  const deleteQuota = useCallback(async (policy: AgentQuotaPolicy) => {
    if (quotaScopeOf(policy) === "global") return
    setQuotaDeletingId(policy.id); setQuotaError(null)
    try {
      await deleteAgentQuotaPolicy(policy.id)
      await refreshQuotaPolicies()
      notification.success({ title: "配额策略已删除", description: quotaScopeCopy[quotaScopeOf(policy)].label })
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "配额策略删除失败"
      setQuotaError(message)
      notification.danger({ title: "配额策略删除失败", description: message })
    } finally { setQuotaDeletingId("") }
  }, [refreshQuotaPolicies])

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
  const sortedQuotaPolicies = useMemo(() => [...quotaPolicies].sort((left, right) => {
    const scopeRank = { principal: 0, tenant: 1, global: 2 }
    return scopeRank[quotaScopeOf(left)] - scopeRank[quotaScopeOf(right)]
      || (left.tenantKey || "").localeCompare(right.tenantKey || "")
      || (left.principalKey || "").localeCompare(right.principalKey || "")
  }), [quotaPolicies])
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

        <Card className="shadow-none">
          <CardHeader className="flex flex-col gap-2 border-b border-border/60 px-3 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0">
              <CardTitle className="text-sm">Agent 配额策略</CardTitle>
              <p className="mt-0.5 text-xs text-muted-foreground">每日 UTC 00:00 重置，按用户、租户、全局顺序匹配。</p>
            </div>
            <Button size="xs" onClick={openQuotaCreate} disabled={quotaSaving}>
              <Plus className="size-4" />
              新增策略
            </Button>
          </CardHeader>
          <CardContent className="space-y-3 px-3 py-3">
            {quotaError ? <Alert variant="destructive"><AlertTriangle className="size-4" /><AlertTitle>配额策略操作失败</AlertTitle><AlertDescription>{quotaError}</AlertDescription></Alert> : null}

            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 rounded-md bg-muted/45 px-3 py-1.5 text-xs text-muted-foreground" aria-label="配额匹配顺序">
              <span className="font-medium text-foreground">匹配顺序</span>
              <span className="inline-flex items-center gap-1" title="租户内指定用户，优先级最高"><UserRound className="size-3.5 text-foreground" />用户策略</span>
              <span aria-hidden="true">›</span>
              <span className="inline-flex items-center gap-1" title="覆盖该租户内未单独配置的用户"><Building2 className="size-3.5 text-foreground" />租户策略</span>
              <span aria-hidden="true">›</span>
              <span className="inline-flex items-center gap-1" title="作为最终兜底，请始终保留一条"><Globe2 className="size-3.5 text-foreground" />全局默认</span>
            </div>

            <section className="overflow-hidden rounded-lg border border-border/60" aria-label="Agent 配额策略列表">
              <div className="hidden grid-cols-[minmax(220px,1.4fr)_minmax(120px,0.7fr)_minmax(140px,0.8fr)_auto] gap-3 border-b border-border/60 bg-muted/35 px-3 py-1.5 text-xs font-medium text-muted-foreground md:grid">
                <span>适用范围</span><span className="text-right">每日请求上限</span><span className="text-right">每日 Token 上限</span><span className="text-right">操作</span>
              </div>
              {quotaLoading ? (
                <div className="space-y-2 p-3"><Skeleton className="h-10 w-full" /><Skeleton className="h-10 w-full" /></div>
              ) : null}
              {!quotaLoading && sortedQuotaPolicies.map((policy) => {
                const scope = quotaScopeOf(policy)
                const scopeCopy = quotaScopeCopy[scope]
                const ScopeIcon = scope === "principal" ? UserRound : scope === "tenant" ? Building2 : Globe2
                const deleting = quotaDeletingId === policy.id
                return (
                  <div key={policy.id} className="grid gap-2 border-b border-border/60 px-3 py-2 last:border-b-0 md:grid-cols-[minmax(220px,1.4fr)_minmax(120px,0.7fr)_minmax(140px,0.8fr)_auto] md:items-center md:gap-3">
                    <div className="flex min-w-0 items-start gap-2">
                      <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-md bg-muted text-muted-foreground"><ScopeIcon className="size-3" /></span>
                      <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="text-sm font-medium">{scopeCopy.label}</p><Badge variant={policy.enabled ? "outline" : "secondary"} className="h-5 px-1.5 text-[10px] font-medium">{policy.enabled ? "已启用" : "已停用"}</Badge></div><p className="mt-0.5 truncate text-xs text-muted-foreground" title={scope === "global" ? scopeCopy.description : scope === "tenant" ? `租户：${policy.tenantKey}` : `租户：${policy.tenantKey} / 用户：${policy.principalKey}`}>{scope === "global" ? scopeCopy.description : scope === "tenant" ? `租户：${policy.tenantKey}` : `租户：${policy.tenantKey} / 用户：${policy.principalKey}`}</p></div>
                    </div>
                    <div className="flex items-baseline justify-between gap-3 md:block md:text-right"><span className="text-xs text-muted-foreground md:hidden">每日请求上限</span><span className="font-mono text-sm tabular-nums">{number(policy.dailyRequestLimit)}</span></div>
                    <div className="flex items-baseline justify-between gap-3 md:block md:text-right"><span className="text-xs text-muted-foreground md:hidden">每日 Token 上限</span><span className="font-mono text-sm tabular-nums">{number(policy.dailyTokenLimit)}</span></div>
                    <div className="flex items-center justify-end gap-1"><Button size="sm" variant="outline" onClick={() => openQuotaEdit(policy)} disabled={quotaSaving || deleting}>编辑</Button>{scope === "global" ? <Button size="icon-sm" variant="ghost" disabled title="全局默认用于兜底，不能在此删除" aria-label="不能删除全局默认"><Trash2 className="size-4" /></Button> : <Button size="icon-sm" variant="ghost" disabled={quotaSaving || deleting} title="删除策略" aria-label={`删除${scopeCopy.label}`} onClick={() => void deleteQuota(policy)}>{deleting ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}</Button>}</div>
                  </div>
                )
              })}
              {!quotaLoading && !sortedQuotaPolicies.length ? <div className="p-5 text-center text-sm text-muted-foreground">暂无配额策略。先创建全局默认，确保所有 Agent 请求都有可用上限。</div> : null}
            </section>
          </CardContent>
        </Card>

        <Dialog open={quotaDialogOpen} onOpenChange={(open) => !quotaSaving && setQuotaDialogOpen(open)}>
          <DialogContent className="sm:max-w-xl">
            <DialogHeader>
              <DialogTitle>{quotaForm.id ? "编辑配额策略" : "新增配额策略"}</DialogTitle>
              <DialogDescription>配置与调用身份对应的每日额度。更具体的范围会覆盖更宽泛的范围。</DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-1">
              <div className="space-y-2"><Label htmlFor="quota-policy-scope">适用范围</Label><Select value={quotaForm.scope} disabled={Boolean(quotaForm.id)} onValueChange={(value) => setQuotaForm((current) => ({ ...current, scope: value as QuotaPolicyScope, tenantKey: value === "global" ? "" : current.tenantKey, principalKey: value === "principal" ? current.principalKey : "" }))}><SelectTrigger id="quota-policy-scope"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="global">全局默认</SelectItem><SelectItem value="tenant">租户策略</SelectItem><SelectItem value="principal">用户策略</SelectItem></SelectContent></Select><p className="text-xs leading-5 text-muted-foreground">{quotaScopeCopy[quotaForm.scope].description}{quotaForm.id ? " 编辑时范围不可变更；如需调整范围，请新建策略。" : ""}</p></div>
              {quotaForm.scope !== "global" ? <div className="space-y-2"><Label htmlFor="quota-policy-tenant">租户标识</Label><Input id="quota-policy-tenant" value={quotaForm.tenantKey} maxLength={128} onChange={(event) => setQuotaForm((current) => ({ ...current, tenantKey: event.target.value }))} placeholder="例如：default" autoComplete="off" /><p className="text-xs text-muted-foreground">必须与 Agent 调用时传入的 tenantKey 完全一致。</p></div> : null}
              {quotaForm.scope === "principal" ? <div className="space-y-2"><Label htmlFor="quota-policy-principal">用户标识</Label><Input id="quota-policy-principal" value={quotaForm.principalKey} maxLength={128} onChange={(event) => setQuotaForm((current) => ({ ...current, principalKey: event.target.value }))} placeholder="例如：user-10001" autoComplete="off" /><p className="text-xs text-muted-foreground">必须与 Agent 调用时传入的 principalKey 完全一致。</p></div> : null}
              <div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="quota-policy-requests">每日请求上限</Label><Input id="quota-policy-requests" type="number" min={1} step={1} value={quotaForm.dailyRequestLimit} onChange={(event) => setQuotaForm((current) => ({ ...current, dailyRequestLimit: event.target.value }))} /><p className="text-xs text-muted-foreground">一个 UTC 自然日内可发起的 Agent 请求数。</p></div><div className="space-y-2"><Label htmlFor="quota-policy-tokens">每日 Token 上限</Label><Input id="quota-policy-tokens" type="number" min={1024} step={1} value={quotaForm.dailyTokenLimit} onChange={(event) => setQuotaForm((current) => ({ ...current, dailyTokenLimit: event.target.value }))} /><p className="text-xs text-muted-foreground">一个 UTC 自然日内累计使用的 Token 数。</p></div></div>
              <div className="flex items-start gap-2 rounded-md bg-muted/50 p-3"><Checkbox id="quota-policy-enabled" checked={quotaForm.enabled} onCheckedChange={(checked) => setQuotaForm((current) => ({ ...current, enabled: checked === true }))} /><div className="grid gap-1 leading-none"><Label htmlFor="quota-policy-enabled">启用此策略</Label><p className="text-xs leading-5 text-muted-foreground">停用后该范围不会命中此策略，将继续匹配更宽泛的有效策略。</p></div></div>
            </div>
            <DialogFooter><Button variant="outline" onClick={() => setQuotaDialogOpen(false)} disabled={quotaSaving}>取消</Button><Button onClick={() => void saveQuota()} disabled={quotaSaving}>{quotaSaving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}{quotaSaving ? "正在保存" : "保存策略"}</Button></DialogFooter>
          </DialogContent>
        </Dialog>

        <Card className="gap-0 overflow-hidden py-0 shadow-none">
          <CardContent className="grid gap-0 border-b border-border/60 px-4 py-0 sm:grid-cols-2 xl:grid-cols-5">
            <SummaryMetric label="Requests" value={loading ? "--" : number(metrics.requests)} detail={`${number(metrics.succeeded)} succeeded`} icon={Activity} tone="bg-sky-50 text-sky-600" />
            <SummaryMetric label="Tokens" value={loading ? "--" : number(data?.totalTokens)} detail={`${number(metrics.input)} in · ${number(metrics.output)} out`} icon={Gauge} tone="bg-violet-50 text-violet-600" />
            <SummaryMetric label="Estimated cost" value={loading ? "--" : money(data?.totalCost)} detail="Across selected range" icon={DollarSign} tone="bg-amber-50 text-amber-600" />
            <SummaryMetric label="Success rate" value={loading ? "--" : `${metrics.successRate}%`} detail={`${number(metrics.failed)} failed`} icon={CheckCircle2} tone="bg-emerald-50 text-emerald-600" />
            <SummaryMetric label="Avg latency" value={loading ? "--" : metrics.latency ? `${number(metrics.latency)} ms` : "--"} detail="Observed completions" icon={Clock3} tone="bg-slate-100 text-slate-600" />
          </CardContent>
          <CardContent className="grid h-[min(22rem,40vh)] min-h-0 grid-rows-[minmax(0,1fr)_minmax(0,1fr)] gap-0 overflow-hidden p-0 xl:grid-cols-[1.25fr_0.75fr] xl:grid-rows-1">
              <section className="min-h-0 min-w-0 overflow-y-auto overscroll-contain px-3 pb-1.5 pt-2 xl:border-r xl:border-border/60"><div className="mb-1 flex items-center justify-between gap-3"><h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Usage by capability</h2><span className="text-xs text-muted-foreground">{number(data?.totalTokens)} tokens</span></div><div className="divide-y divide-border/60">{breakdown.map((item) => <CapabilityRow key={item.key} item={item} maxTokens={maxTokens} />)}{!breakdown.length ? <p className="py-5 text-center text-sm text-muted-foreground">No breakdown yet</p> : null}</div></section>
              <section className="min-h-0 min-w-0 overflow-y-auto overscroll-contain bg-muted/25 px-3 pb-1.5 pt-2"><div className="mb-1 flex items-center justify-between gap-3"><h2 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Provider health</h2><span className="text-xs text-muted-foreground">{providerHealth.length} providers</span></div><div className="divide-y divide-border/60">{providerHealth.map((item) => <ProviderHealthRow key={`${item.provider}:${item.model}`} {...item} />)}{!providerHealth.length ? <p className="py-5 text-center text-sm text-muted-foreground">No provider activity yet</p> : null}</div></section>
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
