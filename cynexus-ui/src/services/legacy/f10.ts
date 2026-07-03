import { LEGACY_API_BASE, fetchWithRetry, readLegacyJson, withTimeout } from "@/services/legacy/shared"

const STOCK_CHART_LEGACY_API_BASE = `${LEGACY_API_BASE}/stock-chart`
const F10_REQ_TIMEOUT_MS = 8000

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
  subType: number | string | null
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

function parseF10ResultSets(raw: unknown): Record<string, unknown>[] {
  if (!raw || typeof raw !== "object") return []
  const resultSets = (raw as { ResultSets?: Array<{ ColName?: string[]; Content?: unknown[][] }> }).ResultSets
  if (!Array.isArray(resultSets)) return []
  return resultSets.flatMap((table) => {
    const columns = Array.isArray(table.ColName) ? table.ColName : []
    const content = Array.isArray(table.Content) ? table.Content : []
    return content.map((row) => {
      const item: Record<string, unknown> = {}
      columns.forEach((column, index) => {
        item[column] = Array.isArray(row) ? row[index] : null
      })
      return item
    })
  })
}

export async function fetchStockValuation(
  symbol: string,
  options: { reqId?: string; limit?: number } = {},
): Promise<StockValuationResponse> {
  const params = new URLSearchParams({ symbol })
  if (options.reqId) params.set("req_id", options.reqId)
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/valuation?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取估值数据失败")
  const parsed = parseF10ResultSets(data.raw).filter((item) => "DATE" in item)
  const limit = options.limit ?? 5
  return {
    symbol: toStr(data.symbol) ?? symbol,
    reqId: toStr(data.req_id) ?? "200191",
    rows: parsed.slice(0, limit).map((item) => ({
      date: toStr(item.DATE) ?? "",
      peTtm: toNum(item.PETTM),
      peBfw: toNum(item.PEBFW),
      pbMrq: toNum(item.PBMRQ),
      pbBfw: toNum(item.PBBFW),
      pcfOcfTtm: toNum(item.PCFOCFTTM),
      pcfBfw: toNum(item.PCFBFW),
      psTtm: toNum(item.PSTTM),
      psBfw: toNum(item.PSBFW),
      peg: toNum(item.PEG),
      avgMarketCap: toNum(item.AVGMVM),
      aliqMarketCap: toNum(item.ALIQMV),
    })),
    fetchedAt: toStr(data.fetched_at),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockFinanceReport(
  symbol: string,
  reportType: "zcfzb" | "lrb" | "xjllb" = "zcfzb",
): Promise<StockFinanceReportResponse> {
  const params = new URLSearchParams({ symbol, report_type: reportType })
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/finance-report?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取财务报表失败")
  const parsed = parseF10ResultSets(data.raw).filter((item) => "rq" in item)
  return {
    symbol: toStr(data.symbol) ?? symbol,
    reportType,
    rows: parsed.map((item) => ({
      rq: toStr(item.rq),
      fields: Object.fromEntries(
        Object.entries(item).map(([key, value]) => [
          key,
          value === null || value === undefined
            ? null
            : typeof value === "number" || typeof value === "string"
              ? value
              : null,
        ]),
      ),
    })),
    fetchedAt: toStr(data.fetched_at),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockBusinessComposition(
  symbol: string,
  options: { reportDate?: string | null; limit?: number } = {},
): Promise<StockBusinessCompositionResponse> {
  const params = new URLSearchParams({ symbol })
  if (options.reportDate) params.set("report_date", options.reportDate)
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/business-composition?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取主营构成失败")
  const parsed = parseF10ResultSets(data.raw)
  const limit = options.limit ?? 8
  return {
    symbol: toStr(data.symbol) ?? symbol,
    reportDate: options.reportDate ?? null,
    items: parsed.slice(0, limit).map((row) => {
      const values = Object.values(row)
      return {
        category: toStr(row.N000),
        subType:
          values[1] === undefined
            ? null
            : typeof values[1] === "number" || typeof values[1] === "string"
              ? values[1]
              : null,
        name: toStr(row.N002),
        revenue: toNum(row.N003),
        ratio: toNum(row.N004),
        extras: values
          .slice(5)
          .map((value) =>
            value === null || value === undefined
              ? null
              : typeof value === "number" || typeof value === "string"
                ? value
                : null,
          ),
      }
    }),
    fetchedAt: toStr(data.fetched_at),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockProfitForecast(
  symbol: string,
): Promise<StockProfitForecastResponse> {
  const params = new URLSearchParams({ symbol })
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/profit-forecast?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取业绩预测失败")
  const resultSets = (data.raw as { ResultSets?: Array<{ ColName?: string[]; Content?: unknown[][] }> } | undefined)?.ResultSets
  const yearRow = Array.isArray(resultSets?.[0]?.Content?.[0]) ? (resultSets?.[0]?.Content?.[0] as unknown[]) : []
  const yearBase = toStr(yearRow[0]) ?? ""
  const forecastTable = Array.isArray(resultSets?.[1]?.Content?.[0]) ? (resultSets?.[1]?.Content?.[0] as unknown[]) : []
  const forecastCols = Array.isArray(resultSets?.[1]?.ColName) ? resultSets?.[1]?.ColName ?? [] : []

  const items: StockProfitForecastItem[] = []
  for (let index = 0; index < forecastCols.length; index += 1) {
    const colName = forecastCols[index]
    const value = forecastTable[index]
    const offset = forecastCols.length - 1 - index
    const year = yearBase && offset > 0 ? `${Number(yearBase) + offset}` : yearBase || ""
    items.push({
      year,
      netProfit: null,
      eps: toNum(value),
      revenue: null,
      extras: [colName, value == null ? null : typeof value === "number" || typeof value === "string" ? value : null],
    })
  }

  return {
    symbol: toStr(data.symbol) ?? symbol,
    items,
    fetchedAt: toStr(data.fetched_at),
    source: toStr(data.source) ?? "eltdx",
  }
}

export async function fetchStockAnnouncements(symbol: string): Promise<StockAnnouncementsResponse> {
  const params = new URLSearchParams({ symbol })
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/announcements?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取公告列表失败")
  const rawItems = Array.isArray(data.items) ? (data.items as Record<string, unknown>[]) : []
  return {
    symbol: toStr(data.symbol) ?? symbol,
    count: typeof data.count === "number" ? data.count : rawItems.length,
    fetchedAt: toStr(data.fetched_at),
    items: rawItems.map((item) => ({
      issueDate: toStr(item.issue_date),
      title: toStr(item.title),
      typecode: toStr(item.typecode),
      typename: toStr(item.typename),
      recId: toStr(item.rec_id),
      tableid: toStr(item.tableid),
      url: toStr(item.url),
      redistime: toStr(item.redistime),
      source: toStr(item.source),
    })),
  }
}

export async function fetchStockNews(symbol: string): Promise<StockNewsResponse> {
  const params = new URLSearchParams({ symbol })
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/news?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取新闻列表失败")
  const rawItems = Array.isArray(data.items) ? (data.items as Record<string, unknown>[]) : []
  return {
    symbol: toStr(data.symbol) ?? symbol,
    count: typeof data.count === "number" ? data.count : rawItems.length,
    fetchedAt: toStr(data.fetched_at),
    items: rawItems.map((item) => ({
      issueDate: toStr(item.issue_date),
      title: toStr(item.title),
      recId: toStr(item.rec_id),
      tableid: toStr(item.tableid),
      redistime: toStr(item.redistime),
      source: toStr(item.source),
      relatecolumn: toStr(item.relatecolumn),
    })),
  }
}

export async function fetchStockRoadshows(symbol: string): Promise<StockRoadshowsResponse> {
  const params = new URLSearchParams({ symbol })
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/roadshows?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取路演列表失败")
  const rawItems = Array.isArray(data.items) ? (data.items as Record<string, unknown>[]) : []
  return {
    symbol: toStr(data.symbol) ?? symbol,
    count: typeof data.count === "number" ? data.count : rawItems.length,
    fetchedAt: toStr(data.fetched_at),
    items: rawItems.map((item) => ({
      title: toStr(item.title),
      roadshowType: toStr(item.roadshow_type),
      startDate: toStr(item.start_date),
      startTime: toStr(item.start_time),
      endTime: toStr(item.end_time),
      summary: toStr(item.summary),
      url: toStr(item.url),
    })),
  }
}

export async function fetchStockCompanyNews(
  symbol: string,
  options: { section?: string } = {},
): Promise<StockCompanyNewsResponse> {
  const params = new URLSearchParams({ symbol })
  const section = options.section ?? "gsyj"
  params.set("section", section)
  const res = await fetchWithRetry(
    `${STOCK_CHART_LEGACY_API_BASE}/f10/company-news?${params.toString()}`,
    withTimeout(F10_REQ_TIMEOUT_MS),
  )
  const data = await readLegacyJson<Record<string, unknown>>(res, "获取公司研报失败")
  const rawItems = Array.isArray(data.items) ? (data.items as Record<string, unknown>[]) : []
  return {
    symbol: toStr(data.symbol) ?? symbol,
    section,
    count: typeof data.count === "number" ? data.count : rawItems.length,
    fetchedAt: toStr(data.fetched_at),
    items: rawItems.map((item) => ({
      rating: toStr(item.rating),
      analysts: toStr(item.analysts),
      recId: toStr(item.rec_id),
      issueDate: toStr(item.issue_date),
      title: toStr(item.title),
      nflag: toStr(item.nflag),
      docHash: toStr(item.doc_hash),
    })),
  }
}
