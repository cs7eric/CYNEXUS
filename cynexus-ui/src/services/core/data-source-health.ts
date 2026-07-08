import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const DATA_SOURCE_HEALTH_API_BASE = `${SERVICE_PREFIX.core}/data-source-health`

export type DataSourceHealthStatus = "healthy" | "degraded" | "failed" | "skipped" | string

export interface DataSourceHealthSummary {
  total: number
  healthy: number
  degraded: number
  failed: number
  skipped: number
  successRate?: number | null
  avgLatencyMs?: number | null
}

export interface DataSourceHealthRun {
  runId: string
  triggerType?: string | null
  status: string
  startedAt?: string | null
  finishedAt?: string | null
  durationMs?: number | null
}

export interface DataSourceHealthItem {
  sourceCode: string
  sourceName: string
  sourceType: string
  category: string
  provider: string
  endpoint?: string | null
  status: DataSourceHealthStatus
  connectivity: boolean
  authenticityStatus?: string | null
  latencyMs?: number | null
  sampleCount?: number | null
  successRate24h?: number | null
  checkedAt?: string | null
  errorMessage?: string | null
  checks?: string | null
  rawPayload?: string | null
}

export interface DataSourceHealthResponse {
  ok: boolean
  generatedAt: string
  durationMs?: number | null
  includeSlow: boolean
  summary: DataSourceHealthSummary
  run?: DataSourceHealthRun | null
  items: DataSourceHealthItem[]
}

export async function fetchDataSourceHealthLatest(): Promise<DataSourceHealthResponse> {
  const res = await fetchWithRetry(`${DATA_SOURCE_HEALTH_API_BASE}/latest`, { cache: "no-store" })
  return cynexusResult<DataSourceHealthResponse>(res)
}

export async function runDataSourceHealthCheck(includeSlow = false): Promise<DataSourceHealthResponse> {
  const res = await fetchWithRetry(`${DATA_SOURCE_HEALTH_API_BASE}/checks/run?includeSlow=${includeSlow}`, {
    method: "POST",
    cache: "no-store",
  })
  return cynexusResult<DataSourceHealthResponse>(res)
}
