import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface MarketInformationNews {
  id: string
  title: string
  summary?: string | null
  source?: string | null
  category?: string | null
  kind?: "news" | "announcement" | string | null
  issuer?: string | null
  labels?: string[] | null
  publishedAt?: string | null
  url?: string | null
  clusterId?: number | null
  analysisStatus?: "NOT_REQUESTED" | "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | string | null
  materiality?: "high" | "medium" | "low" | "unknown" | string | null
  marketDirection?: "positive" | "negative" | "mixed" | "neutral" | "unknown" | string | null
  impactIndustries?: MarketInformationImpactIndustry[] | null
  impactStatus?: "Direct" | "Inferred" | "Unmapped" | string | null
}

export interface MarketInformationImpactIndustry {
  tdxCode: string
  tdxName: string
  thsCode?: string | null
  thsName?: string | null
  status: "Direct" | "Inferred" | "Unmapped" | string
  source?: string | null
  confidence?: number | null
  relatedStocks?: MarketInformationRelatedStock[] | null
}

export interface MarketInformationRelatedStock {
  code: string
  name?: string | null
}

export interface MarketInformationBrief {
  briefDate?: string | null
  status?: string | null
  providerCode?: string | null
  modelCode?: string | null
  fallbackUsed?: boolean | null
  contentJson?: string | null
  updatedAt?: string | null
}

export type MarketBriefDirection = "positive" | "negative" | "mixed" | "neutral" | "unknown"
export type MarketBriefMateriality = "high" | "medium" | "low" | "unknown"
export type MarketBriefTimeHorizon = "intraday" | "short" | "medium" | "long" | "unknown"

export interface MarketBriefTone {
  direction: MarketBriefDirection
  confidence: number
  rationale: string
}

export interface MarketBriefKeyEvent {
  clusterId: number
  title: string
  summary: string
  eventType: "policy" | "macro" | "industry" | "company" | "regulation" | "market"
  direction: MarketBriefDirection
  materiality: MarketBriefMateriality
  timeHorizon: MarketBriefTimeHorizon
  affectedScopes: string[]
  affectedIndustries?: string[]
  affectedCompanies?: string[]
  confidence: number
}

export interface MarketBriefSignal {
  summary: string
  direction: MarketBriefDirection
  materiality: MarketBriefMateriality
  affectedScopes: string[]
  eventReferences: number[]
}

export interface MarketBriefRiskSignal {
  summary: string
  severity: "high" | "medium" | "low"
  timeHorizon: MarketBriefTimeHorizon
  affectedScopes: string[]
  eventReferences: number[]
}

export interface MarketBriefContent {
  headline?: string
  executiveSummary?: string
  marketTone?: MarketBriefTone | string
  keyEvents?: Array<MarketBriefKeyEvent | string>
  policySignals?: Array<MarketBriefSignal | string>
  industrySignals?: Array<MarketBriefSignal | string>
  companySignals?: Array<MarketBriefSignal | string>
  riskSignals?: MarketBriefRiskSignal[]
  risks?: string[]
  uncertainties?: string[]
  sourceCoverage?: Record<string, number>
  analysisWindow?: { from?: string | null; to?: string | null; eventCount?: number }
  eventReferences?: number[]
  disclaimer?: string
}

export interface MarketInformationEventMember {
  source: string
  sourceRecordId: string
  title?: string | null
  summary?: string | null
  url?: string | null
  publishedAt?: string | null
  contentStatus?: string | null
  contentQuality?: string | null
}

export interface MarketInformationEventDetail {
  clusterId: number
  title: string
  category: string
  entities: string[]
  priorityScore: number
  highValue: boolean
  firstPublishedAt?: string | null
  lastPublishedAt?: string | null
  contentHash?: string | null
  analysisStatus?: string | null
  providerCode?: string | null
  modelCode?: string | null
  fallbackUsed?: boolean | null
  analysisJson?: string | null
  members: MarketInformationEventMember[]
  impactIndustries?: MarketInformationImpactIndustry[] | null
}

export interface MarketInformationNewsPage {
  items: MarketInformationNews[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
}

export interface MarketInformationPolicyAnnouncement {
  id: string
  title: string
  category?: string | null
  publishedDate?: string | null
  url?: string | null
  issuer?: string | null
}

export interface MarketInformationLeaderEvent {
  code: string
  name?: string | null
  title: string
  summary?: string | null
  source?: string | null
  publishedAt?: string | null
  url?: string | null
}

export interface MarketInformationSignal {
  code: string
  name?: string | null
  pool: string
  reason?: string | null
  changePct?: number | null
  sealAmount?: number | null
  snapshotDate?: string | null
}

export interface MarketInformationHotStock {
  code: string
  name?: string | null
  rank: number
  heat?: number | null
  pct?: number | null
  snapshotDate?: string | null
}

export interface MarketInformationDragonTiger {
  code: string
  name?: string | null
  reason?: string | null
  changePct?: number | null
  netBuyWan?: number | null
  tradeDate?: string | null
}

export interface MarketInformationFeed {
  queriedAt?: string | null
  brief?: MarketInformationBrief | null
  newsPage?: MarketInformationNewsPage
  news: MarketInformationNews[]
  macroNews?: MarketInformationNews[]
  policyAnnouncements?: MarketInformationPolicyAnnouncement[]
  signals: MarketInformationSignal[]
  hotStocks: MarketInformationHotStock[]
  leaders?: MarketInformationHotStock[]
  leaderEvents?: MarketInformationLeaderEvent[]
  dragonTiger: MarketInformationDragonTiger[]
  impactIndustries?: MarketInformationImpactIndustry[]
}

export async function fetchMarketInformation(page = 1, pageSize = 30, impactIndustryCode?: string, analysisStatus?: string): Promise<MarketInformationFeed> {
  const query = new URLSearchParams({
    page: String(Math.max(page, 1)),
    pageSize: String(Math.min(Math.max(pageSize, 1), 60)),
  })
  if (impactIndustryCode) query.set("impactIndustryCode", impactIndustryCode)
  if (analysisStatus) query.set("analysisStatus", analysisStatus)
  return cynexusResult<MarketInformationFeed>(
    await fetchWithRetry(`${SERVICE_PREFIX.market}/information?${query.toString()}`, {
      cache: "no-store",
    }),
  )
}

export async function fetchMarketInformationEvent(clusterId: number): Promise<MarketInformationEventDetail> {
  return cynexusResult<MarketInformationEventDetail>(
    await fetchWithRetry(`${SERVICE_PREFIX.market}/information/events/${clusterId}`, { cache: "no-store" }),
  )
}

export async function requestMarketInformationAnalysis(clusterId: number, force = false): Promise<{ clusterId: number; queued: boolean }> {
  return cynexusResult<{ clusterId: number; queued: boolean }>(
    await fetchWithRetry(`${SERVICE_PREFIX.market}/information/events/${clusterId}/analyze?force=${String(force)}`, {
      method: "POST",
      cache: "no-store",
    }),
  )
}

export async function fetchMarketInformationAnalysisStatuses(clusterIds: number[]): Promise<Array<Pick<MarketInformationNews, "clusterId" | "analysisStatus" | "materiality" | "marketDirection">>> {
  if (!clusterIds.length) return []
  const query = new URLSearchParams()
  clusterIds.forEach((clusterId) => query.append("clusterIds", String(clusterId)))
  return cynexusResult<Array<Pick<MarketInformationNews, "clusterId" | "analysisStatus" | "materiality" | "marketDirection">>>(
    await fetchWithRetry(`${SERVICE_PREFIX.market}/information/events/status?${query.toString()}`, { cache: "no-store" }),
  )
}
