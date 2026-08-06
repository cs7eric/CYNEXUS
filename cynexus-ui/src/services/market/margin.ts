import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const MARGIN_API_BASE = `${SERVICE_PREFIX.market}/margin`

export type MarginExchange = "SSE" | "SZSE" | string

export type MarginStockSort =
  | "margin_balance"
  | "financing_balance"
  | "financing_buy_amount"
  | "financing_repay_amount"
  | "securities_lending_balance"
  | "securities_lending_balance_amount"

export type MarginRankingSort =
  | MarginStockSort
  | "stock_code"
  | "stock_name"
  | "exchange"
  | "industry_code"
  | "industry_name"
  | "tdx_code"
  | "tdx_name"
  | "constituent_count"
  | "matched_stock_count"

export type MarginSortDirection = "asc" | "desc"

export interface MarginMarketSummary {
  tradeDate: string
  exchange: MarginExchange
  source: string
  financingBalance: number | null
  financingBuyAmount: number | null
  financingRepayAmount: number | null
  securitiesLendingBalance: number | null
  securitiesLendingBalanceAmount: number | null
  securitiesLendingSellVolume: number | null
  securitiesLendingRepayVolume: number | null
  marginBalance: number | null
  extra?: string | null
  updatedAt?: string | null
}

export interface MarginStockDetail {
  tradeDate: string
  exchange: MarginExchange
  stockCode: string
  stockName?: string | null
  source: string
  financingBalance: number | null
  financingBuyAmount: number | null
  financingRepayAmount: number | null
  securitiesLendingBalance: number | null
  securitiesLendingBalanceAmount: number | null
  securitiesLendingSellVolume: number | null
  securitiesLendingRepayVolume: number | null
  marginBalance: number | null
  extra?: string | null
  updatedAt?: string | null
}

export interface MarginIndustrySummary {
  tradeDate: string
  constituentSnapshotDate?: string | null
  industryCode: string
  industryName?: string | null
  tdxCode?: string | null
  tdxName?: string | null
  constituentCount: number
  matchedStockCount: number
  financingBalance: number | null
  financingBuyAmount: number | null
  financingRepayAmount: number | null
  securitiesLendingBalance: number | null
  securitiesLendingBalanceAmount: number | null
  securitiesLendingSellVolume: number | null
  securitiesLendingRepayVolume: number | null
  marginBalance: number | null
  turnoverAmount: number | null
}

export interface MarginAccountStats {
  tradeDate: string
  source: string
  financingBalance: number | null
  securitiesLendingBalance: number | null
  financingBuyAmount: number | null
  securitiesLendingSellAmount: number | null
  brokerCount: number | null
  businessDepartmentCount: number | null
  personalInvestorCount: number | null
  institutionInvestorCount: number | null
  activeInvestorCount: number | null
  liabilityInvestorCount: number | null
  totalGuarantee: number | null
  averageGuaranteeRatio: number | null
  extra?: string | null
  updatedAt?: string | null
}

export interface MarginLatestResponse {
  ok: boolean
  error?: string | null
  tradeDate?: string | null
  requestedTradeDate?: string | null
  fallbackTradeDate?: boolean
  source?: string | null
  updatedAt?: string | null
  summaryCount: number
  stockCount: number
  summaries: MarginMarketSummary[]
  account?: MarginAccountStats | null
  topStocks: MarginStockDetail[]
}

export interface MarginHistoryResponse {
  ok: boolean
  error?: string | null
  start: string
  end: string
  count: number
  items: MarginMarketSummary[]
}

export interface MarginStocksResponse {
  ok: boolean
  error?: string | null
  tradeDate?: string | null
  exchange?: MarginExchange | null
  industryCode?: string | null
  sort: MarginRankingSort | string
  sortDir?: MarginSortDirection | string
  page?: number
  pageSize?: number
  limit: number
  count: number
  items: MarginStockDetail[]
}

export interface MarginStockHistoryResponse {
  ok: boolean
  error?: string | null
  stockCode: string
  count: number
  items: MarginStockDetail[]
}

export interface MarginIndustriesResponse {
  ok: boolean
  error?: string | null
  tradeDate?: string | null
  constituentSnapshotDate?: string | null
  sort: MarginRankingSort | string
  sortDir?: MarginSortDirection | string
  page?: number
  pageSize?: number
  limit: number
  count: number
  items: MarginIndustrySummary[]
}

export interface MarginIndustryHistoryResponse {
  ok: boolean
  error?: string | null
  industryCode: string
  tradeDate?: string | null
  count: number
  items: MarginIndustrySummary[]
}

export async function fetchMarginLatest(params: { tradeDate?: string; top?: number } = {}): Promise<MarginLatestResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.top != null) query.set("top", String(params.top))
  const queryString = query.toString()
  const suffix = queryString ? `?${queryString}` : ""
  const res = await fetchWithRetry(`${MARGIN_API_BASE}/latest${suffix}`, { cache: "no-store" })
  return cynexusResult<MarginLatestResponse>(res)
}

export async function fetchMarginSummaryHistory(params: { start?: string; end?: string } = {}): Promise<MarginHistoryResponse> {
  const query = new URLSearchParams()
  if (params.start) query.set("start", params.start)
  if (params.end) query.set("end", params.end)
  const queryString = query.toString()
  const suffix = queryString ? `?${queryString}` : ""
  const res = await fetchWithRetry(`${MARGIN_API_BASE}/summary/history${suffix}`, { cache: "no-store" })
  return cynexusResult<MarginHistoryResponse>(res)
}

export async function fetchMarginStocks(
  params: {
    tradeDate?: string
    exchange?: MarginExchange
    industryCode?: string
    sort?: MarginRankingSort
    sortDir?: MarginSortDirection
    page?: number
    pageSize?: number
    limit?: number
  } = {},
): Promise<MarginStocksResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.exchange) query.set("exchange", params.exchange)
  if (params.industryCode) query.set("industryCode", params.industryCode)
  if (params.sort) query.set("sort", params.sort)
  if (params.sortDir) query.set("sortDir", params.sortDir)
  if (params.page != null) query.set("page", String(params.page))
  if (params.pageSize != null) query.set("pageSize", String(params.pageSize))
  if (params.limit != null) query.set("limit", String(params.limit))
  const queryString = query.toString()
  const suffix = queryString ? `?${queryString}` : ""
  const res = await fetchWithRetry(`${MARGIN_API_BASE}/stocks${suffix}`, { cache: "no-store" })
  return cynexusResult<MarginStocksResponse>(res)
}

export async function fetchMarginStockHistory(stockCode: string, limit = 120): Promise<MarginStockHistoryResponse> {
  const query = new URLSearchParams({ limit: String(limit) })
  const res = await fetchWithRetry(
    `${MARGIN_API_BASE}/stocks/${encodeURIComponent(stockCode)}/history?${query}`,
    { cache: "no-store" },
  )
  return cynexusResult<MarginStockHistoryResponse>(res)
}

export async function backfillMarginStockHistory(stockCode: string): Promise<void> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/scheduler/data-jobs/sync_margin/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      triggeredBy: "application-analysis-margin-backfill",
      dryRun: false,
      params: {
        mode: "stock_history",
        codes: [stockCode],
        page_size: 120,
        create_table: true,
      },
    }),
  })
  await cynexusResult(response)
}

export async function fetchMarginIndustries(
  params: {
    tradeDate?: string
    sort?: MarginRankingSort
    sortDir?: MarginSortDirection
    page?: number
    pageSize?: number
  } = {},
): Promise<MarginIndustriesResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.sort) query.set("sort", params.sort)
  if (params.sortDir) query.set("sortDir", params.sortDir)
  if (params.page != null) query.set("page", String(params.page))
  if (params.pageSize != null) query.set("pageSize", String(params.pageSize))
  const queryString = query.toString()
  const suffix = queryString ? `?${queryString}` : ""
  const res = await fetchWithRetry(`${MARGIN_API_BASE}/industries${suffix}`, { cache: "no-store" })
  return cynexusResult<MarginIndustriesResponse>(res)
}

export async function fetchMarginIndustryHistory(
  industryCode: string,
  params: { tradeDate?: string; limit?: number } = {},
): Promise<MarginIndustryHistoryResponse> {
  const query = new URLSearchParams()
  if (params.tradeDate) query.set("tradeDate", params.tradeDate)
  if (params.limit != null) query.set("limit", String(params.limit))
  const queryString = query.toString()
  const suffix = queryString ? `?${queryString}` : ""
  const res = await fetchWithRetry(
    `${MARGIN_API_BASE}/industries/${encodeURIComponent(industryCode)}/history${suffix}`,
    { cache: "no-store" },
  )
  return cynexusResult<MarginIndustryHistoryResponse>(res)
}
