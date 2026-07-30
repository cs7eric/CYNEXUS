import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const FUTURES_POSITION_API_BASE = `${SERVICE_PREFIX.market}/futures-position`

export type FuturesScopeType = "CONTRACT" | "VARIETY"
export type FuturesRankType = "VOLUME" | "LONG" | "SHORT"
export type FuturesSort = "volume" | "longPosition" | "shortPosition" | "netPosition" | "ratio" | "instrumentCode"

export interface FuturesPositionInstrument {
  exchangeCode?: string | null
  instrumentCode: string
  varietyCode?: string | null
  contractCode?: string | null
  volume?: number | null
  longPosition?: number | null
  shortPosition?: number | null
  netPosition?: number | null
  longShortRatio?: number | null
  longChange?: number | null
  shortChange?: number | null
  source?: string | null
}

export interface FuturesPositionAggregate {
  volume?: number | null
  longPosition?: number | null
  shortPosition?: number | null
  netPosition?: number | null
  longShortRatio?: number | null
  longChange?: number | null
  shortChange?: number | null
}

export interface FuturesPositionMember {
  tradeDate: string
  exchangeCode: string
  scopeType: FuturesScopeType
  instrumentCode: string
  varietyCode?: string | null
  contractCode?: string | null
  rankType: FuturesRankType
  rankNo: number
  memberName?: string | null
  rankValue: number
  rankChange?: number | null
  source?: string | null
}

export interface FuturesPositionHistory {
  tradeDate: string
  instrumentCode: string
  varietyCode?: string | null
  volume?: number | null
  longPosition?: number | null
  shortPosition?: number | null
  netPosition?: number | null
  longShortRatio?: number | null
  longChange?: number | null
  shortChange?: number | null
  source?: string | null
}

export interface FuturesPositionCoverageItem {
  tradeDate: string
  exchangeCode: string
  provider: string
  status: string
  selected: boolean
  expectedInstrumentCount: number
  actualInstrumentCount: number
  rowCount: number
  completedAt?: string | null
}

export interface FuturesPositionCoverage {
  tradeDate?: string | null
  items: FuturesPositionCoverageItem[]
}

export interface FuturesPositionLatestResponse {
  ok: boolean
  error?: string | null
  requestedTradeDate?: string | null
  tradeDate?: string | null
  fallbackTradeDate?: boolean
  scopeType: FuturesScopeType
  exchangeCode?: string | null
  varietyCode?: string | null
  top: number
  page: number
  pageSize: number
  count: number
  instruments: FuturesPositionInstrument[]
  aggregate?: FuturesPositionAggregate | null
  coverage?: FuturesPositionCoverage | null
  warnings?: string[]
}

function queryString(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value))
  })
  return query.toString()
}

export async function fetchFuturesPositionLatest(params: {
  tradeDate?: string
  scopeType?: FuturesScopeType
  exchangeCode?: string
  varietyCode?: string
  top?: number
  sort?: FuturesSort
  sortDir?: "asc" | "desc"
  page?: number
  pageSize?: number
} = {}): Promise<FuturesPositionLatestResponse> {
  const query = queryString(params)
  const response = await fetchWithRetry(`${FUTURES_POSITION_API_BASE}/latest${query ? `?${query}` : ""}`, { cache: "no-store" })
  return cynexusResult<FuturesPositionLatestResponse>(response)
}

export async function fetchFuturesPositionMembers(params: {
  tradeDate?: string
  scopeType: FuturesScopeType
  exchangeCode: string
  instrumentCode: string
  rankType?: FuturesRankType
  top?: number
}): Promise<FuturesPositionMember[]> {
  const query = queryString({ tradeDate: params.tradeDate, rankType: params.rankType, top: params.top })
  const response = await fetchWithRetry(
    `${FUTURES_POSITION_API_BASE}/${params.scopeType}/${encodeURIComponent(params.exchangeCode)}/${encodeURIComponent(params.instrumentCode)}/members${query ? `?${query}` : ""}`,
    { cache: "no-store" },
  )
  return cynexusResult<FuturesPositionMember[]>(response)
}

export async function fetchFuturesPositionHistory(params: {
  scopeType: FuturesScopeType
  exchangeCode: string
  instrumentCode: string
  top?: number
  limit?: number
}): Promise<FuturesPositionHistory[]> {
  const query = queryString({ top: params.top, limit: params.limit })
  const response = await fetchWithRetry(
    `${FUTURES_POSITION_API_BASE}/${params.scopeType}/${encodeURIComponent(params.exchangeCode)}/${encodeURIComponent(params.instrumentCode)}/history${query ? `?${query}` : ""}`,
    { cache: "no-store" },
  )
  return cynexusResult<FuturesPositionHistory[]>(response)
}

export async function fetchFuturesPositionCoverage(tradeDate?: string): Promise<FuturesPositionCoverage> {
  const query = queryString({ tradeDate })
  const response = await fetchWithRetry(`${FUTURES_POSITION_API_BASE}/coverage${query ? `?${query}` : ""}`, { cache: "no-store" })
  return cynexusResult<FuturesPositionCoverage>(response)
}
