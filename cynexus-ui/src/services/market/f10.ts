import { fetchWithRetry } from "@/services/common/http"
import { cynexusResult } from "@/services/common/cynexus-result"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const STOCK_CHART_API_BASE = `${SERVICE_PREFIX.market}/stock-chart`

export interface StockValuationRow {
  date: string
  peTtm: number | null
  peBfw: number | null
  pbMrq: number | null
  pbBfw: number | null
  pcfOcfTtm: number | null
  pcfBfw: number | null
  psTtm: number | null
  psBfw: number | null
  peg: number | null
  avgMarketCap: number | null
  aliqMarketCap: number | null
}

export interface StockValuationResponse {
  symbol: string
  reqId: string
  rows: StockValuationRow[]
  fetchedAt: string | null
  source: string
}

export interface StockFinanceReportRow {
  rq: string | null
  fields: Record<string, number | string | null>
}

export interface StockFinanceReportResponse {
  symbol: string
  reportType: "zcfzb" | "lrb" | "xjllb"
  rows: StockFinanceReportRow[]
  fetchedAt: string | null
  source: string
}

export interface StockBusinessCompositionItem {
  category: string | null
  subType: string | null
  name: string | null
  revenue: number | null
  ratio: number | null
  extras: Array<number | string | null>
}

export interface StockBusinessCompositionResponse {
  symbol: string
  reportDate: string | null
  items: StockBusinessCompositionItem[]
  fetchedAt: string | null
  source: string
}

export interface StockProfitForecastItem {
  year: string
  netProfit: number | null
  eps: number | null
  revenue: number | null
  extras: Array<number | string | null>
}

export interface StockProfitForecastResponse {
  symbol: string
  items: StockProfitForecastItem[]
  fetchedAt: string | null
  source: string
}

export interface StockAnnouncementItem {
  issueDate: string | null
  title: string | null
  typecode: string | null
  typename: string | null
  recId: string | null
  tableid: string | null
  url: string | null
  redistime: string | null
  source: string | null
}

export interface StockAnnouncementsResponse {
  symbol: string
  items: StockAnnouncementItem[]
  count: number
  fetchedAt: string | null
  source?: string | null
}

export interface StockNewsItem {
  issueDate: string | null
  title: string | null
  recId: string | null
  tableid: string | null
  redistime: string | null
  source: string | null
  relatecolumn: string | null
}

export interface StockNewsResponse {
  symbol: string
  items: StockNewsItem[]
  count: number
  fetchedAt: string | null
  source?: string | null
}

export interface StockRoadshowItem {
  title: string | null
  roadshowType: string | null
  startDate: string | null
  startTime: string | null
  endTime: string | null
  summary: string | null
  url: string | null
}

export interface StockRoadshowsResponse {
  symbol: string
  items: StockRoadshowItem[]
  count: number
  fetchedAt: string | null
  source?: string | null
}

export interface StockCompanyNewsItem {
  rating: string | null
  analysts: string | null
  recId: string | null
  issueDate: string | null
  title: string | null
  nflag: string | null
  docHash: string | null
}

export interface StockCompanyNewsResponse {
  symbol: string
  section: string
  items: StockCompanyNewsItem[]
  count: number
  fetchedAt: string | null
  source?: string | null
}

function toNum(value: unknown): number | null {
  if (value === null || value === undefined || value === "") return null
  if (typeof value === "number" && Number.isFinite(value)) return value
  if (typeof value === "string") {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

function toStr(value: unknown): string | null {
  if (value === null || value === undefined) return null
  return String(value)
}

function primitiveMap(raw: unknown): Record<string, number | string | null> {
  if (!raw || typeof raw !== "object") return {}
  return Object.fromEntries(
    Object.entries(raw as Record<string, unknown>).map(([key, value]) => [
      key,
      value === null || value === undefined
        ? null
        : typeof value === "number" || typeof value === "string"
          ? value
          : null,
    ]),
  )
}

function primitiveList(raw: unknown): Array<number | string | null> {
  if (!Array.isArray(raw)) return []
  return raw.map((value) =>
    value === null || value === undefined
      ? null
      : typeof value === "number" || typeof value === "string"
        ? value
        : null,
  )
}

export async function fetchStockValuation(
  symbol: string,
  options: { reqId?: string; limit?: number } = {},
): Promise<StockValuationResponse> {
  const query = new URLSearchParams({ symbol })
  if (options.reqId) query.set("req_id", options.reqId)
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/valuation?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<StockValuationResponse>(res)
  const limit = options.limit ?? data.rows?.length ?? 0
  return {
    symbol: data.symbol,
    reqId: data.reqId,
    rows: (data.rows ?? []).slice(0, limit).map((item) => ({
      date: toStr(item.date) ?? "",
      peTtm: toNum(item.peTtm),
      peBfw: toNum(item.peBfw),
      pbMrq: toNum(item.pbMrq),
      pbBfw: toNum(item.pbBfw),
      pcfOcfTtm: toNum(item.pcfOcfTtm),
      pcfBfw: toNum(item.pcfBfw),
      psTtm: toNum(item.psTtm),
      psBfw: toNum(item.psBfw),
      peg: toNum(item.peg),
      avgMarketCap: toNum(item.avgMarketCap),
      aliqMarketCap: toNum(item.aliqMarketCap),
    })),
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockFinanceReport(
  symbol: string,
  reportType: "zcfzb" | "lrb" | "xjllb" = "zcfzb",
): Promise<StockFinanceReportResponse> {
  const query = new URLSearchParams({ symbol, report_type: reportType })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/finance-report?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<StockFinanceReportResponse>(res)
  return {
    symbol: data.symbol,
    reportType,
    rows: (data.rows ?? []).map((row) => ({
      rq: toStr(row.rq),
      fields: primitiveMap(row.fields),
    })),
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockBusinessComposition(
  symbol: string,
  options: { reportDate?: string | null; limit?: number } = {},
): Promise<StockBusinessCompositionResponse> {
  const query = new URLSearchParams({ symbol })
  if (options.reportDate) query.set("report_date", options.reportDate)
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/business-composition?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<StockBusinessCompositionResponse>(res)
  const limit = options.limit ?? data.items?.length ?? 0
  return {
    symbol: data.symbol,
    reportDate: toStr(data.reportDate),
    items: (data.items ?? []).slice(0, limit).map((item) => ({
      category: toStr(item.category),
      subType: toStr(item.subType),
      name: toStr(item.name),
      revenue: toNum(item.revenue),
      ratio: toNum(item.ratio),
      extras: primitiveList(item.extras),
    })),
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockProfitForecast(symbol: string): Promise<StockProfitForecastResponse> {
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/profit-forecast?symbol=${encodeURIComponent(symbol)}`, { cache: "no-store" })
  const data = await cynexusResult<StockProfitForecastResponse>(res)
  return {
    symbol: data.symbol,
    items: (data.items ?? []).map((item) => ({
      year: toStr(item.year) ?? "",
      netProfit: toNum(item.netProfit),
      eps: toNum(item.eps),
      revenue: toNum(item.revenue),
      extras: primitiveList(item.extras),
    })),
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockAnnouncements(symbol: string): Promise<StockAnnouncementsResponse> {
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/announcements?symbol=${encodeURIComponent(symbol)}`, { cache: "no-store" })
  const data = await cynexusResult<StockAnnouncementsResponse>(res)
  return {
    symbol: data.symbol,
    items: (data.items ?? []).map((item) => ({
      issueDate: toStr(item.issueDate),
      title: toStr(item.title),
      typecode: toStr(item.typecode),
      typename: toStr(item.typename),
      recId: toStr(item.recId),
      tableid: toStr(item.tableid),
      url: toStr(item.url),
      redistime: toStr(item.redistime),
      source: toStr(item.source),
    })),
    count: typeof data.count === "number" ? data.count : (data.items ?? []).length,
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source),
  }
}

export async function fetchStockNews(symbol: string): Promise<StockNewsResponse> {
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/news?symbol=${encodeURIComponent(symbol)}`, { cache: "no-store" })
  const data = await cynexusResult<StockNewsResponse>(res)
  return {
    symbol: data.symbol,
    items: (data.items ?? []).map((item) => ({
      issueDate: toStr(item.issueDate),
      title: toStr(item.title),
      recId: toStr(item.recId),
      tableid: toStr(item.tableid),
      redistime: toStr(item.redistime),
      source: toStr(item.source),
      relatecolumn: toStr(item.relatecolumn),
    })),
    count: typeof data.count === "number" ? data.count : (data.items ?? []).length,
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source),
  }
}

export async function fetchStockRoadshows(symbol: string): Promise<StockRoadshowsResponse> {
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/roadshows?symbol=${encodeURIComponent(symbol)}`, { cache: "no-store" })
  const data = await cynexusResult<StockRoadshowsResponse>(res)
  return {
    symbol: data.symbol,
    items: (data.items ?? []).map((item) => ({
      title: toStr(item.title),
      roadshowType: toStr(item.roadshowType),
      startDate: toStr(item.startDate),
      startTime: toStr(item.startTime),
      endTime: toStr(item.endTime),
      summary: toStr(item.summary),
      url: toStr(item.url),
    })),
    count: typeof data.count === "number" ? data.count : (data.items ?? []).length,
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source),
  }
}

export async function fetchStockCompanyNews(
  symbol: string,
  options: { section?: string } = {},
): Promise<StockCompanyNewsResponse> {
  const section = options.section ?? "gsyj"
  const query = new URLSearchParams({ symbol, section })
  const res = await fetchWithRetry(`${STOCK_CHART_API_BASE}/f10/company-news?${query.toString()}`, { cache: "no-store" })
  const data = await cynexusResult<StockCompanyNewsResponse>(res)
  return {
    symbol: data.symbol,
    section,
    items: (data.items ?? []).map((item) => ({
      rating: toStr(item.rating),
      analysts: toStr(item.analysts),
      recId: toStr(item.recId),
      issueDate: toStr(item.issueDate),
      title: toStr(item.title),
      nflag: toStr(item.nflag),
      docHash: toStr(item.docHash),
    })),
    count: typeof data.count === "number" ? data.count : (data.items ?? []).length,
    fetchedAt: toStr(data.fetchedAt),
    source: toStr(data.source),
  }
}
