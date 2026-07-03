import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import { cynexusResult } from "@/services/common/cynexus-result"

const SCHEDULER_API_BASE = `${SERVICE_PREFIX.core}/scheduler/management`

export interface SchedulerProgress {
  schemaVersion?: number
  type?: string
  status?: string
  percent?: number | null
  currentStep?: string | null
  currentStepProgress?: SchedulerCurrentStepProgress | null
  completedSteps?: string[]
  stepResults?: SchedulerChainStepResult[]
  message?: string | null
  metrics?: Record<string, unknown>
  payload?: Record<string, unknown>
  updatedAt?: string | null
}

export interface SchedulerCurrentStepProgress {
  step?: string
  unitLabel?: string
  processedItems?: number
  totalItems?: number
  processedFiles?: number
  totalFiles?: number
  rowsRead?: number
  rowsWritten?: number
}

export interface SchedulerChainStepResult {
  jobId: string
  ok: boolean
  lastStatus?: string
  lastRunError?: string
  lastMessage?: string
}

export interface SchedulerResultSummary {
  schemaVersion?: number
  type?: string
  status?: string
  message?: string | null
  completedCount?: number | null
  failedCount?: number | null
  metrics?: Record<string, unknown>
  payload?: Record<string, unknown>
  updatedAt?: string | null
}

export interface SchedulerJobItem {
  id: string
  name: string
  description?: string
  config_file?: string
  service_module?: string
  service_class?: string
  registered_at?: string
  categories?: string[]
  categorySortOrders?: Record<string, number>
  supports_enable: boolean
  enabled: boolean
  config_enabled: boolean
  config: Record<string, unknown>
  live: Record<string, unknown>
  progress?: SchedulerProgress | null
  last_run?: {
    last_run_at: string | null
    last_status: string | null
    last_targets_processed: number | null
    last_duration_seconds: number | null
    last_error: string | null
    total_runs: number | null
  }
}

export interface SchedulerJobsResponse {
  ok: boolean
  items: SchedulerJobItem[]
  count: number
  error?: string
}

export interface SchedulerCategory {
  id: string
  label: string
  icon_hint: string
  sort_order: number
  description?: string
  count: number
}

export interface SchedulerCategoriesResponse {
  ok: boolean
  items: SchedulerCategory[]
  count: number
  error?: string
}

export interface SchedulerDailyStatItem {
  date: string
  total: number
  success: number
  failed: number
  skipped: number
}

export interface SchedulerDailyStatsSummary {
  total: number
  failed: number
  success_rate: number
}

export interface SchedulerDailyStatsResponse {
  ok: boolean
  items: SchedulerDailyStatItem[]
  summary: SchedulerDailyStatsSummary
  error?: string
}

export interface SchedulerJobActionResponse {
  ok: boolean
  job_id?: string
  enabled?: boolean
  status?: string
  run_id?: string
  accepted?: boolean
  count?: number
  failed_count?: number
  items?: Array<{
    jobId?: string
    ok?: boolean
    lastStatus?: string
    lastRunError?: string
    lastMessage?: string
  }>
  config?: Record<string, unknown>
  error?: string
}

export interface SchedulerJobHistoryItem {
  id?: string
  start_at: string
  end_at: string
  trigger_type: "auto" | "manual" | string
  status: "success" | "failed" | "skipped" | "running" | "processing" | string
  error: string | null
  message?: string | null
  result_summary?: SchedulerResultSummary | null
  duration_seconds: number | null
  target_count?: number
  succeeded?: number
}

export interface SchedulerJobHistoryResponse {
  ok: boolean
  job_id?: string
  items: SchedulerJobHistoryItem[]
  count: number
  error?: string
}

export async function fetchSchedulerDailyStats(days = 14): Promise<SchedulerDailyStatsResponse> {
  const res = await fetchWithRetry(
    `${SCHEDULER_API_BASE}/stats/daily?days=${encodeURIComponent(String(days))}`,
    { cache: "no-store" },
  )
  return cynexusResult<SchedulerDailyStatsResponse>(res)
}

export async function fetchSchedulerCategories(): Promise<SchedulerCategoriesResponse> {
  const res = await fetchWithRetry(`${SCHEDULER_API_BASE}/categories`, { cache: "no-store" })
  return cynexusResult<SchedulerCategoriesResponse>(res)
}

export async function fetchSchedulerJobs(): Promise<SchedulerJobsResponse> {
  const res = await fetchWithRetry(`${SCHEDULER_API_BASE}/jobs`, { cache: "no-store" })
  return cynexusResult<SchedulerJobsResponse>(res)
}

export async function fetchSchedulerJobHistory(
  jobId: string,
  limit = 50,
): Promise<SchedulerJobHistoryResponse> {
  const res = await fetchWithRetry(
    `${SCHEDULER_API_BASE}/jobs/${encodeURIComponent(jobId)}/history?limit=${encodeURIComponent(String(limit))}`,
    { cache: "no-store" },
  )
  return cynexusResult<SchedulerJobHistoryResponse>(res)
}

async function postSchedulerAction(
  jobId: string,
  action: "enable" | "disable" | "trigger" | "start" | "stop",
  body?: Record<string, unknown>,
): Promise<SchedulerJobActionResponse> {
  const res = await fetchWithRetry(`${SCHEDULER_API_BASE}/jobs/${encodeURIComponent(jobId)}/${action}`, {
    method: "POST",
    ...(body
      ? {
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }
      : {}),
  })
  return cynexusResult<SchedulerJobActionResponse>(res)
}

export const enableSchedulerJob = (jobId: string) => postSchedulerAction(jobId, "enable")
export const disableSchedulerJob = (jobId: string) => postSchedulerAction(jobId, "disable")

export const triggerSchedulerJob = (jobId: string, options?: { targetDate?: string | null }) =>
  postSchedulerAction(
    jobId,
    "trigger",
    options?.targetDate ? { target_date: options.targetDate } : undefined,
  )

export const startSchedulerJob = (jobId: string) => postSchedulerAction(jobId, "start")
export const stopSchedulerJob = (jobId: string) => postSchedulerAction(jobId, "stop")

export async function deleteSchedulerJob(jobId: string): Promise<SchedulerJobActionResponse> {
  const res = await fetchWithRetry(`${SCHEDULER_API_BASE}/jobs/${encodeURIComponent(jobId)}`, {
    method: "DELETE",
  })
  return cynexusResult<SchedulerJobActionResponse>(res)
}
