import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const INDICATOR_API_BASE = `${SERVICE_PREFIX.market}/indicator`

export type IndicatorStatus = "all" | "normal" | "limit_up_touch" | "limit_up_sealed" | "limit_down_touch" | "limit_down_sealed"

export interface MarketIndicatorSnapshot {
  code: string
  name?: string | null
  industry?: string | null
  market?: string | null
  lastPrice?: number | null
  prevClose?: number | null
  openPrice?: number | null
  highPrice?: number | null
  lowPrice?: number | null
  changePct?: number | null
  amount?: number | null
  volume?: number | null
  turnoverRate?: number | null
  limitUpPrice?: number | null
  limitDownPrice?: number | null
  limitStatus?: string | null
  source?: string | null
  capturedAt?: string | null
}

export interface MarketIndicatorBreadth {
  totalCount?: number | null
  advancingCount?: number | null
  decliningCount?: number | null
  flatCount?: number | null
  limitUpCount?: number | null
  limitUpTouchCount?: number | null
  limitDownCount?: number | null
  limitDownTouchCount?: number | null
  rapidRiseCount?: number | null
  rapidFallCount?: number | null
  capturedAt?: string | null
}

export interface MarketIndicatorIndustry {
  id?: number | null
  tradeDate?: string | null
  capturedAt?: string | null
  industryCode?: string | null
  industryName: string
  stockCount?: number | null
  advancingCount?: number | null
  decliningCount?: number | null
  flatCount?: number | null
  limitUpCount?: number | null
  limitUpTouchCount?: number | null
  limitDownCount?: number | null
  limitDownTouchCount?: number | null
  rapidRiseCount?: number | null
  rapidFallCount?: number | null
  firstLimitUpAt?: string | null
  firstLimitUpCode?: string | null
  firstLimitUpName?: string | null
  firstLimitUpChangePct?: number | null
  firstEventAt?: string | null
  firstEventCode?: string | null
  firstEventName?: string | null
  firstEventType?: string | null
  firstEventSignalValue?: number | null
  source?: string | null
}

export interface MarketIndicatorIndustryRelation {
  industryCode: string
  industryName: string
  categoryLabel?: string | null
  overlapCount?: number | null
  sourceStockCount?: number | null
  targetStockCount?: number | null
  advancingCount?: number | null
  decliningCount?: number | null
  flatCount?: number | null
  averageChangePct?: number | null
  averageRelationLevel?: number | null
  relationScore?: number | null
  relationState?: string | null
}

export interface MarketIndicatorIndustryDetail {
  industryCode: string
  industryName?: string | null
  categoryLabel?: string | null
  totalCount?: number | null
  page: number
  pageSize: number
  advancingCount?: number | null
  decliningCount?: number | null
  flatCount?: number | null
  averageChangePct?: number | null
  capturedAt?: string | null
  constituents: MarketIndicatorSnapshot[]
  relations: MarketIndicatorIndustryRelation[]
}

export interface MarketIndicatorEvent {
  id?: number | null
  tradeDate?: string | null
  code: string
  name?: string | null
  market?: string | null
  eventType: string
  eventAt?: string | null
  signalValue?: number | null
  lastPrice?: number | null
  changePct?: number | null
  limitStatus?: string | null
  source?: string | null
}

export interface MarketIndicatorResponse {
  ok: boolean
  error?: string | null
  tradeDate?: string | null
  capturedAt?: string | null
  source?: string | null
  snapshotCount: number
  breadth?: MarketIndicatorBreadth | null
  snapshots: MarketIndicatorSnapshot[]
  limitUp: MarketIndicatorSnapshot[]
  limitDown: MarketIndicatorSnapshot[]
  industries: MarketIndicatorIndustry[]
  events: MarketIndicatorEvent[]
}

export interface MarketIndicatorTransmissionRecord {
  episodeId?: string
  episode_id?: string
  sectorName?: string
  sector_name?: string
  direction?: string
  stage?: string
  coverageRatio?: number
  coverage_ratio?: number
  confidenceScore?: number
  confidence_score?: number
  confidenceLevel?: string
  confidence_level?: string
  actionClass?: string
  action_class?: string
  targetName?: string
  target_name?: string
  generatedAt?: string
  generated_at?: string
  outcome?: string
  payload?: string | Record<string, unknown> | null
  scope?: string | null
  universeMode?: string | null
  fullConfirmationRequired?: boolean | null
  capturedAt?: string | null
  [key: string]: unknown
}

export interface MarketIndicatorTransmissionResponse {
  tradeDate?: string | null
  episodes: MarketIndicatorTransmissionRecord[]
  advice: MarketIndicatorTransmissionRecord[]
  outcomes: MarketIndicatorTransmissionRecord[]
  disclaimer?: string
}

export async function fetchMarketIndicators(params: {
  tradeDate?: string
  status?: IndicatorStatus
  eventType?: string
  limit?: number
} = {}): Promise<MarketIndicatorResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.status) query.set("status", params.status)
  if (params.eventType) query.set("eventType", params.eventType)
  if (params.limit != null) query.set("limit", String(params.limit))
  const suffix = query.toString() ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/latest${suffix}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorResponse>(response)
}

export async function fetchMarketIndicatorIndustryDetail(params: {
  industryCode: string
  tradeDate?: string
  page?: number
  pageSize?: number
}): Promise<MarketIndicatorIndustryDetail> {
  const query = new URLSearchParams({ industryCode: params.industryCode })
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  query.set("page", String(params.page ?? 0))
  query.set("pageSize", String(params.pageSize ?? 50))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/industry/detail?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorIndustryDetail>(response)
}

export async function fetchMarketIndicatorTransmission(params: {
  tradeDate?: string
  limit?: number
} = {}): Promise<MarketIndicatorTransmissionResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.limit != null) query.set("limit", String(params.limit))
  const suffix = query.toString() ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/transmission${suffix}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorTransmissionResponse>(response)
}

export interface MarketIndicatorWatchlistPreview {
  previewToken: string
  expiresAt: string
  targetTradeDate?: string
  items: Array<Record<string, unknown>>
  disclaimer: string
}

export async function previewMarketIndicatorWatchlist(payload: {
  targetTradeDate?: string
  items: Array<Record<string, unknown>>
}): Promise<MarketIndicatorWatchlistPreview> {
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/watchlist/preview`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return cynexusResult<MarketIndicatorWatchlistPreview>(response)
}

export async function confirmMarketIndicatorWatchlist(payload: {
  previewToken: string
}): Promise<Record<string, unknown>> {
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/watchlist/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return cynexusResult<Record<string, unknown>>(response)
}
