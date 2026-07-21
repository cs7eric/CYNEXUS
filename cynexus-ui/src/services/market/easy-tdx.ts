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

async function get<T>(path: string): Promise<EasyTdxResponse<T>> {
  const headers: HeadersInit = INTERNAL_TOKEN ? { "x-cynexus-internal-token": INTERNAL_TOKEN } : {}
  const response = await fetchWithRetry(`${DATA_API_BASE}${path}`, { headers, cache: "no-store" })
  if (!response.ok) {
    throw new Error(`easy_tdx 请求失败 (${response.status})`)
  }
  return response.json() as Promise<EasyTdxResponse<T>>
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
  return get<EasyTdxRow>(`${"/internal/a-stock/tdx/timeshare"}?${query.toString()}`)
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
  return get<EasyTdxRow>(`${"/internal/a-stock/tdx/board-members"}?${query.toString()}`)
}

export function fetchEasyTdxBelongBoard(code: string) {
  return get<EasyTdxBelongBoardRow>(`/internal/a-stock/tdx/belong-board?code=${encodeURIComponent(code)}`)
}

export function fetchEasyTdxBoardSummary(boardSymbol: string) {
  return get<EasyTdxRow>(`${"/internal/a-stock/tdx/board-summary"}?board_symbol=${encodeURIComponent(boardSymbol)}`)
}

export function fetchEasyTdxBoardRanking(params: { boardType: string; topN: number; change: boolean }) {
  const query = new URLSearchParams({
    board_type: params.boardType,
    top_n: String(params.topN),
    ...(params.change ? { days: "20" } : {}),
  })
  return get<EasyTdxRow>(`${params.change ? "/internal/a-stock/tdx/board-change-ranking" : "/internal/a-stock/tdx/board-ranking"}?${query.toString()}`)
}
