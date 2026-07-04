import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const DATA_SOURCE_API_BASE = `${SERVICE_PREFIX.market}/data-source`

export type DataSourceStatus =
  | "fresh"
  | "lagging"
  | "stale"
  | "empty"
  | "untracked"
  | "error"

export interface DataSourceFreshnessSummary {
  total: number
  fresh: number
  lagging: number
  stale: number
  empty: number
  untracked: number
  error: number
}

export interface DataSourceFreshnessItem {
  sourceType: "postgres" | "clickhouse" | string
  databaseName?: string | null
  schemaName?: string | null
  tableName: string
  effectiveDateColumn?: string | null
  latestEffectiveDate?: string | null
  latestUpdatedAt?: string | null
  rowCount?: number | null
  lagDays?: number | null
  status: DataSourceStatus
  error?: string | null
  businessLogic?: string | null
  usedBy?: string[] | null
  updateJob?: string | null
  dataSource?: string | null
}

export interface DataSourceFreshnessResponse {
  ok: boolean
  error?: string | null
  generatedAt: string
  summary: DataSourceFreshnessSummary
  items: DataSourceFreshnessItem[]
}

export async function fetchDataSourceFreshness(): Promise<DataSourceFreshnessResponse> {
  const res = await fetchWithRetry(`${DATA_SOURCE_API_BASE}/freshness`, { cache: "no-store" })
  return cynexusResult<DataSourceFreshnessResponse>(res)
}
