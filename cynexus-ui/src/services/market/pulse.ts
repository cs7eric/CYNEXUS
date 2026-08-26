import { fetchWithRetry } from "@/services/common/http"
import {
  fetchIndustryFundFlow,
  fetchIndustryFundFlowHistory,
  type IndustryFundFlowRow,
} from "@/services/market/industry-application"
import {
  fetchSchedulerJobs,
  triggerSchedulerJob,
  type SchedulerJobItem,
} from "@/services/scheduler/management"

const MARKET_PULSE_API_BASE = `${(typeof import.meta !== "undefined" && import.meta.env?.VITE_CYNEXUS_API_BASE) || ""}/api/market/pulse`
const INDUSTRY_FUND_FLOW_FULL_TOP = 500
const DEFAULT_MARKET_PULSE_DAYS = 10
const DEFAULT_MARKET_PULSE_TOP_N = 10
const DEFAULT_FLOW_SIDE_COUNT = 20
const MARKET_PULSE_INSIDE_JOB_ID = "market_pulse_inside"
const MARKET_PULSE_CLOSE_JOB_ID = "market_pulse_close"
const MARKET_PULSE_CONSTITUENTS_JOB_ID = "market_pulse_constituents"

type ApiResult<T> = {
  success?: boolean
  code?: string
  message?: string
  data?: T
}

async function readPayload<T>(res: Response, fallbackMessage: string): Promise<T> {
  const data = (await res.json().catch(() => null)) as (ApiResult<T> & T) | null
  if (!res.ok || !data) {
    throw new Error((data as { error?: string } | null)?.error || fallbackMessage)
  }
  if (typeof data === "object" && "success" in data && "data" in data) {
    const wrapped = data as ApiResult<T>
    if (wrapped.success === false) {
      throw new Error(wrapped.message || fallbackMessage)
    }
    return wrapped.data as T
  }
  return data as T
}

async function fetchLatestMarketPulseSectorSnapshot(): Promise<MarketPulseSectorSnapshotResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/sector-snapshot/latest`, { cache: "no-store" })
  return readPayload<MarketPulseSectorSnapshotResponse>(res, "获取 market pulse 最新快照失败")
}

async function fetchMarketPulseSectorSnapshotHistory(days: number): Promise<MarketPulseSectorSnapshotHistoryResponse> {
  const params = new URLSearchParams({ days: String(days) })
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/sector-snapshot/history?${params.toString()}`, { cache: "no-store" })
  return readPayload<MarketPulseSectorSnapshotHistoryResponse>(res, "获取 market pulse 历史快照失败")
}

function toPrevDayFlow(value: unknown): MarketOverview["prevDayFlow"] {
  if (!value || typeof value !== "object") return null
  const raw = value as Record<string, unknown>
  return {
    mainNetInflow: (raw.mainNetInflow as number) ?? null,
    superLargeNetInflow: (raw.superLargeNetInflow as number) ?? null,
    largeNetInflow: (raw.largeNetInflow as number) ?? null,
    mediumNetInflow: (raw.mediumNetInflow as number) ?? null,
    smallNetInflow: (raw.smallNetInflow as number) ?? null,
    totalAmount: (raw.totalAmount as number) ?? null,
  }
}

export type PulseRange = "20d" | "60d" | "120d" | "1y"

export interface MarketOverviewEltdx {
  tradingDate: string | null
  fetchedAt: string | null
  totalAmount: number | null
  totalVolume: number | null
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  stockCount: number | null
  prevDayTotalAmount?: number | null
  prevDayTradingDate?: string | null
}

export interface MarketOverview {
  tradingDate: string | null
  fetchedAt: string | null
  source?: string
  isTradeTime?: boolean
  totalAmount: number | null
  totalVolume: number | null
  stockCount: number | null
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  mainNetInflow: number | null
  superLargeNetInflow: number | null
  largeNetInflow: number | null
  mediumNetInflow: number | null
  smallNetInflow: number | null
  mainNetInflowRatio: number | null
  superLargeNetInflowRatio: number | null
  largeNetInflowRatio: number | null
  mediumNetInflowRatio: number | null
  smallNetInflowRatio: number | null
  prevDayFlow: {
    mainNetInflow: number | null
    superLargeNetInflow: number | null
    largeNetInflow: number | null
    mediumNetInflow: number | null
    smallNetInflow: number | null
    totalAmount: number | null
  } | null
  prevDayTradingDate: string | null
  error?: string
}

export interface ManualFundFlow {
  ok?: boolean
  tradingDate: string
  savedAt?: string | null
  source?: string
  mainNetInflow: number | null
  mainNetInflowRatio: number | null
  superLargeNetInflow: number | null
  superLargeNetInflowRatio: number | null
  largeNetInflow: number | null
  largeNetInflowRatio: number | null
  mediumNetInflow: number | null
  mediumNetInflowRatio: number | null
  smallNetInflow: number | null
  smallNetInflowRatio: number | null
}

export interface MarketHistoryPoint {
  date: string
  totalAmount: number | null
  totalVolume: number | null
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  mainNetInflow: number | null
  superLargeNetInflow: number | null
  largeNetInflow: number | null
  mediumNetInflow: number | null
  smallNetInflow: number | null
  source?: string
}

export interface MarketHistoryResponse {
  ok: boolean
  range: PulseRange | string
  source?: string
  count: number
  items: MarketHistoryPoint[]
}

export interface IndexReturnItem {
  name: string
  code: string
  fullCode?: string
  current: number | null
  currentDate: string | null
  baseClose: number | null
  baseDate: string | null
  returnPct: number | null
  daily?: unknown[]
  available?: boolean
}

export interface IndexReturnsResponse {
  ok: boolean
  days: number
  items: IndexReturnItem[]
}

export interface IndexReturnsHistoryItem {
  tradeDate: string
  code: string
  name: string
  current: number | null
  currentDate: string | null
  baseClose: number | null
  baseDate: string | null
  returnPct: number | null
}

export interface IndexReturnsHistoryResponse {
  ok: boolean
  window: number
  start: string
  end: string
  count: number
  items: IndexReturnsHistoryItem[]
}

export interface IndexKlinePoint {
  time: string
  timestamp: number
  open: number
  high: number
  low: number
  close: number
  volume?: number | null
  turnover?: number | null
}

export interface IndexKlineItem {
  ok: boolean
  code: string
  name: string
  date: string
  interval: string
  previousClose: number | null
  source?: string
  points: IndexKlinePoint[]
  error?: string
}

export interface IndexKlineBatchResponse {
  ok: boolean
  date: string
  interval: string
  items: IndexKlineItem[]
}

export interface StyleSectorItem {
  name: string
  change_pct: number | null
  valid_size: number
  sample_size: number
}

export interface StyleSectorListResponse {
  ok: boolean
  items: StyleSectorItem[]
  count?: number
  names?: string[]
}

export interface StyleSectorConstituent {
  code: string
  name: string
  valid?: boolean
  price?: number | null
  last_price?: number | null
  changePct?: number | null
  change_pct?: number | null
  change_amount?: number | null
  turnover_rate?: number | null
  amplitude?: number | null
  turnover_amount?: number | null
  circulating_shares?: number | null
  circulating_market_cap?: number | null
  amount?: number | null
  industry?: string | null
  concepts?: string[]
}

export interface StyleSectorConstituentsResponse {
  ok: boolean
  error?: string
  name: string
  codes: string[]
  constituents: StyleSectorConstituent[]
  sample_size?: number
  valid_size?: number
  change_pct?: number | null
  fetched_at?: string
}

export type HeatmapKind = "industries" | "concepts"

export interface MarketHeatmapItem {
  name: string
  value?: number
  change_pct?: number | null
  changePct?: number | null
  count?: number
  amount?: number | null
  [key: string]: unknown
}

export interface MarketHeatmapResponse {
  ok: boolean
  kind: HeatmapKind | string
  source?: string
  count?: number
  items: MarketHeatmapItem[]
  [key: string]: unknown
}

export type MarketPulseOptions = {
  refreshRotation?: boolean
  days?: number
  topN?: number
}

type MarketPulseSectorSnapshotRow = {
  tradeDate: string
  sectorName: string
  sectorIndex?: string | null
  rankByChange?: number | null
  changePct?: number | null
  inflow?: number | null
  outflow?: number | null
  mainNet?: number | null
  stockCount?: number | null
  leadingStock?: string | null
  leadingChangePct?: number | null
  leadingPrice?: number | null
  sourceKind?: string | null
  sourceName?: string | null
  capturedAt?: string | null
}

type MarketPulseSectorSnapshotResponse = {
  ok: boolean
  tradeDate?: string | null
  requestedTradeDate?: string | null
  fallbackTradeDate?: boolean
  servingMode?: string | null
  sourceKind?: string | null
  sourceName?: string | null
  status?: string | null
  fetchedAt?: string | null
  rowCount?: number
  rows?: MarketPulseSectorSnapshotRow[]
  error?: string | null
}

type MarketPulseSectorSnapshotHistoryResponse = {
  ok: boolean
  days: number
  dates: string[]
  rowCount?: number
  sourceKind?: string | null
  sourceName?: string | null
  fetchedAt?: string | null
  rows?: MarketPulseSectorSnapshotRow[]
  error?: string | null
}

export type StrongRow = {
  name: string
  changePct?: number
  changePercent?: number
  amount?: number
  leadingStock?: string | null
  leadingChangePct?: number | null
  stockCount?: number
}

export type FlowRow = {
  name: string
  changePct?: number
  mainNet: number
  inflow?: number
  outflow?: number
  stockCount?: number
  leadingStock?: string | null
  leadingChangePct?: number | null
  leadingPrice?: number | null
}

export type RotationItem = FlowRow & {
  changePct: number
  rank: number
}

export type RotationRow = {
  date: string
  topN: number
  items: RotationItem[]
}

export type MarketPulse = {
  ok: boolean
  strong: {
    ok: boolean
    top: StrongRow[]
    bottom: StrongRow[]
    fetchedAt?: string | null
    count?: number
    tradeDate?: string | null
    source?: string | null
    sourceKind?: string | null
    servingMode?: string | null
    requestedTradeDate?: string | null
    isFallbackTradeDate?: boolean
  }
  flow: {
    ok: boolean
    inflow: FlowRow[]
    outflow: FlowRow[]
    inflowCount?: number
    outflowCount?: number
    elapsedMs?: number
    kind?: string
    source?: string | null
    sourceKind?: string | null
    unit?: string
    tradeDate?: string | null
    servingMode?: string | null
    requestedTradeDate?: string | null
    isFallbackTradeDate?: boolean
  }
  rotation: {
    ok: boolean
    dates: string[]
    rows: RotationRow[]
    topN?: number
    tradeDate?: string | null
    source?: string | null
    sourceKind?: string | null
    servingMode?: string | null
    requestedTradeDate?: string | null
    isFallbackTradeDate?: boolean
  }
}

export type RotationTrendData = {
  ok: boolean
  topN: number
  days: number
  compositeWindowDays?: number
  dates: string[]
  industries: Array<{
    name: string
    appearances: number
    avgRank: number | null
    bestRank: number | null
    worstRank: number | null
    latestRank: number | null
    latestChangePct: number | null
    avgMainNet10?: number | null
    appearanceRate?: number | null
    avgRankScore?: number | null
    flowScore?: number | null
    compositeScore?: number | null
    compositeRank?: number | null
    ranks: (number | null)[]
    changePcts: (number | null)[]
  }>
}

export type IndustryCompareResponse = {
  ok: boolean
  days: number
  dates: string[]
  requestedIndustries: string[]
  count: number
  industries: Array<{
    name: string
    days: number
    appearances: number
    latestMainNet: number | null
    latestRank: number | null
    latestChangePct: number | null
    averages: Record<"5" | "10" | "30" | "60", number | null>
    points: Array<{ date: string; mainNet: number | null; rank: number | null; changePct: number | null }>
  }>
}

export type IndustryFundFlowIndustryListResponse = {
  ok: boolean
  days: number
  count: number
  items: Array<{ industry: string; industryCode?: string | null; days: number; firstDate?: string | null; lastDate?: string | null }>
  error?: string
}

export type IndustryDetail = {
  ok: boolean
  name: string
  changePct?: number
  mainNet?: number
  inflow?: number
  outflow?: number
  stockCount?: number
  leadingStock?: string | null
  leadingChangePct?: number | null
  leadingQuote?: Record<string, unknown> | null
  leadingKLine?: Array<Record<string, unknown>>
  leadingFlow30d?: Array<{ date?: string; mainNet?: number; largeNet?: number; mediumNet?: number; smallNet?: number }>
  leadingFlowSeed?: string
  constituents?: unknown[]
  error?: string
}

export type SchedulerStatus = {
  isRunning?: boolean
  schedulerStartedAt?: string
  lastRunAt?: string | null
  lastRunOk?: boolean | null
  lastInsideRefreshAt?: string | null
  lastCloseSnapshotAt?: string | null
  totalInside?: number
  totalClose?: number
  insideIntervalSeconds?: number
  closeSnapshotCron?: string
  isTradeTime?: boolean
  isTradingDay?: boolean
  now?: string
  lastTopN?: Array<{ name?: string; changePct?: number }>
}

export interface IndustryConstituentRow {
  "序号": number
  "代码": string
  "名称": string | null
  "现价": number | string | null
  "涨跌幅(%)": number | string | null
  "涨跌": number | string | null
  "涨速(%)": number | string | null
  "换手(%)": number | string | null
  "量比": number | string | null
  "振幅(%)": number | string | null
  "成交额": string | number | null
  "流通股": string | number | null
  "流通市值": string | number | null
  "市盈率": string | number | null
}

export interface IndustryConstituentsIndexResponse {
  ok: boolean
  error?: string
  name: string
  code: string
  count: number
  matched: number
  indexFetchedAt: string | null
  rowsFetchedAt: string | null
  dataSource: string
  isTradingDay: boolean
  isMarketOpen: boolean
  tradingHoursMode: "trading" | "trading_day_off_hours" | "non_trading_day"
  snapshotDate: string
  rows: IndustryConstituentRow[]
}

export type LimitEmotionStreakSentimentLevel =
  | "ice"
  | "weak"
  | "normal"
  | "warm"
  | "active"
  | "hot"

export type LimitEmotionMarketStatus = "pre_open" | "trading" | "closed" | "unknown"
export type LimitEmotionDataStatus = "normal" | "partial" | "stale" | "empty"
export type LimitEmotionBreakBoardStatus = "ready" | "unavailable"

export interface LimitEmotionStock {
  code: string
  name: string
  streak?: number | null
  previousStreak?: number | null
  changePct?: number | null
  industry?: string | null
  concepts?: string[]
  limitUpPrice?: number | null
  limitDownPrice?: number | null
}

export interface LimitEmotionLeader extends LimitEmotionStock {
  streak: number
}

export interface LimitEmotionDistributionRow {
  streak: number
  height?: number
  count: number
  stocks: LimitEmotionStock[]
}

export interface LimitEmotionPromotionLevel {
  from: number
  to: number
  yesterdayCount: number
  todayPromotedCount: number
  rate: number | null
}

export interface LimitEmotionPromotion {
  overallRate: number | null
  levels: LimitEmotionPromotionLevel[]
}

export interface LimitEmotionBrokenStock extends LimitEmotionStock {
  previousStreak: number
}

export interface LimitEmotionBroken {
  count: number
  highStreakBrokenCount: number
  stocks: LimitEmotionBrokenStock[]
}

export interface LimitEmotionStreak {
  maxHeight: number | null
  label: "连板高度"
  leaders: LimitEmotionLeader[]
  distribution: LimitEmotionDistributionRow[]
  promotion: LimitEmotionPromotion
  broken: LimitEmotionBroken
  sentiment: {
    level: LimitEmotionStreakSentimentLevel
    text: string
  }
}

export interface LimitEmotionPayload {
  ok?: boolean
  limitUp: { count: number | null; label: string; stocks?: LimitEmotionStock[] }
  limitDown: { count: number | null; label: string; stocks?: LimitEmotionStock[] }
  breakBoard: {
    rate: number | null
    label: string
    status: LimitEmotionBreakBoardStatus
    touchedCount?: number | null
    brokenCount?: number | null
    brokenStocks?: LimitEmotionStock[]
  }
  streak: LimitEmotionStreak
  tradeDate: string | null
  updateTime: string | null
  marketStatus: LimitEmotionMarketStatus | null
  dataStatus: LimitEmotionDataStatus
  above5PctCount?: number | null
  promotedCount?: number | null
  medianChangePct?: number | null
  advancingCount?: number | null
  decliningCount?: number | null
  flatCount?: number | null
  error?: string
}

export async function fetchMarketOverviewEltdx(): Promise<MarketOverviewEltdx> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-eltdx`)
  return readPayload<MarketOverviewEltdx>(res, "获取全A成交额/涨跌家数失败")
}

export async function fetchMarketOverviewAkshare(): Promise<MarketOverview> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-akshare`)
  const data = await readPayload<MarketOverview>(res, "获取大盘成交额/主力净流入失败")
  return { ...data, prevDayFlow: toPrevDayFlow(data.prevDayFlow) }
}

export async function triggerMarketOverviewAkshareRefresh(): Promise<{ ok: boolean; snapshot?: MarketOverview; error?: string }> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-akshare/refresh`, { method: "POST" })
  const snapshot = await readPayload<MarketOverview>(res, "刷新大盘成交额/主力净流入失败")
  return { ok: true, snapshot }
}

export async function fetchManualFundFlow(tradingDate: string): Promise<ManualFundFlow | null> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-manual-fund-flow?tradingDate=${encodeURIComponent(tradingDate)}`)
  if (res.status === 404) return null
  return readPayload<ManualFundFlow>(res, "获取手工资金流失败")
}

export async function saveManualFundFlow(payload: Partial<ManualFundFlow> & { tradingDate: string }): Promise<ManualFundFlow> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-manual-fund-flow`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return readPayload<ManualFundFlow>(res, "保存手工资金流失败")
}

export async function fetchMarketOverviewAkshareArchive(tradingDate: string): Promise<MarketOverview> {
  const normalized = tradingDate.replace(/-/g, "")
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-akshare/archive/${normalized}`)
  const data = await readPayload<MarketOverview>(res, `获取 ${tradingDate} 大盘归档失败`)
  return { ...data, prevDayFlow: toPrevDayFlow(data.prevDayFlow) }
}

export async function fetchMarketPulseHistory(range: PulseRange = "60d"): Promise<MarketHistoryResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/market-overview-akshare/history?range=${encodeURIComponent(range)}`)
  return readPayload<MarketHistoryResponse>(res, "获取 Market Pulse 历史序列失败")
}

export async function fetchMarketPulseIndexReturns(days = 5): Promise<IndexReturnsResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/index-returns?days=${days}`)
  return readPayload<IndexReturnsResponse>(res, "获取宽基指数收益失败")
}

export async function fetchMarketPulseIndexReturnsHistory(
  window: number,
  start: string,
  end: string,
): Promise<IndexReturnsHistoryResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/index-returns/history?window=${window}&start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`)
  return readPayload<IndexReturnsHistoryResponse>(res, "获取宽基指数收益历史失败")
}

export async function fetchIndexKlineBatch(params: {
  codes: string[]
  date: string
  interval?: string
}): Promise<IndexKlineBatchResponse> {
  const query = new URLSearchParams({
    codes: params.codes.join(","),
    date: params.date,
    interval: params.interval ?? "1m",
  })
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/index-kline/batch?${query.toString()}`)
  return readPayload<IndexKlineBatchResponse>(res, "获取指数K线失败")
}

export async function fetchStyleSectors(): Promise<StyleSectorListResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/style-sectors`)
  return readPayload<StyleSectorListResponse>(res, "获取风格板块失败")
}

export async function fetchStyleSectorConstituents(name: string): Promise<StyleSectorConstituentsResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/style-sectors/${encodeURIComponent(name)}/constituents`)
  return readPayload<StyleSectorConstituentsResponse>(res, `获取 ${name} 成分股失败`)
}

export async function fetchIndustryConstituentsByCode(code: string): Promise<IndustryConstituentsIndexResponse> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/industries/${encodeURIComponent(code)}/constituents`)
  return readPayload<IndustryConstituentsIndexResponse>(res, `读取行业 ${code} 成分股失败`)
}

export async function fetchMarketHeatmap(kind: HeatmapKind = "industries", top_n = 200): Promise<MarketHeatmapResponse> {
  const params = new URLSearchParams({ kind, top_n: String(top_n) })
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/heatmap?${params.toString()}`)
  return readPayload<MarketHeatmapResponse>(res, "拉取市场热力图失败")
}

function toNumber(value: unknown): number | null {
  if (value == null || value === "") return null
  if (typeof value === "number") return Number.isFinite(value) ? value : null
  const normalized = String(value).replace(/,/g, "").trim()
  if (!normalized || normalized === "-") return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function yiToYuan(value: number | null | undefined): number | undefined {
  return value == null ? undefined : value * 1e8
}

function rowIndustry(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): string {
  return "sectorName" in row ? row.sectorName ?? "" : ((row as IndustryFundFlowRow)["行业"] ?? "")
}

function rowChangePct(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): number | null {
  return "changePct" in row ? toNumber(row.changePct) : toNumber((row as IndustryFundFlowRow)["行业指数涨跌幅"])
}

function rowNet(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): number {
  return "mainNet" in row ? (toNumber(row.mainNet) ?? 0) : (toNumber((row as IndustryFundFlowRow)["净额(亿)"]) ?? 0)
}

function rowRank(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow, fallback: number): number {
  return "rankByChange" in row ? (toNumber(row.rankByChange) ?? fallback) : (toNumber((row as IndustryFundFlowRow)["序号"]) ?? fallback)
}

function toFlowRow(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): FlowRow {
  return {
    name: rowIndustry(row),
    changePct: rowChangePct(row) ?? undefined,
    mainNet: rowNet(row),
    inflow: "inflow" in row ? (toNumber(row.inflow) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["流入资金(亿)"]) ?? undefined),
    outflow: "outflow" in row ? (toNumber(row.outflow) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["流出资金(亿)"]) ?? undefined),
    stockCount: "stockCount" in row ? (toNumber(row.stockCount) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["公司家数"]) ?? undefined),
    leadingStock: "leadingStock" in row ? (row.leadingStock ?? null) : ((row as IndustryFundFlowRow)["领涨股"] ?? null),
    leadingChangePct: "leadingChangePct" in row ? (toNumber(row.leadingChangePct) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["领涨股涨跌幅"]) ?? undefined),
    leadingPrice: "leadingPrice" in row ? (toNumber(row.leadingPrice) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["当前价(元)"]) ?? undefined),
  }
}

function toStrongRow(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): StrongRow {
  const changePct = rowChangePct(row) ?? undefined
  return {
    name: rowIndustry(row),
    changePct,
    changePercent: changePct,
    amount: yiToYuan(rowNet(row)),
    leadingStock: "leadingStock" in row ? (row.leadingStock ?? null) : ((row as IndustryFundFlowRow)["领涨股"] ?? null),
    leadingChangePct: "leadingChangePct" in row ? (toNumber(row.leadingChangePct) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["领涨股涨跌幅"]) ?? undefined),
    stockCount: "stockCount" in row ? (toNumber(row.stockCount) ?? undefined) : (toNumber((row as IndustryFundFlowRow)["公司家数"]) ?? undefined),
  }
}

function toRotationItem(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow, fallbackRank: number): RotationItem {
  return {
    ...toFlowRow(row),
    changePct: rowChangePct(row) ?? 0,
    rank: rowRank(row, fallbackRank),
  }
}

function rowTradeDate(row: IndustryFundFlowRow | MarketPulseSectorSnapshotRow): string | null {
  return "tradeDate" in row ? (row.tradeDate ?? null) : (row.tradeDate ?? null)
}

function groupRowsByDate<T extends IndustryFundFlowRow | MarketPulseSectorSnapshotRow>(rows: T[]): Map<string, T[]> {
  const out = new Map<string, T[]>()
  for (const row of rows) {
    const date = rowTradeDate(row)
    if (!date) continue
    const bucket = out.get(date) ?? []
    bucket.push(row)
    out.set(date, bucket)
  }
  return out
}

function buildRotationRows(rows: Array<IndustryFundFlowRow | MarketPulseSectorSnapshotRow>, dates: string[], topN: number): RotationRow[] {
  const byDate = groupRowsByDate(rows)
  return dates.map((date) => ({
    date,
    topN,
    items: (byDate.get(date) ?? []).slice(0, topN).map((row, index) => toRotationItem(row, index + 1)),
  }))
}

function average(values: Array<number | null | undefined>): number | null {
  const filtered = values.filter((value): value is number => value != null && Number.isFinite(value))
  if (!filtered.length) return null
  return filtered.reduce((sum, value) => sum + value, 0) / filtered.length
}

function buildRotationTrend(rows: Array<IndustryFundFlowRow | MarketPulseSectorSnapshotRow>, dates: string[], days: number, topN: number): RotationTrendData {
  const byDate = groupRowsByDate(rows)
  const names = [...new Set(rows.map(rowIndustry).filter(Boolean))]
  const draft = names.map((name) => {
    const ranks = dates.map((date) => {
      const foundIndex = (byDate.get(date) ?? []).findIndex((row) => rowIndustry(row) === name)
      if (foundIndex < 0) return null
      return rowRank((byDate.get(date) ?? [])[foundIndex], foundIndex + 1)
    })
    const changePcts = dates.map((date) => {
      const found = (byDate.get(date) ?? []).find((row) => rowIndustry(row) === name)
      return found ? rowChangePct(found) : null
    })
    const mainNets = dates.map((date) => {
      const found = (byDate.get(date) ?? []).find((row) => rowIndustry(row) === name)
      return found ? rowNet(found) : null
    })
    const validRanks = ranks.filter((rank): rank is number => rank != null)
    const avgRank = average(validRanks)
    const avgMainNet10 = average(mainNets.slice(-10))
    const appearanceRate = dates.length ? validRanks.length / dates.length : 0
    const avgRankScore = avgRank == null ? null : Math.max(0, ((topN - avgRank + 1) / topN) * 100)
    return {
      name,
      appearances: validRanks.length,
      avgRank,
      bestRank: validRanks.length ? Math.min(...validRanks) : null,
      worstRank: validRanks.length ? Math.max(...validRanks) : null,
      latestRank: ranks[ranks.length - 1] ?? null,
      latestChangePct: changePcts[changePcts.length - 1] ?? null,
      avgMainNet10,
      appearanceRate,
      avgRankScore,
      flowScore: null as number | null,
      compositeScore: null as number | null,
      compositeRank: null as number | null,
      ranks,
      changePcts,
    }
  })

  const flowValues = draft.map((item) => item.avgMainNet10).filter((value): value is number => value != null)
  const minFlow = flowValues.length ? Math.min(...flowValues) : 0
  const maxFlow = flowValues.length ? Math.max(...flowValues) : 0
  const scored = draft.map((item) => {
    const flowScore =
      item.avgMainNet10 == null
        ? null
        : maxFlow === minFlow
          ? 50
          : ((item.avgMainNet10 - minFlow) / (maxFlow - minFlow)) * 100
    const compositeScore =
      (item.avgRankScore ?? 0) * 0.6 + item.appearanceRate * 100 * 0.25 + (flowScore ?? 0) * 0.15
    return { ...item, flowScore, compositeScore }
  })
  scored
    .sort((a, b) => (b.compositeScore ?? -Infinity) - (a.compositeScore ?? -Infinity))
    .forEach((item, index) => {
      item.compositeRank = index + 1
    })

  return {
    ok: scored.length > 0,
    topN,
    days,
    compositeWindowDays: Math.min(30, dates.length),
    dates,
    industries: scored,
  }
}

function buildIndustryCompare(
  rows: Array<IndustryFundFlowRow | MarketPulseSectorSnapshotRow>,
  dates: string[],
  requestedIndustries: string[],
  days: number,
): IndustryCompareResponse {
  const byDate = groupRowsByDate(rows)
  const industries = requestedIndustries.map((name) => {
    const points = dates.map((date) => {
      const dayRows = byDate.get(date) ?? []
      const foundIndex = dayRows.findIndex((row) => rowIndustry(row) === name)
      if (foundIndex < 0) {
        return { date, mainNet: null, rank: null, changePct: null }
      }
      const row = dayRows[foundIndex]
      return {
        date,
        mainNet: rowNet(row),
        rank: rowRank(row, foundIndex + 1),
        changePct: rowChangePct(row),
      }
    })
    const latest = [...points].reverse().find((point) => point.mainNet != null || point.rank != null)
    return {
      name,
      days,
      appearances: points.filter((point) => point.rank != null).length,
      latestMainNet: latest?.mainNet ?? null,
      latestRank: latest?.rank ?? null,
      latestChangePct: latest?.changePct ?? null,
      averages: {
        "5": average(points.slice(-5).map((point) => point.mainNet)),
        "10": average(points.slice(-10).map((point) => point.mainNet)),
        "30": average(points.slice(-30).map((point) => point.mainNet)),
        "60": average(points.slice(-60).map((point) => point.mainNet)),
      },
      points,
    }
  })
  return {
    ok: true,
    days,
    dates,
    requestedIndustries,
    count: industries.length,
    industries,
  }
}

function isTradingDayNow(now = new Date()): boolean {
  const day = now.getDay()
  return day >= 1 && day <= 5
}

function isTradeTimeNow(now = new Date()): boolean {
  if (!isTradingDayNow(now)) return false
  const minutes = now.getHours() * 60 + now.getMinutes()
  return (minutes >= 9 * 60 + 30 && minutes <= 11 * 60 + 30) || (minutes >= 13 * 60 && minutes <= 15 * 60)
}

function latestRunAt(job?: SchedulerJobItem): string | null {
  return job?.last_run?.last_run_at ?? null
}

function totalRuns(job?: SchedulerJobItem): number | undefined {
  return job?.last_run?.total_runs ?? undefined
}

function latestRunOk(job?: SchedulerJobItem): boolean | null {
  const status = job?.last_run?.last_status
  if (!status) return null
  return status === "success"
}

export async function fetchMarketPulse(options: MarketPulseOptions = {}): Promise<MarketPulse> {
  const startedAt = Date.now()
  const days = Math.max(1, options.days ?? DEFAULT_MARKET_PULSE_DAYS)
  const topN = Math.max(1, options.topN ?? DEFAULT_MARKET_PULSE_TOP_N)

  if (options.refreshRotation) {
    await triggerSchedulerJob(MARKET_PULSE_CLOSE_JOB_ID).catch(() => null)
  }

  const [latest, history] = await Promise.all([
    fetchLatestMarketPulseSectorSnapshot(),
    fetchMarketPulseSectorSnapshotHistory(days),
  ])
  const currentRows = latest.rows ?? []
  const historyRows = history.rows ?? []
  const dates = history.dates ?? []
  const changeSorted = [...currentRows].sort((a, b) => (rowChangePct(b) ?? -Infinity) - (rowChangePct(a) ?? -Infinity))
  const netSorted = [...currentRows].sort((a, b) => rowNet(b) - rowNet(a))
  const inflow = netSorted.filter((row) => rowNet(row) >= 0).slice(0, DEFAULT_FLOW_SIDE_COUNT).map(toFlowRow)
  const outflow = netSorted.filter((row) => rowNet(row) < 0).reverse().slice(0, DEFAULT_FLOW_SIDE_COUNT).map(toFlowRow)

  return {
    ok: latest.ok,
    strong: {
      ok: latest.ok,
      top: changeSorted.slice(0, topN).map(toStrongRow),
      bottom: [...changeSorted].reverse().slice(0, topN).map(toStrongRow),
      fetchedAt: latest.fetchedAt ?? null,
      count: latest.rowCount ?? currentRows.length,
      tradeDate: latest.tradeDate ?? null,
      source: latest.sourceName ?? history.sourceName ?? null,
      sourceKind: latest.sourceKind ?? null,
      servingMode: latest.servingMode ?? null,
      requestedTradeDate: latest.requestedTradeDate ?? latest.tradeDate ?? null,
      isFallbackTradeDate: latest.fallbackTradeDate ?? false,
    },
    flow: {
      ok: latest.ok,
      inflow,
      outflow,
      inflowCount: inflow.length,
      outflowCount: outflow.length,
      elapsedMs: Date.now() - startedAt,
      kind: "industry",
      source: latest.sourceName ?? null,
      sourceKind: latest.sourceKind ?? null,
      unit: "亿",
      tradeDate: latest.tradeDate ?? null,
      servingMode: latest.servingMode ?? null,
      requestedTradeDate: latest.requestedTradeDate ?? latest.tradeDate ?? null,
      isFallbackTradeDate: latest.fallbackTradeDate ?? false,
    },
    rotation: {
      ok: history.ok,
      dates,
      rows: buildRotationRows(historyRows, dates, topN),
      topN,
      tradeDate: latest.tradeDate ?? null,
      source: history.sourceName ?? latest.sourceName ?? null,
      sourceKind: history.sourceKind ?? latest.sourceKind ?? null,
      servingMode: latest.servingMode ?? null,
      requestedTradeDate: latest.requestedTradeDate ?? latest.tradeDate ?? null,
      isFallbackTradeDate: latest.fallbackTradeDate ?? false,
    },
  }
}

export async function fetchMarketPulseRotationTrend(
  days = DEFAULT_MARKET_PULSE_DAYS,
  topN = DEFAULT_MARKET_PULSE_TOP_N,
): Promise<RotationTrendData> {
  const history = await fetchMarketPulseSectorSnapshotHistory(days)
  return buildRotationTrend(history.rows ?? [], history.dates ?? [], days, topN)
}

export async function fetchMarketPulseIndustryCompare(
  industries: string[],
  days = 120,
): Promise<IndustryCompareResponse> {
  const requestedIndustries = [...new Set(industries.map((item) => item.trim()).filter(Boolean))]
  if (!requestedIndustries.length) {
    return { ok: true, days, dates: [], requestedIndustries: [], count: 0, industries: [] }
  }
  const history = await fetchMarketPulseSectorSnapshotHistory(days)
  return buildIndustryCompare(history.rows ?? [], history.dates ?? [], requestedIndustries, days)
}

export async function fetchIndustryFundFlowIndustryList(
  days = 365,
): Promise<IndustryFundFlowIndustryListResponse> {
  const history = await fetchMarketPulseSectorSnapshotHistory(days)
  const meta = new Map<string, { industry: string; industryCode?: string | null; dates: string[] }>()
  for (const row of history.rows ?? []) {
    const industry = rowIndustry(row)
    const date = rowTradeDate(row)
    if (!industry || !date) continue
    const current = meta.get(industry) ?? { industry, industryCode: null, dates: [] }
    current.dates.push(date)
    meta.set(industry, current)
  }
  const items = [...meta.values()]
    .map((item) => {
      const datesForIndustry = [...new Set(item.dates)].sort()
      return {
        industry: item.industry,
        industryCode: item.industryCode,
        days: datesForIndustry.length,
        firstDate: datesForIndustry[0] ?? null,
        lastDate: datesForIndustry[datesForIndustry.length - 1] ?? null,
      }
    })
    .sort((a, b) => a.industry.localeCompare(b.industry, "zh-CN"))
  return { ok: history.ok, days, count: items.length, items, error: history.error ?? undefined }
}

export async function fetchIndustryDetail(name: string, topN = 30): Promise<IndustryDetail> {
  const [latest, history] = await Promise.all([
    fetchIndustryFundFlow({ top: INDUSTRY_FUND_FLOW_FULL_TOP }),
    fetchIndustryFundFlowHistory({ days: topN, top: INDUSTRY_FUND_FLOW_FULL_TOP }).catch(() => null),
  ])
  const current = (latest.rows ?? []).find((row) => rowIndustry(row) === name)
  if (!current) {
    return { ok: false, name, error: `未找到行业 ${name} 的资金流快照` }
  }
  const code = current.code
  const constituents = code
    ? await fetchIndustryConstituentsByCode(code)
        .then((result) => result.rows ?? [])
        .catch(() => [])
    : []
  const leadingFlow30d = (history?.rows ?? [])
    .filter((row) => rowIndustry(row) === name)
    .map((row) => ({
      date: row.tradeDate ?? undefined,
      mainNet: yiToYuan(rowNet(row)),
    }))
  const leaderPrice = toNumber(current["当前价(元)"])
  return {
    ok: true,
    name,
    changePct: rowChangePct(current) ?? undefined,
    mainNet: yiToYuan(rowNet(current)),
    inflow: yiToYuan(toNumber(current["流入资金(亿)"])),
    outflow: yiToYuan(toNumber(current["流出资金(亿)"])),
    stockCount: toNumber(current["公司家数"]) ?? undefined,
    leadingStock: current["领涨股"] ?? null,
    leadingChangePct: toNumber(current["领涨股涨跌幅"]) ?? undefined,
    leadingQuote: leaderPrice == null ? null : { lastPrice: leaderPrice, amount: null },
    leadingKLine: [],
    leadingFlow30d,
    leadingFlowSeed: code ?? name,
    constituents,
  }
}

export async function fetchMarketPulseSchedulerStatus(): Promise<SchedulerStatus> {
  const data = await fetchSchedulerJobs()
  const jobs = data.items ?? []
  const inside = jobs.find((job) => job.id === MARKET_PULSE_INSIDE_JOB_ID)
  const close = jobs.find((job) => job.id === MARKET_PULSE_CLOSE_JOB_ID)
  const constituents = jobs.find((job) => job.id === MARKET_PULSE_CONSTITUENTS_JOB_ID)
  const runTimes = [latestRunAt(inside), latestRunAt(close), latestRunAt(constituents)].filter((item): item is string => Boolean(item))
  const sortedRunTimes = runTimes.sort()
  const lastRunAt = sortedRunTimes[sortedRunTimes.length - 1] ?? null
  const runningStatuses = new Set(["running", "processing"])
  return {
    isRunning: jobs.some((job) => runningStatuses.has(job.progress?.status ?? "")),
    schedulerStartedAt: inside?.registered_at ?? close?.registered_at,
    lastRunAt,
    lastRunOk: latestRunOk(close) ?? latestRunOk(inside),
    lastInsideRefreshAt: latestRunAt(inside),
    lastCloseSnapshotAt: latestRunAt(close),
    totalInside: totalRuns(inside),
    totalClose: totalRuns(close),
    insideIntervalSeconds: 600,
    closeSnapshotCron: "16:00",
    isTradeTime: isTradeTimeNow(),
    isTradingDay: isTradingDayNow(),
    now: new Date().toISOString(),
  }
}

export async function triggerMarketPulseSnapshot(): Promise<Record<string, unknown>> {
  const result = await triggerSchedulerJob(MARKET_PULSE_CLOSE_JOB_ID)
  return result as unknown as Record<string, unknown>
}

export async function fetchMarketPulseLimitEmotion(date?: string): Promise<LimitEmotionPayload> {
  const query = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/limit-emotion${query}`, { cache: "no-store" })
  return readPayload<LimitEmotionPayload>(res, "获取涨跌停情绪失败")
}

export async function refreshMarketPulseLimitEmotion(): Promise<LimitEmotionPayload> {
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/limit-emotion/refresh`, { method: "POST" })
  return readPayload<LimitEmotionPayload>(res, "刷新涨跌停情绪失败")
}
