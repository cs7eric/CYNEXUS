import { fetchWithRetry } from "@/services/common/http"

const DATA_API_BASE = import.meta.env.VITE_CYNEXUS_DATA_API_BASE || "/data-api"
const INTERNAL_TOKEN = import.meta.env.VITE_CYNEXUS_DATA_INTERNAL_TOKEN || ""

export interface EasyTdxResponse<T = Record<string, unknown>> {
  items: T[]
  count: number
  source: string
}

export type EasyTdxRow = Record<string, unknown>

export interface EasyTdxBelongBoardRow {
  board_type: number
  market: number
  board_code: string
  board_name: string
  close: number
  pre_close: number
}

export interface EasyTdxBoardSummary {
  member_count?: number | null
  amount?: number | null
  vol?: number | null
  main_net_amount?: number | null
  main_net_3d?: number | null
  main_net_5d?: number | null
  up_count?: number | null
  down_count?: number | null
  members?: EasyTdxRow[]
}

async function get<T>(path: string): Promise<EasyTdxResponse<T>> {
  const headers: HeadersInit = INTERNAL_TOKEN ? { "x-cynexus-internal-token": INTERNAL_TOKEN } : {}
  const response = await fetchWithRetry(`${DATA_API_BASE}${path}`, { headers, cache: "no-store" })
  if (!response.ok) {
    throw new Error(`easy_tdx 请求失败 (${response.status})`)
  }
  return response.json() as Promise<EasyTdxResponse<T>>
}

async function getRaw<T>(path: string): Promise<T> {
  const headers: HeadersInit = INTERNAL_TOKEN ? { "x-cynexus-internal-token": INTERNAL_TOKEN } : {};
  const response = await fetchWithRetry(`${DATA_API_BASE}${path}`, { headers, cache: "no-store" });
  if (!response.ok) throw new Error(`easy_tdx 请求失败 (${response.status})`);
  return response.json() as Promise<T>;
}

export function fetchEasyTdxKline(params: { code: string; period: string; adjust: string; limit: number }) {
  const query = new URLSearchParams({
    code: params.code,
    period: params.period,
    adjust: params.adjust,
    limit: String(params.limit),
  })
  return get<EasyTdxRow>(`${"/internal/a-stock/tdx/kline"}?${query.toString()}`)
}

export function fetchEasyTdxTimeshare(params: { code: string; tradeDate: string }) {
  const query = new URLSearchParams({ code: params.code, trade_date: params.tradeDate })
  const key = `${params.code}:${params.tradeDate}`
  const cached = timeshareRequestCache.get(key)
  if (cached && cached.expiresAt > Date.now()) return cached.promise
  const promise = get<EasyTdxRow>(`${"/internal/a-stock/tdx/timeshare"}?${query.toString()}`)
  timeshareRequestCache.set(key, { promise, expiresAt: Date.now() + 30_000 })
  void promise.catch(() => {
    if (timeshareRequestCache.get(key)?.promise === promise) timeshareRequestCache.delete(key)
  })
  return promise
}

const timeshareRequestCache = new Map<string, {
  promise: Promise<EasyTdxResponse<EasyTdxRow>>
  expiresAt: number
}>()

export interface HistoricalTradeTick {
  symbol?: string
  code?: string
  trade_date?: string
  trade_datetime?: string | null
  trade_time?: string
  time?: string
  time_label?: string
  price?: number | null
  volume?: number | null
  amount?: number | null
  buy_sell?: string | null
  side?: "BUY" | "SELL" | "NEUTRAL" | string | null
  order_count?: number | null
  index?: number | null
  absolute_index?: number | null
}

export function fetchEasyTdxTransactions(params: { code: string; tradeDate: string; targetType?: "stock" | "etf" | "index" }) {
  const query = new URLSearchParams({
    symbol: params.code,
    trade_date: params.tradeDate,
    ...(params.targetType ? { target_type: params.targetType } : {}),
  })
  return get<HistoricalTradeTick>(`${"/internal/a-stock/tdx/transactions"}?${query.toString()}`)
}

export function fetchEasyTdxFundFlow(params: { code: string; history: boolean; count: number }) {
  const query = new URLSearchParams({ code: params.code, history: String(params.history), count: String(params.count) })
  return getWithEmptyRetry<EasyTdxRow>(`${"/internal/a-stock/tdx/fund-flow"}?${query.toString()}`)
}

async function getWithEmptyRetry<T>(path: string): Promise<EasyTdxResponse<T>> {
  const first = await get<T>(path)
  if (first.items.length > 0) return first
  await new Promise((resolve) => setTimeout(resolve, 500))
  return get<T>(path)
}

export function fetchEasyTdxBoards(params: { boardType: string; count: number }) {
  const query = new URLSearchParams({ board_type: params.boardType, count: String(params.count) })
  return get<EasyTdxRow>(`${"/internal/a-stock/tdx/boards"}?${query.toString()}`)
}

export function fetchEasyTdxBoardMembers(params: { boardSymbol: string; count: number }) {
  const query = new URLSearchParams({ board_symbol: params.boardSymbol, count: String(params.count) })
  const key = `${params.boardSymbol}:${params.count}`
  const cached = boardMembersRequestCache.get(key)
  if (cached && cached.expiresAt > Date.now()) return cached.promise
  const promise = get<EasyTdxRow>(`${"/internal/a-stock/tdx/board-members"}?${query.toString()}`)
  boardMembersRequestCache.set(key, { promise, expiresAt: Date.now() + 30_000 })
  void promise.catch(() => {
    if (boardMembersRequestCache.get(key)?.promise === promise) boardMembersRequestCache.delete(key)
  })
  return promise
}

const boardMembersRequestCache = new Map<string, {
  promise: Promise<EasyTdxResponse<EasyTdxRow>>
  expiresAt: number
}>()

export function fetchEasyTdxBelongBoard(code: string) {
  return get<EasyTdxBelongBoardRow>(`/internal/a-stock/tdx/belong-board?code=${encodeURIComponent(code)}`)
}

export function fetchEasyTdxBoardSummary(boardSymbol: string) {
  return getRaw<EasyTdxBoardSummary>(`${"/internal/a-stock/tdx/board-summary"}?board_symbol=${encodeURIComponent(boardSymbol)}`)
}

export function fetchEasyTdxBoardRanking(params: { boardType: string; topN: number; change: boolean }) {
  const query = new URLSearchParams({
    board_type: params.boardType,
    top_n: String(params.topN),
    ...(params.change ? { days: "20" } : {}),
  })
  return get<EasyTdxRow>(`${params.change ? "/internal/a-stock/tdx/board-change-ranking" : "/internal/a-stock/tdx/board-ranking"}?${query.toString()}`)
}
