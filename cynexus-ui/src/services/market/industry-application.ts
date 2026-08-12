import { fetchWithRetry } from "@/services/common/http"
import { cynexusResult } from "@/services/common/cynexus-result"
import type {
  IndustryApplicationConfig,
  IndustryApplicationIndicators,
  IndustryApplicationIndexBar,
  IndustryApplicationKlinePayload,
  IndustryApplicationOverviewResponse,
  IndustryApplicationTargetCode,
  IndustryApplicationTargetKind,
  MarketHeatmapResponse,
} from "@/views/industry-application/lib/types"

const MARKET_API_BASE = `${(typeof import.meta !== "undefined" && import.meta.env?.VITE_CYNEXUS_API_BASE) || ""}/api/market`
const INDUSTRY_APPLICATION_API_BASE = `${MARKET_API_BASE}/industry-application`
const MARKET_PULSE_API_BASE = `${MARKET_API_BASE}/pulse`

export type HeatmapKind = "industries" | "concepts" | "styles"

export interface ApplicationAnalysisTargetView {
  id: string
  target_type: string
  symbol: string
  name: string
  adjust: string
  enabled: boolean
  interval_minutes: number
  tags?: string[]
  last_updated_at?: string | null
  last_result_path?: string | null
}

export interface IndustryApplicationRefreshResponse {
  ok: boolean
  items?: Array<{ id: string; ok: boolean; kline_count?: number; error?: string }>
  count?: number
  error?: string
}

export interface IndustryApplicationResultResponse {
  target: { id?: string; target_type?: string; symbol?: string; name?: string; tags?: string[] }
  updated_at: string
  kline: IndustryApplicationIndexBar[]
  indicators: IndustryApplicationIndicators
  meta?: Record<string, unknown>
}

export interface IndustryFundFlowRow {
  "序号": number
  "行业": string
  code?: string | null
  "行业指数涨跌幅": number | string | null
  "流入资金(亿)": number | string | null
  "流出资金(亿)": number | string | null
  "净额(亿)": number | string | null
  "公司家数": number | string | null
  "领涨股": string | null
  "领涨股涨跌幅": number | string | null
  "当前价(元)": number | string | null
  tradeDate?: string | null
  source?: string | null
}

export interface IndustryFundFlowResponse {
  ok: boolean
  tradeDate?: string | null
  requestedTradeDate?: string | null
  fallbackTradeDate?: boolean
  servingMode?: string | null
  rowCount: number
  totalPages: number | null
  pageRowCounts: number[]
  fetchedAt: string | null
  rows: IndustryFundFlowRow[]
  source?: string | null
  sourceKind?: string | null
  stale?: boolean
  staleReason?: string
  error?: string
}

export interface IndustryFundFlowHistoryResponse {
  ok: boolean
  days: number
  dates: string[]
  rowCount: number
  fetchedAt: string | null
  rows: IndustryFundFlowRow[]
  source?: string | null
  error?: string
}

export interface IndustryApplicationTdxFundFlowPoint {
  trade_date?: string | null
  flow_trade_date?: string | null
  snapshot_status?: string | null
  selection_mode?: string | null
  main_net?: number | null
  large_order_net?: number | null
  medium_order_net?: number | null
  small_order_net?: number | null
}

export interface IndustryApplicationTdxThsMapping {
  tdx_code6?: string | null
  tdx_name?: string | null
  ths_code?: string | null
  ths_name?: string | null
  match_type?: string | null
  confidence?: number | null
  priority?: number | null
  is_primary?: boolean | null
  remark?: string | null
  source?: string | null
}

export interface IndustryApplicationMergedConstituent {
  code?: string | null
  name?: string | null
  price?: number | string | null
  change_pct?: number | string | null
  turnover_pct?: number | string | null
  amount_text?: string | null
  open?: number | string | null
  high?: number | string | null
  low?: number | string | null
  pre_close?: number | string | null
  volume?: number | string | null
  amount?: number | string | null
  vol_ratio?: number | string | null
  eps?: number | string | null
  net_assets?: number | string | null
  pe_dynamic?: number | string | null
  pe_ttm?: number | string | null
  pe_static?: number | string | null
  main_net_amount?: number | string | null
  main_net_3d_amount?: number | string | null
  main_net_5d_amount?: number | string | null
  ths_snapshot_date?: string | null
  tdx_snapshot_date?: string | null
  quote_trade_date?: string | null
  sources?: string[] | null
}

export interface IndustryApplicationTdxFundFlowResponse {
  applicable: boolean
  ok: boolean
  target_type?: string | null
  symbol?: string | null
  requested_trade_date?: string | null
  trade_date?: string | null
  code6?: string | null
  full_code?: string | null
  tdx_name?: string | null
  flow_sector_name?: string | null
  seed_code?: string | null
  flow_trade_date?: string | null
  snapshot_status?: string | null
  selection_mode?: string | null
  name_match_type?: string | null
  main_net?: number | null
  large_order_net?: number | null
  medium_order_net?: number | null
  small_order_net?: number | null
  row_count?: number | null
  history_days?: number | null
  history: IndustryApplicationTdxFundFlowPoint[]
  ths_mappings: IndustryApplicationTdxThsMapping[]
  constituents: IndustryApplicationMergedConstituent[]
  source?: string | null
  error?: string | null
}

const YI_TO_YUAN = 1e8

export async function fetchIndustryApplicationTargets(): Promise<IndustryApplicationConfig> {
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/targets`)
  const data = await cynexusResult<IndustryApplicationConfig>(res)
  return {
    version: data.version ?? 1,
    updated_at: data.updated_at ?? null,
    horizon: data.horizon ?? { days: 120, segments: 4 },
    items: data.items ?? [],
  }
}

export async function saveIndustryApplicationTargets(payload: {
  horizon: { days: number; segments: number }
  items: IndustryApplicationConfig["items"]
}): Promise<IndustryApplicationConfig> {
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/targets`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return cynexusResult<IndustryApplicationConfig>(res)
}

export async function fetchIndustryApplicationTargetCodes(kind?: IndustryApplicationTargetKind): Promise<{
  items: IndustryApplicationTargetCode[]
  count: number
  source: string
}> {
  const params = new URLSearchParams()
  if (kind) params.set("kind", kind)
  const qs = params.toString()
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/target-codes${qs ? `?${qs}` : ""}`)
  const data = await cynexusResult<{ items?: IndustryApplicationTargetCode[]; count?: number; source?: string }>(res)
  return {
    items: data.items ?? [],
    count: data.count ?? data.items?.length ?? 0,
    source: data.source ?? "java",
  }
}

export async function fetchIndustryApplicationKline(
  target_type: IndustryApplicationTargetKind,
  symbol: string,
  opts: { period?: string; count?: number } = {},
): Promise<IndustryApplicationKlinePayload> {
  const params = new URLSearchParams({ target_type, symbol })
  if (opts.period) params.set("period", opts.period)
  if (opts.count) params.set("count", String(opts.count))
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/kline?${params.toString()}`)
  return cynexusResult<IndustryApplicationKlinePayload>(res)
}

export async function refreshIndustryApplication(targetId: string | null): Promise<IndustryApplicationRefreshResponse> {
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ target_id: targetId || null }),
  })
  return cynexusResult<IndustryApplicationRefreshResponse>(res)
}

export async function fetchIndustryApplicationResult(targetId: string): Promise<IndustryApplicationResultResponse> {
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/results/${encodeURIComponent(targetId)}`)
  return cynexusResult<IndustryApplicationResultResponse>(res)
}

export async function fetchIndustryApplicationOverview(
  opts: { sort_by?: string; ascending?: boolean; count?: number } = {},
): Promise<IndustryApplicationOverviewResponse> {
  const params = new URLSearchParams()
  if (opts.sort_by) params.set("sort_by", opts.sort_by)
  if (opts.ascending != null) params.set("ascending", String(opts.ascending))
  if (opts.count) params.set("count", String(opts.count))
  const qs = params.toString()
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/overview${qs ? `?${qs}` : ""}`)
  return cynexusResult<IndustryApplicationOverviewResponse>(res)
}

export async function fetchIndustryFundFlow(
  options: { tradeDate?: string; top?: number } = {},
): Promise<IndustryFundFlowResponse> {
  const params = new URLSearchParams()
  if (options.tradeDate) params.set("trade_date", options.tradeDate)
  if (options.top) params.set("top", String(options.top))
  const qs = params.toString()
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/fund-flow${qs ? `?${qs}` : ""}`)
  const data = await cynexusResult<IndustryFundFlowResponse>(res)
  return {
    ok: data.ok ?? false,
    tradeDate: data.tradeDate ?? null,
    requestedTradeDate: data.requestedTradeDate ?? null,
    fallbackTradeDate: data.fallbackTradeDate ?? false,
    servingMode: data.servingMode ?? null,
    rowCount: data.rowCount ?? data.rows?.length ?? 0,
    totalPages: data.totalPages ?? null,
    pageRowCounts: data.pageRowCounts ?? [],
    fetchedAt: data.fetchedAt ?? null,
    rows: data.rows ?? [],
    source: data.source ?? null,
    sourceKind: data.sourceKind ?? null,
    stale: data.stale ?? false,
    staleReason: data.staleReason,
    error: data.error,
  }
}

export async function fetchIndustryFundFlowHistory(
  options: { maxTradeDate?: string; days?: number; top?: number } = {},
): Promise<IndustryFundFlowHistoryResponse> {
  const params = new URLSearchParams()
  if (options.maxTradeDate) params.set("max_trade_date", options.maxTradeDate)
  if (options.days) params.set("days", String(options.days))
  if (options.top) params.set("top", String(options.top))
  const qs = params.toString()
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/fund-flow/history${qs ? `?${qs}` : ""}`)
  const data = await cynexusResult<IndustryFundFlowHistoryResponse>(res)
  return {
    ok: data.ok ?? false,
    days: data.days ?? options.days ?? 0,
    dates: data.dates ?? [],
    rowCount: data.rowCount ?? data.rows?.length ?? 0,
    fetchedAt: data.fetchedAt ?? null,
    rows: data.rows ?? [],
    source: data.source ?? null,
    error: data.error,
  }
}

export async function refreshIndustryFundFlow(options: { tradeDate?: string; top?: number } = {}): Promise<IndustryFundFlowResponse> {
  const params = new URLSearchParams()
  if (options.tradeDate) params.set("trade_date", options.tradeDate)
  if (options.top) params.set("top", String(options.top))
  const qs = params.toString()
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/fund-flow/refresh${qs ? `?${qs}` : ""}`, {
    method: "POST",
  })
  return fetchIndustryFundFlowResult(res)
}

export async function fetchIndustryApplicationTdxFundFlow(options: {
  targetType: IndustryApplicationTargetKind
  symbol: string
  tradeDate?: string
  days?: number
}): Promise<IndustryApplicationTdxFundFlowResponse> {
  const params = new URLSearchParams({
    target_type: options.targetType,
    symbol: options.symbol,
  })
  if (options.tradeDate) params.set("trade_date", options.tradeDate)
  if (options.days) params.set("days", String(options.days))
  const res = await fetchWithRetry(`${INDUSTRY_APPLICATION_API_BASE}/tdx-fund-flow?${params.toString()}`)
  const data = await cynexusResult<IndustryApplicationTdxFundFlowResponse>(res)
  return {
    applicable: data.applicable ?? false,
    ok: data.ok ?? false,
    target_type: data.target_type ?? null,
    symbol: data.symbol ?? null,
    requested_trade_date: data.requested_trade_date ?? null,
    trade_date: data.trade_date ?? null,
    code6: data.code6 ?? null,
    full_code: data.full_code ?? null,
    tdx_name: data.tdx_name ?? null,
    flow_sector_name: data.flow_sector_name ?? null,
    seed_code: data.seed_code ?? null,
    flow_trade_date: data.flow_trade_date ?? null,
    snapshot_status: data.snapshot_status ?? null,
    selection_mode: data.selection_mode ?? null,
    name_match_type: data.name_match_type ?? null,
    main_net: data.main_net ?? null,
    large_order_net: data.large_order_net ?? null,
    medium_order_net: data.medium_order_net ?? null,
    small_order_net: data.small_order_net ?? null,
    row_count: data.row_count ?? null,
    history_days: data.history_days ?? null,
    history: data.history ?? [],
    ths_mappings: data.ths_mappings ?? [],
    constituents: data.constituents ?? [],
    source: data.source ?? null,
    error: data.error ?? null,
  }
}

async function fetchIndustryFundFlowResult(res: Response): Promise<IndustryFundFlowResponse> {
  const data = await cynexusResult<IndustryFundFlowResponse>(res)
  return {
    ok: data.ok ?? false,
    tradeDate: data.tradeDate ?? null,
    requestedTradeDate: data.requestedTradeDate ?? null,
    fallbackTradeDate: data.fallbackTradeDate ?? false,
    servingMode: data.servingMode ?? null,
    rowCount: data.rowCount ?? data.rows?.length ?? 0,
    totalPages: data.totalPages ?? null,
    pageRowCounts: data.pageRowCounts ?? [],
    fetchedAt: data.fetchedAt ?? null,
    rows: data.rows ?? [],
    source: data.source ?? null,
    sourceKind: data.sourceKind ?? null,
    stale: data.stale ?? false,
    staleReason: data.staleReason,
    error: data.error,
  }
}

export async function fetchMarketHeatmap(kind: HeatmapKind = "industries", top_n = 200): Promise<MarketHeatmapResponse> {
  const params = new URLSearchParams({ kind, top_n: String(top_n) })
  const res = await fetchWithRetry(`${MARKET_PULSE_API_BASE}/heatmap?${params.toString()}`)
  const raw = await cynexusResult<MarketHeatmapResponse>(res)
  return {
    ...raw,
    items: (raw.items ?? []).map((sector) => ({
      ...sector,
      amount: kind === "styles" ? (sector.amount ?? 0) : (sector.amount ?? 0) * YI_TO_YUAN,
      circulatingMarketCap: kind === "styles" ? (sector.circulatingMarketCap ?? 0) : (sector.circulatingMarketCap ?? 0) * YI_TO_YUAN,
      children: [],
      conceptTags: sector.conceptTags ?? [],
    })),
  }
}
