import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchEasyTdxFundFlow } from "@/services/market/easy-tdx"
import type {
  StockAdjust,
  StockAnnotation,
  StockAuctionSnapshot,
  StockIntradayResponse,
  StockKlineBar,
  StockPeriod,
  StockSearchItem,
  StockTargetType,
} from "@/views/stock-chart/lib/types"

const STOCK_CHART_API_BASE = `${SERVICE_PREFIX.market}/stock-chart`

interface DataJobTriggerResult {
  accepted: boolean
  status: string
  result?: {
    success?: boolean
    errors?: string[]
    metadata?: {
      dataset_results?: Record<string, {
        history_complete_count?: number
        daily_fallback_count?: number
        sources?: string[]
      }>
    }
  }
}

export async function backfillStockFundFlowHistory(symbol: string): Promise<void> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/scheduler/data-jobs/sync_a_stock_dataset/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      triggeredBy: "application-analysis-fund-flow-backfill",
      dryRun: false,
      params: {
        codes: [symbol],
        datasets: ["eastmoney_fund_flow_120d"],
        fund_flow_mode: "history",
        fund_flow_provider: "easy_tdx",
        create_table: true,
        ignore_errors: false,
      },
    }),
  })
  const trigger = await cynexusResult<DataJobTriggerResult>(response)
  const dataset = trigger.result?.metadata?.dataset_results?.eastmoney_fund_flow_120d
  if (!trigger.accepted || trigger.status !== "success" || trigger.result?.success === false) {
    throw new Error(trigger.result?.errors?.[0] || "Fund-flow backfill failed")
  }
  if ((dataset?.history_complete_count ?? 0) < 1) {
    const source = dataset?.sources?.join(", ") || "EastMoney Push2His"
    if ((dataset?.daily_fallback_count ?? 0) > 0) {
      throw new Error(`${source} only returned the latest day; history is temporarily rate-limited`)
    }
    throw new Error("EastMoney Push2His did not return complete history")
  }
}

export interface StockMetaResponse {
  symbol: string
  name: string
  totalMarketCap: number | null
  circMarketCap: number | null
  industry: string | null
  capStyle: "large" | "mid" | "small" | "micro" | null
  sectorIndexSymbol?: string | null
  sectorIndexName?: string | null
}

export interface MarketBreadthResponse {
  upCount: number | null
  downCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  breakRate: number | null
  maxLianBan: number | null
  yesterdayLimitUpReturn: number | null
  totalTurnover: number | null
  downOver5Count: number | null
  new20HighCount: number | null
  new20LowCount: number | null
}

export interface StockFundFlowPoint {
  trade_date: string
  lastPrice?: number | null
  turnoverRate?: number | null
  activeNetAmountWan: number | null
  mainNetAmountWan: number | null
  superNetAmountWan: number | null
  largeNetAmountWan: number | null
  mediumNetAmountWan: number | null
  smallNetAmountWan: number | null
  activeBuyRatio: number | null
  activeSellRatio: number | null
  buyBigRatio?: number | null
  sellBigRatio?: number | null
  amountWan: number | null
  changePct: number | null
}

export interface StockFundFlowResponse {
  ok: boolean
  symbol: string
  name?: string | null
  trade_date?: string | null
  lastPrice?: number | null
  changePct?: number | null
  amountWan?: number | null
  turnoverRate?: number | null
  activeNetAmountWan?: number | null
  mainNetAmountWan?: number | null
  superNetAmountWan?: number | null
  largeNetAmountWan?: number | null
  mediumNetAmountWan?: number | null
  smallNetAmountWan?: number | null
  activeBuyRatio?: number | null
  activeSellRatio?: number | null
  buyBigRatio?: number | null
  sellBigRatio?: number | null
  source?: string | null
  providerAttempts?: Array<{ provider: string; ok: boolean; rows?: number; error?: string }> | null
  captured_at?: string | null
  error?: string | null
  history: StockFundFlowPoint[]
}

export interface StockSectorEntry {
  sector_id?: string | null
  name: string | null
  topic_id: string | null
  category_raw: number | null
  changePercent: number | null
  source?: "sectors" | "eltdx" | null
}

export interface StockSectorsResponse {
  code: string
  industries: StockSectorEntry[]
  concepts: StockSectorEntry[]
  styles: StockSectorEntry[]
  count: number
  source: string
}

export interface SectorMeta {
  sector_id: string | null
  name: string | null
  category_raw: number | null
  category_label: string | null
  topic_id: string | null
  source?: string | null
}

export interface SectorStock {
  symbol: string
  name: string | null
  market: string | null
  relation_level: number | null
  trade_date?: string | null
  last_price?: number | null
  pre_close?: number | null
  open?: number | null
  high?: number | null
  low?: number | null
  change?: number | null
  change_pct?: number | null
  volume_lots?: number | null
  amount_wan?: number | null
  turnover_rate?: number | null
  amplitude?: number | null
  total_market_cap?: number | null
  float_market_cap?: number | null
  return_pct_5d?: number | null
  return_pct_10d?: number | null
  return_pct_30d?: number | null
  return_pct_60d?: number | null
  return_pct_252d?: number | null
  captured_at?: string | null
  source?: string | null
}

export interface SectorStocksResponse {
  sector: SectorMeta | null
  stocks: SectorStock[]
  count: number
  source: string
}

export interface AuctionAiAnalysisResponse {
  analysis_input: Record<string, unknown>
  analysis_result: Record<string, unknown>
  raw_result?: Record<string, unknown>
  raw_root_keys?: string[] | null
  dump_paths?: Record<string, string>
}

const annotationStoreKey = (targetType: StockTargetType, symbol: string, period: string) =>
  `cynexus.stock-chart.annotations.${targetType}.${symbol}.${period}`

function readLocalAnnotations(targetType: StockTargetType, symbol: string, period: string): StockAnnotation[] {
  if (typeof window === "undefined") return []
  const raw = window.localStorage.getItem(annotationStoreKey(targetType, symbol, period))
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as StockAnnotation[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeLocalAnnotations(targetType: StockTargetType, symbol: string, period: string, items: StockAnnotation[]) {
  if (typeof window === "undefined") return
  window.localStorage.setItem(annotationStoreKey(targetType, symbol, period), JSON.stringify(items))
}

export async function searchStockChart(query: string): Promise<StockSearchItem[]> {
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/search?q=${encodeURIComponent(query)}`, { cache: "no-store" })
  const data = await cynexusResult<{ items?: StockSearchItem[] }>(res)
  return data.items ?? []
}

export async function fetchStockSectors(code: string): Promise<StockSectorsResponse> {
  const query = new URLSearchParams({ symbol: code })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/stock-sectors?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchSectorStocks(params: {
  sectorId?: string | null
  categoryRaw?: number | null
  topicId?: string | null
  name?: string | null
  limit?: number
}): Promise<SectorStocksResponse> {
  const query = new URLSearchParams()
  if (params.sectorId) query.set("sector_id", params.sectorId)
  if (params.categoryRaw != null) query.set("category_raw", String(params.categoryRaw))
  if (params.topicId) query.set("topic_id", params.topicId)
  if (params.name) query.set("name", params.name)
  if (params.limit != null) query.set("limit", String(params.limit))
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/sector-stocks?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<SectorStocksResponse>(res)
  return { ...data, stocks: data.stocks ?? [] }
}

export async function fetchStockKlines(params: {
  targetType: StockTargetType
  symbol: string
  name?: string
  period: StockPeriod
  adjust: StockAdjust
}): Promise<{ symbol: string; target_type: StockTargetType; period: StockPeriod; adjust: StockAdjust; items: StockKlineBar[] }> {
  const query = new URLSearchParams({
    target_type: params.targetType,
    symbol: params.symbol,
    name: params.name || params.symbol,
    period: params.period,
    adjust: params.adjust,
  })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/klines?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchStockAuction(symbol: string, tradeDate?: string | null): Promise<StockAuctionSnapshot> {
  const query = new URLSearchParams({ symbol })
  if (tradeDate) query.set("trade_date", tradeDate)
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/auction?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchStockAuctionHistory(symbol: string, limit = 30): Promise<{ symbol: string; source?: string; items: StockAuctionSnapshot[] }> {
  const query = new URLSearchParams({ symbol, limit: String(limit) })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/auction/history?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchStockIntraday(params: {
  targetType: StockTargetType
  symbol: string
  name?: string
  adjust: StockAdjust
  tradeDate?: string
  periods?: StockPeriod[]
}): Promise<StockIntradayResponse> {
  const query = new URLSearchParams({
    target_type: params.targetType,
    symbol: params.symbol,
    name: params.name || params.symbol,
    adjust: params.adjust,
  })
  if (params.tradeDate) query.set("trade_date", params.tradeDate)
  if (params.periods?.length) query.set("periods", params.periods.join(","))
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/intraday?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchStockMeta(params: {
  targetType: StockTargetType
  symbol: string
}): Promise<StockMetaResponse> {
  const query = new URLSearchParams({
    target_type: params.targetType,
    symbol: params.symbol,
  })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/stock-meta?${query.toString()}`, { cache: "no-store" })
  return cynexusResult(res)
}

export async function fetchStockFundFlow(symbol: string): Promise<StockFundFlowResponse> {
  const storedRequest = fetchWithRetry(`${STOCK_CHART_API_BASE}/fund-flow?symbol=${encodeURIComponent(symbol)}`, { cache: "no-store" })
    .then((res) => cynexusResult<StockFundFlowResponse>(res))
  const easyRequest = fetchEasyTdxFundFlow({ code: symbol, history: true, count: 120 }) as Promise<{
    items: Array<Record<string, unknown>>
    source: string
  }>
  const [stored, easy] = await Promise.allSettled([storedRequest, easyRequest])
  const storedData = stored.status === "fulfilled" ? stored.value : null
  if (easy.status === "fulfilled" && easy.value.items.length > 0) {
    const easyHistory = easy.value.items
      .map((row) => toEasyTdxFundFlowPoint(row))
      .filter((row): row is StockFundFlowPoint => row !== null)
    const history = mergeFundFlowHistory(easyHistory, storedData?.history ?? [])
    const latest = history[0]
    const latestMatchesStored = latest?.trade_date === storedData?.trade_date
    return {
      ...(storedData ?? { ok: true, symbol }),
      symbol: storedData?.symbol ?? symbol,
      trade_date: latest?.trade_date ?? storedData?.trade_date ?? null,
      lastPrice: latest?.lastPrice ?? (latestMatchesStored ? storedData?.lastPrice ?? null : null),
      turnoverRate: latest?.turnoverRate ?? (latestMatchesStored ? storedData?.turnoverRate ?? null : null),
      changePct: latest?.changePct ?? null,
      amountWan: latest?.amountWan ?? null,
      activeBuyRatio: latest?.activeBuyRatio ?? null,
      activeSellRatio: latest?.activeSellRatio ?? null,
      mainNetAmountWan: latest?.mainNetAmountWan ?? storedData?.mainNetAmountWan ?? null,
      superNetAmountWan: latest?.superNetAmountWan ?? storedData?.superNetAmountWan ?? null,
      largeNetAmountWan: latest?.largeNetAmountWan ?? storedData?.largeNetAmountWan ?? null,
      mediumNetAmountWan: latest?.mediumNetAmountWan ?? storedData?.mediumNetAmountWan ?? null,
      smallNetAmountWan: latest?.smallNetAmountWan ?? storedData?.smallNetAmountWan ?? null,
      buyBigRatio: latest?.buyBigRatio ?? (latestMatchesStored ? storedData?.buyBigRatio ?? null : null),
      sellBigRatio: latest?.sellBigRatio ?? (latestMatchesStored ? storedData?.sellBigRatio ?? null : null),
      source: easy.value.source || "easy_tdx.tdx",
      error: null,
      history,
    }
  }
  if (storedData) return { ...storedData, history: storedData.history ?? [] }
  if (easy.status === "rejected") throw easy.reason
  throw new Error("资金流暂无数据")
}

function mergeFundFlowHistory(
  primary: StockFundFlowPoint[],
  fallback: StockFundFlowPoint[],
): StockFundFlowPoint[] {
  const byDate = new Map(fallback.map((item) => [item.trade_date, item]))
  const merged = primary.map((item) => {
    const stored = byDate.get(item.trade_date)
    byDate.delete(item.trade_date)
    return mergeFundFlowPoint(item, stored)
  })
  merged.push(...byDate.values())
  return merged.sort((left, right) => right.trade_date.localeCompare(left.trade_date))
}

function mergeFundFlowPoint(primary: StockFundFlowPoint, fallback?: StockFundFlowPoint): StockFundFlowPoint {
  if (!fallback) return primary
  return {
    trade_date: primary.trade_date,
    lastPrice: primary.lastPrice ?? fallback.lastPrice,
    turnoverRate: primary.turnoverRate ?? fallback.turnoverRate,
    activeNetAmountWan: primary.activeNetAmountWan ?? fallback.activeNetAmountWan,
    mainNetAmountWan: primary.mainNetAmountWan ?? fallback.mainNetAmountWan,
    superNetAmountWan: primary.superNetAmountWan ?? fallback.superNetAmountWan,
    largeNetAmountWan: primary.largeNetAmountWan ?? fallback.largeNetAmountWan,
    mediumNetAmountWan: primary.mediumNetAmountWan ?? fallback.mediumNetAmountWan,
    smallNetAmountWan: primary.smallNetAmountWan ?? fallback.smallNetAmountWan,
    activeBuyRatio: primary.activeBuyRatio ?? fallback.activeBuyRatio,
    activeSellRatio: primary.activeSellRatio ?? fallback.activeSellRatio,
    buyBigRatio: primary.buyBigRatio ?? fallback.buyBigRatio,
    sellBigRatio: primary.sellBigRatio ?? fallback.sellBigRatio,
    amountWan: primary.amountWan ?? fallback.amountWan,
    changePct: primary.changePct ?? fallback.changePct,
  }
}

function toEasyTdxFundFlowPoint(row: Record<string, unknown>): StockFundFlowPoint | null {
  const tradeDate = String(row.trade_date ?? row.date ?? "").slice(0, 10)
  if (!tradeDate) return null
  const wan = (value: unknown) => {
    const parsed = typeof value === "number" ? value : Number(value)
    return Number.isFinite(parsed) ? parsed / 10000 : null
  }
  const wanValue = (value: unknown) => {
    const parsed = typeof value === "number" ? value : Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return {
    trade_date: tradeDate,
    lastPrice: numberValue(row.last_price),
    turnoverRate: numberValue(row.turnover_rate),
    activeNetAmountWan: wanValue(row.active_net_amount_wan),
    mainNetAmountWan: wan(row.main_net),
    superNetAmountWan: wan(row.super_net),
    largeNetAmountWan: wan(row.large_net),
    mediumNetAmountWan: wan(row.medium_net ?? row.mid_net),
    smallNetAmountWan: wan(row.small_net),
    activeBuyRatio: wanValue(row.active_buy_ratio),
    activeSellRatio: wanValue(row.active_sell_ratio),
    buyBigRatio: wanValue(row.buy_big_ratio),
    sellBigRatio: wanValue(row.sell_big_ratio),
    amountWan: wan(row.amount),
    changePct: wanValue(row.change_pct),
  }
}

function numberValue(value: unknown): number | null {
  const parsed = typeof value === "number" ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

export async function fetchMarketBreadth(): Promise<MarketBreadthResponse> {
  const [limitRes, maRes, turnoverRes] = await Promise.all([
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/limit-emotion-summary`, { cache: "no-store" }),
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/ma-count`, { cache: "no-store" }),
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/turnover-activity`, { cache: "no-store" }),
  ])
  const [limit, ma, turnover] = await Promise.all([
    cynexusResult<Record<string, unknown>>(limitRes),
    cynexusResult<Record<string, unknown>>(maRes),
    cynexusResult<Record<string, unknown>>(turnoverRes),
  ])
  return {
    upCount: null,
    downCount: null,
    limitUpCount: (limit.limitUpCount as number) ?? null,
    limitDownCount: (limit.limitDownCount as number) ?? null,
    breakRate: (limit.breakBoardRate as number) ?? null,
    maxLianBan: null,
    yesterdayLimitUpReturn: (limit.yesterdayLimitUpAvgReturn as number) ?? null,
    totalTurnover: (turnover.totalAmount as number) ?? null,
    downOver5Count: null,
    new20HighCount: (ma.newHigh252dCount as number) ?? null,
    new20LowCount: (ma.newLow60dCount as number) ?? null,
  }
}

export async function fetchMarketBreadthSeries(): Promise<Array<MarketBreadthResponse & { totalCount: number | null; date: string }>> {
  const end = new Date()
  const start = new Date(end.getTime() - 90 * 86400000)
  const fmt = (date: Date) => date.toISOString().slice(0, 10)
  const [limitRes, maRes, turnoverRes] = await Promise.all([
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/limit-emotion-summary/history?start=${fmt(start)}&end=${fmt(end)}`, { cache: "no-store" }),
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/ma-count/history?start=${fmt(start)}&end=${fmt(end)}`, { cache: "no-store" }),
    fetchWithRetry(`${SERVICE_PREFIX.market}/sentiment/turnover-activity/history?start=${fmt(start)}&end=${fmt(end)}`, { cache: "no-store" }),
  ])
  const [limit, ma, turnover] = await Promise.all([
    cynexusResult<{ items?: Array<Record<string, unknown>> }>(limitRes),
    cynexusResult<{ items?: Array<Record<string, unknown>> }>(maRes),
    cynexusResult<{ items?: Array<Record<string, unknown>> }>(turnoverRes),
  ])
  const byDate = new Map<string, MarketBreadthResponse & { totalCount: number | null; date: string }>()
  for (const item of limit.items ?? []) {
    const date = String(item.tradeDate ?? "")
    if (!date) continue
    byDate.set(date, {
      date,
      upCount: null,
      downCount: null,
      limitUpCount: (item.limitUpCount as number) ?? null,
      limitDownCount: (item.limitDownCount as number) ?? null,
      breakRate: (item.breakBoardRate as number) ?? null,
      maxLianBan: null,
      yesterdayLimitUpReturn: (item.yesterdayLimitUpAvgReturn as number) ?? null,
      totalTurnover: null,
      downOver5Count: null,
      new20HighCount: null,
      new20LowCount: null,
      totalCount: null,
    })
  }
  for (const item of ma.items ?? []) {
    const date = String(item.tradeDate ?? "")
    if (!date) continue
    const row = byDate.get(date) ?? {
      date,
      upCount: null,
      downCount: null,
      limitUpCount: null,
      limitDownCount: null,
      breakRate: null,
      maxLianBan: null,
      yesterdayLimitUpReturn: null,
      totalTurnover: null,
      downOver5Count: null,
      new20HighCount: null,
      new20LowCount: null,
      totalCount: null,
    }
    row.new20HighCount = (item.newHigh252dCount as number) ?? null
    row.new20LowCount = (item.newLow60dCount as number) ?? null
    row.totalCount = (item.totalEligible as number) ?? null
    byDate.set(date, row)
  }
  for (const item of turnover.items ?? []) {
    const date = String(item.tradeDate ?? "")
    const row = byDate.get(date)
    if (row) row.totalTurnover = (item.totalAmount as number) ?? null
  }
  return Array.from(byDate.values()).sort((a, b) => a.date.localeCompare(b.date))
}

export async function listStockAnnotations(targetType: StockTargetType, symbol: string, period: string): Promise<StockAnnotation[]> {
  return readLocalAnnotations(targetType, symbol, period)
}

export async function createStockAnnotation(payload: {
  target_type: StockTargetType
  symbol: string
  period: string
  overlay_type: string
  points: Array<{ timestamp: number; value: number }>
  styles?: Record<string, unknown>
  text?: string
}): Promise<StockAnnotation> {
  const now = new Date().toISOString()
  const item: StockAnnotation = {
    id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    overlay_type: payload.overlay_type,
    points: payload.points,
    styles: payload.styles ?? {},
    text: payload.text ?? "",
    created_at: now,
    updated_at: now,
  }
  const items = [...readLocalAnnotations(payload.target_type, payload.symbol, payload.period), item]
  writeLocalAnnotations(payload.target_type, payload.symbol, payload.period, items)
  return item
}

export async function deleteStockAnnotation(targetType: StockTargetType, symbol: string, period: string, annotationId: string): Promise<void> {
  writeLocalAnnotations(
    targetType,
    symbol,
    period,
    readLocalAnnotations(targetType, symbol, period).filter((item) => item.id !== annotationId),
  )
}

export async function fetchAuctionAiAnalysisSnapshot(_params?: {
  targetType?: StockTargetType
  symbol?: string
  date?: string
}): Promise<AuctionAiAnalysisResponse & { ok?: boolean; has_snapshot?: boolean }> {
  void _params
  return { ok: true, has_snapshot: false, analysis_input: {}, analysis_result: {} }
}

export async function runAuctionAiAnalysis(_params?: {
  targetType?: StockTargetType
  symbol?: string
  name?: string
  adjust?: StockAdjust
  date?: string
  maxChars?: number
}): Promise<AuctionAiAnalysisResponse> {
  void _params
  throw new Error("Java 竞价 AI 执行器尚未接入")
}

export async function triggerAuctionAiAnalysisScheduler(): Promise<{ ok?: boolean; error?: string; succeeded?: number; failed?: number }> {
  return { ok: false, error: "Java 竞价 AI 执行器尚未接入", succeeded: 0, failed: 0 }
}
