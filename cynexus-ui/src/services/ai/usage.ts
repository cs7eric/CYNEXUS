import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface AiUsageBreakdown {
  key: string
  providerCode?: string | null
  modelCode?: string | null
  capability?: string | null
  requests: number
  succeeded: number
  failed: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
  totalCost: number
}

export interface AiUsageObservation {
  id: string
  name?: string | null
  providerCode?: string | null
  modelCode?: string | null
  status?: string | null
  inputTokens: number
  outputTokens: number
  totalTokens: number
  totalCost: number
  startedAt?: string | null
  latencyMs?: number | null
  statusMessage?: string | null
}

export interface AiUsagePage {
  source: string
  available: boolean
  message?: string | null
  from: string
  to: string
  requests: number
  succeeded: number
  failed: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
  totalCost: number
  breakdown: AiUsageBreakdown[]
  observations: AiUsageObservation[]
}

export async function fetchAiUsage(params: { from?: string; to?: string; limit?: number } = {}) {
  const query = new URLSearchParams()
  if (params.from) query.set("from", params.from)
  if (params.to) query.set("to", params.to)
  query.set("limit", String(params.limit ?? 1000))
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/usage?${query.toString()}`)
  return cynexusResult<AiUsagePage>(response)
}
