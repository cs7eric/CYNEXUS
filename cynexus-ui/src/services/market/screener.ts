import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const SCREENER_API_BASE = `${SERVICE_PREFIX.market}/screener`

export type ScreenerUniverse = "ALL" | "SH" | "SZ" | "BJ"
export type ScreenerBoard = "ALL" | "main_sh" | "main_sz" | "chinext" | "star" | "bse" | "unknown"
export type ScreenerSort = "amount" | "price" | "change_pct" | "amplitude_pct" | "return_5d" | "return_10d" | "return_30d" | "return_60d" | "return_252d" | "main_net" | "main_net_5d" | "turnover_pct" | "volume_ratio" | "pe" | "market_cap" | "code"
export type ScreenerSortDirection = "asc" | "desc"

export interface StockScreenerItem {
  code: string
  name: string
  market: string
  industry?: string | null
  tdxIndustryCode?: string | null
  tdxIndustryName?: string | null
  thsIndustryCode?: string | null
  board?: ScreenerBoard | null
  st: boolean
  price?: number | null
  changePct?: number | null
  amplitudePct?: number | null
  amount?: number | null
  totalMarketCap?: number | null
  floatMarketCap?: number | null
  returnPct5d?: number | null
  returnPct10d?: number | null
  returnPct30d?: number | null
  returnPct60d?: number | null
  returnPct252d?: number | null
  mainNet?: number | null
  mainNet5d?: number | null
  thsIndustryName?: string | null
  concepts?: ScreenerSectorMembership[]
  styles?: ScreenerSectorMembership[]
  turnoverPct?: number | null
  volumeRatio?: number | null
  pe?: number | null
  priceVolumePattern?: PriceVolumePatternMetrics | null
}

export interface PriceVolumePatternMetrics {
  observedContractionDays?: number | null
  breakoutVolumeMultiple?: number | null
  bullishBodyPct?: number | null
  closeAboveMa20Pct?: number | null
  ma5AboveMa20Pct?: number | null
  breakoutHighDays?: number | null
  atrPct20?: number | null
  consecutiveBullishDays?: number | null
  volumeVsMa5Multiple?: number | null
  candlestickPatterns?: string[]
}

export interface StockScreenerResponse {
  ok: boolean
  requestedTradeDate?: string | null
  tradeDate?: string | null
  universe: ScreenerUniverse
  excludeSt: boolean
  excludeChiNext: boolean
  excludeStar: boolean
  industry?: string | null
  tdxIndustryCode?: string | null
  thsIndustryCode?: string | null
  conceptSectorId?: string | null
  styleSectorId?: string | null
  board?: ScreenerBoard | null
  sort: ScreenerSort
  sortDir: ScreenerSortDirection
  page: number
  pageSize: number
  universeCount: number
  count: number
  source: string
  items: StockScreenerItem[]
  warnings: string[]
  error?: string | null
  dataQuality?: StockScreenerDataQuality | null
}

export interface StockScreenerFieldCoverage {
  field: string
  availableCount: number
  eligibleCount: number
  coveragePct: number
}

export interface StockScreenerDataQuality {
  status: "READY" | "PARTIAL" | "STALE"
  snapshotDate?: string | null
  freshnessLagDays: number
  eligibleStockCount: number
  dailySnapshotCount: number
  fieldCoverage: StockScreenerFieldCoverage[]
  warnings: string[]
}

export type MarketEventCohort = "LIMIT_UP" | "LARGE_MOVE"

export interface MarketEventFactorMetric {
  key: string
  label: string
  value?: string | null
  unit?: string | null
}

export interface MarketEventFactorFamily {
  stage: string
  code: string
  label: string
  status: "AVAILABLE" | "UNAVAILABLE" | "PARTIAL" | string
  asOfDate?: string | null
  source?: string | null
  metrics: MarketEventFactorMetric[]
  supportingEvidence: string[]
  counterEvidence: string[]
  warnings: string[]
}

export interface MarketEventFactorEvent {
  code: string
  name: string
  eventType: MarketEventCohort
  changePct?: number | null
  streak?: number | null
  thsIndustryCode?: string | null
  thsIndustryName?: string | null
  tdxIndustryCode?: string | null
  tdxIndustryName?: string | null
  concepts: string[]
  styles: string[]
  factors: MarketEventFactorFamily[]
}

export interface MarketEventFactorObservatoryResponse {
  ok: boolean
  requestedTradeDate?: string | null
  tradeDate?: string | null
  cohort: MarketEventCohort
  minLargeMovePct?: number | null
  populationCount: number
  sampledCount: number
  marketFactors: MarketEventFactorFamily[]
  events: MarketEventFactorEvent[]
  /** Compatibility presets only; research factor sets replace this field. */
  screeningHintsDeprecated?: boolean
  screeningHints: Array<{ criterion: string; operator: string; value: string; label: string; basis: string }>
  warnings: string[]
  error?: string | null
}

export interface MarketEventReplayResponse {
  code: string
  tradeDate: string
  anchorAt: string
  status: "AVAILABLE" | "PARTIAL" | string
  source: string
  dataCutoffAt: string
  coverage: { stockBarsRead: number; stockBarsIncluded: number; marketAvailable: boolean; industryAvailable: boolean; stockAvailable: boolean }
  warnings: string[]
  stockMinuteBars: Array<{ timestamp?: number | null; tradeDate?: string | null; open?: number | null; high?: number | null; low?: number | null; close?: number | null; volume?: number | null; turnover?: number | null }>
  marketReplay: { dataStatus?: string | null; dataTradeDate?: string | null; source?: string | null; requestedFrom?: string | null; requestedTo?: string | null; eventCount: number; industryPointCount: number; message?: string | null }
}

export interface MarketEventFactorAnalysisHypothesis {
  label: string
  scope: "COHORT" | "EVENT"
  event_code?: string
  evidence_refs: string[]
  counter_evidence_refs: string[]
  research_conditions: string[]
}

export interface MarketEventFactorAnalysisResponse {
  ok: boolean
  trade_date?: string | null
  cohort: MarketEventCohort
  min_large_move_pct?: number | null
  source: string
  population_count: number
  sampled_count: number
  data_quality: "READY" | "PARTIAL" | string
  warnings: string[]
  evidence_refs: Record<string, string>
  analysis_result: {
    summary: string
    hypotheses: MarketEventFactorAnalysisHypothesis[]
    caveats: string[]
  }
  provider_code?: string | null
  model_code?: string | null
  prompt_version?: string | null
}

export type MarketFactorStudyStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "PARTIAL" | "NO_DATA" | "FAILED" | "CANCELLED"

export interface MarketFactorStudyResponse {
  studyId: string
  status: MarketFactorStudyStatus
  eventType: "LIMIT_UP_CLOSE" | "LIMIT_UP_TOUCH" | "LARGE_MOVE_CLOSE"
  startDate: string
  endDate: string
  dataCutoffAt: string
  featureRegistryVersion: string
  leakageCheckPassed: boolean
  eventSampleCount: number
  controlSampleCount: number
  progress?: { completedSteps?: number; totalSteps?: number; stage?: string; eventSampleCount?: number; controlSampleCount?: number }
  warnings: string[]
}

export interface CreateMarketFactorStudyRequest {
  eventType: "LIMIT_UP_CLOSE" | "LIMIT_UP_TOUCH" | "LARGE_MOVE_CLOSE"
  startDate: string
  endDate: string
  cohortDefinitionVersion?: number
  outcomeHorizons?: number[]
  controlsPerEvent?: number
  dailyPreWindowSessions?: number
  minutePreWindowMinutes?: number
  discoveryMethods?: string[]
  eventParams?: Record<string, unknown>
  featureRegistryVersion?: string
  idempotencyKey?: string
}

export interface MarketFactorDiscovery {
  discoveryCode: string
  recipeVersion: number
  method: string
  scope: "COMMON" | "SUBGROUP" | "RARE_SPECIAL" | "UNEXPLAINED"
  recipe: Record<string, unknown>
  learnedBounds: Record<string, unknown>
  statistics: Record<string, unknown>
  validation: Record<string, unknown>
  evidence: Record<string, unknown>
  verdict: "EXPLORATORY" | "CANDIDATE" | "WEAK" | "REJECTED" | "UNAVAILABLE" | "PROMOTED"
  rank: number
}

export interface MarketFactorCluster {
  clusterCode: string
  method: string
  memberCount: number
  discoveryCodes: string[]
}

export interface MarketFactorStudySample {
  code: string
  market: string
  eventTradeDate: string
  anchorAt?: string | null
  eventType: string
  sampleType: "EVENT" | "CONTROL"
  matchingMetadata: Record<string, unknown>
  source: Record<string, unknown>
  coverage: Record<string, unknown>
}

export interface MarketFactorStudySequencePoint {
  code: string
  market: string
  eventTradeDate: string
  sampleType: "EVENT" | "CONTROL"
  frequency: "DAILY" | "MINUTE_1"
  sequenceCode: string
  offset: number
  observedAt: string
  availableAt: string
  rawValue?: number | null
  normalizedValue?: number | null
  source: Record<string, unknown>
}

export interface MarketFactorSetVersion {
  id: number
  versionNo: number
  status: "DRAFT" | "VALIDATED" | "PROMOTED" | "RETIRED"
  expression: Record<string, unknown>
  validationSnapshot: Record<string, unknown>
}

export interface MarketFactorSet {
  id: number
  code: string
  name: string
  status: "DRAFT" | "ACTIVE" | "RETIRED"
  activeVersionId?: number | null
  version: number
  activeVersion?: MarketFactorSetVersion | null
}

export interface MarketFactorSetPreviewItem {
  code: string
  name?: string | null
  market?: string | null
  industry?: string | null
  price?: number | null
  changePct?: number | null
  reason: string
}

export interface MarketFactorSetPreview {
  factorSetVersionId: string
  code: string
  versionNo: number
  requestedTradeDate: string
  snapshotTradeDate?: string | null
  status: "AVAILABLE" | "PARTIAL" | "UNAVAILABLE" | "NO_DATA"
  count: number
  items: MarketFactorSetPreviewItem[]
  warnings: string[]
}

export interface MarketFactorStudyOutcome {
  code: string
  market: string
  eventTradeDate: string
  sampleType: "EVENT" | "CONTROL"
  outcomes: Array<{
    horizon: number
    status: "AVAILABLE" | "UNAVAILABLE"
    stockReturnPct?: number | null
    benchmarkReturnPct?: number | null
    excessReturnPct?: number | null
    maximumFavorablePct?: number | null
    maximumAdversePct?: number | null
  }>
}

export interface MarketFactorStudySynthesisResponse {
  ok: boolean
  studyId: string
  studyStatus: string
  dataCutoffAt?: string | null
  leakageCheckStatus: string
  warnings: string[]
  evidence_refs: Record<string, string>
  synthesis_result: { summary: string; findings: Array<{ label: string; evidence_refs: string[]; counter_evidence_refs: string[]; interpretation: string }>; caveats: string[] }
  providerCode?: string | null
  modelCode?: string | null
  promptVersion?: string | null
}

export interface StockScreenerParams {
  tradeDate?: string
  universe?: ScreenerUniverse
  excludeSt?: boolean
  excludeChiNext?: boolean
  excludeStar?: boolean
  minPrice?: number
  maxPrice?: number
  minChangePct?: number
  maxChangePct?: number
  minAmount?: number
  minMarketCap?: number
  maxMarketCap?: number
  industry?: string
  tdxIndustryCode?: string
  thsIndustryCode?: string
  conceptSectorId?: string
  styleSectorId?: string
  board?: Exclude<ScreenerBoard, "ALL">
  minAmplitudePct?: number
  maxAmplitudePct?: number
  minReturnPct5d?: number
  maxReturnPct5d?: number
  minReturnPct10d?: number
  maxReturnPct10d?: number
  minReturnPct30d?: number
  maxReturnPct30d?: number
  minReturnPct60d?: number
  maxReturnPct60d?: number
  minReturnPct252d?: number
  maxReturnPct252d?: number
  minMainNet?: number
  minMainNet5d?: number
  minTurnoverPct?: number
  maxTurnoverPct?: number
  minVolumeRatio?: number
  maxVolumeRatio?: number
  minPe?: number
  maxPe?: number
  volumeContractionDays?: number
  minBreakoutVolumeMultiple?: number
  maxBreakoutVolumeMultiple?: number
  minBullishBodyPct?: number
  minCloseAboveMa20Pct?: number
  minMa5AboveMa20Pct?: number
  breakoutHighDays?: number
  maxAtrPct20?: number
  minConsecutiveBullishDays?: number
  minVolumeVsMa5Multiple?: number
  maxVolumeVsMa5Multiple?: number
  candlestickPattern?: "bullish_engulfing" | "bearish_engulfing" | "hammer" | "shooting_star" | "doji" | "gap_up" | "gap_down" | "inside_bar"
  sort?: ScreenerSort
  sortDir?: ScreenerSortDirection
  page?: number
  pageSize?: number
}

export interface ScreenerIndustryOption {
  code: string
  name: string
  stockCount: number
  snapshotDate?: string | null
  source: string
}

export interface ScreenerSectorMembership {
  sectorId: string
  name: string
}

export interface ScreenerClassificationOptions {
  tdxIndustryGroups: ScreenerIndustryOption[]
  thsIndustries: ScreenerIndustryOption[]
  concepts: ScreenerIndustryOption[]
  styles: ScreenerIndustryOption[]
}

export interface StockSelectionStrategyVersion {
  id: number
  versionNo: number
  parameterSchema: string
  evidenceSchema: string
  changeSummary: string
  effectiveFrom: string
  definitionVersion: number
}

export interface StockSelectionStrategy {
  code: string
  name: string
  description: string
  type: "SCREENER_PRESET" | "USER_SCREENER"
  activeVersion: StockSelectionStrategyVersion
}

export interface StockSelectionStrategyCatalog {
  items: StockSelectionStrategy[]
}

export interface SaveStockSelectionStrategyRequest {
  name: string
  description?: string
  criteria: Record<string, unknown>
  changeSummary: string
  expectedDefinitionVersion?: number
}

export type StockSelectionStrategyHealthStatus = "VALID" | "WEAKENING" | "INVALID" | "NOT_ASSESSED"

export interface StockSelectionStrategyHealthCheck {
  code: string
  label: string
  status: "PASS" | "WARN" | "FAIL"
  actualValue: string
  threshold: string
  detail: string
}

export interface StockSelectionStrategyRecentPerformance {
  evaluatedTradeCount: number
  winRatePct: number
  averageExcessReturnPct: number
  asOfDate?: string | null
}

export interface StockSelectionStrategyHealthResponse {
  strategyCode: string
  strategyVersionNo: number
  requestedTradeDate?: string | null
  actualTradeDate?: string | null
  status: StockSelectionStrategyHealthStatus
  reason: string
  recommendedAction: string
  universeCount: number
  candidateCount: number
  signalDensityPct: number
  freshnessLagDays: number
  recentPerformance?: StockSelectionStrategyRecentPerformance | null
  source: string
  checks: StockSelectionStrategyHealthCheck[]
  warnings: string[]
}

export interface StockSelectionStrategyEvidence {
  code: string
  label: string
  operator: string
  expectedValue: unknown
  actualValue: unknown
  matched: boolean
}

export interface StockSelectionStrategyCandidate {
  id: string
  stockCode: string
  stockName: string
  rank: number
  score: number
  reasonSummary: string
  snapshot: Record<string, unknown>
  evidence: StockSelectionStrategyEvidence[]
}

export interface StockSelectionStrategyRunResponse {
  id: string
  strategyCode: string
  strategyVersionId: string
  strategyVersionNo: number
  requestedTradeDate?: string | null
  actualTradeDate?: string | null
  status: "SUCCEEDED" | "NO_DATA"
  universeCount: number
  matchedCount: number
  source: string
  warnings: string[]
  candidates: StockSelectionStrategyCandidate[]
}

export interface StockSelectionStrategyBacktestRequest {
  asOfDate?: string
  lookbackSessions?: number
  signalIntervalSessions?: number
  holdingSessions?: number
  maxCandidatesPerSignal?: number
  transactionCostBpsPerSide?: number
  stockCode?: string
  stockMarket?: "SH" | "SZ" | "BJ"
  targetSymbols?: StockSelectionStrategyTargetSymbol[]
}

export interface StockSelectionStrategyTargetSymbol {
  market: "SH" | "SZ" | "BJ"
  code: string
}

export interface StockSelectionStrategyBacktestConfig {
  requestedAsOfDate: string
  lookbackSessions: number
  signalIntervalSessions: number
  holdingSessions: number
  maxCandidatesPerSignal: number
  transactionCostBpsPerSide: number
  stockCode?: string | null
  stockMarket?: "SH" | "SZ" | "BJ" | null
  targetSymbols: StockSelectionStrategyTargetSymbol[]
}

export interface StockSelectionStrategyBacktestSummary {
  signalCount: number
  evaluatedTradeCount: number
  skippedTradeCount: number
  winRatePct: number
  averageNetReturnPct: number
  medianNetReturnPct: number
  benchmarkAverageReturnPct: number
  averageExcessReturnPct: number
}

export interface StockSelectionStrategyBacktestTrade {
  id: string
  signalDate: string
  entryDate: string
  exitDate: string
  market: string
  stockCode: string
  stockName?: string | null
  signalRank: number
  score: number
  entryPrice: number
  exitPrice: number
  grossReturnPct: number
  netReturnPct: number
  benchmarkReturnPct: number
  excessReturnPct: number
  outcome: "WIN" | "LOSS" | "FLAT"
}

export interface StockSelectionStrategyBacktestResponse {
  id: string
  strategyCode: string
  strategyVersionId: string
  strategyVersionNo: number
  requestedAsOfDate: string
  actualAsOfDate?: string | null
  status: "QUEUED" | "RUNNING" | "SUCCEEDED" | "NO_DATA" | "FAILED"
  config: StockSelectionStrategyBacktestConfig
  benchmark: string
  dataSource: string
  warnings: string[]
  summary: StockSelectionStrategyBacktestSummary
  startedAt: string
  completedAt?: string | null
  trades: StockSelectionStrategyBacktestTrade[]
}

export type StockSelectionWatchlistStatus = "WATCHING" | "PAUSED" | "INVALIDATED" | "ARCHIVED"
export type StockSelectionWatchlistDecision = "CONTINUE" | "PAUSE" | "INVALIDATE" | "ARCHIVE"

export interface StockSelectionWatchlistReview {
  id: string
  decision: StockSelectionWatchlistDecision
  note?: string | null
  reviewedAt: string
}

export interface StockSelectionWatchlistItem {
  id: string
  watchlistId: string
  watchlistName: string
  stockCode: string
  stockName?: string | null
  market?: string | null
  entryTradeDate?: string | null
  entryPrice?: number | null
  sourceStrategyCandidateId?: string | null
  status: StockSelectionWatchlistStatus
  attentionReason: string
  invalidationCondition?: string | null
  nextReviewDate?: string | null
  version: string
  updatedAt: string
  reviews: StockSelectionWatchlistReview[]
}

export interface StockSelectionWatchlistResponse {
  items: StockSelectionWatchlistItem[]
}

export interface AddStockSelectionWatchlistItemRequest {
  candidateId: string
  watchlistName?: string
  attentionReason: string
  invalidationCondition?: string
  nextReviewDate?: string
}

export interface StockSelectionWatchlistPerformance {
  watchlistItemId: string
  latestClose?: number | null
  latestTradeDate?: string | null
  returnSinceEntryPct?: number | null
  benchmarkReturnPct?: number | null
  excessReturnPct?: number | null
  dataSource: string
  warnings: string[]
}

export interface StockSelectionWatchlistPerformanceResponse {
  items: StockSelectionWatchlistPerformance[]
  dataSource: string
  warnings: string[]
}

export interface StockSelectionWatchlistPerformancePoint {
  tradeDate: string
  close?: number | null
  returnSinceEntryPct?: number | null
  benchmarkReturnPct?: number | null
  excessReturnPct?: number | null
  warnings: string[]
}

export interface StockSelectionWatchlistPerformanceHistoryResponse {
  watchlistItemId: string
  entryTradeDate?: string | null
  entryPrice?: number | null
  points: StockSelectionWatchlistPerformancePoint[]
  dataSource: string
  warnings: string[]
}

export interface StockSelectionAiCandidateAnalysis {
  code: string
  name?: string | null
  thesis: string
  evidence: string[]
  risks: string[]
  watch: string[]
}

export interface StockSelectionAiAnalysisResult {
  summary: string
  market_context: string
  data_quality: string
  candidates: StockSelectionAiCandidateAnalysis[]
  disclaimer: string
}

export interface StockSelectionAiScreeningResponse {
  ok: boolean
  trade_date?: string | null
  source?: string | null
  universe_count: number
  candidate_count: number
  warnings: string[]
  analysis_result: StockSelectionAiAnalysisResult
  provider_code?: string | null
  model_code?: string | null
  prompt_version?: string | null
}

export interface StockSelectionAiScreeningRequest {
  researchIntent?: string
  maxCandidates?: number
  criteria: Omit<StockScreenerParams, "page" | "pageSize">
}

export interface StockSelectionAiIntentPlanRequest {
  researchIntent?: string
  criteria: Omit<StockScreenerParams, "page" | "pageSize">
}

export interface StockSelectionAiIntentPlanResponse {
  normalizedIntent: string
  criteria: Omit<StockScreenerParams, "page" | "pageSize">
  proposedConditions?: StockSelectionAiProposedCondition[]
  appliedRules: string[]
  assumptions: string[]
  unsupportedPhrases: string[]
  warnings: string[]
  providerCode?: string | null
  modelCode?: string | null
  promptVersion?: string | null
}

export interface StockSelectionAiProposedCondition {
  category: string
  sourcePhrase: string
  label: string
  definition: string
  executionStatus: "EXECUTABLE" | "RESEARCH_ONLY" | "NEEDS_DATA"
  dataRequirements: string[]
}

export async function fetchStockScreener(params: StockScreenerParams = {}): Promise<StockScreenerResponse> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value != null && value !== "") query.set(key, String(value))
  }
  const suffix = query.size ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}${suffix}`, { cache: "no-store" })
  return cynexusResult<StockScreenerResponse>(response)
}

export async function fetchMarketEventFactorObservatory(params: {
  tradeDate?: string
  cohort?: MarketEventCohort
  minLargeMovePct?: number
  limit?: number
  symbol?: string
} = {}): Promise<MarketEventFactorObservatoryResponse> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value != null && value !== "") query.set(key, String(value))
  }
  const suffix = query.size ? `?${query.toString()}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-observatory${suffix}`, { cache: "no-store" })
  return cynexusResult<MarketEventFactorObservatoryResponse>(response)
}

export async function fetchMarketEventReplay(params: { code: string; tradeDate: string; anchorAt?: string; minuteLimit?: number }): Promise<MarketEventReplayResponse> {
  const query = new URLSearchParams({ code: params.code, tradeDate: params.tradeDate })
  if (params.anchorAt) query.set("anchorAt", params.anchorAt)
  if (params.minuteLimit != null) query.set("minuteLimit", String(params.minuteLimit))
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/event-replay?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketEventReplayResponse>(response)
}

export async function createMarketFactorStudy(request: CreateMarketFactorStudyRequest): Promise<MarketFactorStudyResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(request), cache: "no-store",
  })
  return cynexusResult<MarketFactorStudyResponse>(response)
}

export async function cancelMarketFactorStudy(studyId: string): Promise<MarketFactorStudyResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/cancel`, { method: "POST", cache: "no-store" })
  return cynexusResult<MarketFactorStudyResponse>(response)
}

export async function fetchMarketFactorStudies(offset = 0, limit = 50): Promise<MarketFactorStudyResponse[]> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies?offset=${Math.max(0, offset)}&limit=${Math.min(100, Math.max(1, limit))}`, { cache: "no-store" })
  return cynexusResult<MarketFactorStudyResponse[]>(response)
}

export async function fetchMarketFactorDiscoveries(studyId: string, offset = 0, limit = 50): Promise<MarketFactorDiscovery[]> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/discoveries?offset=${Math.max(0, offset)}&limit=${Math.min(100, Math.max(1, limit))}`, { cache: "no-store" })
  return cynexusResult<MarketFactorDiscovery[]>(response)
}

export async function fetchMarketFactorDiscovery(studyId: string, discoveryCode: string): Promise<MarketFactorDiscovery> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/discoveries/${encodeURIComponent(discoveryCode)}`, { cache: "no-store" })
  return cynexusResult<MarketFactorDiscovery>(response)
}

export async function fetchMarketFactorClusters(studyId: string): Promise<MarketFactorCluster[]> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/clusters`, { cache: "no-store" })
  return cynexusResult<MarketFactorCluster[]>(response)
}

export async function fetchMarketFactorStudySamples(studyId: string, sampleType?: "EVENT" | "CONTROL", offset = 0, limit = 50): Promise<MarketFactorStudySample[]> {
  const query = new URLSearchParams({ offset: String(Math.max(0, offset)), limit: String(Math.min(100, Math.max(1, limit)) ) })
  if (sampleType) query.set("sampleType", sampleType)
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/samples?${query}`, { cache: "no-store" })
  return cynexusResult<MarketFactorStudySample[]>(response)
}

export async function fetchMarketFactorStudySequences(studyId: string, sequenceCode?: string, offset = 0, limit = 200): Promise<MarketFactorStudySequencePoint[]> {
  const query = new URLSearchParams({ offset: String(Math.max(0, offset)), limit: String(Math.min(500, Math.max(1, limit)) ) })
  if (sequenceCode) query.set("sequenceCode", sequenceCode)
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/sequences?${query}`, { cache: "no-store" })
  return cynexusResult<MarketFactorStudySequencePoint[]>(response)
}

export async function fetchMarketFactorOutcomes(studyId: string, offset = 0, limit = 50): Promise<MarketFactorStudyOutcome[]> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/studies/${encodeURIComponent(studyId)}/outcomes?offset=${Math.max(0, offset)}&limit=${Math.min(100, Math.max(1, limit))}`, { cache: "no-store" })
  const outcomes = await cynexusResult<MarketFactorStudyOutcome[]>(response)
  return outcomes.map((outcome) => ({
    ...outcome,
    outcomes: Array.isArray(outcome.outcomes) ? outcome.outcomes : [],
  }))
}

export async function fetchMarketFactorSets(offset = 0, limit = 50): Promise<MarketFactorSet[]> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets?offset=${Math.max(0, offset)}&limit=${Math.min(100, Math.max(1, limit))}`, { cache: "no-store" })
  const result = await cynexusResult<{ items: MarketFactorSet[] }>(response)
  return result.items
}

export async function createMarketFactorSet(request: { code: string; name: string; expression: Record<string, unknown>; sourceStudyId?: string | number }): Promise<MarketFactorSet> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(request), cache: "no-store" })
  return cynexusResult<MarketFactorSet>(response)
}

export async function updateMarketFactorSet(code: string, request: { name: string; expression: Record<string, unknown>; sourceStudyId?: string | number; expectedVersion: number }): Promise<MarketFactorSet> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets/${encodeURIComponent(code)}`, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(request), cache: "no-store" })
  return cynexusResult<MarketFactorSet>(response)
}

export async function validateMarketFactorSet(code: string, versionNo: number, validationSnapshot: Record<string, unknown>): Promise<MarketFactorSetVersion> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets/${encodeURIComponent(code)}/versions/validate`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ versionNo, validationSnapshot }), cache: "no-store" })
  return cynexusResult<MarketFactorSetVersion>(response)
}

export async function promoteMarketFactorSet(code: string, versionNo: number): Promise<MarketFactorSetVersion> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets/${encodeURIComponent(code)}/versions/${versionNo}/promote`, { method: "POST", cache: "no-store" })
  return cynexusResult<MarketFactorSetVersion>(response)
}

export async function previewMarketFactorSet(code: string, versionNo: number, tradeDate?: string, limit = 50): Promise<MarketFactorSetPreview> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/factor-research/factor-sets/${encodeURIComponent(code)}/versions/${versionNo}/screen-preview`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ tradeDate, limit }), cache: "no-store",
  })
  return cynexusResult<MarketFactorSetPreview>(response)
}

export async function analyzeMarketEventFactors(request: {
  tradeDate?: string
  cohort?: MarketEventCohort
  minLargeMovePct?: number
  maxEvents?: number
  symbol?: string
}): Promise<MarketEventFactorAnalysisResponse> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/market-event-factors/analysis`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<MarketEventFactorAnalysisResponse>(response)
}

export async function synthesizeMarketFactorStudy(studyId: string): Promise<MarketFactorStudySynthesisResponse> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/market-factor-studies/synthesis`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ studyId }), cache: "no-store",
  })
  return cynexusResult<MarketFactorStudySynthesisResponse>(response)
}

export async function runStockSelectionAiScreening(request: StockSelectionAiScreeningRequest): Promise<StockSelectionAiScreeningResponse> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/stock-selection/screening`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionAiScreeningResponse>(response)
}

export async function parseStockSelectionAiIntent(request: StockSelectionAiIntentPlanRequest): Promise<StockSelectionAiIntentPlanResponse> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.ai}/stock-selection/intent-plan`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionAiIntentPlanResponse>(response)
}

export async function fetchScreenerIndustryOptions(tradeDate?: string): Promise<ScreenerIndustryOption[]> {
  const suffix = tradeDate ? `?tradeDate=${encodeURIComponent(tradeDate)}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/industries${suffix}`, { cache: "no-store" })
  return cynexusResult<ScreenerIndustryOption[]>(response)
}

export async function fetchScreenerClassificationOptions(tradeDate?: string): Promise<ScreenerClassificationOptions> {
  const suffix = tradeDate ? `?tradeDate=${encodeURIComponent(tradeDate)}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/classifications${suffix}`, { cache: "no-store" })
  return cynexusResult<ScreenerClassificationOptions>(response)
}

export async function fetchScreenerStrategies(): Promise<StockSelectionStrategyCatalog> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies`, { cache: "no-store" })
  return cynexusResult<StockSelectionStrategyCatalog>(response)
}

export async function createScreenerStrategy(request: SaveStockSelectionStrategyRequest): Promise<StockSelectionStrategy> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionStrategy>(response)
}

export async function updateScreenerStrategy(strategyCode: string, request: SaveStockSelectionStrategyRequest): Promise<StockSelectionStrategy> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies/${encodeURIComponent(strategyCode)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionStrategy>(response)
}

export async function fetchScreenerStrategyHealth(strategyCode: string, tradeDate?: string): Promise<StockSelectionStrategyHealthResponse> {
  const suffix = tradeDate ? `?tradeDate=${encodeURIComponent(tradeDate)}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies/${encodeURIComponent(strategyCode)}/health${suffix}`, { cache: "no-store" })
  return cynexusResult<StockSelectionStrategyHealthResponse>(response)
}

export async function runScreenerStrategy(strategyCode: string, tradeDate?: string): Promise<StockSelectionStrategyRunResponse> {
  const suffix = tradeDate ? `?tradeDate=${encodeURIComponent(tradeDate)}` : ""
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies/${encodeURIComponent(strategyCode)}/runs${suffix}`, {
    method: "POST",
    cache: "no-store",
  })
  return cynexusResult<StockSelectionStrategyRunResponse>(response)
}

export async function runScreenerStrategyBacktest(strategyCode: string, request: StockSelectionStrategyBacktestRequest): Promise<StockSelectionStrategyBacktestResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/strategies/${encodeURIComponent(strategyCode)}/backtests`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionStrategyBacktestResponse>(response)
}

export async function fetchScreenerStrategyBacktest(backtestId: string): Promise<StockSelectionStrategyBacktestResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/backtests/${encodeURIComponent(backtestId)}`, { cache: "no-store" })
  return cynexusResult<StockSelectionStrategyBacktestResponse>(response)
}

export async function fetchStockSelectionWatchlist(): Promise<StockSelectionWatchlistResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/watchlist`, { cache: "no-store" })
  return cynexusResult<StockSelectionWatchlistResponse>(response)
}

export async function fetchStockSelectionWatchlistPerformance(): Promise<StockSelectionWatchlistPerformanceResponse> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/watchlist/performance`, { cache: "no-store" })
  return cynexusResult<StockSelectionWatchlistPerformanceResponse>(response)
}

export async function fetchStockSelectionWatchlistPerformanceHistory(itemId: string, limit = 180): Promise<StockSelectionWatchlistPerformanceHistoryResponse> {
  const safeLimit = Math.max(30, Math.min(limit, 252))
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/watchlist/${encodeURIComponent(itemId)}/performance/history?limit=${safeLimit}`, { cache: "no-store" })
  return cynexusResult<StockSelectionWatchlistPerformanceHistoryResponse>(response)
}

export async function addStockSelectionWatchlistItem(request: AddStockSelectionWatchlistItemRequest): Promise<StockSelectionWatchlistItem> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/watchlist`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionWatchlistItem>(response)
}

export async function reviewStockSelectionWatchlistItem(itemId: string, decision: StockSelectionWatchlistDecision, note?: string): Promise<StockSelectionWatchlistItem> {
  const response = await fetchWithRetry(`${SCREENER_API_BASE}/watchlist/${encodeURIComponent(itemId)}/reviews`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ decision, note: note?.trim() || undefined }),
    cache: "no-store",
  })
  return cynexusResult<StockSelectionWatchlistItem>(response)
}
