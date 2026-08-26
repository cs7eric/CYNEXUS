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
  scope?: "full" | "priority" | string | null
  universeMode?: string | null
  fullConfirmationRequired?: boolean | null
  coverageRatio?: number | null
  observedCount?: number | null
  eligibleCount?: number | null
  freshnessMs?: number | null
  algorithmVersion?: string | null
  snapshotCount: number
  breadth?: MarketIndicatorBreadth | null
  snapshots: MarketIndicatorSnapshot[]
  limitUp: MarketIndicatorSnapshot[]
  limitDown: MarketIndicatorSnapshot[]
  industries: MarketIndicatorIndustry[]
  events: MarketIndicatorEvent[]
}

export interface LimitBoardSessionSummary {
  trade_date?: string | null
  trade_session?: "morning" | "afternoon" | string | null
  code: string
  name?: string | null
  market?: string | null
  direction?: "up" | "down" | string | null
  closing_status?: "touch" | "sealed" | "opened" | "unknown" | string | null
  is_sealed_at_close?: boolean | null
  first_touch_at?: string | null
  first_sealed_at?: string | null
  last_sealed_at?: string | null
  last_observed_at?: string | null
  sealed_duration_seconds?: number | null
  open_count?: number | null
  reseal_count?: number | null
  sample_count?: number | null
  valid_sample_count?: number | null
  gap_count?: number | null
  coverage_ratio?: number | null
  max_sealed_amount?: number | null
  avg_sealed_amount?: number | null
  median_sealed_amount?: number | null
  peak_sealed_amount_at?: string | null
  first_sealed_amount?: number | null
  last_sealed_amount?: number | null
  last_turnover_amount?: number | null
  last_sealed_turnover_ratio?: number | null
  max_sealed_turnover_ratio?: number | null
  sealed_amount_change?: number | null
  sealed_amount_slope?: number | null
  trend_direction?: "increasing" | "decreasing" | "flat" | "insufficient" | string | null
  trend_series_origin_at?: string | null
  trend_time_offsets?: string | number[] | null
  trend_sealed_amounts?: string | number[] | null
  source?: string | null
  algorithm_version?: string | null
  trend_features?: string | LimitBoardTrendFeatures | Record<string, unknown> | null
}

export interface LimitBoardTrendFeatures {
  sample_interval_seconds?: number
  expected_samples?: number
  observed_interval_count?: number
  observed_duration_seconds?: number
  max_observed_gap_seconds?: number
  duration_is_partial?: boolean
  quality_status?: "observed" | "partial" | string
  sealed_amount_points?: Array<[number, number]>
}

export interface LimitBoardSessionSummaryResponse {
  tradeDate?: string | null
  tradeSession?: string | null
  direction?: string | null
  source?: string | null
  items: LimitBoardSessionSummary[]
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
  scope?: string | null
  universeMode?: string | null
  fullConfirmationRequired?: boolean | null
  capturedAt?: string | null
  runtime?: {
    scope?: string | null
    universeMode?: string | null
    fullConfirmationRequired?: boolean | null
    targetCadenceSeconds?: number | null
    effectiveCadenceSeconds?: number | null
    benchmarkGate?: string | null
    capturedAt?: string | null
  } | null
  episodes: MarketIndicatorTransmissionRecord[]
  advice: MarketIndicatorTransmissionRecord[]
  outcomes: MarketIndicatorTransmissionRecord[]
  disclaimer?: string
}

export interface MarketIndicatorReplayIndexPoint {
  time?: string | null
  timestamp?: number | null
  close?: number | null
  turnover?: number | null
}

export interface MarketIndicatorReplayIndustryPoint {
  capturedAt?: string | null
  industryCode?: string | null
  industryName?: string | null
  stockCount?: number | null
  advancingCount?: number | null
  decliningCount?: number | null
  limitUpCount?: number | null
  limitUpTouchCount?: number | null
  limitDownCount?: number | null
  limitDownTouchCount?: number | null
  rapidRiseCount?: number | null
  rapidFallCount?: number | null
  firstLimitUpAt?: string | null
  firstLimitUpCode?: string | null
  firstLimitUpName?: string | null
  firstEventAt?: string | null
  firstEventCode?: string | null
  firstEventName?: string | null
  firstEventType?: string | null
}

export interface MarketIndicatorReplayTimelineResponse {
  ok: boolean
  error?: string | null
  tradeDate?: string | null
  dataTradeDate?: string | null
  dataStatus?: "ready" | "partial" | "pending" | "unavailable" | string | null
  source?: string | null
  requestedFrom?: string | null
  requestedTo?: string | null
  indexPointCount?: number | null
  eventCount?: number | null
  industryPointCount?: number | null
  indexPoints: MarketIndicatorReplayIndexPoint[]
  events: MarketIndicatorEvent[]
  industryPoints: MarketIndicatorReplayIndustryPoint[]
  message?: string | null
}

export interface MarketIndicatorRadarState {
  tradeDate?: string | null
  asOf?: string | null
  breadth?: MarketIndicatorBreadth | null
  eventCount?: number | null
  rapidRiseCount?: number | null
  rapidFallCount?: number | null
  marketState?: string | null
  stateReason?: string | null
  source?: string | null
  capturedAt?: string | null
  coverage?: number | null
}

export interface MarketIndicatorRadarEventsResponse {
  tradeDate?: string | null
  asOf?: string | null
  events: MarketIndicatorEvent[]
}

export interface MarketIndicatorLimitLadderResponse {
  tradeDate?: string | null
  board?: string | null
  items: MarketIndicatorSnapshot[]
}

export interface MarketIndicatorReplaySummary {
  tradeDate?: string | null
  dataStatus?: string | null
  dataTradeDate?: string | null
  indexPointCount?: number | null
  eventCount?: number | null
  industryPointCount?: number | null
  episodeCount?: number | null
  outcomeCount?: number | null
  effectiveEpisodeCount?: number | null
  failedEpisodeCount?: number | null
  firstBoardIndustryCount?: number | null
  outcomeCounts?: Record<string, number>
  coverageWarning?: string | null
  episodes?: MarketIndicatorTransmissionRecord[]
  outcomes?: MarketIndicatorTransmissionRecord[]
  source?: string | null
}

export interface MarketIndicatorReplayEpisodeResponse extends MarketIndicatorTransmissionRecord {
  ok?: boolean
  error?: string | null
  tradeDate?: string | null
  timeline?: MarketIndicatorReplayTimelineResponse | null
  outcomes?: MarketIndicatorTransmissionRecord[]
  explainability?: string | null
}

export interface MarketIndicatorReplayExplanation {
  ok?: boolean
  capability?: string
  tradeDate?: string | null
  episodeId?: string | null
  status?: string | null
  summary?: string | null
  drivers?: string[]
  counterEvidence?: string[]
  failureConditions?: string[]
  uncertainties?: string[]
  evidenceRefs?: string[]
}

export interface TradeReviewMarketStateResponse {
  tradeDate?: string
  asOf?: string | null
  membership?: "ths" | "tdx" | "union" | string
  windowDays?: number
  /** Stable read-model quality contract; `dataStatus` remains for compatibility. */
  status?: "ready" | "partial" | "unavailable" | string
  dataStatus?: "ready" | "partial" | "unavailable" | string
  qualityReasons?: string[]
  /** @deprecated use qualityReasons. */
  missingReasons?: string[]
  source?: string
  manifest?: Record<string, unknown> | null
  market?: Record<string, unknown> | null
  industries?: Array<Record<string, unknown>>
  pools?: Array<Record<string, unknown>>
  events?: Array<Record<string, unknown>>
  history?: Array<Record<string, unknown>>
  trend?: Record<string, unknown> | null
  industryTrends?: Array<Record<string, unknown>>
}

// The replay lower slot and the right-side Market State tab consume the same
// materialized snapshot.  Deduplicate their concurrent request and retain a
// tiny, short-lived cursor cache.  This is deliberately bounded: a day-level
// response can contain industry pools and must not grow with every replay bar.
const tradeReviewMarketStatePending = new Map<string, Promise<TradeReviewMarketStateResponse>>()
const tradeReviewMarketStateCache = new Map<string, { expiresAt: number; value: TradeReviewMarketStateResponse }>()
const TRADE_REVIEW_STATE_CACHE_LIMIT = 12

function tradeReviewStateKey(params: {
  tradeDate: string; mode?: "intraday" | "daily"; asOf?: string; membership?: "ths" | "tdx" | "union"; industryCode?: string; windowDays?: number
}) {
  return [params.tradeDate, params.mode ?? "intraday", params.asOf ?? "", params.membership ?? "union", params.industryCode ?? "", params.windowDays ?? 60].join("|")
}

export async function fetchTradeReviewMarketState(params: {
  tradeDate: string
  mode?: "intraday" | "daily"
  asOf?: string
  membership?: "ths" | "tdx" | "union"
  industryCode?: string
  windowDays?: number
}): Promise<TradeReviewMarketStateResponse> {
  const key = tradeReviewStateKey(params)
  const cached = tradeReviewMarketStateCache.get(key)
  if (cached && cached.expiresAt > Date.now()) return cached.value
  const pending = tradeReviewMarketStatePending.get(key)
  if (pending) return pending
  const query = new URLSearchParams({ tradeDate: params.tradeDate, membership: params.membership ?? "union" })
  if (params.mode) query.set("mode", params.mode)
  if (params.asOf) query.set("asOf", params.asOf)
  if (params.industryCode) query.set("industryCode", params.industryCode)
  if (params.windowDays != null) query.set("windowDays", String(params.windowDays))
  const request = fetchWithRetry(`${SERVICE_PREFIX.market}/trade-review/market-state?${query.toString()}`, { cache: "no-store" })
    .then(cynexusResult<TradeReviewMarketStateResponse>)
    .then((value) => {
      const ttl = params.mode === "daily" ? 5 * 60_000 : 30_000
      tradeReviewMarketStateCache.set(key, { value, expiresAt: Date.now() + ttl })
      while (tradeReviewMarketStateCache.size > TRADE_REVIEW_STATE_CACHE_LIMIT) {
        const oldest = tradeReviewMarketStateCache.keys().next().value
        if (oldest == null) break
        tradeReviewMarketStateCache.delete(oldest)
      }
      return value
    })
    .finally(() => tradeReviewMarketStatePending.delete(key))
  tradeReviewMarketStatePending.set(key, request)
  return request
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

export async function fetchLimitBoardSessionSummaries(params: {
  tradeDate?: string
  tradeSession?: "all" | "morning" | "afternoon"
  direction?: "all" | "up" | "down"
  limit?: number
} = {}): Promise<LimitBoardSessionSummaryResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.tradeSession) query.set("tradeSession", params.tradeSession)
  if (params.direction) query.set("direction", params.direction)
  query.set("limit", String(params.limit ?? 240))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/limit-board/summaries?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<LimitBoardSessionSummaryResponse>(response)
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

export async function fetchMarketIndicatorReplayTimeline(params: {
  tradeDate?: string
  from?: string
  to?: string
  eventType?: string
  industryCode?: string
  limit?: number
} = {}): Promise<MarketIndicatorReplayTimelineResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.from) query.set("from", params.from)
  if (params.to) query.set("to", params.to)
  if (params.eventType) query.set("eventType", params.eventType)
  if (params.industryCode) query.set("industryCode", params.industryCode)
  query.set("limit", String(params.limit ?? 3000))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/replay/timeline?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorReplayTimelineResponse>(response)
}

export async function fetchMarketIndicatorRadarState(params: { tradeDate?: string; asOf?: string } = {}): Promise<MarketIndicatorRadarState> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.asOf) query.set("asOf", params.asOf)
  const suffix = query.toString() ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/radar/state${suffix}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorRadarState>(response)
}

export async function fetchMarketIndicatorRadarEvents(params: { tradeDate?: string; asOf?: string; eventType?: string; limit?: number } = {}): Promise<MarketIndicatorRadarEventsResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.asOf) query.set("asOf", params.asOf)
  if (params.eventType) query.set("kind", params.eventType)
  query.set("limit", String(params.limit ?? 50))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/radar/events?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorRadarEventsResponse>(response)
}

export async function fetchMarketIndicatorLimitLadder(params: { tradeDate?: string; board?: string; limit?: number } = {}): Promise<MarketIndicatorLimitLadderResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.board) query.set("board", params.board)
  query.set("limit", String(params.limit ?? 100))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/radar/limit-ladder?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorLimitLadderResponse>(response)
}

export async function fetchMarketIndicatorReplaySummary(params: { tradeDate?: string; limit?: number } = {}): Promise<MarketIndicatorReplaySummary> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  query.set("limit", String(params.limit ?? 50))
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/replay/summary?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorReplaySummary>(response)
}

export async function fetchMarketIndicatorReplayEpisode(params: { tradeDate?: string; episodeId: string }): Promise<MarketIndicatorReplayEpisodeResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  const suffix = query.toString() ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/replay/episode/${encodeURIComponent(params.episodeId)}${suffix}`, { cache: "no-store" })
  return cynexusResult<MarketIndicatorReplayEpisodeResponse>(response)
}

export async function explainMarketIndicatorReplay(payload: Record<string, unknown>): Promise<MarketIndicatorReplayExplanation> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/market-indicator/replay-explanation`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return cynexusResult<MarketIndicatorReplayExplanation>(response)
}

export async function explainMarketIndicatorReplayFallback(payload: { tradeDate?: string; episodeId?: string }): Promise<MarketIndicatorReplayExplanation> {
  const response = await fetchWithRetry(`${INDICATOR_API_BASE}/replay/explain`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return cynexusResult<MarketIndicatorReplayExplanation>(response)
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
