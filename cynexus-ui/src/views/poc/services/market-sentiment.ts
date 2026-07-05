const API_BASE = (typeof import.meta !== "undefined" && import.meta.env?.VITE_CYNEXUS_API_BASE) || ""
const MARKET_SENTIMENT_API_BASE = `${API_BASE}/api/market/sentiment`

type ApiResult<T> = {
  success?: boolean
  code?: string
  message?: string
  data?: T
}

async function fetchJson<T>(url: string, fallbackMessage: string): Promise<T> {
  const res = await fetch(url)
  const data = (await res.json().catch(() => null)) as (ApiResult<T> & T) | null
  if (!res.ok || !data) {
    throw new Error(fallbackMessage)
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

const nullableNumber = (value: unknown): number | null => {
  if (value == null) return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

const nullableInt = (value: unknown): number | null => {
  const n = nullableNumber(value)
  return n == null ? null : Math.round(n)
}

export interface MarketSentimentIndexComponents {
  vol: number | null
  turnover: number | null
  price_strength: number | null
  risk_appetite: number | null
  breadth: number | null
  limit_emotion: number | null
  profit_effect: number | null
  sector_breadth: number | null
  style_risk: number | null
}

export interface MarketSentimentIndexHistoryItem {
  tradeDate: string
  compositeScore: number | null
  level: string
  componentCount: number
  components: MarketSentimentIndexComponents
  fromCache?: boolean
}

export interface MarketSentimentIndexHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: MarketSentimentIndexHistoryItem[]
  error?: string
}

export interface IndexDailyItem {
  tradeDate: string
  close: number
  amount?: number
}

export interface IndexDailyResponse {
  ok: boolean
  code: string
  name: string
  start: string
  end: string
  count: number
  items: IndexDailyItem[]
  error?: string
}

export interface MarketOverviewHistoryItem {
  tradeDate: string
  totalAmount: number | null
  totalVolume: number | null
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  stockCount: number | null
  mainNetInflow: number | null
  superLargeNetInflow: number | null
  largeNetInflow: number | null
  mediumNetInflow: number | null
  smallNetInflow: number | null
  mainNetInflowRatio: number | null
  source?: string
  fromCache?: boolean
}

export interface MarketOverviewHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: MarketOverviewHistoryItem[]
  error?: string
}

function parseMsiComponents(raw: unknown): MarketSentimentIndexComponents {
  const c = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>
  return {
    vol: nullableNumber(c.vol),
    turnover: nullableNumber(c.turnover),
    price_strength: nullableNumber(c.price_strength ?? c.priceStrength),
    risk_appetite: nullableNumber(c.risk_appetite ?? c.riskAppetite),
    breadth: nullableNumber(c.breadth),
    limit_emotion: nullableNumber(c.limit_emotion ?? c.limitEmotion),
    profit_effect: nullableNumber(c.profit_effect ?? c.profitEffect),
    sector_breadth: nullableNumber(c.sector_breadth ?? c.sectorBreadth),
    style_risk: nullableNumber(c.style_risk ?? c.styleRisk),
  }
}

export async function fetchMarketSentimentIndexHistory(
  start: string,
  end: string,
): Promise<MarketSentimentIndexHistoryResponse> {
  const params = new URLSearchParams({ start, end })
  const data = await fetchJson<Record<string, unknown>>(
    `${MARKET_SENTIMENT_API_BASE}/index/history?${params.toString()}`,
    "获取 POC 市场情绪指数失败",
  )
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: String(data.start ?? start),
    end: String(data.end ?? end),
    count: typeof data.count === "number" ? data.count : raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      compositeScore: nullableNumber(it.compositeScore),
      level: String(it.level ?? "normal"),
      componentCount: Number(it.componentCount ?? 0),
      components: parseMsiComponents(it.components),
      fromCache: Boolean(it.fromCache),
    })),
    error: typeof data.error === "string" ? data.error : undefined,
  }
}

export async function fetchIndexDailyHistory(params: {
  code: string
  start: string
  end: string
}): Promise<IndexDailyResponse> {
  const query = new URLSearchParams({
    code: params.code,
    start: params.start,
    end: params.end,
  })
  const data = await fetchJson<Record<string, unknown>>(
    `${MARKET_SENTIMENT_API_BASE}/index/daily?${query.toString()}`,
    "获取 POC 指数日线失败",
  )
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    code: String(data.code ?? params.code),
    name: String(data.name ?? params.code),
    start: String(data.start ?? params.start),
    end: String(data.end ?? params.end),
    count: typeof data.count === "number" ? data.count : raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      close: Number(it.close ?? 0),
      amount: nullableNumber(it.amount) ?? undefined,
    })),
    error: typeof data.error === "string" ? data.error : undefined,
  }
}

export async function fetchMarketOverviewHistory(params: {
  start: string
  end: string
}): Promise<MarketOverviewHistoryResponse> {
  const query = new URLSearchParams({ start: params.start, end: params.end })
  const data = await fetchJson<Record<string, unknown>>(
    `${MARKET_SENTIMENT_API_BASE}/market-overview/history?${query.toString()}`,
    "获取 POC 大盘概况历史失败",
  )
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: String(data.start ?? params.start),
    end: String(data.end ?? params.end),
    count: typeof data.count === "number" ? data.count : raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? it.date ?? ""),
      totalAmount: nullableNumber(it.totalAmount),
      totalVolume: nullableNumber(it.totalVolume),
      risingCount: nullableInt(it.risingCount),
      fallingCount: nullableInt(it.fallingCount),
      flatCount: nullableInt(it.flatCount),
      limitUpCount: nullableInt(it.limitUpCount),
      limitDownCount: nullableInt(it.limitDownCount),
      stockCount: nullableInt(it.stockCount),
      mainNetInflow: nullableNumber(it.mainNetInflow),
      superLargeNetInflow: nullableNumber(it.superLargeNetInflow),
      largeNetInflow: nullableNumber(it.largeNetInflow),
      mediumNetInflow: nullableNumber(it.mediumNetInflow),
      smallNetInflow: nullableNumber(it.smallNetInflow),
      mainNetInflowRatio: nullableNumber(it.mainNetInflowRatio),
      source: typeof it.source === "string" ? it.source : undefined,
      fromCache: Boolean(it.fromCache),
    })),
    error: typeof data.error === "string" ? data.error : undefined,
  }
}
