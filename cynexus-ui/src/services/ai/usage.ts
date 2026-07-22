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

export interface AgentQuotaPolicy {
  id: string
  tenantKey?: string | null
  principalKey?: string | null
  dailyRequestLimit: number
  dailyTokenLimit: number
  enabled: boolean
  effectiveAt?: string | null
}

interface JavaAgentQuotaPolicy {
  id: string
  tenantKey?: string | null
  principalKey?: string | null
  dailyRequestLimit: number
  dailyTokenLimit: number
  enabled: boolean
  effectiveAt?: string | null
}

export async function fetchAiUsage(params: { from?: string; to?: string; limit?: number } = {}) {
  const query = new URLSearchParams()
  if (params.from) query.set("from", params.from)
  if (params.to) query.set("to", params.to)
  query.set("limit", String(params.limit ?? 1000))
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/usage?${query.toString()}`)
  return cynexusResult<AiUsagePage>(response)
}

function toQuotaPolicy(dto: JavaAgentQuotaPolicy): AgentQuotaPolicy {
  return { id: String(dto.id), tenantKey: dto.tenantKey, principalKey: dto.principalKey,
    dailyRequestLimit: dto.dailyRequestLimit, dailyTokenLimit: dto.dailyTokenLimit,
    enabled: dto.enabled, effectiveAt: dto.effectiveAt }
}

export async function fetchAgentQuotaPolicies() {
  const values = await cynexusResult<JavaAgentQuotaPolicy[]>(
    await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/quota-policies`, { cache: "no-store" }),
  )
  return (values || []).map(toQuotaPolicy)
}

export async function createAgentQuotaPolicy(policy: Omit<AgentQuotaPolicy, "id">) {
  const id = await cynexusResult<string>(await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/quota-policies`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(policy),
  }))
  return String(id)
}

export async function updateAgentQuotaPolicy(policy: AgentQuotaPolicy) {
  return cynexusResult<boolean>(await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/quota-policies`, {
    method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(policy),
  }))
}

export async function deleteAgentQuotaPolicy(id: string) {
  return cynexusResult<boolean>(await fetchWithRetry(`${SERVICE_PREFIX.aiConfig}/quota-policies/${encodeURIComponent(id)}`, { method: "DELETE" }))
}
