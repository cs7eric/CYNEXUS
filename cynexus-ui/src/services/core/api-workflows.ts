import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export type ApiWorkflowType = "MEDIA_TRANSCRIPTION" | "PORTFOLIO_TRADE_IMPORT" | "FINANCE_AI_CAPTURE"
export type ApiWorkflowStatus = "QUEUED" | "RUNNING" | "WAITING_REVIEW" | "SUCCEEDED" | "FAILED" | "CANCELLED"

export interface ApiWorkflowEvent {
  attemptNo?: number
  eventType: string
  fromStatus?: string | null
  toStatus?: string | null
  phase: string
  status: ApiWorkflowStatus
  progress: number
  message?: string | null
  providerCode?: string | null
  modelCode?: string | null
  errorCode?: string | null
  errorMessage?: string | null
  details?: string | null
  durationMs?: number | null
  occurredAt?: string | null
}

export interface ApiWorkflowProcess {
  id: string
  workflowType: ApiWorkflowType
  workflowKey: string
  source: string
  sourceRef: string
  title?: string | null
  status: ApiWorkflowStatus
  phase: string
  progress: number
  attemptCount: number
  errorCode?: string | null
  errorMessage?: string | null
  resultRoute?: string | null
  resultRef?: string | null
  createdAt?: string | null
  startedAt?: string | null
  completedAt?: string | null
  updatedAt?: string | null
  events: ApiWorkflowEvent[]
}

export interface ApiWorkflowPage {
  items: ApiWorkflowProcess[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
}

const CORE_API = `${SERVICE_PREFIX.core}/api-workflows`
const MARKET_API = `${SERVICE_PREFIX.market}/api-workflows`

export async function listApiWorkflows(page = 1, pageSize = 20, type = "all", status = "all"): Promise<ApiWorkflowPage> {
  const safePage = Math.max(1, page)
  const safePageSize = Math.max(1, pageSize)
  const fetchSize = Math.min(2_000, safePage * safePageSize)
  const query = new URLSearchParams({ page: "1", pageSize: String(fetchSize) })
  if (type !== "all") query.set("type", type)
  if (status !== "all") query.set("status", status)
  const queryString = query.toString()
  const [coreResponse, marketResponse] = await Promise.all([
    fetchWithRetry(`${CORE_API}?${queryString}`, { cache: "no-store" }),
    fetchWithRetry(`${MARKET_API}?${queryString}`, { cache: "no-store" }),
  ])
  const [core, market] = await Promise.all([
    cynexusResult<ApiWorkflowPage>(coreResponse),
    cynexusResult<ApiWorkflowPage>(marketResponse),
  ])
  const items = [...core.items, ...market.items].sort((left, right) => {
    return new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime()
  }).slice((safePage - 1) * safePageSize, safePage * safePageSize)
  const total = core.total + market.total
  const totalPages = total === 0 ? 0 : Math.ceil(total / safePageSize)
  return { items, total, page: safePage, pageSize: safePageSize, totalPages, hasPrevious: safePage > 1, hasNext: safePage < totalPages }
}

export async function retryApiWorkflow(item: ApiWorkflowProcess): Promise<void> {
  if (item.workflowType === "MEDIA_TRANSCRIPTION") {
    const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/media-transcription/processes/${encodeURIComponent(item.sourceRef)}/retry`, { method: "POST" })
    await cynexusResult(response)
    return
  }
  if (item.workflowType === "PORTFOLIO_TRADE_IMPORT") {
    const response = await fetchWithRetry(`${SERVICE_PREFIX.market}/portfolio/imports/${encodeURIComponent(item.sourceRef)}/retry`, { method: "POST" })
    await cynexusResult(response)
    return
  }
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/finance/ai-captures/${encodeURIComponent(item.sourceRef)}/retry`, { method: "POST" })
  await cynexusResult(response)
}
