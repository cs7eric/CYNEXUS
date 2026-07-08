import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const PROVIDER_CAPABILITIES_API_BASE = `${SERVICE_PREFIX.core}/provider-capabilities`

export interface ProviderCapabilitySummary {
  total: number
  byCategory: Record<string, number>
  byProvider: Record<string, number>
  byTargetStore: Record<string, number>
}

export interface ProviderCapabilityItem {
  capabilityCode: string
  name: string
  category: string
  providerCode: string
  providerName: string
  capabilityType: string
  sourceLayer: string
  targetStore: string
  status: string
  dataGranularity?: string | null
  storageTables: string[]
  updateJobs: string[]
  upstreamEndpoints: string[]
  usedBy: string[]
  rateLimitPolicy?: string | null
  freshnessPolicy?: string | null
  antiScrapingNotes?: string | null
  description?: string | null
  sortOrder?: number | null
}

export interface ProviderCapabilityResponse {
  generatedAt: string
  summary: ProviderCapabilitySummary
  items: ProviderCapabilityItem[]
}

export async function fetchProviderCapabilities(): Promise<ProviderCapabilityResponse> {
  const res = await fetchWithRetry(PROVIDER_CAPABILITIES_API_BASE, { cache: "no-store" })
  return cynexusResult<ProviderCapabilityResponse>(res)
}
