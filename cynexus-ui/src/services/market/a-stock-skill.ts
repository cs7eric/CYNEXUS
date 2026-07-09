import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const A_STOCK_SKILL_API_BASE = `${SERVICE_PREFIX.market}/a-stock-skill`

export interface AStockSkillDataset {
  code: string
  name: string
  groupName: string
  description?: string | null
  store: "postgres" | "clickhouse" | string
  databaseName?: string | null
  schemaName?: string | null
  tableName: string
  source?: string | null
  grain?: string | null
  codeColumn?: string | null
  dateColumn?: string | null
  defaultSort?: string | null
  columns: string[]
  rowCount?: number | null
  latestAt?: string | null
  error?: string | null
}

export interface AStockSkillDatasetList {
  generatedAt: string
  datasets: AStockSkillDataset[]
}

export interface AStockSkillRows {
  dataset: AStockSkillDataset
  queriedAt: string
  limit: number
  code?: string | null
  source?: string | null
  columns: string[]
  rows: Record<string, unknown>[]
}

export async function fetchAStockSkillDatasets(): Promise<AStockSkillDatasetList> {
  const res = await fetchWithRetry(`${A_STOCK_SKILL_API_BASE}/datasets`, { cache: "no-store" })
  return cynexusResult<AStockSkillDatasetList>(res)
}

export async function fetchAStockSkillRows(params: {
  datasetCode: string
  limit?: number
  code?: string
  source?: string
}): Promise<AStockSkillRows> {
  const search = new URLSearchParams()
  search.set("limit", String(params.limit || 50))
  if (params.code?.trim()) search.set("code", params.code.trim())
  if (params.source?.trim()) search.set("source", params.source.trim())
  const res = await fetchWithRetry(
    `${A_STOCK_SKILL_API_BASE}/datasets/${encodeURIComponent(params.datasetCode)}/rows?${search.toString()}`,
    { cache: "no-store" },
  )
  return cynexusResult<AStockSkillRows>(res)
}
