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

const CORE_API = `${SERVICE_PREFIX.core}/api-workflows`
const MARKET_API = `${SERVICE_PREFIX.market}/api-workflows`

export async function listApiWorkflows(limit = 50): Promise<ApiWorkflowProcess[]> {
  const [coreResponse, marketResponse] = await Promise.all([
    fetchWithRetry(`${CORE_API}?limit=${limit}`, { cache: "no-store" }),
    fetchWithRetry(`${MARKET_API}?limit=${limit}`, { cache: "no-store" }),
  ])
  const [core, market] = await Promise.all([
    cynexusResult<ApiWorkflowProcess[]>(coreResponse),
    cynexusResult<ApiWorkflowProcess[]>(marketResponse),
  ])
  return [...core, ...market].sort((left, right) => {
    return new Date(right.createdAt ?? 0).getTime() - new Date(left.createdAt ?? 0).getTime()
  }).slice(0, limit)
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
