import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import { cynexusResult } from "@/services/common/cynexus-result"
import type { ApplicationAnalysisDailySnapshot } from "@/views/application-analysis/lib/types"
import type {
  ApplicationAnalysisResponse,
  StockAdjust,
  StockTargetType,
} from "@/views/stock-chart/lib/types"

const APPLICATION_ANALYSIS_API_BASE = `${SERVICE_PREFIX.ai}/application-analysis`

export interface ApplicationAnalysisTarget {
  id: string
  target_type: StockTargetType | string
  symbol: string
  name: string
  adjust: string
  enabled: boolean
  interval_minutes: number
  tags?: string[]
  last_updated_at?: string | null
  last_result_path?: string | null
}

export interface ApplicationAnalysisSchedulerStatus {
  running: boolean
  started_at?: string | null
  tick_count?: number
  runs_count?: number
  enabled_target_count?: number
  total_target_count?: number
  inflight?: Record<string, string>
  last_run?: Record<string, { status: string; finished_at?: string; elapsed_seconds?: number; overlay_count?: number; error?: string }>
}

export interface ApplicationAnalysisResultFile {
  filename: string
  path: string
  size_bytes: number
  updated_at: string
}

export interface ApplicationAnalysisDailySnapshotFile {
  filename: string
  path: string
  date: string
  size_bytes: number
  updated_at: string
}

export interface ApplicationAnalysisDailySnapshotResponse {
  ok: boolean
  error?: string
  target_id?: string
  date?: string
  snapshot_path?: string
  short_term_trend?: Record<string, unknown> | null
  current_situation?: Record<string, unknown> | null
  updated?: boolean
  skip_reason?: string
  snapshots?: ApplicationAnalysisDailySnapshotFile[]
  snapshot?: {
    target: Record<string, unknown>
    date: string
    updated_at: string
    short_term_trend?: Record<string, unknown> | null
    current_situation?: Record<string, unknown> | null
    summary?: Record<string, unknown> | null
  }
}

export type ApplicationAnalysisRecent30FullItem = ApplicationAnalysisDailySnapshotFile & {
  snapshot: ApplicationAnalysisDailySnapshot
}

export type ApplicationAnalysisRecent30FullResponse = {
  ok: boolean
  target_id?: string
  items?: ApplicationAnalysisRecent30FullItem[]
  error?: string
}

export async function runApplicationAnalysis(params: {
  targetType: StockTargetType
  symbol: string
  name: string
  adjust: StockAdjust
}): Promise<ApplicationAnalysisResponse> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      target_type: params.targetType,
      symbol: params.symbol,
      name: params.name,
      adjust: params.adjust,
    }),
  })
  return cynexusResult<ApplicationAnalysisResponse>(res)
}

export async function fetchApplicationAnalysisTargets(): Promise<{
  items: ApplicationAnalysisTarget[]
  config: Record<string, unknown>
}> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/targets`, { cache: "no-store" })
  const data = await cynexusResult<{ items?: ApplicationAnalysisTarget[]; config?: Record<string, unknown> }>(res)
  return {
    items: data.items ?? [],
    config: data.config ?? {},
  }
}

export async function saveApplicationAnalysisTargets(payload: {
  horizon: Record<string, number>
  items: ApplicationAnalysisTarget[]
}): Promise<{ ok: boolean; config: Record<string, unknown> }> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/targets`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  const data = await cynexusResult<{ config?: Record<string, unknown> }>(res)
  return { ok: true, config: data.config ?? {} }
}

export async function fetchApplicationAnalysisResult(
  targetId: string,
): Promise<ApplicationAnalysisResponse & { _meta_result_path: string; _meta_history: ApplicationAnalysisResultFile[] }> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/results/${encodeURIComponent(targetId)}`, {
    cache: "no-store",
  })
  return cynexusResult<ApplicationAnalysisResponse & { _meta_result_path: string; _meta_history: ApplicationAnalysisResultFile[] }>(res)
}

export async function triggerApplicationAnalysis(
  targetId: string | null,
): Promise<{ ok: boolean; target_id?: string; error?: string; items?: unknown[] }> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ target_id: targetId || null }),
  })
  return cynexusResult<{ ok: boolean; target_id?: string; error?: string; items?: unknown[] }>(res)
}

export async function fetchApplicationAnalysisSchedulerStatus(): Promise<ApplicationAnalysisSchedulerStatus> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/scheduler`, { cache: "no-store" })
  return cynexusResult<ApplicationAnalysisSchedulerStatus>(res)
}

export async function refreshApplicationAnalysisRecent30(
  targetId: string,
  date?: string,
): Promise<ApplicationAnalysisDailySnapshotResponse> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/recent30/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(date ? { target_id: targetId, date } : { target_id: targetId }),
  })
  return cynexusResult<ApplicationAnalysisDailySnapshotResponse>(res)
}

export async function listApplicationAnalysisRecent30(
  targetId: string,
  limit = 60,
): Promise<ApplicationAnalysisDailySnapshotResponse> {
  const res = await fetchWithRetry(
    `${APPLICATION_ANALYSIS_API_BASE}/recent30/${encodeURIComponent(targetId)}?limit=${encodeURIComponent(String(limit))}`,
    { cache: "no-store" },
  )
  return cynexusResult<ApplicationAnalysisDailySnapshotResponse>(res)
}

export async function readApplicationAnalysisRecent30(
  targetId: string,
  date: string,
): Promise<ApplicationAnalysisDailySnapshotResponse> {
  const res = await fetchWithRetry(
    `${APPLICATION_ANALYSIS_API_BASE}/recent30/${encodeURIComponent(targetId)}/${encodeURIComponent(date)}`,
    { cache: "no-store" },
  )
  return cynexusResult<ApplicationAnalysisDailySnapshotResponse>(res)
}

export async function listApplicationAnalysisRecent30Full(
  targetId: string,
  limit = 60,
): Promise<ApplicationAnalysisRecent30FullResponse> {
  const res = await fetchWithRetry(
    `${APPLICATION_ANALYSIS_API_BASE}/recent30/${encodeURIComponent(targetId)}/full?limit=${encodeURIComponent(String(limit))}`,
    { cache: "no-store" },
  )
  return cynexusResult<ApplicationAnalysisRecent30FullResponse>(res)
}

export async function controlApplicationAnalysisScheduler(
  action: "start" | "stop",
): Promise<{ ok: boolean; status: ApplicationAnalysisSchedulerStatus }> {
  const res = await fetchWithRetry(`${APPLICATION_ANALYSIS_API_BASE}/scheduler/${action}`, { method: "POST" })
  const data = await cynexusResult<{ ok: boolean; status: ApplicationAnalysisSchedulerStatus; error?: string }>(res)
  if (!data.ok) {
    throw new Error(data.error || "Java Application Analysis scheduler 未接入")
  }
  return data
}
