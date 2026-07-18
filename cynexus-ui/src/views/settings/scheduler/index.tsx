/**
 * Design entry:
 * - Data/API: scheduler jobs/categories/daily-stats/history + enable/disable/start/stop/trigger/delete actions
 * - Front design: design/front/settings-scheduler.md
 * - Backend design: design/backend/scheduler-registry-runtime.md
 * - Change rule: review design before edits; sync design if registry/live status/action contract changes.
 */
import { useCallback, useEffect, useMemo, useState } from "react"
import { cn } from "@/lib/utils"
import {
  Activity,
  AlertTriangle,
  BarChart3,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  CirclePause,
  Clock,
  Copy,
  Database,
  FileCode2,
  Hand,
  History as HistoryIcon,
  LayoutGrid,
  ListChecks,
  Pencil,
  Play,
  Plus,
  Power,
  PowerOff,
  RefreshCcw,
  RefreshCw,
  Settings2,
  Sparkles,
  Tags,
  TimerReset,
  Trash2,
  TrendingUp,
  Zap,
} from "lucide-react"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { DatePicker } from "@/components/ui/date-picker"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs"
import { notification } from "@/components/ui/notification"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import DogLoader from "@/components/loader/dog-loader"
import { toLocalIso, todayLocal } from "@/lib/date-utils"
import { SchedulerCategoryExecutionRadar } from "./category-execution-radar"
import { SchedulerDailyStatsAreaChart } from "./daily-stats-area-chart"
import {
  type SchedulerCategory,
  type SchedulerCurrentStepProgress,
  type SchedulerDailyStatItem,
  type SchedulerFundFlowQcSummary,
  type SchedulerJobHistoryItem,
  type SchedulerJobItem,
  createSchedulerCategory,
  deleteSchedulerCategory,
  deleteSchedulerJob,
  disableSchedulerJob,
  enableSchedulerJob,
  fetchSchedulerCategories,
  fetchSchedulerDailyStats,
  fetchSchedulerJobHistory,
  fetchSchedulerJobs,
  startSchedulerJob,
  stopSchedulerJob,
  triggerSchedulerJob,
  updateSchedulerCategory,
  updateSchedulerJobCategories,
} from "@/services/scheduler/management"

type ActionKey = "enable" | "disable" | "start" | "stop" | "trigger" | "delete" | "categories"
type ActionState = Record<string, ActionKey | null>

const REFRESH_INTERVAL_MS = 5_000
const ALL_TAB = "all"  // 兜底 tab (不过滤)
const MARKET_SENTIMENT_CATEGORY_LABEL = "市场情绪"
const MARKET_SENTIMENT_CATEGORY_FALLBACK_ID = "5"

type SchedulerCategoryInput = {
  label: string
  code?: string
  icon_hint?: string
  description?: string
  sort_order?: number
}

type ScheduleWindowSummary = {
  label: string
  count: number
  ratio: number
}

type SchedulerTodayStatSnapshot = NonNullable<SchedulerJobItem["today_stats"]>
type TodayRunStatKey = "success" | "failed" | "skipped" | "enabled" | "disabled" | "manual" | "auto"

type TodayRunStatConfig = {
  key: TodayRunStatKey
  label: string
  value: number
  description: string
  className: string
  jobs: SchedulerJobItem[]
}

// ---------------------------------------------------------------------------
// Icon mapping: 后端 icon_hint (lucide 图标名) → 实际组件.
// 这是前端唯一与后端耦合的点: 后端返 "activity" / "sparkles" 等字符串,
// 前端用这张表查组件. 加新 category 时, 后端给个新 icon_hint 即可, 前端
// 在这里加一行 fallback. 没找到就回退到 LayoutGrid.
// ---------------------------------------------------------------------------
const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  activity: Activity,
  sparkles: Sparkles,
  database: Database,
  refresh: RefreshCcw,
  trending: TrendingUp,
  flask: Settings2,        // lucide 没 Flask, 用 Settings2 凑
  grid: LayoutGrid,
}

function iconFor(hint: string | undefined): React.ComponentType<{ className?: string }> {
  if (!hint) return LayoutGrid
  return ICON_MAP[hint] || LayoutGrid
}

function sortCategories(items: SchedulerCategory[]): SchedulerCategory[] {
  return [...items].sort((a, b) => (a.sort_order ?? 999) - (b.sort_order ?? 999))
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString("zh-CN", { hour12: false, timeZone: "Asia/Shanghai" })
}

function formatLastRunParts(value: string | null | undefined): { date: string; time: string } {
  if (!value) return { date: "—", time: "—" }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return { date: value, time: "" }
  return {
    date: date.toLocaleDateString("zh-CN", { timeZone: "Asia/Shanghai" }),
    time: date.toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
      timeZone: "Asia/Shanghai",
    }),
  }
}

function formatSeconds(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—"
  if (value < 60) return `${value.toFixed(1)}s`
  const minutes = Math.floor(value / 60)
  const seconds = Math.round(value % 60)
  return `${minutes}m ${seconds}s`
}

function formatInteger(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—"
  return new Intl.NumberFormat("zh-CN").format(value)
}

function formatPercentValue(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—"
  return `${(value * 100).toFixed(1)}%`
}

function formatWan(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—"
  return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 1 }).format(value / 10000)} 万`
}

function todayStatNumber(job: SchedulerJobItem, key: keyof SchedulerTodayStatSnapshot): number {
  const value = job.today_stats?.[key]
  return typeof value === "number" && Number.isFinite(value) ? value : 0
}

function todaySuccessCount(job: SchedulerJobItem): number {
  const explicit = job.today_stats?.success_count
  if (typeof explicit === "number" && Number.isFinite(explicit)) return explicit
  return Math.max(
    0,
    todayStatNumber(job, "actual_count")
      - todayStatNumber(job, "failed_count")
      - todayStatNumber(job, "skipped_count"),
  )
}

function parseScheduleConfig(job: SchedulerJobItem): Record<string, unknown> {
  const rawSchedule = pickValue<unknown>(job.config, "schedule")
  if (typeof rawSchedule === "string" && rawSchedule.trim()) {
    try {
      const parsed = JSON.parse(rawSchedule) as unknown
      return parsed && typeof parsed === "object" && !Array.isArray(parsed)
        ? parsed as Record<string, unknown>
        : {}
    } catch {
      return {}
    }
  }
  if (rawSchedule && typeof rawSchedule === "object" && !Array.isArray(rawSchedule)) {
    return rawSchedule as Record<string, unknown>
  }
  return {}
}

function buildScheduleWindows(jobs: SchedulerJobItem[]): ScheduleWindowSummary[] {
  const bucket = new Map<string, number>()
  let fallbackCount = 0
  for (const job of jobs) {
    if (!job.config_enabled) continue
    const expectedCount = job.today_stats?.expected_count ?? 0
    if (expectedCount <= 0) continue
    const schedule = parseScheduleConfig(job)
    const everyMinutes = Number(schedule.every_minutes)
    const windows = schedule.intraday_windows
    let matched = false
    if (Number.isFinite(everyMinutes) && everyMinutes > 0 && Array.isArray(windows)) {
      for (const item of windows) {
        if (!item || typeof item !== "object") continue
        const record = item as Record<string, unknown>
        const start = typeof record.start === "string" ? record.start : ""
        const end = typeof record.end === "string" ? record.end : ""
        if (!start || !end) continue
        const count = estimateWindowCount(start, end, everyMinutes)
        if (count <= 0) continue
        const label = `${start.slice(0, 5)}-${end.slice(0, 5)}`
        bucket.set(label, (bucket.get(label) || 0) + count)
        matched = true
      }
    }
    const runTime = typeof schedule.run_time === "string" ? schedule.run_time : ""
    if (!matched && runTime) {
      const label = runTime.slice(0, 5)
      bucket.set(label, (bucket.get(label) || 0) + expectedCount)
      matched = true
    }
    if (!matched) {
      fallbackCount += expectedCount
    }
  }
  if (fallbackCount > 0) {
    bucket.set("cron/其他", (bucket.get("cron/其他") || 0) + fallbackCount)
  }
  const max = Math.max(1, ...bucket.values())
  return Array.from(bucket.entries())
    .map(([label, count]) => ({ label, count, ratio: count / max }))
    .sort((a, b) => a.label.localeCompare(b.label))
}

function estimateWindowCount(start: string, end: string, everyMinutes: number): number {
  const startMinutes = parseTimeMinutes(start)
  const endMinutes = parseTimeMinutes(end)
  if (startMinutes == null || endMinutes == null || endMinutes <= startMinutes) return 0
  return Math.floor((endMinutes - startMinutes) / everyMinutes)
}

function parseTimeMinutes(value: string): number | null {
  const [hourRaw, minuteRaw = "0"] = value.split(":")
  const hour = Number(hourRaw)
  const minute = Number(minuteRaw)
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return null
  return hour * 60 + minute
}

function statusBadgeVariant(status: string | null | undefined): "default" | "secondary" | "destructive" | "outline" {
  switch ((status || "").toLowerCase()) {
    case "success":
      return "outline"
    case "failed":
    case "error":
    case "partial_failed":
      return "destructive"
    case "running":
    case "in_progress":
    case "processing":
      return "secondary"
    case "skipped":
      return "secondary"
    default:
      return "outline"
  }
}

function statusBadgeClass(status: string | null | undefined): string {
  switch ((status || "").toLowerCase()) {
    case "success":
      return "bg-emerald-500/10 text-emerald-600 border-emerald-500/30"
    default:
      return ""
  }
}

function pickValue<T = unknown>(record: Record<string, unknown> | undefined, key: string): T | undefined {
  if (!record) return undefined
  const value = record[key]
  return value as T | undefined
}

// Chain step display labels
const CHAIN_STEP_LABELS: Record<string, string> = {
  tdx_hsjday_download: "通达信行情下载",
  initial_backfill_refresh: "初始回填补全",
  qfq_reconciliation_refresh: "前复权对账",
  build_style_sector: "风格板块快照",
  build_index_returns: "宽基收益快照",
  limit_emotion_refresh: "涨跌停情绪",
  risk_appetite_refresh: "风险偏好",
  ma_count_refresh: "均线计数",
  volatility_sentiment_refresh: "波动率情绪",
  style_risk_appetite_refresh: "风格风险偏好",
  profit_effect_refresh: "盈利效应",
  market_overview_daily: "市场概览",
  turnover_activity_refresh: "换手活跃度",
  ths_industry_fund_flow_daily: "行业资金流",
  build_market_heatmap: "行业热力图快照",
  sector_breadth_refresh: "板块广度",
  market_sentiment_index_refresh: "市场情绪指数",
}

interface ChainStepResult {
  jobId: string
  ok: boolean
  lastStatus?: string
  lastRunError?: string
  lastMessage?: string
}

function stepProgressPercent(progress: SchedulerCurrentStepProgress | null | undefined): number | null {
  const processed = progress?.processedItems ?? progress?.processedFiles
  const total = progress?.totalItems ?? progress?.totalFiles
  if (typeof processed !== "number" || typeof total !== "number" || total <= 0) return null
  return Math.max(0, Math.min(100, Math.round((processed / total) * 100)))
}

function stepProgressUnit(progress: SchedulerCurrentStepProgress | null | undefined): string {
  return progress?.unitLabel || (progress?.processedFiles !== undefined || progress?.totalFiles !== undefined ? "文件" : "项")
}

function stepProgressProcessed(progress: SchedulerCurrentStepProgress | null | undefined): number | undefined {
  return progress?.processedItems ?? progress?.processedFiles
}

function stepProgressTotal(progress: SchedulerCurrentStepProgress | null | undefined): number | undefined {
  return progress?.totalItems ?? progress?.totalFiles
}

function totalCodeProgressPercent(progress: SchedulerCurrentStepProgress | null | undefined): number | null {
  const completed = progress?.completedCodes
  const total = progress?.totalCodes
  if (typeof completed !== "number" || typeof total !== "number" || total <= 0) return null
  return Math.max(0, Math.min(100, Math.round((completed / total) * 100)))
}

function totalCodePending(progress: SchedulerCurrentStepProgress | null | undefined): number | undefined {
  if (typeof progress?.pendingCodes === "number") return progress.pendingCodes
  if (typeof progress?.completedCodes !== "number" || typeof progress?.totalCodes !== "number") return undefined
  return Math.max(0, progress.totalCodes - progress.completedCodes)
}

function ChainStepProgress({
  currentStep,
  currentStepProgress,
  completedSteps,
  stepResults,
  overallStatus,
  updatedAt,
}: {
  currentStep: string | null | undefined
  currentStepProgress?: SchedulerCurrentStepProgress | null
  completedSteps: string[] | undefined
  stepResults: ChainStepResult[] | undefined
  overallStatus: string | null | undefined
  updatedAt?: string | null
}) {
  const allSteps = Object.keys(CHAIN_STEP_LABELS)
  const total = allSteps.length
  const completedSet = new Set(completedSteps || [])
  const resultMap = new Map((stepResults || []).map((r) => [r.jobId, r]))
  const isFinished = overallStatus === "success" || overallStatus === "failed" || overallStatus === "skipped"

  const doneCount = completedSet.size
  const hasFailed = (stepResults || []).some((r) => !r.ok)
  const failedStep = hasFailed ? (stepResults || []).find((r) => !r.ok) : null
  const currentLabel = currentStep ? (CHAIN_STEP_LABELS[currentStep] || currentStep) : null
  // Current step ordinal (1-based)
  const currentStepIdx = currentStep ? allSteps.indexOf(currentStep) : -1
  const currentOrdinal = currentStepIdx >= 0 ? currentStepIdx + 1 : 0
  // For progress: show actual completed count, never fabricate 14/14
  const displayCount = doneCount
  const subPercent = stepProgressPercent(currentStepProgress)
  const hasSubProgressValue = typeof stepProgressProcessed(currentStepProgress) === "number"
    || typeof stepProgressTotal(currentStepProgress) === "number"
    || typeof currentStepProgress?.rowsRead === "number"
    || typeof currentStepProgress?.rowsWritten === "number"
  const subProgressUnit = stepProgressUnit(currentStepProgress)
  const totalCodePercent = totalCodeProgressPercent(currentStepProgress)
  const pendingCodes = totalCodePending(currentStepProgress)
  const showSubProgress = Boolean(
    !isFinished
    && currentStepProgress
    && hasSubProgressValue
    && (!currentStepProgress.step || currentStepProgress.step === currentStep),
  )

  // Only show steps with meaningful status (skip pending)
  const activeSteps = allSteps.filter((code) => {
    const result = resultMap.get(code)
    const isCompleted = completedSet.has(code)
    const isCurrent = !isFinished && currentStep === code
    const isFailed = result && !result.ok
    return isCompleted || isCurrent || isFailed
  })

  return (
    <div className="space-y-1.5 text-xs">
      <div className="flex items-center gap-2 text-muted-foreground">
        <span className="text-[10px] font-medium uppercase tracking-wide">链路进度</span>
        {/* Progress bar */}
        <div className="flex flex-1 items-center gap-1">
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
            <div
              className={cn(
                "h-full rounded-full transition-all duration-300",
                hasFailed ? "bg-destructive" : isFinished ? "bg-emerald-500" : "bg-blue-500",
              )}
              style={{ width: `${Math.round((displayCount / total) * 100)}%` }}
            />
          </div>
          <span className="tabular-nums text-[10px]">{displayCount}/{total}</span>
        </div>
        {/* Current step label: show ordinal + name while running */}
        {!isFinished && currentOrdinal > 0 ? (
          <span className="flex items-center gap-1 text-blue-600">
            <RefreshCw className="size-3 animate-spin" />
            {currentOrdinal}/{total} {currentLabel}
          </span>
        ) : null}
      </div>

      {/* Only list steps that are done / failed / running — skip pending */}
      {activeSteps.length > 0 ? (
        <div className="flex flex-wrap gap-1">
          {activeSteps.map((stepCode) => {
            const label = CHAIN_STEP_LABELS[stepCode] || stepCode
            const result = resultMap.get(stepCode)
            const isCompleted = completedSet.has(stepCode)
            const isCurrent = !isFinished && currentStep === stepCode
            const isFailed = result && !result.ok

            if (isFailed) {
              return (
                <Badge
                  key={stepCode}
                  variant="destructive"
                  className="max-w-full gap-0.5 px-1.5 py-0 text-[10px]"
                  title={result.lastRunError || "failed"}
                >
                  <AlertTriangle className="size-2.5" />
                  {label}
                </Badge>
              )
            }
            if (isCurrent) {
              return (
                <Badge
                  key={stepCode}
                  variant="secondary"
                  className="border-blue-500/30 bg-blue-500/10 px-1.5 py-0 text-[10px] text-blue-600"
                >
                  <RefreshCw className="size-2.5 animate-spin" />
                  {label}
                </Badge>
              )
            }
            if (isCompleted) {
              return (
                <Badge
                  key={stepCode}
                  variant="outline"
                  className="gap-0.5 px-1.5 py-0 text-[10px] text-emerald-600"
                >
                  <CheckCircle2 className="size-2.5" />
                  {label}
                </Badge>
              )
            }
            return null
          })}
        </div>
      ) : null}

      {showSubProgress ? (
        <div className="rounded-md border border-border/40 bg-background/60 px-2 py-1.5">
          {totalCodePercent !== null ? (
            <div className="mb-1.5 space-y-1">
              <div className="flex flex-wrap items-center justify-between gap-2 text-[10px] text-muted-foreground">
                <span className="font-medium text-foreground/80">总进度</span>
                <span className="tabular-nums">
                  股票 {formatInteger(currentStepProgress?.completedCodes)}/{formatInteger(currentStepProgress?.totalCodes)}
                </span>
                <span className="tabular-nums">
                  {typeof currentStepProgress?.failedCodes === "number" ? `失败 ${formatInteger(currentStepProgress.failedCodes)} · ` : ""}
                  待跑 {formatInteger(pendingCodes)}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <div className="h-1 flex-1 overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-emerald-500 transition-all duration-300"
                    style={{ width: `${totalCodePercent}%` }}
                  />
                </div>
                <span className="w-9 text-right text-[10px] tabular-nums text-muted-foreground">{totalCodePercent}%</span>
              </div>
            </div>
          ) : null}
          <div className="flex flex-wrap items-center justify-between gap-2 text-[10px] text-muted-foreground">
            <span className="inline-flex items-center gap-1 text-foreground/80">
              <RefreshCw className="size-2.5 animate-spin text-blue-600" />
              当前步骤明细
            </span>
            <span className="tabular-nums">
              {subProgressUnit} {formatInteger(stepProgressProcessed(currentStepProgress))}/{formatInteger(stepProgressTotal(currentStepProgress))}
            </span>
            <span className="tabular-nums">
              读 {formatInteger(currentStepProgress?.rowsRead)} · 写 {formatInteger(currentStepProgress?.rowsWritten)}
            </span>
            {updatedAt ? <span>{formatDateTime(updatedAt)}</span> : null}
          </div>
          {subPercent !== null ? (
            <div className="mt-1 flex items-center gap-2">
              <div className="h-1 flex-1 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-blue-500 transition-all duration-300"
                  style={{ width: `${subPercent}%` }}
                />
              </div>
              <span className="w-9 text-right text-[10px] tabular-nums text-muted-foreground">{subPercent}%</span>
            </div>
          ) : null}
        </div>
      ) : null}

      {/* Failed step error detail */}
      {failedStep?.lastRunError ? (
        <div className="flex items-start gap-1 rounded border border-destructive/20 bg-destructive/5 px-2 py-1 text-[10px] leading-4 text-destructive">
          <AlertTriangle className="mt-0.5 size-2.5 shrink-0" />
          <span className="break-all">{failedStep.lastRunError}</span>
        </div>
      ) : null}
    </div>
  )
}

function FundFlowQcSummaryCard({ summary }: { summary?: SchedulerFundFlowQcSummary | null }) {
  if (!summary) return null
  const samples = summary.samples || []
  return (
    <div className="space-y-2 rounded-xl border border-border/50 bg-background/70 p-3 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="font-medium text-foreground">同日质检结果</div>
        <Badge variant="outline" className="px-1.5 py-0 text-[10px]">
          抽样 {formatInteger(summary.sampleSize)} · 成功 {formatInteger(summary.successCount)} · 失败 {formatInteger(summary.failedCount)}
        </Badge>
      </div>
      <div className="grid gap-2 sm:grid-cols-3">
        <div className="rounded-md bg-muted/50 px-2 py-1.5">
          <div className="text-[10px] text-muted-foreground">方向一致率</div>
          <div className="font-mono text-base font-semibold">{formatPercentValue(summary.directionMatchRate)}</div>
        </div>
        <div className="rounded-md bg-muted/50 px-2 py-1.5">
          <div className="text-[10px] text-muted-foreground">方向一致数量</div>
          <div className="font-mono text-base font-semibold">
            {formatInteger(summary.directionMatchCount)}/{formatInteger(summary.successCount)}
          </div>
        </div>
        <div className="rounded-md bg-muted/50 px-2 py-1.5">
          <div className="text-[10px] text-muted-foreground">平均绝对偏差率</div>
          <div className="font-mono text-base font-semibold">{formatPercentValue(summary.avgAbsDiffRatio)}</div>
        </div>
      </div>
      {samples.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-[11px]">
            <thead className="text-muted-foreground">
              <tr>
                <th className="py-1 pr-2 font-medium">代码</th>
                <th className="py-1 pr-2 font-medium">日期</th>
                <th className="py-1 pr-2 font-medium">easy_tdx</th>
                <th className="py-1 pr-2 font-medium">EastMoney</th>
                <th className="py-1 pr-2 font-medium">偏差率</th>
                <th className="py-1 pr-2 font-medium">方向</th>
              </tr>
            </thead>
            <tbody>
              {samples.map((item, index) => (
                <tr key={`${item.code || "row"}-${index}`} className="border-t border-border/40">
                  <td className="py-1 pr-2 font-mono">{item.code || "—"}</td>
                  <td className="py-1 pr-2 text-muted-foreground">{item.tradeDate || "—"}</td>
                  <td className="py-1 pr-2 font-mono">{formatWan(item.easyTdxMainNet)}</td>
                  <td className="py-1 pr-2 font-mono">{formatWan(item.eastmoneyMainNet)}</td>
                  <td className="py-1 pr-2 font-mono">{formatPercentValue(item.mainNetDiffRatio)}</td>
                  <td className="py-1 pr-2">
                    {item.error ? (
                      <span className="text-destructive" title={item.error}>失败</span>
                    ) : item.directionMatch ? (
                      <span className="text-emerald-600">一致</span>
                    ) : (
                      <span className="text-amber-600">相反</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}

interface JobCardProps {
  job: SchedulerJobItem
  categories: SchedulerCategory[]
  pending: ActionKey | null
  onAction: (jobId: string, action: ActionKey) => void
  onUpdateCategories: (jobId: string, categoryIds: string[]) => void
}

function jobStateLabel(isRunning: boolean, enabled: boolean): string {
  if (isRunning) return "运行中"
  return enabled ? "空闲" : "已停止"
}

function jobStateClass(isRunning: boolean, enabled: boolean): string {
  if (isRunning) return "border-emerald-500/30 bg-emerald-500/10 text-emerald-600"
  return enabled ? "border-blue-500/30 bg-blue-500/10 text-blue-600" : ""
}

function TodayRunMetric({
  icon,
  label,
  value,
  hint,
  tone,
  valueClassName,
  className,
}: {
  icon: React.ReactNode
  label: string
  value: React.ReactNode
  hint?: string
  tone?: "default" | "success" | "danger"
  valueClassName?: string
  className?: string
}) {
  return (
    <div
      className={cn(
        "min-w-0 rounded-md bg-background/55 px-2.5 py-2",
        tone === "success" && "bg-emerald-500/10",
        tone === "danger" && "bg-destructive/10",
        className,
      )}
    >
      <div className="flex min-w-0 items-center gap-1.5 text-[10px] font-medium text-muted-foreground">
        {icon}
        <span className="truncate">{label}</span>
        {hint ? <span className="ml-2 min-w-0 truncate font-normal text-muted-foreground/80">{hint}</span> : null}
      </div>
      <div
        className={cn(
          "mt-1 truncate font-mono text-base font-semibold tabular-nums text-foreground",
          tone === "success" && "text-emerald-600",
          tone === "danger" && "text-destructive",
          valueClassName,
        )}
      >
        {value}
      </div>
    </div>
  )
}

function JobCard({ job, categories, pending, onAction, onUpdateCategories }: JobCardProps) {
  const [dialogOpen, setDialogOpen] = useState(false)
  // 历史记录: dialog 打开后才拉, 5s 自动 refresh
  const [history, setHistory] = useState<SchedulerJobHistoryItem[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const live = job.live || {}
  const config = job.config || {}
  const isRunning = Boolean(pickValue<boolean>(live, "running"))
  const lastRunSummary = job.last_run
  const lastRunAt = lastRunSummary?.last_run_at ?? pickValue<string>(config, "last_run_at")
  const lastStatus = lastRunSummary?.last_status ?? pickValue<string>(config, "last_status")
  const lastError = lastRunSummary?.last_error ?? pickValue<string>(config, "last_error")
  const lastMessage = pickValue<string>(live, "lastMessage") ?? pickValue<string>(config, "lastMessage")
  const lastDuration =
    lastRunSummary?.last_duration_seconds ?? pickValue<number>(config, "last_duration_seconds")
  const lastTargets =
    lastRunSummary?.last_targets_processed ?? pickValue<number>(config, "last_targets_processed")
  const totalRuns = lastRunSummary?.total_runs ?? pickValue<number>(config, "total_runs")
  const registeredAt = job.registered_at
  const inflight = pickValue<Record<string, string>>(live, "inflight")
  const progress = job.progress || null

  // Chain step progress fields (market_sentiment_chain_refresh)
  const chainCurrentStep = progress?.currentStep ?? pickValue<string>(config, "lastStep")
  const chainCurrentStepProgress = progress?.currentStepProgress ?? null
  const chainCompletedSteps = progress?.completedSteps ?? pickValue<string[]>(config, "lastCompletedSteps")
  const chainStepResults = progress?.stepResults ?? pickValue<ChainStepResult[]>(config, "lastStepResults")
  const hasChainProgress = chainCurrentStep || (chainCompletedSteps && chainCompletedSteps.length > 0)
  const qcSummary = progress?.payload?.qcSummary

  const isActionPending = (action: ActionKey) => pending === action
  const expectedCount = job.today_stats?.expected_count ?? 0
  const actualCount = job.today_stats?.actual_count ?? 0
  const failedCount = job.today_stats?.failed_count ?? 0
  const executionTone = failedCount > 0 ? "danger" : actualCount > 0 ? "success" : "default"
  const executionHint = failedCount > 0
    ? `失败 ${formatInteger(failedCount)} 次`
    : actualCount > 0
      ? "今日执行正常"
      : "今日暂无执行"
  const lastRunParts = formatLastRunParts(lastRunAt)
  const lastRunValue = lastRunParts.time && lastRunParts.date !== "—"
    ? `${lastRunParts.date} ${lastRunParts.time}`
    : lastRunParts.date
  const categoryLabelById = useMemo(
    () => new Map(categories.map((category) => [category.id, category.label])),
    [categories],
  )

  // history: dialog 打开时才拉, 5s 轮询. pending 变化 (trigger) 也重拉.
  useEffect(() => {
    if (!dialogOpen) {
      const clearTimer = window.setTimeout(() => setHistory([]), 0)
      return () => window.clearTimeout(clearTimer)
    }
    let cancelled = false
    const load = () => {
      setHistoryLoading(true)
      fetchSchedulerJobHistory(job.id, 80)
        .then((res) => {
          if (!cancelled) setHistory(res.items || [])
        })
        .catch(() => {
          if (!cancelled) setHistory([])
        })
        .finally(() => {
          if (!cancelled) setHistoryLoading(false)
        })
    }
    load()
    const t = window.setInterval(load, REFRESH_INTERVAL_MS)
    return () => {
      cancelled = true
      window.clearInterval(t)
    }
  }, [dialogOpen, job.id, pending])

  return (
    <Card className="mb-4 max-w-full overflow-hidden break-inside-avoid border-0 bg-muted/60 py-0 shadow-none">
      {/* 列表项: 点击打开 dialog 查看详情 */}
      <CardHeader
        className="cursor-pointer select-none px-3 py-3 transition-colors hover:bg-muted/80 sm:px-4"
        onClick={() => setDialogOpen(true)}
      >
        <div className="space-y-3">
          <div className="flex min-w-0 items-start gap-3">
            <div
              className={cn(
                "mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-md bg-background/70 text-muted-foreground",
                isRunning && "bg-emerald-500/10 text-emerald-600",
              )}
            >
              {isRunning ? <RefreshCw className="size-4 animate-spin" /> : <Settings2 className="size-4" />}
            </div>
            <div className="min-w-0 flex-1 space-y-1">
              <div className="flex min-w-0 flex-wrap items-center gap-2">
                <CardTitle className="min-w-0 truncate text-base font-semibold leading-6">{job.name}</CardTitle>
                <Badge
                  variant="secondary"
                  className={cn("shrink-0 px-2 py-0 text-[10px]", jobStateClass(isRunning, job.config_enabled))}
                >
                  {jobStateLabel(isRunning, job.config_enabled)}
                </Badge>
                {job.supports_enable ? (
                  <Badge
                    className={cn(
                      "shrink-0 px-2 py-0 text-[10px]",
                      job.config_enabled && "border-emerald-500/30 bg-emerald-500/10 text-emerald-600",
                    )}
                    variant={job.config_enabled ? "outline" : "destructive"}
                  >
                    {job.config_enabled ? "已启用" : "已禁用"}
                  </Badge>
                ) : null}
              </div>
              <div className="truncate font-mono text-[10px] text-muted-foreground">{job.id}</div>
            </div>
          </div>

          <div className="grid grid-cols-[minmax(0,0.85fr)_minmax(0,0.85fr)_minmax(0,1.15fr)] gap-1.5">
            <TodayRunMetric
              icon={<CalendarDays className="size-3" />}
              label="今日预计"
              value={`${formatInteger(expectedCount)} 次`}
              hint="按 cron 推算"
            />
            <TodayRunMetric
              icon={<ListChecks className="size-3" />}
              label="已执行"
              value={`${formatInteger(actualCount)} 次`}
              hint={executionHint}
              tone={executionTone}
            />
            <TodayRunMetric
              icon={<Clock className="size-3" />}
              label="上次"
              value={lastRunValue}
              tone={lastStatus && ["failed", "error", "partial_failed"].includes(lastStatus.toLowerCase()) ? "danger" : undefined}
            />
          </div>

          {job.description ? (
            <p className="line-clamp-2 text-xs leading-5 text-muted-foreground">
              {job.description}
            </p>
          ) : null}
        </div>

        <div
          className="flex min-w-0 flex-wrap items-center justify-between gap-2 rounded-md bg-background/45 px-2 py-2"
        >
          <button
            type="button"
            className="flex min-w-0 items-center gap-1.5 rounded-md px-1 py-1 text-left text-xs text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            onClick={() => setDialogOpen(true)}
          >
            <ChevronRight className="size-3.5" />
            <span>点击查看运行历史与配置</span>
          </button>
          <div
            className="flex shrink-0 flex-wrap items-center justify-end gap-1.5"
            onClick={(e) => e.stopPropagation()}
          >
          {/* 启用/停用按钮 */}
          {job.supports_enable ? (
            job.config_enabled ? (
              <Button
                size="sm"
                variant="outline"
                className="h-7 shrink-0 rounded-xl border-destructive/30 bg-destructive/5 px-1.5 text-[11px] text-destructive hover:bg-destructive/10 hover:text-destructive sm:h-8 sm:w-auto sm:px-2.5 sm:text-sm"
                disabled={isActionPending("disable")}
                onClick={() => onAction(job.id, "disable")}
              >
                <PowerOff className="size-3" />
                {isActionPending("disable") ? "禁用中…" : "禁用"}
              </Button>
            ) : (
              <Button
                size="sm"
                variant="default"
                className="h-7 shrink-0 rounded-xl px-1.5 text-[11px] sm:h-8 sm:w-auto sm:px-2.5 sm:text-sm"
                disabled={isActionPending("enable")}
                onClick={() => onAction(job.id, "enable")}
              >
                <Power className="size-3" />
                {isActionPending("enable") ? "启用中…" : "启用"}
              </Button>
            )
          ) : null}
          {/* 触发按钮 */}
          <Button
            size="sm"
            variant="secondary"
            className="h-7 shrink-0 rounded-xl px-1.5 text-[11px] sm:h-8 sm:w-auto sm:px-2.5 sm:text-sm"
            disabled={isActionPending("trigger")}
            onClick={() => onAction(job.id, "trigger")}
          >
            <Zap className="size-3" />
            {isActionPending("trigger") ? "触发中…" : "触发"}
          </Button>
          <CategoryManageButton
            job={job}
            categories={categories}
            pending={isActionPending("categories")}
            compact
            onSave={onUpdateCategories}
          />
          </div>
        </div>
      </CardHeader>

      {hasChainProgress && isRunning ? (
        <CardContent className="px-3 pb-3 pt-0 sm:px-4">
          <ChainStepProgress
            currentStep={chainCurrentStep}
            currentStepProgress={chainCurrentStepProgress}
            completedSteps={chainCompletedSteps}
            stepResults={chainStepResults}
            overallStatus={lastStatus}
            updatedAt={progress?.updatedAt}
          />
        </CardContent>
      ) : null}

      {/* Dialog 详情 */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-h-[88vh] w-[calc(100vw-1rem)] overflow-y-auto p-4 sm:w-[calc(100vw-2rem)] sm:max-w-[1120px] sm:p-6">
          <DialogHeader>
            <DialogTitle className="flex min-w-0 flex-wrap items-center gap-2 pr-6 leading-6">
              <Settings2 className="size-4" />
              <span className="min-w-0 break-words">{job.name}</span>
              <span className="max-w-full break-all rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                {job.id}
              </span>
            </DialogTitle>
            {(job.categories || []).length > 0 ? (
              <div className="flex flex-wrap gap-1">
                {(job.categories || []).map((c) => (
                  <Badge key={c} variant="outline" className="px-1.5 py-0 text-[10px] font-normal">
                    {categoryLabelById.get(c) ?? c}
                  </Badge>
                ))}
              </div>
            ) : null}
          </DialogHeader>

          {/* 状态条 */}
          <div className="flex flex-wrap items-center gap-2">
            {isRunning ? (
              <Badge variant="secondary" className="bg-emerald-500/10 text-emerald-600 border-emerald-500/30">运行中</Badge>
            ) : job.config_enabled ? (
              <Badge variant="secondary" className="border-blue-500/30 bg-blue-500/10 text-blue-600">空闲</Badge>
            ) : (
              <Badge variant="secondary">已停止</Badge>
            )}
            {job.supports_enable ? (
              <Badge
                variant={job.config_enabled ? "outline" : "destructive"}
                className={job.config_enabled ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600" : undefined}
              >
                {job.config_enabled ? "已启用" : "已禁用"}
              </Badge>
            ) : null}
          </div>

          {/* 计算逻辑 */}
          {job.description ? (
            <>
              <div className="space-y-2 text-sm">
                <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  计算逻辑
                </div>
                <div className="whitespace-pre-wrap break-words rounded-2xl border border-border/40 bg-background/70 p-3 text-xs leading-6 text-muted-foreground">
                  {job.description}
                </div>
              </div>
              <Separator />
            </>
          ) : null}

          {/* 统计网格 */}
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Stat icon={<ListChecks className="size-3.5" />} label="累计运行" value={totalRuns ?? "—"} />
            <Stat icon={<TimerReset className="size-3.5" />} label="最后耗时" value={formatSeconds(lastDuration ?? null)} />
            <Stat icon={<Activity className="size-3.5" />} label="处理标的" value={lastTargets ?? "—"} />
            <Stat icon={<Clock className="size-3.5" />} label="注册时间" value={formatDateTime(registeredAt)} />
          </div>

          <Separator />

          {/* 链路步骤进度 (仅 chain job) */}
          {hasChainProgress ? (
            <>
              <ChainStepProgress
                currentStep={chainCurrentStep}
                currentStepProgress={chainCurrentStepProgress}
                completedSteps={chainCompletedSteps}
                stepResults={chainStepResults}
                overallStatus={lastStatus}
                updatedAt={progress?.updatedAt}
              />
              <Separator />
            </>
          ) : null}

          {qcSummary ? (
            <>
              <FundFlowQcSummaryCard summary={qcSummary} />
              <Separator />
            </>
          ) : null}

          {/* 上次运行 + 注册信息 */}
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1.5 text-sm">
              <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">上次运行</div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">时间：</span>
                <span>{formatDateTime(lastRunAt)}</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">状态：</span>
                {lastStatus ? (
                  <Badge variant={statusBadgeVariant(lastStatus)} className={statusBadgeClass(lastStatus)}>{lastStatus}</Badge>
                ) : <span>—</span>}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">处理标的：</span>
                <span>{lastTargets ?? "—"} 个</span>
              </div>
              {lastError ? (
                <div className={cn("flex items-start gap-2", lastStatus === "skipped" ? "text-amber-600" : "text-destructive")}>
                  <AlertTriangle className="mt-0.5 size-3.5" />
                  <span className="break-words text-xs leading-5">{lastError}</span>
                </div>
              ) : null}
              {!lastError && lastMessage ? (
                <div className="flex items-start gap-2 text-emerald-600">
                  <CheckCircle2 className="mt-0.5 size-3.5" />
                  <span className="break-words text-xs leading-5">{lastMessage}</span>
                </div>
              ) : null}
            </div>

            <div className="space-y-1.5 text-sm">
              <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">注册信息</div>
              <div className="flex flex-wrap items-center gap-2">
                <FileCode2 className="size-3.5 text-muted-foreground" />
                <span className="text-muted-foreground">config：</span>
                <span className="break-all font-mono text-xs">{job.config_file || "—"}</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">service：</span>
                <span className="break-all font-mono text-xs">{job.service_class || "—"}</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">module：</span>
                <span className="break-all font-mono text-xs">{job.service_module || "—"}</span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-muted-foreground">注册时间：</span>
                <span>{formatDateTime(job.registered_at)}</span>
              </div>
            </div>
          </div>

          {/* Inflight */}
          {inflight && Object.keys(inflight).length > 0 ? (
            <>
              <Separator />
              <div className="space-y-1.5 text-sm">
                <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">正在执行</div>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(inflight).map(([targetId, startedAtValue]) => (
                    <Badge key={targetId} variant="secondary" className="max-w-full whitespace-normal break-all text-left">
                      {targetId} · {formatDateTime(startedAtValue)}
                    </Badge>
                  ))}
                </div>
              </div>
            </>
          ) : null}

          <Separator />
          <JobHistorySection items={history} loading={historyLoading} />
          <Separator />

          {/* 操作按钮组 */}
          <div className="grid grid-cols-1 gap-2 sm:flex sm:flex-wrap sm:items-center">
            {isRunning ? (
              <Button size="sm" variant="destructive" className="w-full rounded-xl sm:w-auto" disabled={isActionPending("stop")} onClick={() => onAction(job.id, "stop")}>
                <CirclePause className="size-3.5" />
                {isActionPending("stop") ? "停止中…" : "停止调度"}
              </Button>
            ) : (
              <Button size="sm" variant="default" className="w-full rounded-xl sm:w-auto" disabled={isActionPending("start")} onClick={() => onAction(job.id, "start")}>
                <Play className="size-3.5" />
                {isActionPending("start") ? "启动中…" : "启动调度"}
              </Button>
            )}

            {job.supports_enable ? (
              job.config_enabled ? (
                <Button size="sm" variant="outline" className="w-full rounded-xl border-destructive/30 bg-destructive/5 text-destructive hover:bg-destructive/10 hover:text-destructive sm:w-auto" disabled={isActionPending("disable")} onClick={() => onAction(job.id, "disable")}>
                  <PowerOff className="size-3.5" />
                  {isActionPending("disable") ? "禁用中…" : "禁用"}
                </Button>
              ) : (
                <Button size="sm" variant="default" className="w-full rounded-xl sm:w-auto" disabled={isActionPending("enable")} onClick={() => onAction(job.id, "enable")}>
                  <Power className="size-3.5" />
                  {isActionPending("enable") ? "启用中…" : "启用"}
                </Button>
              )
            ) : null}

            {job.config_enabled && !isRunning ? (
              <span className="inline-flex items-center gap-1 text-xs text-amber-600">
                <AlertTriangle className="size-3" />
                配置启用但线程未运行
              </span>
            ) : null}
            {job.config_enabled === false && isRunning ? (
              <span className="inline-flex items-center gap-1 text-xs text-amber-600">
                <AlertTriangle className="size-3" />
                线程运行中但配置已禁用
              </span>
            ) : null}

            <Button
              size="sm"
              variant="secondary"
              className="w-full rounded-xl sm:w-auto"
              disabled={isActionPending("trigger")}
              onClick={() => onAction(job.id, "trigger")}
            >
              <Zap className="size-3.5" />
              {isActionPending("trigger") ? "触发中…" : "手动触发"}
            </Button>

            <CategoryManageButton
              job={job}
              categories={categories}
              pending={isActionPending("categories")}
              onSave={onUpdateCategories}
            />

            <DeleteJobButton
              jobId={job.id}
              jobName={job.name}
              pending={isActionPending("delete")}
              onConfirm={() => {
                onAction(job.id, "delete")
                setDialogOpen(false)
              }}
            />
          </div>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

/** Job 历史记录 section. 每行: 开始时间 + 触发方式 (自动/手动) + 状态 (成功/失败/跳过) + 错误 + 耗时. */
function JobHistorySection({
  items,
  loading,
}: {
  items: SchedulerJobHistoryItem[]
  loading: boolean
}) {
  const [expanded, setExpanded] = useState(false)
  const visibleItems = expanded ? items : items.slice(0, 3)

  return (
    <div className="space-y-1.5 text-sm">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          <HistoryIcon className="size-3.5" />
          <span>历史记录</span>
          <span className="text-[10px] text-muted-foreground/60">
            (每 5s 自动刷新)
          </span>
          {loading ? (
            <RefreshCw className="size-3 animate-spin text-muted-foreground/60" />
          ) : null}
        </div>
        {items.length > 3 ? (
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className="h-6 px-2 text-[11px]"
            onClick={() => setExpanded((value) => !value)}
          >
            {expanded ? "收起" : `展开 ${items.length} 条`}
          </Button>
        ) : null}
      </div>

      {items.length === 0 ? (
        <div className="rounded-md border border-dashed border-border/40 bg-background/30 px-3 py-4 text-center text-xs text-muted-foreground">
          {loading ? "加载中…" : "暂无历史记录 (等下次 cron 触发 或 点 \"立即触发\")"}
        </div>
      ) : (
        <div className="overflow-hidden rounded-md border border-border/30">
          <div className="hidden grid-cols-[1fr_auto_auto_auto] gap-2 bg-muted/30 px-2 py-1 text-[10px] font-medium uppercase tracking-wide text-muted-foreground sm:grid">
            <span>开始时间</span>
            <span>触发</span>
            <span>状态</span>
            <span>耗时</span>
          </div>
          <div className={cn("divide-y divide-border/30", expanded && "max-h-[360px] overflow-y-auto")}>
            {visibleItems.map((it, idx) => (
              <HistoryRow key={it.id || `${it.start_at}-${idx}`} item={it} />
            ))}
            {!expanded && items.length > 3 ? (
              <div className="flex items-center justify-center gap-2 bg-muted/20 px-2 py-2 text-[11px] text-muted-foreground">
                <span>还有 {items.length - 3} 条历史记录</span>
                <button
                  type="button"
                  className="font-medium text-foreground/80 hover:text-foreground"
                  onClick={() => setExpanded(true)}
                >
                  展开
                </button>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  )
}

function HistoryRow({ item }: { item: SchedulerJobHistoryItem }) {
  const [expanded, setExpanded] = useState(false)
  const isFailed = item.status === "failed"
  const isSkipped = item.status === "skipped"
  const isSuccess = item.status === "success"
  const isProcessing = item.status === "processing" || item.status === "running" || item.status === "in_progress"
  const isManual = item.trigger_type === "manual"
  const summaryMessage = item.result_summary?.message || item.message
  const hasDetail = (isFailed || isSkipped) ? !!item.error : (isSuccess && !!summaryMessage)

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-2 px-2 py-2 text-xs sm:grid-cols-[1fr_auto_auto_auto] sm:items-center sm:py-1.5",
        isFailed && "bg-red-50/50 dark:bg-red-950/10",
        isSkipped && "bg-amber-50/50 dark:bg-amber-950/10",
        isProcessing && "bg-sky-50/50 dark:bg-sky-950/10",
        isSuccess && (item.error || item.message) && "bg-emerald-50/50 dark:bg-emerald-950/10",
      )}
    >
      <div className="flex min-w-0 items-center gap-1.5">
        {hasDetail ? (
          <button
            type="button"
            aria-label={expanded ? "收起详情" : "展开详情"}
            onClick={() => setExpanded((v) => !v)}
            className="shrink-0 text-muted-foreground hover:text-foreground"
          >
            {expanded ? (
              <ChevronDown className="size-3" />
            ) : (
              <ChevronRight className="size-3" />
            )}
          </button>
        ) : (
          <span className="w-3" />
        )}
        <span className="truncate font-mono tabular-nums" title={item.start_at}>
          {formatDateTime(item.start_at)}
        </span>
      </div>
      <Badge
        variant={isManual ? "secondary" : "outline"}
        className="w-fit px-1.5 py-0 text-[10px]"
        title={isManual ? "用户手动触发" : "cron 自动触发"}
      >
        {isManual ? (
          <>
            <Hand className="mr-0.5 size-2.5" />
            手动
          </>
        ) : (
          "自动"
        )}
      </Badge>
      <Badge variant={statusBadgeVariant(item.status)} className={cn(statusBadgeClass(item.status), "w-fit px-1.5 py-0 text-[10px]")}>
        {isProcessing ? (
          <>
            <RefreshCw className="mr-0.5 size-2.5 animate-spin" />
            processing
          </>
        ) : (
          item.status
        )}
      </Badge>
      <span className="font-mono tabular-nums text-muted-foreground">
        {formatSeconds(item.duration_seconds)}
      </span>

      {hasDetail && expanded ? (
        <div className={cn(
          "mt-1 flex items-start gap-1.5 rounded border px-2 py-1.5 text-[11px] leading-5 sm:col-span-4",
          isSkipped
            ? "border-amber-300 bg-amber-50 text-amber-700 dark:border-amber-800 dark:bg-amber-950/20 dark:text-amber-400"
            : isSuccess
              ? "border-emerald-300 bg-emerald-50 text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950/20 dark:text-emerald-400"
              : "border-destructive/30 bg-destructive/5 text-destructive",
        )}>
          {isSuccess ? (
            <CheckCircle2 className="mt-0.5 size-3 shrink-0" />
          ) : (
            <AlertTriangle className="mt-0.5 size-3 shrink-0" />
          )}
          <span className="break-all whitespace-pre-wrap">{isSuccess ? summaryMessage : item.error}</span>
        </div>
      ) : null}

      {!isFailed && item.target_count != null && item.target_count > 0 ? (
        <div className="mt-0.5 text-[10px] text-muted-foreground sm:col-span-4">
          处理标的 {item.target_count} 个, 成功 {item.succeeded ?? 0}
        </div>
      ) : null}
    </div>
  )
}

function CategoryManageButton({
  job,
  categories,
  pending,
  compact = false,
  onSave,
}: {
  job: SchedulerJobItem
  categories: SchedulerCategory[]
  pending: boolean
  compact?: boolean
  onSave: (jobId: string, categoryIds: string[]) => void
}) {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState<string[]>([])

  const selected = useMemo(() => new Set(draft), [draft])

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (nextOpen) {
      setDraft(job.categories || [])
    }
  }

  const toggleCategory = (categoryId: string, checked: boolean) => {
    setDraft((current) => {
      if (checked) {
        return current.includes(categoryId) ? current : [...current, categoryId]
      }
      return current.filter((id) => id !== categoryId)
    })
  }

  const handleSave = () => {
    onSave(job.id, draft)
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button
          size="sm"
          variant="outline"
          className={cn(
            "rounded-xl",
            compact
              ? "h-7 shrink-0 px-1.5 text-[11px] sm:h-8 sm:w-auto sm:px-2.5 sm:text-sm"
              : "w-full sm:w-auto",
          )}
          disabled={pending || categories.length === 0}
        >
          <Tags className={compact ? "size-3" : "size-3.5"} />
          {pending ? "保存中…" : "分类"}
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-1rem)] p-4 sm:max-w-2xl sm:p-6">
        <DialogHeader>
          <DialogTitle>设置 Job 分类</DialogTitle>
          <DialogDescription>
            <span className="break-all font-mono">{job.id}</span>
          </DialogDescription>
        </DialogHeader>
        <div className="max-h-[360px] space-y-1 overflow-y-auto rounded-lg bg-muted/40 p-2">
          {categories.map((category) => {
            const Icon = iconFor(category.icon_hint)
            const checked = selected.has(category.id)
            return (
              <label
                key={category.id}
                className="flex cursor-pointer items-start gap-2 rounded-md px-2 py-2 text-sm hover:bg-background/70"
              >
                <Checkbox
                  checked={checked}
                  onCheckedChange={(value) => toggleCategory(category.id, value === true)}
                  className="mt-0.5"
                />
                <span className="min-w-0 flex-1">
                  <span className="flex min-w-0 items-center gap-1.5 font-medium text-foreground">
                    <Icon className="size-3.5 shrink-0 text-muted-foreground" />
                    <span className="truncate">{category.label}</span>
                  </span>
                  {category.description ? (
                    <span className="mt-0.5 block line-clamp-2 text-xs leading-5 text-muted-foreground">
                      {category.description}
                    </span>
                  ) : null}
                </span>
              </label>
            )
          })}
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="w-full rounded-xl sm:w-auto">取消</Button>
          </DialogClose>
          <Button
            className="w-full rounded-xl sm:w-auto"
            disabled={pending}
            onClick={handleSave}
          >
            保存
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function CreateCategoryButton({
  pending,
  onCreate,
}: {
  pending: boolean
  onCreate: (input: SchedulerCategoryInput) => void
}) {
  const [open, setOpen] = useState(false)
  const [label, setLabel] = useState("")
  const [code, setCode] = useState("")
  const [iconHint, setIconHint] = useState("grid")
  const [description, setDescription] = useState("")

  const reset = () => {
    setLabel("")
    setCode("")
    setIconHint("grid")
    setDescription("")
  }

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (!nextOpen) {
      reset()
    }
  }

  const handleCreate = () => {
    const trimmedLabel = label.trim()
    if (!trimmedLabel) {
      notification.danger({ title: "分类名称不能为空" })
      return
    }
    onCreate({
      label: trimmedLabel,
      code: code.trim() || undefined,
      icon_hint: iconHint.trim() || undefined,
      description: description.trim() || undefined,
    })
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button
          type="button"
          size="sm"
          variant="ghost"
          className="ml-1 h-[calc(100%-1px)] rounded-md px-2 text-foreground/60 hover:bg-background hover:text-foreground"
          disabled={pending}
        >
          <Plus className="size-3.5" />
          添加分类
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-1rem)] p-4 sm:max-w-lg sm:p-6">
        <DialogHeader>
          <DialogTitle>添加 Job 分类</DialogTitle>
          <DialogDescription>新分类会立即出现在 Scheduler tab 中。</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor="scheduler-category-label">分类名称</Label>
            <Input
              id="scheduler-category-label"
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              placeholder="例如：盘内实时"
              autoFocus
            />
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="scheduler-category-code">Code</Label>
              <Input
                id="scheduler-category-code"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="可留空自动生成"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="scheduler-category-icon">Icon hint</Label>
              <Input
                id="scheduler-category-icon"
                value={iconHint}
                onChange={(event) => setIconHint(event.target.value)}
                placeholder="grid"
              />
            </div>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="scheduler-category-description">描述</Label>
            <textarea
              id="scheduler-category-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="min-h-20 w-full resize-y rounded-md border-0 bg-muted/60 px-3 py-2 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring/50"
              placeholder="可选"
            />
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="w-full rounded-xl sm:w-auto">取消</Button>
          </DialogClose>
          <Button
            className="w-full rounded-xl sm:w-auto"
            disabled={pending}
            onClick={handleCreate}
          >
            <Plus className="size-3.5" />
            {pending ? "添加中…" : "添加"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function AddJobToCategoryButton({
  categories,
  jobs,
  activeCategory,
  pending,
  onAdd,
}: {
  categories: SchedulerCategory[]
  jobs: SchedulerJobItem[]
  activeCategory: string
  pending: boolean
  onAdd: (categoryId: string, jobIds: string[]) => void
}) {
  const defaultCategoryId = activeCategory !== ALL_TAB
    ? activeCategory
    : (categories[0]?.id ?? "")
  const [open, setOpen] = useState(false)
  const [categoryId, setCategoryId] = useState(defaultCategoryId)
  const [selectedJobIds, setSelectedJobIds] = useState<string[]>([])
  const [keyword, setKeyword] = useState("")

  const selectedCategory = categories.find((category) => category.id === categoryId)
  const availableJobs = useMemo(() => {
    const lowerKeyword = keyword.trim().toLowerCase()
    return jobs
      .filter((job) => !job.categories?.includes(categoryId))
      .filter((job) => {
        if (!lowerKeyword) return true
        return `${job.id} ${job.name} ${job.description || ""}`.toLowerCase().includes(lowerKeyword)
      })
  }, [categoryId, jobs, keyword])

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (nextOpen) {
      setCategoryId(defaultCategoryId)
      setSelectedJobIds([])
      setKeyword("")
    }
  }

  const handleCategoryChange = (nextCategoryId: string) => {
    setCategoryId(nextCategoryId)
    setSelectedJobIds([])
  }

  const toggleJob = (jobId: string, checked: boolean) => {
    setSelectedJobIds((current) => {
      if (checked) {
        return current.includes(jobId) ? current : [...current, jobId]
      }
      return current.filter((id) => id !== jobId)
    })
  }

  const handleSave = () => {
    if (!categoryId) {
      notification.danger({ title: "请先选择分类" })
      return
    }
    if (selectedJobIds.length === 0) {
      notification.danger({ title: "请选择要添加的 Job" })
      return
    }
    onAdd(categoryId, selectedJobIds)
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button
          type="button"
          size="sm"
          variant="ghost"
          className="h-[calc(100%-1px)] rounded-md px-2 text-foreground/60 hover:bg-background hover:text-foreground"
          disabled={pending || categories.length === 0 || jobs.length === 0}
        >
          <Plus className="size-3.5" />
          添加 Job
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-1rem)] p-4 sm:max-w-2xl sm:p-6">
        <DialogHeader>
          <DialogTitle>添加 Job 到分类</DialogTitle>
          <DialogDescription>选择一个分类，把已有 job 映射进去。</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4">
          <div className="grid gap-2">
            <Label>分类</Label>
            <div className="flex max-h-28 flex-wrap gap-1.5 overflow-y-auto rounded-lg bg-muted/40 p-2">
              {categories.map((category) => {
                const Icon = iconFor(category.icon_hint)
                const active = category.id === categoryId
                return (
                  <button
                    key={category.id}
                    type="button"
                    className={cn(
                      "inline-flex h-8 items-center gap-1.5 rounded-md px-2 text-xs font-medium text-muted-foreground hover:bg-background hover:text-foreground",
                      active && "bg-background text-foreground shadow-sm",
                    )}
                    onClick={() => handleCategoryChange(category.id)}
                  >
                    <Icon className="size-3.5" />
                    {category.label}
                  </button>
                )
              })}
            </div>
          </div>
          <div className="grid gap-2">
            <div className="flex items-center justify-between gap-2">
              <Label htmlFor="scheduler-category-job-search">Job</Label>
              <span className="text-xs text-muted-foreground">
                {selectedJobIds.length} selected
              </span>
            </div>
            <Input
              id="scheduler-category-job-search"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索 job code / name"
            />
            <div className="max-h-[360px] space-y-1 overflow-y-auto rounded-lg bg-muted/40 p-2">
              {availableJobs.length > 0 ? (
                availableJobs.map((job) => {
                  const checked = selectedJobIds.includes(job.id)
                  return (
                    <label
                      key={job.id}
                      className="flex cursor-pointer items-start gap-2 rounded-md px-2 py-2 text-sm hover:bg-background/70"
                    >
                      <Checkbox
                        checked={checked}
                        onCheckedChange={(value) => toggleJob(job.id, value === true)}
                        className="mt-0.5"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-medium text-foreground">{job.name}</span>
                        <span className="mt-0.5 block truncate font-mono text-xs text-muted-foreground">
                          {job.id}
                        </span>
                      </span>
                    </label>
                  )
                })
              ) : (
                <div className="px-2 py-8 text-center text-sm text-muted-foreground">
                  {selectedCategory ? "没有可添加的 Job" : "请先选择分类"}
                </div>
              )}
            </div>
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="w-full rounded-xl sm:w-auto">取消</Button>
          </DialogClose>
          <Button
            className="w-full rounded-xl sm:w-auto"
            disabled={pending || selectedJobIds.length === 0}
            onClick={handleSave}
          >
            <Plus className="size-3.5" />
            添加
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function EditCategoryButton({
  category,
  count,
  pending,
  onSave,
  onDelete,
}: {
  category: SchedulerCategory
  count: number
  pending: boolean
  onSave: (category: SchedulerCategory, input: SchedulerCategoryInput) => void
  onDelete: (category: SchedulerCategory) => void
}) {
  const [open, setOpen] = useState(false)
  const [label, setLabel] = useState(category.label)
  const [code, setCode] = useState("")
  const [iconHint, setIconHint] = useState(category.icon_hint || "grid")
  const [sortOrder, setSortOrder] = useState(String(category.sort_order ?? ""))
  const [description, setDescription] = useState(category.description || "")

  const reset = () => {
    setLabel(category.label)
    setCode("")
    setIconHint(category.icon_hint || "grid")
    setSortOrder(String(category.sort_order ?? ""))
    setDescription(category.description || "")
  }

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (nextOpen) {
      reset()
    }
  }

  const handleSave = () => {
    const trimmedLabel = label.trim()
    if (!trimmedLabel) {
      notification.danger({ title: "分类名称不能为空" })
      return
    }
    const trimmedSortOrder = sortOrder.trim()
    const numericSortOrder = trimmedSortOrder ? Number(trimmedSortOrder) : undefined
    if (numericSortOrder !== undefined && !Number.isFinite(numericSortOrder)) {
      notification.danger({ title: "排序值必须是数字" })
      return
    }
    onSave(category, {
      label: trimmedLabel,
      code: code.trim() || undefined,
      icon_hint: iconHint.trim() || undefined,
      description: description.trim(),
      sort_order: numericSortOrder,
    })
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button
          type="button"
          size="icon"
          variant="ghost"
          aria-label={`编辑分类 ${category.label}`}
          title="编辑分类"
          className="ml-0.5 h-6 w-6 rounded-md text-muted-foreground hover:bg-background hover:text-foreground"
          disabled={pending}
          onClick={(event) => event.stopPropagation()}
        >
          <Pencil className="size-3" />
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-1rem)] p-4 sm:max-w-lg sm:p-6">
        <DialogHeader>
          <DialogTitle>修改 Job 分类</DialogTitle>
          <DialogDescription>调整分类名称、图标和排序，也可以删除这个分类。</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor={`scheduler-category-label-${category.id}`}>分类名称</Label>
            <Input
              id={`scheduler-category-label-${category.id}`}
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              autoFocus
            />
          </div>
          <div className="grid gap-2 sm:grid-cols-3">
            <div className="grid gap-2">
              <Label htmlFor={`scheduler-category-code-${category.id}`}>Code</Label>
              <Input
                id={`scheduler-category-code-${category.id}`}
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="留空保持原 code"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor={`scheduler-category-icon-${category.id}`}>Icon hint</Label>
              <Input
                id={`scheduler-category-icon-${category.id}`}
                value={iconHint}
                onChange={(event) => setIconHint(event.target.value)}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor={`scheduler-category-sort-${category.id}`}>排序</Label>
              <Input
                id={`scheduler-category-sort-${category.id}`}
                value={sortOrder}
                onChange={(event) => setSortOrder(event.target.value)}
                inputMode="numeric"
              />
            </div>
          </div>
          <div className="grid gap-2">
            <Label htmlFor={`scheduler-category-description-${category.id}`}>描述</Label>
            <textarea
              id={`scheduler-category-description-${category.id}`}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="min-h-20 w-full resize-y rounded-md border-0 bg-muted/60 px-3 py-2 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring/50"
              placeholder="可选"
            />
          </div>
          <div className="rounded-lg bg-destructive/5 p-3 text-xs leading-5 text-muted-foreground">
            删除分类会同时移除 {count} 个 job 的分类关联；job 本身不会被删除。
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button
              variant="ghost"
              className="mr-auto w-full rounded-xl text-destructive hover:bg-destructive/10 hover:text-destructive sm:w-auto"
              disabled={pending}
              onClick={() => onDelete(category)}
            >
              <Trash2 className="size-3.5" />
              删除分类
            </Button>
          </DialogClose>
          <DialogClose asChild>
            <Button variant="outline" className="w-full rounded-xl sm:w-auto">取消</Button>
          </DialogClose>
          <Button
            className="w-full rounded-xl sm:w-auto"
            disabled={pending}
            onClick={handleSave}
          >
            <Pencil className="size-3.5" />
            保存
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** 删除按钮: 点击弹出确认对话框, 确认后调用 onConfirm */
function DeleteJobButton({
  jobId,
  jobName,
  pending,
  onConfirm,
}: {
  jobId: string
  jobName: string
  pending: boolean
  onConfirm: () => void
}) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button
          size="sm"
          variant="ghost"
          className="w-full rounded-xl text-destructive hover:bg-destructive/10 hover:text-destructive sm:ml-auto sm:w-auto"
          disabled={pending}
        >
          <Trash2 className="size-3.5" />
          {pending ? "删除中…" : "删除"}
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-1rem)] p-4 sm:max-w-md sm:p-6">
        <DialogHeader>
          <DialogTitle>删除调度任务</DialogTitle>
          <DialogDescription>
            确认要删除 <span className="break-all font-mono font-semibold">{jobId}</span> 吗？
          </DialogDescription>
        </DialogHeader>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm leading-6 text-foreground">
          <div className="break-words font-medium">{jobName}</div>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-muted-foreground">
            <li>会从 <code className="rounded bg-muted px-1">scheduler/jobs.json</code> 注册表里移除</li>
            <li>后端会停掉该 job 的调度器线程（如果正在运行）</li>
            <li>下次重启 Flask 不会自动重新注册</li>
            <li className="text-destructive">删除后如需恢复，需手动重新注册到 jobs.json</li>
          </ul>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="w-full rounded-xl sm:w-auto">取消</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button
              variant="destructive"
              className="w-full rounded-xl sm:w-auto"
              disabled={pending}
              onClick={onConfirm}
            >
              <Trash2 className="size-3.5" />
              确认删除
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function Stat({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: React.ReactNode
}) {
  return (
    <div className="min-w-0 rounded-xl border border-border/30 bg-muted/30 p-3">
      <div className="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
        {icon}
        {label}
      </div>
      <div className="break-words text-sm font-medium text-foreground">{value}</div>
    </div>
  )
}

export default function SchedulerSettingsPage() {
  const [jobs, setJobs] = useState<SchedulerJobItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const [pending, setPending] = useState<ActionState>({})
  const [activeCategory, setActiveCategory] = useState<string>(ALL_TAB)
  const [categories, setCategories] = useState<SchedulerCategory[]>([])
  const [creatingCategory, setCreatingCategory] = useState(false)
  const [editingCategories, setEditingCategories] = useState(false)
  const [updatingCategoryId, setUpdatingCategoryId] = useState<string | null>(null)
  const [deletingCategoryId, setDeletingCategoryId] = useState<string | null>(null)
  const [addingJobToCategory, setAddingJobToCategory] = useState(false)
  const [dailyStats, setDailyStats] = useState<SchedulerDailyStatItem[]>([])
  const [dailyStatsSummary, setDailyStatsSummary] = useState<{ total: number; failed: number; success_rate: number } | null>(null)
  const [marketSentimentTargetDate, setMarketSentimentTargetDate] = useState(() => toLocalIso(todayLocal()))

  const refresh = useCallback(async (withSpinner = false) => {
    if (withSpinner) setLoading(true)
    try {
      const [jobsRes, catRes, statsRes] = await Promise.all([
        fetchSchedulerJobs(),
        fetchSchedulerCategories(),
        fetchSchedulerDailyStats(14),
      ])
      if (jobsRes.ok) {
        setJobs(jobsRes.items || [])
        setError(null)
        setLastUpdated(new Date())
      } else {
        setError(jobsRes.error || "获取调度任务列表失败")
        if (withSpinner) {
          notification.danger({
            title: "加载调度任务失败",
            description: jobsRes.error || "请检查后端服务",
          })
        }
      }
      if (catRes.ok) {
        setCategories(catRes.items || [])
      }
      if (statsRes.ok) {
        setDailyStats(statsRes.items || [])
        setDailyStatsSummary(statsRes.summary ?? null)
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : "未知错误"
      setError(msg)
      if (withSpinner) {
        notification.danger({ title: "加载调度任务失败", description: msg })
      }
    } finally {
      if (withSpinner) setLoading(false)
    }
  }, [])

  useEffect(() => {
    const initialTimer = window.setTimeout(() => {
      void refresh(true)
    }, 0)
    const timer = window.setInterval(() => {
      void refresh(false)
    }, REFRESH_INTERVAL_MS)
    return () => {
      window.clearTimeout(initialTimer)
      window.clearInterval(timer)
    }
  }, [refresh])

  const handleAction = useCallback(
    async (jobId: string, action: ActionKey) => {
      setPending((prev) => ({ ...prev, [jobId]: action }))
      try {
        let res
        const job = jobs.find((item) => item.id === jobId)
        const marketSentimentCategoryIds = new Set<string>([
          MARKET_SENTIMENT_CATEGORY_FALLBACK_ID,
          ...categories
            .filter((category) => category.label === MARKET_SENTIMENT_CATEGORY_LABEL)
            .map((category) => category.id),
        ])
        const shouldUseTargetDate =
          action === "trigger" &&
          Boolean(job?.categories?.some((categoryId) => marketSentimentCategoryIds.has(categoryId)))
        switch (action) {
          case "enable":
            res = await enableSchedulerJob(jobId)
            break
          case "disable":
            res = await disableSchedulerJob(jobId)
            break
          case "start":
            res = await startSchedulerJob(jobId)
            break
          case "stop":
            res = await stopSchedulerJob(jobId)
            break
          case "trigger":
            res = await triggerSchedulerJob(jobId, {
              targetDate: shouldUseTargetDate ? marketSentimentTargetDate : undefined,
            })
            break
          case "delete":
            res = await deleteSchedulerJob(jobId)
            break
        }
        if (res && res.ok === false) {
          const errorMsg = res.error || "请查看后端日志"
          notification.danger({
            title: "操作失败",
            description: errorMsg,
          })
          return
        }
        // 成功：根据 action 给具体文案
        switch (action) {
          case "enable":
            notification.success({ title: "已启用", description: `job ${jobId} 已开启` })
            break
          case "disable":
            notification.success({ title: "已禁用", description: `job ${jobId} 已关闭` })
            break
          case "start":
            notification.success({ title: "调度已启动", description: `job ${jobId}` })
            break
          case "stop":
            notification.success({ title: "调度已停止", description: `job ${jobId}` })
            break
          case "trigger": {
            const count = (res && (res as { count?: number }).count) ?? 0
            const failed = (res && (res as { failed_count?: number }).failed_count) ?? 0
            const resultItems = res?.items || []
            const detailError = resultItems.length > 0
              ? (resultItems[0].lastRunError || "")
              : ""
            if (count > 0) {
              if (failed > 0) {
                notification.danger({
                  title: `触发完成 — ${failed}/${count} 失败`,
                  description: detailError || "请展开 job 卡片查看详情",
                })
              } else {
                notification.success({
                  title: "已触发一次",
                  description: shouldUseTargetDate
                    ? `${count} 个任务按 ${marketSentimentTargetDate} 执行成功`
                    : `${count} 个标的全部执行成功`,
                })
              }
            } else {
              notification.info({
                title: "已触发",
                description: "本次没有可执行的标的（targets 为空或全部未启用）",
              })
            }
            break
          }
          case "delete":
            notification.success({
              title: "已删除",
              description: `job ${jobId} 已从 jobs.json 中移除（后端会停掉线程）`,
            })
            // 立即从本地列表移除, 不等 refresh
            setJobs((prev) => prev.filter((j) => j.id !== jobId))
            break
        }
      } catch (err) {
        notification.danger({
          title: "操作失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setPending((prev) => ({ ...prev, [jobId]: null }))
        // 操作后等一拍再刷新，让后端先把状态写回 JSON
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      }
    },
    [categories, jobs, marketSentimentTargetDate, refresh],
  )

  const handleUpdateCategories = useCallback(
    async (jobId: string, categoryIds: string[]) => {
      setPending((prev) => ({ ...prev, [jobId]: "categories" }))
      try {
        const res = await updateSchedulerJobCategories(jobId, categoryIds)
        if (res.ok === false) {
          notification.danger({
            title: "分类保存失败",
            description: res.error || "请查看后端日志",
          })
          return
        }
        setJobs((prev) => prev.map((job) => (
          job.id === jobId
            ? (res.job ?? { ...job, categories: categoryIds })
            : job
        )))
        notification.success({
          title: "分类已保存",
          description: `job ${jobId}`,
        })
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      } catch (err) {
        notification.danger({
          title: "分类保存失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setPending((prev) => ({ ...prev, [jobId]: null }))
      }
    },
    [refresh],
  )

  const handleCreateCategory = useCallback(
    async (input: SchedulerCategoryInput) => {
      setCreatingCategory(true)
      try {
        const category = await createSchedulerCategory(input)
        setCategories((prev) => sortCategories([...prev, category]))
        setActiveCategory(category.id)
        notification.success({ title: "分类已添加", description: category.label })
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      } catch (err) {
        notification.danger({
          title: "添加分类失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setCreatingCategory(false)
      }
    },
    [refresh],
  )

  const handleUpdateCategory = useCallback(
    async (category: SchedulerCategory, input: SchedulerCategoryInput) => {
      setUpdatingCategoryId(category.id)
      try {
        const updated = await updateSchedulerCategory(category.id, input)
        setCategories((prev) => sortCategories(prev.map((item) => (
          item.id === category.id ? updated : item
        ))))
        notification.success({ title: "分类已保存", description: updated.label })
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      } catch (err) {
        notification.danger({
          title: "保存分类失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setUpdatingCategoryId(null)
      }
    },
    [refresh],
  )

  const handleDeleteCategory = useCallback(
    async (category: SchedulerCategory) => {
      setDeletingCategoryId(category.id)
      try {
        const res = await deleteSchedulerCategory(category.id)
        if (res.ok === false) {
          notification.danger({
            title: "删除分类失败",
            description: res.error || "请查看后端日志",
          })
          return
        }
        setCategories((prev) => prev.filter((item) => item.id !== category.id))
        setJobs((prev) => prev.map((job) => ({
          ...job,
          categories: (job.categories || []).filter((categoryId) => categoryId !== category.id),
          categorySortOrders: Object.fromEntries(
            Object.entries(job.categorySortOrders || {}).filter(([categoryId]) => categoryId !== category.id),
          ),
        })))
        setActiveCategory((current) => (current === category.id ? ALL_TAB : current))
        setEditingCategories(false)
        notification.success({ title: "分类已删除", description: category.label })
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      } catch (err) {
        notification.danger({
          title: "删除分类失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setDeletingCategoryId(null)
      }
    },
    [refresh],
  )

  const handleAddJobsToCategory = useCallback(
    async (categoryId: string, jobIds: string[]) => {
      setAddingJobToCategory(true)
      try {
        const updatedJobs: SchedulerJobItem[] = []
        for (const jobId of jobIds) {
          const job = jobs.find((item) => item.id === jobId)
          if (!job) continue
          const nextCategoryIds = job.categories?.includes(categoryId)
            ? job.categories
            : [...(job.categories || []), categoryId]
          const res = await updateSchedulerJobCategories(jobId, nextCategoryIds)
          if (res.ok === false) {
            notification.danger({
              title: "添加 Job 失败",
              description: res.error || `job ${jobId}`,
            })
            return
          }
          updatedJobs.push(res.job ?? { ...job, categories: nextCategoryIds })
        }
        const updatedById = new Map(updatedJobs.map((job) => [job.id, job]))
        setJobs((prev) => prev.map((job) => updatedById.get(job.id) ?? job))
        setActiveCategory(categoryId)
        notification.success({
          title: "Job 已添加",
          description: `${updatedJobs.length} 个 job 已映射到分类`,
        })
        window.setTimeout(() => {
          void refresh(false)
        }, 600)
      } catch (err) {
        notification.danger({
          title: "添加 Job 失败",
          description: err instanceof Error ? err.message : "未知错误",
        })
      } finally {
        setAddingJobToCategory(false)
      }
    },
    [jobs, refresh],
  )

  const summary = useMemo(() => {
    const total = jobs.length
    const running = jobs.filter((job) => Boolean(pickValue<boolean>(job.live, "running"))).length
    const enabled = jobs.filter((job) => job.config_enabled).length
    return { total, running, enabled }
  }, [jobs])

  // 每个 category 的 job 数 (用于 tab 上显示 count; 后端也带, 这里前端再算一份兜底)
  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = { [ALL_TAB]: jobs.length }
    for (const job of jobs) {
      for (const c of job.categories || []) {
        counts[c] = (counts[c] || 0) + 1
      }
    }
    return counts
  }, [jobs])

  // 按 category 切分 jobs: 每个 tab 一个数组.
  //   - ALL_TAB 返回全部 jobs
  //   - 其它 tab 返回 (categories || []).includes(categoryId) 的 jobs
  // category id 来自后端 bigint, 前端必须按 string 处理, 避免 JS number 精度丢失.
  const jobsForCategory = useCallback(
    (key: string): SchedulerJobItem[] => {
      if (key === ALL_TAB) return jobs
      const filtered = jobs.filter((job) => (job.categories || []).includes(key))
      // 按 categorySortOrders 排序 (market_sentiment 等按执行顺序), 无 sortOrder 的放最后
      filtered.sort((a, b) => {
        const sa = a.categorySortOrders?.[key] ?? 999
        const sb = b.categorySortOrders?.[key] ?? 999
        return sa - sb
      })
      return filtered
    },
    [jobs],
  )

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="Scheduler">
      <div className="flex flex-col items-stretch justify-between gap-3 sm:flex-row sm:items-end">
        <div className="min-w-0 space-y-2">
          <div className="inline-flex items-center gap-2 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
            <Settings2 className="size-3.5" />
            Settings · Scheduler
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-4xl">
            调度任务管理
          </h1>
          <p className="max-w-3xl text-sm leading-7 text-muted-foreground sm:text-base">
            统一管理 <code className="break-all rounded bg-muted px-1.5 py-0.5 text-xs">scheduler/jobs.json</code> 注册的调度任务：
            实时查看线程状态、上一轮运行情况，并支持启用 / 禁用 / 启动 / 停止 / 手动触发。
          </p>
        </div>
        <div className="flex flex-col items-stretch gap-1.5 sm:items-end">
          <div className="flex flex-col items-stretch gap-2 sm:flex-row sm:items-center sm:justify-end">
            <DatePicker
              value={marketSentimentTargetDate}
              onChange={(date) => setMarketSentimentTargetDate(toLocalIso(date ?? todayLocal()))}
              clearable={false}
              aria-label="选择市场情绪手动触发日期"
              triggerClassName="hidden h-8 w-[9.5rem] sm:inline-flex"
            />
            <Button
              size="sm"
              variant="secondary"
              className="w-full rounded-xl sm:w-auto"
              onClick={() => void refresh(false)}
              disabled={loading}
            >
              <RefreshCcw className="size-3.5" />
              reload
            </Button>
          </div>
          <div className="text-left text-xs text-muted-foreground sm:text-right">
            {lastUpdated
              ? `最近刷新：${lastUpdated.toLocaleTimeString("zh-CN", { hour12: false })} · 每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`
              : `每 ${REFRESH_INTERVAL_MS / 1000}s 自动刷新`}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-2 sm:gap-3">
        <SummaryCard
          icon={<ListChecks className="size-4" />}
          label="注册任务"
          value={summary.total}
          hint="jobs.json 中注册的 job 数量"
        />
        <SummaryCard
          icon={<Activity className="size-4" />}
          label="运行中"
          value={summary.running}
          hint="线程 alive 状态"
        />
        <SummaryCard
          icon={<CheckCircle2 className="size-4" />}
          label="已启用"
          value={summary.enabled}
          hint="config.enabled = true"
        />
      </div>

      {/* 每日运行统计 */}
      {dailyStats.length > 0 ? (
        <Card className="overflow-hidden border-0 bg-muted/60 py-0 shadow-none">
          <div className="flex flex-col gap-3 px-3 pb-2 pt-4 sm:flex-row sm:items-center sm:justify-between sm:px-4">
            <div className="flex items-center gap-2 text-sm font-medium text-foreground">
              <BarChart3 className="size-4 text-muted-foreground" />
              运行统计
            </div>
            {dailyStatsSummary ? (
              <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1">
                  <span className="inline-block size-2 rounded-full bg-emerald-500/80" />
                  成功 {dailyStatsSummary.total - dailyStatsSummary.failed}
                </span>
                <span className="inline-flex items-center gap-1">
                  <span className="inline-block size-2 rounded-full bg-red-500/80" />
                  失败 {dailyStatsSummary.failed}
                </span>
                <span>
                  {dailyStatsSummary.success_rate >= 1
                    ? "100"
                    : (dailyStatsSummary.success_rate * 100).toFixed(1)}
                  %
                </span>
                <span className="text-muted-foreground/60">· {dailyStatsSummary.total} 次</span>
              </div>
            ) : null}
          </div>
          <CardContent className="px-3 pb-3 sm:px-4">
            <DailyStatsPanel
              items={dailyStats}
              jobs={jobs}
              categories={categories}
              summary={dailyStatsSummary}
              pending={pending}
              onAction={handleAction}
            />
          </CardContent>
        </Card>
      ) : null}

      {/* shadcn Tabs: 按 category 切换. 默认 variant (bg-muted) 保证 trigger 可见,
          每个 tab 一个 <TabsContent> 渲染对应 jobs. 不传 forceMount, 让 shadcn
          按 active tab 自动 mount/unmount. */}
      <Tabs value={activeCategory} onValueChange={setActiveCategory} className="w-full min-w-0">
        <div className="sticky top-0 z-20 -mx-3 flex w-[calc(100%+1.5rem)] items-center justify-between gap-2 overflow-hidden border-b border-border/40 bg-background/95 px-3 py-2 backdrop-blur supports-[backdrop-filter]:bg-background/80 md:-mx-6 md:w-[calc(100%+3rem)] md:px-6">
          <div className="min-w-0 flex-1 overflow-x-auto">
            <TabsList className="min-w-max">
              {/* "全部" tab 固定第一个, 不来自 API */}
              <TabsTrigger value={ALL_TAB}>
                <LayoutGrid className="size-3.5" />
                全部
                <Badge
                  variant={activeCategory === ALL_TAB ? "default" : "secondary"}
                  className="ml-1 px-1.5 py-0 text-[10px]"
                >
                  {categoryCounts[ALL_TAB] ?? 0}
                </Badge>
              </TabsTrigger>
              {categories.map((cat) => {
                const Icon = iconFor(cat.icon_hint)
                const count = categoryCounts[cat.id] ?? cat.count
                const value = String(cat.id)
                return (
                  <div key={cat.id} className="inline-flex h-[calc(100%-1px)] items-center">
                    <TabsTrigger
                      value={value}
                      title={cat.description}
                      className={cn(editingCategories && "rounded-r-none pr-1.5")}
                    >
                      <Icon className="size-3.5" />
                      {cat.label}
                      <Badge
                        variant={activeCategory === value ? "default" : "secondary"}
                        className="ml-1 px-1.5 py-0 text-[10px]"
                      >
                        {count}
                      </Badge>
                    </TabsTrigger>
                    {editingCategories ? (
                      <EditCategoryButton
                        category={cat}
                        count={count}
                        pending={updatingCategoryId === cat.id || deletingCategoryId === cat.id}
                        onSave={handleUpdateCategory}
                        onDelete={(category) => void handleDeleteCategory(category)}
                      />
                    ) : null}
                  </div>
                )
              })}
            </TabsList>
          </div>
          <div className="flex h-9 shrink-0 items-center gap-1 rounded-lg bg-muted p-[3px]">
            <Button
              type="button"
              size="sm"
              variant={editingCategories ? "secondary" : "ghost"}
              className="h-[calc(100%-1px)] rounded-md px-2 text-foreground/70 hover:bg-background hover:text-foreground"
              onClick={() => setEditingCategories((value) => !value)}
            >
              <Pencil className="size-3.5" />
              Edit
            </Button>
            <AddJobToCategoryButton
              categories={categories}
              jobs={jobs}
              activeCategory={activeCategory}
              pending={addingJobToCategory}
              onAdd={handleAddJobsToCategory}
            />
            <CreateCategoryButton pending={creatingCategory} onCreate={handleCreateCategory} />
          </div>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertTriangle className="size-4" />
            <AlertTitle>拉取调度任务失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        {loading && jobs.length === 0 ? (
          <DogLoader overlay size={25} label="正在加载调度任务..." />
        ) : jobs.length === 0 ? (
          <Card className="border-0 bg-muted/60 shadow-none py-0">
            <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
              <RefreshCw className="size-8 text-muted-foreground" />
              <div className="text-sm font-medium text-foreground">
                jobs.json 中还没有注册任何调度任务
              </div>
              <p className="max-w-md text-sm leading-6 text-muted-foreground">
                启动一次 Flask 后，scheduler 会自动把 turnover / auction / application_analysis
                三个内置 job 注册进去；或者手动编辑{" "}
                <code className="rounded bg-muted px-1.5 py-0.5 text-xs">scheduler/jobs.json</code>。
              </p>
            </CardContent>
          </Card>
        ) : (
          <>
            <TabsContent value={ALL_TAB}>
              <JobsGrid
                jobs={jobsForCategory(ALL_TAB)}
                categories={categories}
                pending={pending}
                onAction={handleAction}
                onUpdateCategories={handleUpdateCategories}
                emptyState={
                  <Card className="border-0 bg-muted/60 shadow-none py-0">
                    <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
                      <BarChart3 className="size-8 text-muted-foreground" />
                      <div className="text-sm font-medium text-foreground">
                        还没有注册任何调度任务
                      </div>
                    </CardContent>
                  </Card>
                }
              />
            </TabsContent>

            {categories.map((cat) => {
              const value = String(cat.id)
              return (
                <TabsContent key={cat.id} value={value}>
                  <JobsGrid
                    jobs={jobsForCategory(value)}
                    categories={categories}
                    pending={pending}
                    onAction={handleAction}
                    onUpdateCategories={handleUpdateCategories}
                    emptyState={
                      <Card className="border-0 bg-muted/60 shadow-none py-0">
                        <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
                          <BarChart3 className="size-8 text-muted-foreground" />
                          <div className="text-sm font-medium text-foreground">
                            "{cat.label}" category 没有 job
                          </div>
                        </CardContent>
                      </Card>
                    }
                  />
                </TabsContent>
              )
            })}
          </>
        )}
      </Tabs>
    </WorkspaceShell>
  )
}

/** 一个 tab 的 jobs 网格: jobs 非空时渲染 2 列 JobCard 网格, 空时显示传入的 emptyState.
 *  "全部" tab + 每个 category tab 共用. */
function JobsGrid({
  jobs,
  categories,
  pending,
  onAction,
  onUpdateCategories,
  emptyState,
}: {
  jobs: SchedulerJobItem[]
  categories: SchedulerCategory[]
  pending: ActionState
  onAction: (jobId: string, action: ActionKey) => void
  onUpdateCategories: (jobId: string, categoryIds: string[]) => void
  emptyState: React.ReactNode
}) {
  if (jobs.length === 0) return <>{emptyState}</>
  return (
    <div className="max-h-[52vh] max-w-full overflow-y-auto pr-1 md:max-h-none md:columns-2 md:gap-4 md:overflow-visible md:pr-0 2xl:columns-3">
      {jobs.map((job) => (
        <JobCard
          key={job.id}
          job={job}
          categories={categories}
          pending={pending[job.id] ?? null}
          onAction={onAction}
          onUpdateCategories={onUpdateCategories}
        />
      ))}
    </div>
  )
}

function DailyStatsPanel({
  items,
  jobs,
  categories,
  summary,
  pending,
  onAction,
}: {
  items: SchedulerDailyStatItem[]
  jobs: SchedulerJobItem[]
  categories: SchedulerCategory[]
  summary: { total: number; failed: number; success_rate: number } | null
  pending: ActionState
  onAction: (jobId: string, action: ActionKey) => void
}) {
  const [activeRunStat, setActiveRunStat] = useState<TodayRunStatKey | null>(null)
  const orderedItems = useMemo(
    () => [...items].sort((a, b) => a.date.localeCompare(b.date)),
    [items],
  )
  const chartData = useMemo(
    () => orderedItems.map((item) => ({
      date: item.date,
      success: item.success || 0,
      failed: item.failed || 0,
      skipped: item.skipped || 0,
    })),
    [orderedItems],
  )
  const todayKey = toLocalIso(todayLocal())
  const todayItem = orderedItems.find((item) => item.date === todayKey) ?? orderedItems.at(-1)
  const recentItems = orderedItems.slice(-7)
  const recentAverage = recentItems.length
    ? recentItems.reduce((sum, item) => sum + (item.total || 0), 0) / recentItems.length
    : 0
  const todayExpected = jobs.reduce((sum, job) => sum + todayStatNumber(job, "expected_count"), 0)
  const todayActual = jobs.reduce((sum, job) => sum + todayStatNumber(job, "actual_count"), 0)
  const todayFailed = jobs.reduce((sum, job) => sum + todayStatNumber(job, "failed_count"), 0)
  const enabledCount = jobs.filter((job) => job.config_enabled).length
  const disabledCount = Math.max(0, jobs.length - enabledCount)
  const todaySuccessRate = todayItem && todayItem.total > 0
    ? todayItem.success / todayItem.total
    : null
  const expectedRate = todayExpected > 0 ? Math.min(todayActual / todayExpected, 1) : 0
  const scheduleWindows = useMemo(() => buildScheduleWindows(jobs), [jobs])
  const categoryExecutionData = useMemo(() => {
    const categoryById = new Map(categories.map((category) => [category.id, category]))
    const bucket = new Map<string, { label: string; scheduledJobs: number; totalJobs: number }>()

    for (const category of categories) {
      bucket.set(category.id, {
        label: category.label,
        scheduledJobs: 0,
        totalJobs: 0,
      })
    }

    for (const job of jobs) {
      const needsRunToday = (job.today_stats?.expected_count ?? 0) > 0

      for (const categoryId of job.categories || []) {
        const category = categoryById.get(categoryId)
        if (!category) continue
        const current = bucket.get(categoryId) ?? {
          label: category.label,
          scheduledJobs: 0,
          totalJobs: 0,
        }
        current.totalJobs += 1
        if (needsRunToday) {
          current.scheduledJobs += 1
        }
        bucket.set(categoryId, current)
      }
    }

    return Array.from(bucket.values())
      .sort((a, b) => (b.scheduledJobs + b.totalJobs) - (a.scheduledJobs + a.totalJobs))
  }, [categories, jobs])
  const previousAverage = orderedItems.length > 1
    ? orderedItems.slice(0, -1).slice(-7).reduce((sum, item) => sum + (item.total || 0), 0) / Math.min(7, orderedItems.length - 1)
    : 0
  const trendDelta = todayItem ? todayItem.total - previousAverage : 0
  const progressLabel = todayExpected > 0 ? `${Math.round(expectedRate * 100)}%` : "--"
  const rangeSuccessRate = summary ? `${(summary.success_rate * 100).toFixed(1)}%` : "--"
  const runStatItems = useMemo<TodayRunStatConfig[]>(() => [
    {
      key: "success",
      label: "成功",
      value: todayItem?.success ?? 0,
      description: "今日有成功执行记录的 Job",
      className: "text-emerald-600",
      jobs: jobs.filter((job) => todaySuccessCount(job) > 0),
    },
    {
      key: "failed",
      label: "失败",
      value: todayFailed,
      description: "今日产生失败或部分失败记录的 Job",
      className: "text-red-600",
      jobs: jobs.filter((job) => todayStatNumber(job, "failed_count") > 0),
    },
    {
      key: "skipped",
      label: "跳过",
      value: todayItem?.skipped ?? 0,
      description: "今日被调度但跳过执行的 Job",
      className: "text-amber-600",
      jobs: jobs.filter((job) => todayStatNumber(job, "skipped_count") > 0),
    },
    {
      key: "enabled",
      label: "启用",
      value: enabledCount,
      description: "当前配置为启用的 Job",
      className: "text-emerald-600",
      jobs: jobs.filter((job) => job.config_enabled),
    },
    {
      key: "disabled",
      label: "禁用",
      value: disabledCount,
      description: "当前配置为禁用的 Job",
      className: "text-red-600",
      jobs: jobs.filter((job) => !job.config_enabled),
    },
    {
      key: "manual",
      label: "手动",
      value: todayItem?.manual_count ?? 0,
      description: "今日由用户手动触发过的 Job",
      className: "text-blue-600",
      jobs: jobs.filter((job) => todayStatNumber(job, "manual_count") > 0),
    },
    {
      key: "auto",
      label: "自动",
      value: todayItem?.auto_count ?? 0,
      description: "今日由 cron 自动触发过的 Job",
      className: "text-violet-600",
      jobs: jobs.filter((job) => todayStatNumber(job, "auto_count") > 0),
    },
  ], [disabledCount, enabledCount, jobs, todayFailed, todayItem?.auto_count, todayItem?.manual_count, todayItem?.skipped, todayItem?.success])
  const activeRunStatConfig = runStatItems.find((item) => item.key === activeRunStat) ?? null

  if (orderedItems.length === 0) {
    return null
  }

  return (
    <div className="grid gap-3 xl:grid-cols-[1.33fr_1.33fr_1.33fr_5fr]">
      <div className="rounded-lg bg-background px-4 py-3">
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2 text-sm font-medium text-foreground">
              <Activity className="size-4 text-blue-600" />
              今日执行
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-3xl font-semibold tabular-nums leading-none text-foreground">
                {formatInteger(todayActual)}
              </span>
              <span className="text-xs text-muted-foreground">/ {formatInteger(todayExpected)} 次</span>
            </div>
          </div>
          <div
            className="grid size-16 shrink-0 place-items-center rounded-full shadow-inner"
            style={{
              background: `conic-gradient(#2563eb ${Math.round(expectedRate * 360)}deg, hsl(var(--muted)) 0deg)`,
            }}
          >
            <div className="grid size-12 place-items-center rounded-full bg-background text-sm font-semibold tabular-nums text-foreground">
              {progressLabel}
            </div>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-3 gap-1.5">
          {runStatItems.slice(0, 3).map((item) => (
            <RunStatChip
              key={item.key}
              label={item.label}
              value={item.value}
              className={item.className}
              onClick={() => setActiveRunStat(item.key)}
            />
          ))}
        </div>

        <div className="mt-1.5 grid grid-cols-4 gap-1.5">
          {runStatItems.slice(3).map((item) => (
            <RunStatChip
              key={item.key}
              label={item.label}
              value={item.value}
              className={item.className}
              onClick={() => setActiveRunStat(item.key)}
            />
          ))}
        </div>

        <div className="mt-3 rounded-md bg-muted/55 px-3 py-2">
          <div className="flex items-center justify-between gap-2 text-xs">
            <span className="text-muted-foreground">今日质量</span>
            <span className="font-semibold tabular-nums text-foreground">
              {todaySuccessRate == null ? "--" : `${(todaySuccessRate * 100).toFixed(1)}%`}
            </span>
          </div>
          <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-background">
            <div
              className={cn(
                "h-full rounded-full",
                todaySuccessRate != null && todaySuccessRate < 0.95 ? "bg-amber-500" : "bg-emerald-500",
              )}
              style={{ width: `${Math.max(0, Math.min(100, (todaySuccessRate ?? 0) * 100))}%` }}
            />
          </div>
        </div>
      </div>

      <TodayJobsDialog
        open={Boolean(activeRunStatConfig)}
        onOpenChange={(open) => {
          if (!open) setActiveRunStat(null)
        }}
        stat={activeRunStatConfig}
        categories={categories}
        pending={pending}
        onAction={onAction}
      />

      <ScheduleWindowList windows={scheduleWindows} />

      <SchedulerCategoryExecutionRadar data={categoryExecutionData} />

      <SchedulerDailyStatsAreaChart
        data={chartData}
        average={recentAverage}
        delta={trendDelta}
        successRate={rangeSuccessRate}
        className="min-w-0 border-0 py-0 shadow-none"
      />
    </div>
  )
}

function TodayJobsDialog({
  open,
  onOpenChange,
  stat,
  categories,
  pending,
  onAction,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  stat: TodayRunStatConfig | null
  categories: SchedulerCategory[]
  pending: ActionState
  onAction: (jobId: string, action: ActionKey) => void
}) {
  const jobs = stat?.jobs ?? []
  const canTrigger = stat?.key === "failed"

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[86vh] w-[calc(100vw-1rem)] overflow-hidden p-0 sm:max-w-[1350px]">
        <DialogHeader className="border-b border-border/40 px-5 py-4 text-left">
          <DialogTitle className="flex items-center gap-2 text-base">
            <Activity className="size-4 text-blue-600" />
            今日执行 · {stat?.label ?? "Job"}
          </DialogTitle>
          <DialogDescription className="text-xs">
            {stat?.description ?? "今日执行明细"}，命中 {formatInteger(jobs.length)} 个 Job，统计值 {formatInteger(stat?.value ?? 0)}。
          </DialogDescription>
        </DialogHeader>

        <div className="max-h-[68vh] overflow-y-auto px-5 py-4">
          {jobs.length > 0 ? (
            <div className="space-y-2">
              {jobs.map((job) => (
                <TodayJobListItem
                  key={job.id}
                  job={job}
                  categories={categories}
                  pending={pending[job.id] ?? null}
                  canTrigger={canTrigger}
                  onAction={onAction}
                />
              ))}
            </div>
          ) : (
            <div className="rounded-lg bg-muted/50 px-4 py-8 text-center text-sm text-muted-foreground">
              当前筛选下没有匹配的 Job
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}

function TodayJobListItem({
  job,
  categories,
  pending,
  canTrigger,
  onAction,
}: {
  job: SchedulerJobItem
  categories: SchedulerCategory[]
  pending: ActionKey | null
  canTrigger: boolean
  onAction: (jobId: string, action: ActionKey) => void
}) {
  const [failureExpanded, setFailureExpanded] = useState(false)
  const live = job.live || {}
  const isRunning = Boolean(pickValue<boolean>(live, "running"))
  const lastRunSummary = job.last_run
  const failedCount = todayStatNumber(job, "failed_count")
  const failureText = job.today_stats?.failed_error || job.today_stats?.failed_message || lastRunSummary?.last_error
  const failedAt = job.today_stats?.failed_at || (
    failedCount > 0 && lastRunSummary?.last_status
      && ["failed", "error", "partial_failed"].includes(lastRunSummary.last_status.toLowerCase())
      ? lastRunSummary.last_run_at
      : null
  )
  const failureCopyText = [
    `job name: ${job.name}`,
    `job code: ${job.id}`,
    `错误信息: ${failureText || "本次失败记录没有返回错误信息"}`,
  ].join("\n")
  const handleCopyFailure = async (event: React.MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation()
    try {
      await navigator.clipboard.writeText(failureCopyText)
      notification.success({ title: "已复制 Job 错误信息", description: job.name })
    } catch (error) {
      notification.danger({
        title: "复制失败",
        description: error instanceof Error ? error.message : "clipboard write failed",
      })
    }
  }
  const stats = [
    { label: "预计", value: todayStatNumber(job, "expected_count"), className: "text-foreground" },
    { label: "已执行", value: todayStatNumber(job, "actual_count"), className: "text-blue-600" },
    { label: "成功", value: todaySuccessCount(job), className: "text-emerald-600" },
    { label: "失败", value: failedCount, className: "text-red-600" },
    { label: "跳过", value: todayStatNumber(job, "skipped_count"), className: "text-amber-600" },
    { label: "手动", value: todayStatNumber(job, "manual_count"), className: "text-blue-600" },
    { label: "自动", value: todayStatNumber(job, "auto_count"), className: "text-violet-600" },
  ]
  const categoryLabelById = new Map(categories.map((category) => [category.id, category.label]))
  const triggerPending = pending === "trigger"

  return (
    <div className="rounded-lg bg-muted/45 px-3 py-3">
      <div className="flex min-w-0 flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 space-y-2">
          <div className="flex min-w-0 flex-wrap items-center gap-2">
            <span className="min-w-0 truncate text-sm font-semibold text-foreground">{job.name}</span>
            <Badge
              variant="secondary"
              className={cn("px-2 py-0 text-[10px]", jobStateClass(isRunning, job.config_enabled))}
            >
              {jobStateLabel(isRunning, job.config_enabled)}
            </Badge>
            <Badge
              variant={job.config_enabled ? "outline" : "destructive"}
              className={cn(
                "px-2 py-0 text-[10px]",
                job.config_enabled && "border-emerald-500/30 bg-emerald-500/10 text-emerald-600",
              )}
            >
              {job.config_enabled ? "已启用" : "已禁用"}
            </Badge>
            {canTrigger ? (
              <Button
                type="button"
                size="sm"
                variant="secondary"
                className="h-7 rounded-xl px-2 text-[11px]"
                disabled={triggerPending}
                onClick={() => onAction(job.id, "trigger")}
              >
                <Zap className={cn("size-3", triggerPending && "animate-pulse")} />
                {triggerPending ? "执行中…" : "再次执行"}
              </Button>
            ) : null}
          </div>
          <div className="break-all font-mono text-[10px] text-muted-foreground">{job.id}</div>
          {(job.categories || []).length > 0 ? (
            <div className="flex flex-wrap gap-1">
              {(job.categories || []).map((categoryId) => (
                <Badge key={categoryId} variant="outline" className="px-1.5 py-0 text-[10px] font-normal">
                  {categoryLabelById.get(categoryId) ?? categoryId}
                </Badge>
              ))}
            </div>
          ) : null}
        </div>

        <div className="grid min-w-0 grid-cols-4 gap-1.5 sm:grid-cols-7 lg:w-[440px]">
          {stats.map((item) => (
            <div key={item.label} className="rounded-md bg-background/70 px-2 py-1.5 text-center">
              <div className={cn("text-sm font-semibold tabular-nums leading-none", item.className)}>
                {formatInteger(item.value)}
              </div>
              <div className="mt-1 text-[10px] text-muted-foreground">{item.label}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-muted-foreground">
        <span>上次 {formatDateTime(lastRunSummary?.last_run_at)}</span>
        {lastRunSummary?.last_status ? (
          <Badge
            variant={statusBadgeVariant(lastRunSummary.last_status)}
            className={cn(statusBadgeClass(lastRunSummary.last_status), "px-1.5 py-0 text-[10px]")}
          >
            {lastRunSummary.last_status}
          </Badge>
        ) : null}
        <span>耗时 {formatSeconds(lastRunSummary?.last_duration_seconds)}</span>
      </div>

      {failedCount > 0 ? (
        <div
          role="button"
          tabIndex={0}
          className="mt-2 cursor-pointer rounded-md bg-red-500/10 px-3 py-2 text-[11px] leading-5 text-red-700 transition-colors hover:bg-red-500/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500/40 dark:text-red-400"
          onClick={() => setFailureExpanded((value) => !value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault()
              setFailureExpanded((value) => !value)
            }
          }}
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex min-w-0 flex-wrap items-center gap-2 font-medium">
              <AlertTriangle className="size-3.5" />
              <span>失败信息</span>
              {failedAt ? (
                <span className="font-normal text-red-700/70 dark:text-red-400/70">
                  {formatDateTime(failedAt)}
                </span>
              ) : null}
            </div>
            <div className="flex items-center gap-1">
              <Button
                type="button"
                size="sm"
                variant="ghost"
                className="h-6 px-2 text-[11px] text-red-700 hover:bg-red-500/10 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300"
                onClick={handleCopyFailure}
              >
                <Copy className="size-3" />
                Copy
              </Button>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                className="h-6 px-2 text-[11px] text-red-700 hover:bg-red-500/10 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300"
              >
                {failureExpanded ? (
                  <ChevronDown className="size-3" />
                ) : (
                  <ChevronRight className="size-3" />
                )}
                {failureExpanded ? "收起" : "展开"}
              </Button>
            </div>
          </div>
          <div className={cn(
            "mt-1 whitespace-pre-wrap break-all",
            !failureExpanded && "line-clamp-2",
          )}>
            {failureText || "本次失败记录没有返回错误信息"}
          </div>
        </div>
      ) : null}
    </div>
  )
}

function ScheduleWindowList({ windows }: { windows: ScheduleWindowSummary[] }) {
  const visible = windows.slice(0, 6)
  const remaining = windows.length - visible.length
  const total = windows.reduce((sum, item) => sum + item.count, 0)

  return (
    <div className="rounded-lg bg-background px-4 py-3">
      <div className="mb-3 flex items-start justify-between gap-2">
        <div>
          <div className="flex items-center gap-2 text-sm font-medium text-foreground">
            <Clock className="size-4 text-amber-600" />
            预计窗口
          </div>
          <div className="mt-1 text-xs text-muted-foreground">今日时间段分布</div>
        </div>
        <div className="rounded-md bg-muted px-2 py-1 text-sm font-semibold tabular-nums text-foreground">
          {formatInteger(total)}
        </div>
      </div>
      {visible.length > 0 ? (
        <div className="space-y-2">
          {visible.map((item) => (
            <div key={item.label} className="grid grid-cols-[5.5rem_minmax(0,1fr)_2.75rem] items-center gap-2">
              <div className="truncate font-mono text-[11px] text-foreground">{item.label}</div>
              <div className="h-6 min-w-0 overflow-hidden rounded bg-muted">
                <div
                  className="flex h-full items-center justify-end rounded bg-blue-500/75 pr-1"
                  style={{ width: `${Math.max(8, item.ratio * 100)}%` }}
                >
                  <span className="hidden text-[10px] font-medium tabular-nums text-white sm:inline">
                    {Math.round(item.ratio * 100)}%
                  </span>
                </div>
              </div>
              <div className="text-right text-sm font-semibold tabular-nums text-foreground">{formatInteger(item.count)}</div>
            </div>
          ))}
          {remaining > 0 ? (
            <div className="pt-0.5 text-[11px] text-muted-foreground">还有 {remaining} 个时间段</div>
          ) : null}
        </div>
      ) : (
        <div className="rounded-md bg-muted/50 px-3 py-3 text-xs text-muted-foreground">
          今日没有可推算的执行窗口
        </div>
      )}
    </div>
  )
}

function RunStatChip({
  label,
  value,
  className,
  onClick,
}: {
  label: string
  value: number
  className?: string
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      className="min-w-0 rounded-md bg-muted/55 px-2 py-2 text-center transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      onClick={onClick}
    >
      <div className={cn("truncate text-base font-semibold tabular-nums leading-none", className)}>
        {formatInteger(value)}
      </div>
      <div className="mt-1 truncate text-[11px] text-muted-foreground">{label}</div>
    </button>
  )
}

function SummaryCard({
  icon,
  label,
  value,
  hint,
}: {
  icon: React.ReactNode
  label: string
  value: number
  hint: string
}) {
  return (
    <Card className="border-0 bg-muted/60 shadow-none py-0">
      <CardContent className="flex flex-col items-center gap-1 px-2 py-3 text-center sm:flex-row sm:gap-3 sm:px-4 sm:py-4 sm:text-left">
        <div className="flex size-7 items-center justify-center rounded-xl bg-muted text-foreground sm:size-9">
          {icon}
        </div>
        <div className="min-w-0">
          <div className="text-lg font-semibold leading-none text-foreground sm:text-2xl">{value}</div>
          <div className="mt-1 whitespace-nowrap text-[10px] text-muted-foreground sm:text-xs">{label}</div>
        </div>
        <div className="ml-auto hidden max-w-[48%] text-right text-xs text-muted-foreground sm:block sm:max-w-none">{hint}</div>
      </CardContent>
    </Card>
  )
}
