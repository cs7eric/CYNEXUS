import { fetchWithRetry } from "@/services/common/http"
import {
  fetchIndexKlineBatch as fetchMarketPulseIndexKlineBatch,
  fetchMarketPulseIndexReturns as fetchMarketPulseIndexReturnsFromPulse,
  fetchMarketPulseIndexReturnsHistory as fetchMarketPulseIndexReturnsHistoryFromPulse,
  triggerMarketOverviewAkshareRefresh as triggerMarketOverviewAkshareRefreshFromPulse,
  type MarketOverview,
} from "@/services/market/pulse"

const API_BASE = (typeof import.meta !== "undefined" && import.meta.env?.VITE_CYNEXUS_API_BASE) || ""
const MARKET_SENTIMENT_API_BASE = `${API_BASE}/api/market/sentiment`

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

export interface MaCountBoardStat {
  total: number
  aboveMa20: number
  aboveMa60: number
  aboveBoth: number
  pctMa20: number
  pctMa60: number
  pctBoth: number
  /** 近 5 个交易日上涨 (close > close_5d_ago) 数量 */
  up5d?: number
  /** up5d / total * 100 */
  pctUp5d?: number
  /** 创 60 日新低 (close == 60日窗口内 min) 数量 */
  newLow60d?: number
  /** newLow60d / total * 100 */
  pctNewLow60d?: number
  /** 创 252 日新高 (close >= 252日窗口内 max, 满 252 行窗口) 数量 */
  newHigh252d?: number
  /** newHigh252d / total * 100 */
  pctNewHigh252d?: number
  /** 当日上涨 (close > prev close) 数量 */
  advancing?: number
  /** advancing / total * 100 */
  pctAdvancing?: number
}

export interface MaCountResponse {
  ok: boolean
  tradeDate: string
  totalEligible: number
  aboveMa20: number
  aboveMa60: number
  aboveBoth: number
  pctAboveMa20: number
  pctAboveMa60: number
  pctAboveBoth: number
  /** 近 5 日上涨股票数 (close > 5 个交易日前 close) */
  up5dCount: number
  /** up5dCount / totalEligible * 100 */
  pctUp5d: number
  /** 创 60 日新低股票数 (close == 60日窗口内 min, 满 60 行窗口) */
  newLow60dCount: number
  /** newLow60dCount / totalEligible * 100 */
  pctNewLow60d: number
  /** 创 252 日新高股票数 (close >= 252日窗口内 max, 满 252 行窗口) */
  newHigh252dCount: number
  /** newHigh252dCount / totalEligible * 100 */
  pctNewHigh252d: number
  /** 当日上涨股票数 (close > 前一日 close) */
  advancingCount: number
  /** advancingCount / totalEligible * 100 */
  pctAdvancing: number
  /** 252日新高 0-100 历史分位情绪得分 (基于过去 3 年) */
  newHigh252dScore?: number
  /** 原始 pctNewHigh252d 值 */
  newHigh252dRawValue?: number
  /** 市场广度 0-100 历史分位情绪得分 (breadth_raw = 40%上涨 + 35%MA20 + 25%MA60) */
  breadthScore?: number
  /** 原始 breadth_raw 值 */
  breadthRawValue?: number
  byBoard: Record<string, MaCountBoardStat>
  elapsedMs?: number
  source?: string
  error?: string
}

export async function fetchMarketSentimentMaCount(date?: string): Promise<MaCountResponse> {
  const url = date
    ? `${MARKET_SENTIMENT_API_BASE}/ma-count?date=${encodeURIComponent(date)}`
    : `${MARKET_SENTIMENT_API_BASE}/ma-count`
  const res = await fetchWithRetry(url)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, tradeDate: date ?? "", totalEligible: 0,
             aboveMa20: 0, aboveMa60: 0, aboveBoth: 0,
             pctAboveMa20: 0, pctAboveMa60: 0, pctAboveBoth: 0,
             up5dCount: 0, pctUp5d: 0,
             newLow60dCount: 0, pctNewLow60d: 0,
             newHigh252dCount: 0, pctNewHigh252d: 0,
             advancingCount: 0, pctAdvancing: 0,
             byBoard: {}, error: `HTTP ${res.status}` }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? ""),
    totalEligible: (data.totalEligible as number) ?? 0,
    aboveMa20: (data.aboveMa20 as number) ?? 0,
    aboveMa60: (data.aboveMa60 as number) ?? 0,
    aboveBoth: (data.aboveBoth as number) ?? 0,
    pctAboveMa20: (data.pctAboveMa20 as number) ?? 0,
    pctAboveMa60: (data.pctAboveMa60 as number) ?? 0,
    pctAboveBoth: (data.pctAboveBoth as number) ?? 0,
    up5dCount: (data.up5dCount as number) ?? 0,
    pctUp5d: (data.pctUp5d as number) ?? 0,
    newLow60dCount: (data.newLow60dCount as number) ?? 0,
    pctNewLow60d: (data.pctNewLow60d as number) ?? 0,
    newHigh252dCount: (data.newHigh252dCount as number) ?? 0,
    pctNewHigh252d: (data.pctNewHigh252d as number) ?? 0,
    newHigh252dScore: (data.newHigh252dScore as number) ?? undefined,
    newHigh252dRawValue: (data.newHigh252dRawValue as number) ?? undefined,
    breadthScore: (data.breadthScore as number) ?? undefined,
    breadthRawValue: (data.breadthRawValue as number) ?? undefined,
    advancingCount: (data.advancingCount as number) ?? 0,
    pctAdvancing: (data.pctAdvancing as number) ?? 0,
    byBoard: (data.byBoard as Record<string, MaCountBoardStat>) ?? {},
    elapsedMs: (data.elapsedMs as number) ?? undefined,
    source: (data.source as string) ?? undefined,
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// Market Pulse · 宽基指数近 N 日收益 (沪深300 / 中证1000)
// 数据源: duckdb.index_daily_raw
// ---------------------------------------------------------------------------
export interface IndexReturnDaily {
  date: string
  close: number
  dailyReturnPct: number | null
}

export interface IndexReturnItem {
  name: string
  code: string
  fullCode: string
  current: number | null
  currentDate: string | null
  baseClose: number | null
  baseDate: string | null
  returnPct: number | null
  daily: IndexReturnDaily[]
  available: boolean
}

export interface IndexReturnsResponse {
  ok: boolean
  days: number
  items: IndexReturnItem[]
  error?: string
}

export async function fetchMarketPulseIndexReturns(days: number = 5): Promise<IndexReturnsResponse> {
  return fetchMarketPulseIndexReturnsFromPulse(days) as Promise<IndexReturnsResponse>
}

// ---------------------------------------------------------------------------
// MA 计数 + 指数收益 历史趋势 (sparkline 用, 按日期范围查)
// 数据源: duckdb.ma_count_daily / index_returns_daily (持久化, 0.8ms 返回)
// ---------------------------------------------------------------------------
export interface MaCountHistoryItem {
  tradeDate: string
  totalEligible: number
  aboveMa20: number
  aboveMa60: number
  aboveBoth: number
  pctAboveMa20: number
  pctAboveMa60: number
  pctAboveBoth: number
  up5dCount: number
  pctUp5d: number
  newLow60dCount: number
  pctNewLow60d: number
  newHigh252dCount: number
  pctNewHigh252d: number
  advancingCount: number
  pctAdvancing: number
  /** 252日新高 0-100 历史分位情绪得分 */
  newHigh252dScore?: number
  /** 市场广度 0-100 历史分位情绪得分 */
  breadthScore?: number
  fromCache?: boolean
}

export interface MaCountHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: MaCountHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentMaCountHistory(
  start: string,
  end: string,
): Promise<MaCountHistoryResponse> {
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/ma-count/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      totalEligible: (it.totalEligible as number) ?? 0,
      aboveMa20: (it.aboveMa20 as number) ?? 0,
      aboveMa60: (it.aboveMa60 as number) ?? 0,
      aboveBoth: (it.aboveBoth as number) ?? 0,
      pctAboveMa20: (it.pctAboveMa20 as number) ?? 0,
      pctAboveMa60: (it.pctAboveMa60 as number) ?? 0,
      pctAboveBoth: (it.pctAboveBoth as number) ?? 0,
      up5dCount: (it.up5dCount as number) ?? 0,
      pctUp5d: (it.pctUp5d as number) ?? 0,
      newLow60dCount: (it.newLow60dCount as number) ?? 0,
      pctNewLow60d: (it.pctNewLow60d as number) ?? 0,
      newHigh252dCount: (it.newHigh252dCount as number) ?? 0,
      pctNewHigh252d: (it.pctNewHigh252d as number) ?? 0,
      newHigh252dScore: (it.newHigh252dScore as number) ?? undefined,
      breadthScore: (it.breadthScore as number) ?? undefined,
      advancingCount: (it.advancingCount as number) ?? 0,
      pctAdvancing: (it.pctAdvancing as number) ?? 0,
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
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
  error?: string
}

export async function fetchMarketPulseIndexReturnsHistory(
  window: number = 5,
  start: string,
  end: string,
): Promise<IndexReturnsHistoryResponse> {
  return fetchMarketPulseIndexReturnsHistoryFromPulse(window, start, end) as Promise<IndexReturnsHistoryResponse>
}

// ---------------------------------------------------------------------------
// Market Sentiment · 板块扩散 (上涨行业数 / 有效行业数)
// 数据源: duckdb.market_pulse_sector_breadth_daily
//        (由 ths_industry_fund_flow_daily 聚合, 工作日 17:15 收盘后算)
// 归属: /market/sentiment 页面 (跟 ma-count / risk-appetite / limit-emotion-summary 同空间)
// ---------------------------------------------------------------------------
export interface SectorBreadthItem {
  tradeDate: string
  advancing: number        // 上涨行业数
  declining: number        // 下跌行业数
  flat: number             // 平盘行业数
  total: number            // 有效行业数
  advancePct: number       // 0-1, 上涨占比
  /** 0-100 情绪得分 = advancePct × 100 (天然百分比, 不需要百分位) */
  score?: number | null
  source: string | null
  elapsedMs: number | null
  fromCache?: boolean
}

export interface SectorBreadthResponse {
  ok: boolean
  tradeDate: string
  advancing: number
  declining: number
  flat: number
  total: number
  advancePct: number
  score?: number | null
  source: string | null
  elapsedMs: number | null
  fromCache?: boolean
  error?: string
}

export interface SectorBreadthHistoryResponse {
  ok: boolean
  start: string
  end: string
  days: number
  count: number
  items: SectorBreadthItem[]
  error?: string
}

export async function fetchMarketSentimentSectorBreadth(
  date?: string,
): Promise<SectorBreadthResponse> {
  const q = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(`${MARKET_SENTIMENT_API_BASE}/sector-breadth${q}`)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false,
      tradeDate: date ?? "",
      advancing: 0, declining: 0, flat: 0, total: 0, advancePct: 0,
      score: 0,
      source: null, elapsedMs: null, error: `HTTP ${res.status}`,
    }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? date ?? ""),
    advancing: Number(data.advancing ?? 0),
    declining: Number(data.declining ?? 0),
    flat: Number(data.flat ?? 0),
    total: Number(data.total ?? 0),
    advancePct: Number(data.advancePct ?? 0),
    score: (data.score as number) ?? Number(data.advancePct ?? 0) * 100,
    source: (data.source as string) ?? null,
    elapsedMs: (data.elapsedMs as number) ?? null,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export async function fetchMarketSentimentSectorBreadthHistory(
  days: number = 30,
  end?: string,
): Promise<SectorBreadthHistoryResponse> {
  const params = new URLSearchParams({ days: String(days) })
  if (end) params.set("end", end)
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/sector-breadth/history?${params.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start: "", end: "", days, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? "",
    end: (data.end as string) ?? end ?? "",
    days: (data.days as number) ?? days,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      advancing: Number(it.advancing ?? 0),
      declining: Number(it.declining ?? 0),
      flat: Number(it.flat ?? 0),
      total: Number(it.total ?? 0),
      advancePct: Number(it.advancePct ?? 0),
      score: (it.score as number) ?? Number(it.advancePct ?? 0) * 100,
      source: (it.source as string) ?? null,
      elapsedMs: (it.elapsedMs as number) ?? null,
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// Market Sentiment · 风险偏好 (沪深300 20日 - (511010 + 511090)/2 国债 ETF 20日)
// 数据源: duckdb.risk_appetite_daily (持久化, cache-aside)
// 归属: /market/sentiment 页面 (Market Sentiment 路由), 不是 market-pulse
// ---------------------------------------------------------------------------
export interface RiskAppetiteAsset {
  /** 当前收盘价 (前复权) */
  close: number | null
  /** 当前 bar 的实际日期 (可能滞后于请求日期) */
  currentDate: string | null
  /** 20 个交易日前的 close */
  baseClose: number | null
  /** 20 个交易日前的实际日期 */
  baseDate: string | null
  /** window 日累计收益 % */
  returnPct: number | null
  /** 511010 / 511090 才有: 在综合 treasury 里的权重 (默认 0.5 / 0.5) */
  weight?: number
  /** 实际用到的 bar 数 (调试用) */
  barsUsed?: number
}

export interface RiskAppetiteResponse {
  ok: boolean
  tradeDate: string
  windowDays: number
  hs300: RiskAppetiteAsset
  treasury: {
    "511010": RiskAppetiteAsset
    "511090": RiskAppetiteAsset
    weighted: { returnPct: number | null }
  }
  spread: {
    "511010": number | null   // hs300 - 511010
    "511090": number | null   // hs300 - 511090
    weighted: number | null   // hs300 - (511010+511090)/2  ← 主指标
  }
  /** 0-100 历史分位情绪得分 (基于过去 3 年 spread_weighted 的百分位) */
  score?: number
  /** 原始 spread_weighted 值 (百分比) */
  rawValue?: number
  elapsedMs?: number
  source?: string
  fromCache?: boolean
  error?: string
}

export async function fetchMarketSentimentRiskAppetite(date?: string): Promise<RiskAppetiteResponse> {
  const url = date
    ? `${MARKET_SENTIMENT_API_BASE}/risk-appetite?date=${encodeURIComponent(date)}`
    : `${MARKET_SENTIMENT_API_BASE}/risk-appetite`
  const res = await fetchWithRetry(url)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false, tradeDate: date ?? "", windowDays: 20,
      hs300: { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null },
      treasury: {
        "511010": { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null, weight: 0.5 },
        "511090": { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null, weight: 0.5 },
        weighted: { returnPct: null },
      },
      spread: { "511010": null, "511090": null, weighted: null },
      error: `HTTP ${res.status}`,
    }
  }
  const t = (data.treasury as Record<string, unknown>) || {}
  const s = (data.spread as Record<string, unknown>) || {}
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? ""),
    windowDays: (data.windowDays as number) ?? 20,
    hs300: (data.hs300 as RiskAppetiteAsset) ?? { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null },
    treasury: {
      "511010": (t["511010"] as RiskAppetiteAsset) ?? { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null, weight: 0.5 },
      "511090": (t["511090"] as RiskAppetiteAsset) ?? { close: null, currentDate: null, baseClose: null, baseDate: null, returnPct: null, weight: 0.5 },
      weighted: (t.weighted as { returnPct: number | null }) ?? { returnPct: null },
    },
    spread: {
      "511010": (s["511010"] as number | null) ?? null,
      "511090": (s["511090"] as number | null) ?? null,
      weighted: (s.weighted as number | null) ?? null,
    },
    elapsedMs: (data.elapsedMs as number) ?? undefined,
    source: (data.source as string) ?? undefined,
    fromCache: Boolean(data.fromCache),
    score: (data.score as number) ?? undefined,
    rawValue: (data.rawValue as number) ?? undefined,
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// 风险偏好 历史 (sparkline 用, 跟 ma-count history 同一模式)
// ---------------------------------------------------------------------------
export interface RiskAppetiteHistoryItem {
  tradeDate: string
  hs300ReturnPct: number | null
  treasury511010ReturnPct: number | null
  treasury511090ReturnPct: number | null
  treasuryWeightedReturnPct: number | null
  spread511010: number | null
  spread511090: number | null
  spreadWeighted: number | null
  /** 0-100 历史分位情绪得分 */
  score?: number
  fromCache?: boolean
}

export interface RiskAppetiteHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: RiskAppetiteHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentRiskAppetiteHistory(
  start: string,
  end: string,
): Promise<RiskAppetiteHistoryResponse> {
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/risk-appetite/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => {
      const hs = (it.hs300 as Record<string, unknown>) || {}
      const t = (it.treasury as Record<string, unknown>) || {}
      const s = (it.spread as Record<string, unknown>) || {}
      const tw = (t.weighted as Record<string, unknown>) || {}
      return {
        tradeDate: String(it.tradeDate ?? ""),
        hs300ReturnPct: (it.hs300ReturnPct as number | null) ?? (hs.returnPct as number | null) ?? null,
        treasury511010ReturnPct: (it.treasury511010ReturnPct as number | null) ?? ((t["511010"] as Record<string, unknown>)?.returnPct as number | null) ?? null,
        treasury511090ReturnPct: (it.treasury511090ReturnPct as number | null) ?? ((t["511090"] as Record<string, unknown>)?.returnPct as number | null) ?? null,
        treasuryWeightedReturnPct: (it.treasuryWeightedReturnPct as number | null) ?? (tw.returnPct as number | null) ?? null,
        spread511010: (it.spread511010 as number | null) ?? (s["511010"] as number | null) ?? null,
        spread511090: (it.spread511090 as number | null) ?? (s["511090"] as number | null) ?? null,
        spreadWeighted: (it.spreadWeighted as number | null) ?? (s.weighted as number | null) ?? null,
        score: (it.score as number) ?? undefined,
        fromCache: Boolean(it.fromCache),
      }
    }),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// Market Sentiment · 涨跌停情绪综合分 (短线情绪)
//
// 公式 (跟 backend/repositories/market/limit_repo.calc_limit_emotion_summary 一致):
//   涨跌停比    = limit_up / max(limit_down, 1)
//   炸板率      = broken / touched
//   昨日涨停收益 = AVG(今日 changePct) for codes where 昨日 isLimitUp
//   up_down_score       = clamp(50 + 25 * log2(ratio))            ∈ [0, 100]
//   break_board_score   = clamp(100 - 100 * rate)                 ∈ [0, 100]   (反向)
//   yesterday_return_score = clamp(50 + 10 * avg_return_pct)      ∈ [0, 100]
//   composite = 0.4 * A + 0.3 * B + 0.3 * C
//   level: hot (>=80) / active (>=60) / normal (>=40) / weak (>=20) / ice (<20)
//
// 归属: /market/sentiment 页面, 不是 market-pulse.
// 后端路径: /api/market/sentiment/limit-emotion-summary
// ---------------------------------------------------------------------------
export type LimitEmotionLevel = "hot" | "active" | "warm" | "normal" | "weak" | "ice"

export interface LimitEmotionSummary {
  ok: boolean
  tradeDate: string
  prevTradeDate: string | null
  /** 今日涨停股数 */
  limitUpCount: number
  /** 今日跌停股数 */
  limitDownCount: number
  /** 今日盘中触板股数 (high >= 涨停价) */
  touchedCount: number
  /** 今日炸板股数 (触板但未封板) */
  brokenCount: number
  /** 炸板率 broken / touched, ∈ [0, 1], null when touched=0 */
  breakBoardRate: number | null
  /** 涨跌停比 = limitUp / max(limitDown, 1) */
  limitUpDownRatio: number
  /** 昨日涨停股数 */
  yesterdayLimitUpCount: number
  /** 昨日涨停股今日平均涨跌幅 (%) */
  yesterdayLimitUpAvgReturn: number | null
  components: {
    upDownScore: number
    breakBoardScore: number
    yesterdayReturnScore: number
  }
  compositeScore: number
  level: LimitEmotionLevel
  elapsedMs?: number
  source?: string
  fromCache?: boolean
  error?: string
}

export async function fetchMarketSentimentLimitEmotionSummary(
  date?: string,
): Promise<LimitEmotionSummary> {
  const url = date
    ? `${MARKET_SENTIMENT_API_BASE}/limit-emotion-summary?date=${encodeURIComponent(date)}`
    : `${MARKET_SENTIMENT_API_BASE}/limit-emotion-summary`
  const res = await fetchWithRetry(url)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false, tradeDate: date ?? "", prevTradeDate: null,
      limitUpCount: 0, limitDownCount: 0, touchedCount: 0, brokenCount: 0,
      breakBoardRate: null, limitUpDownRatio: 0,
      yesterdayLimitUpCount: 0, yesterdayLimitUpAvgReturn: null,
      components: { upDownScore: 0, breakBoardScore: 50, yesterdayReturnScore: 50 },
      compositeScore: 0, level: "weak", error: `HTTP ${res.status}`,
    }
  }
  const comp = (data.components as Record<string, unknown>) || {}
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? ""),
    prevTradeDate: (data.prevTradeDate as string | null) ?? null,
    limitUpCount: (data.limitUpCount as number) ?? 0,
    limitDownCount: (data.limitDownCount as number) ?? 0,
    touchedCount: (data.touchedCount as number) ?? 0,
    brokenCount: (data.brokenCount as number) ?? 0,
    breakBoardRate: (data.breakBoardRate as number | null) ?? null,
    limitUpDownRatio: (data.limitUpDownRatio as number) ?? 0,
    yesterdayLimitUpCount: (data.yesterdayLimitUpCount as number) ?? 0,
    yesterdayLimitUpAvgReturn: (data.yesterdayLimitUpAvgReturn as number | null) ?? null,
    components: {
      upDownScore: (comp.upDownScore as number) ?? 0,
      breakBoardScore: (comp.breakBoardScore as number) ?? 50,
      yesterdayReturnScore: (comp.yesterdayReturnScore as number) ?? 50,
    },
    compositeScore: (data.compositeScore as number) ?? 0,
    level: (data.level as LimitEmotionLevel) ?? "weak",
    elapsedMs: (data.elapsedMs as number) ?? undefined,
    source: (data.source as string) ?? undefined,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export interface LimitEmotionSummaryHistoryItem {
  tradeDate: string
  limitUpCount: number
  limitDownCount: number
  touchedCount: number
  brokenCount: number
  breakBoardRate: number | null
  limitUpDownRatio: number
  yesterdayLimitUpCount: number
  yesterdayLimitUpAvgReturn: number | null
  compositeScore: number
  level: LimitEmotionLevel
  fromCache?: boolean
}

export interface LimitEmotionSummaryHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: LimitEmotionSummaryHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentLimitEmotionSummaryHistory(
  start: string,
  end: string,
): Promise<LimitEmotionSummaryHistoryResponse> {
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/limit-emotion-summary/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      limitUpCount: (it.limitUpCount as number) ?? 0,
      limitDownCount: (it.limitDownCount as number) ?? 0,
      touchedCount: (it.touchedCount as number) ?? 0,
      brokenCount: (it.brokenCount as number) ?? 0,
      breakBoardRate: (it.breakBoardRate as number | null) ?? null,
      limitUpDownRatio: (it.limitUpDownRatio as number) ?? 0,
      yesterdayLimitUpCount: (it.yesterdayLimitUpCount as number) ?? 0,
      yesterdayLimitUpAvgReturn: (it.yesterdayLimitUpAvgReturn as number | null) ?? null,
      compositeScore: (it.compositeScore as number) ?? 0,
      level: (it.level as LimitEmotionLevel) ?? "weak",
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// Market Sentiment · 波动率情绪 (情绪分项 ⑤)
//
// 公式 (跟 backend/repositories/market/volatility_sentiment_repo 一致):
//   realized_vol_20d = std(近 20 日日收益率) × √252 × 100   (%, 年化)
//   percentile_1y    = rank(近 252 个交易日的 vol, 含等于) / 252  ∈ [0, 1]
//   sentiment_score  = (1 - percentile_1y) × 100              ∈ [0, 100]
//                     高分=平静 (低波, 情绪好)  低分=波动 (高波, 情绪差)
//
// 数据源: duckdb.volatility_sentiment_daily (cache-aside, /api/.../volatility-sentiment)
// 归属: /market/sentiment 页面, 不是 market-pulse
// ---------------------------------------------------------------------------
export interface VolatilitySentimentItem {
  tradeDate: string
  /** 6 位 code (有 sh/sz/bj 前缀), 例 "sh000300" */
  underlyingCode: string
  /** 中文名, 例 "沪深300" */
  underlyingName: string
  /** 沪深300 当日收盘价 */
  close: number | null
  /** 当日日收益率 % */
  dailyReturnPct: number | null
  /** 20 日年化波动率 % */
  realizedVol20d: number | null
  volWindowDays: number
  volLookbackDays: number
  /** 历史分位 0-1, 越小=越平静 */
  percentile1y: number | null
  /** 情绪得分 0-100, 反向 (高分=情绪好) */
  score?: number | null
  sentimentScore: number | null
  sampleCount: number
  elapsedMs?: number
  source?: string
  fromCache?: boolean
}

export interface VolatilitySentimentResponse {
  ok: boolean
  tradeDate: string
  underlyingCode: string
  underlyingName: string
  close: number | null
  dailyReturnPct: number | null
  realizedVol20d: number | null
  volWindowDays: number
  volLookbackDays: number
  percentile1y: number | null
  score?: number | null
  sentimentScore: number | null
  sampleCount: number
  elapsedMs?: number
  source?: string
  fromCache?: boolean
  error?: string
}

export async function fetchMarketSentimentVolatilitySentiment(
  date?: string,
): Promise<VolatilitySentimentResponse> {
  const q = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(`${MARKET_SENTIMENT_API_BASE}/volatility-sentiment${q}`)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false,
      tradeDate: date ?? "",
      underlyingCode: "sh000300",
      underlyingName: "沪深300",
      close: null,
      dailyReturnPct: null,
      realizedVol20d: null,
      volWindowDays: 20,
      volLookbackDays: 252,
      percentile1y: null,
      score: null,
      sentimentScore: null,
      sampleCount: 0,
      error: `HTTP ${res.status}`,
    }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? date ?? ""),
    underlyingCode: String(data.underlyingCode ?? "sh000300"),
    underlyingName: String(data.underlyingName ?? "沪深300"),
    close: (data.close as number | null) ?? null,
    dailyReturnPct: (data.dailyReturnPct as number | null) ?? null,
    realizedVol20d: (data.realizedVol20d as number | null) ?? null,
    volWindowDays: Number(data.volWindowDays ?? 20),
    volLookbackDays: Number(data.volLookbackDays ?? 252),
    percentile1y: (data.percentile1y as number | null) ?? null,
    score: (data.score as number | null) ?? (data.sentimentScore as number | null) ?? null,
    sentimentScore: (data.sentimentScore as number | null) ?? (data.score as number | null) ?? null,
    sampleCount: Number(data.sampleCount ?? 0),
    elapsedMs: (data.elapsedMs as number) ?? null,
    source: (data.source as string) ?? null,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export interface VolatilitySentimentHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: VolatilitySentimentItem[]
  error?: string
}

export async function fetchMarketSentimentVolatilitySentimentHistory(
  start: string,
  end: string,
): Promise<VolatilitySentimentHistoryResponse> {
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/volatility-sentiment/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      underlyingCode: String(it.underlyingCode ?? "sh000300"),
      underlyingName: String(it.underlyingName ?? "沪深300"),
      close: (it.close as number | null) ?? null,
      dailyReturnPct: (it.dailyReturnPct as number | null) ?? null,
      realizedVol20d: (it.realizedVol20d as number | null) ?? null,
      volWindowDays: Number(it.volWindowDays ?? 20),
      volLookbackDays: Number(it.volLookbackDays ?? 252),
      percentile1y: (it.percentile1y as number | null) ?? null,
      score: (it.score as number | null) ?? (it.sentimentScore as number | null) ?? null,
      sentimentScore: (it.sentimentScore as number | null) ?? (it.score as number | null) ?? null,
      sampleCount: Number(it.sampleCount ?? 0),
      elapsedMs: (it.elapsedMs as number) ?? null,
      source: (it.source as string) ?? null,
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// 成交活跃度 (Turnover Activity · Market Sentiment)
// ---------------------------------------------------------------------------
export interface TurnoverActivityResponse {
  ok: boolean
  tradeDate: string
  totalAmount: number | null
  avg20dAmount: number | null
  ratio: number | null
  /** 0-100 历史分位情绪得分 (基于过去 3 年 ratio 的百分位) */
  score?: number
  /** 原始 ratio 值 */
  rawValue?: number
  elapsedMs: number | null
  source: string
  error?: string
}

export interface TurnoverActivityHistoryItem {
  tradeDate: string
  totalAmount: number | null
  avg20dAmount: number | null
  ratio: number | null
  /** 0-100 历史分位情绪得分 */
  score?: number
  elapsedMs: number | null
  source: string
  fromCache: boolean
}

export interface TurnoverActivityHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: TurnoverActivityHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentTurnoverActivity(
  date?: string,
): Promise<TurnoverActivityResponse> {
  const params = new URLSearchParams()
  if (date) params.set("date", date)
  const qs = params.toString()
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/turnover-activity${qs ? "?" + qs : ""}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, tradeDate: date ?? "", totalAmount: null, avg20dAmount: null, ratio: null, elapsedMs: null, source: "", error: `HTTP ${res.status}` }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? ""),
    totalAmount: (data.totalAmount as number | null) ?? null,
    avg20dAmount: (data.avg20dAmount as number | null) ?? null,
    ratio: (data.ratio as number | null) ?? null,
    score: (data.score as number) ?? undefined,
    rawValue: (data.rawValue as number) ?? undefined,
    elapsedMs: (data.elapsedMs as number | null) ?? null,
    source: (data.source as string) ?? "",
    error: data.error as string | undefined,
  }
}

export async function fetchMarketSentimentTurnoverActivityHistory(
  start: string,
  end: string,
): Promise<TurnoverActivityHistoryResponse> {
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/turnover-activity/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      totalAmount: (it.totalAmount as number | null) ?? null,
      avg20dAmount: (it.avg20dAmount as number | null) ?? null,
      ratio: (it.ratio as number | null) ?? null,
      score: (it.score as number) ?? undefined,
      elapsedMs: (it.elapsedMs as number) ?? null,
      source: (it.source as string) ?? "",
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// 风格风险偏好 (Style Risk Appetite) — Market Sentiment
// ---------------------------------------------------------------------------
// 风格强弱 = 中证1000 近5日收益率 - 沪深300 近5日收益率
// spread > 0: 小盘更强 (市场风险偏好积极)
// spread < 0: 大盘更强 (避险倾向)
// ---------------------------------------------------------------------------

export interface StyleRiskAppetiteIndex {
  name: string
  code: string
  returnPct: number | null
  current?: number | null
  baseClose?: number | null
}

export interface StyleRiskAppetiteResponse {
  ok: boolean
  tradeDate: string
  windowDays: number
  hs300: StyleRiskAppetiteIndex
  csi1000: StyleRiskAppetiteIndex
  spread: number | null
  /** 0-100 历史分位情绪得分 (基于过去 3 年 spread 的百分位) */
  score?: number
  /** 原始 spread 值 (百分比) */
  rawValue?: number
  elapsedMs?: number
  source?: string
  fromCache?: boolean
  error?: string
}

export async function fetchMarketSentimentStyleRiskAppetite(
  date?: string,
): Promise<StyleRiskAppetiteResponse> {
  const q = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/style-risk-appetite${q}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false,
      tradeDate: date ?? "",
      windowDays: 5,
      hs300: { name: "沪深300", code: "sh000300", returnPct: null },
      csi1000: { name: "中证1000", code: "sh000852", returnPct: null },
      spread: null,
      error: `HTTP ${res.status}`,
    }
  }
  const hs300Raw = (data.hs300 as Record<string, unknown>) ?? {}
  const csi1000Raw = (data.csi1000 as Record<string, unknown>) ?? {}
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? date ?? ""),
    windowDays: (data.windowDays as number) ?? 5,
    hs300: {
      name: String(hs300Raw.name ?? "沪深300"),
      code: String(hs300Raw.code ?? "sh000300"),
      returnPct: (hs300Raw.returnPct as number | null) ?? null,
    },
    csi1000: {
      name: String(csi1000Raw.name ?? "中证1000"),
      code: String(csi1000Raw.code ?? "sh000852"),
      returnPct: (csi1000Raw.returnPct as number | null) ?? null,
    },
    spread: (data.spread as number | null) ?? null,
    score: (data.score as number) ?? undefined,
    rawValue: (data.rawValue as number) ?? undefined,
    elapsedMs: (data.elapsedMs as number) ?? undefined,
    source: (data.source as string) ?? undefined,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export interface StyleRiskAppetiteHistoryItem {
  tradeDate: string
  spread: number | null
  /** 0-100 历史分位情绪得分 */
  score?: number
  fromCache?: boolean
}

export interface StyleRiskAppetiteHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: StyleRiskAppetiteHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentStyleRiskAppetiteHistory(
  start: string,
  end: string,
): Promise<StyleRiskAppetiteHistoryResponse> {
  const params = new URLSearchParams({ start, end })
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/style-risk-appetite/history?${params.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      spread: (it.spread as number | null) ?? null,
      score: (it.score as number) ?? undefined,
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// 赚钱效应 (Profit Effect) — Market Sentiment 分项④
// ---------------------------------------------------------------------------
// score = 60% × 近5日上涨占比 + 40% × (100 - 60日新低占比)
// score ≥ 60 → 赚钱面宽 (积极), ≥ 40 → 中性, < 40 → 亏钱效应
// ---------------------------------------------------------------------------

export interface ProfitEffectResponse {
  ok: boolean
  tradeDate: string
  up5dPct: number | null
  newLow60dPct: number | null
  score: number | null
  /** 原始 score (百分位之前的 raw 合成值) */
  rawValue?: number
  elapsedMs?: number
  source?: string
  fromCache?: boolean
  error?: string
}

export async function fetchMarketSentimentProfitEffect(
  date?: string,
): Promise<ProfitEffectResponse> {
  const q = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/profit-effect${q}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false,
      tradeDate: date ?? "",
      up5dPct: null,
      newLow60dPct: null,
      score: null,
      error: `HTTP ${res.status}`,
    }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? date ?? ""),
    up5dPct: (data.up5dPct as number | null) ?? null,
    newLow60dPct: (data.newLow60dPct as number | null) ?? null,
    score: (data.score as number | null) ?? null,
    rawValue: (data.rawValue as number) ?? undefined,
    elapsedMs: (data.elapsedMs as number) ?? undefined,
    source: (data.source as string) ?? undefined,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export interface ProfitEffectHistoryItem {
  tradeDate: string
  score: number | null
  fromCache?: boolean
}

export interface ProfitEffectHistoryResponse {
  ok: boolean
  start: string
  end: string
  count: number
  items: ProfitEffectHistoryItem[]
  error?: string
}

export async function fetchMarketSentimentProfitEffectHistory(
  start: string,
  end: string,
): Promise<ProfitEffectHistoryResponse> {
  const params = new URLSearchParams({ start, end })
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/profit-effect/history?${params.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      score: (it.score as number | null) ?? null,
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

// ---------------------------------------------------------------------------
// Market Sentiment · 市场情绪指数 (9 张卡加权 composite, 顶部大卡)
// 数据源: duckdb.market_sentiment_index_daily (持久化, cache-aside)
//   公式: 15%×vol + 15%×turnover + 10%×priceStrength + 10%×riskAppetite
//       + 15%×breadth + 15%×limitEmotion + 10%×profitEffect
//       +  5%×sectorBreadth +  5%×styleRisk
// 归属: /market/sentiment 页面 (顶部 1 张大卡)
// ---------------------------------------------------------------------------
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

export interface MarketSentimentIndexWeights {
  vol: number
  turnover: number
  price_strength: number
  risk_appetite: number
  breadth: number
  limit_emotion: number
  profit_effect: number
  sector_breadth: number
  style_risk: number
}

export interface MarketSentimentIndexResponse {
  ok: boolean
  tradeDate: string
  /** 9 个 component score, 缺失为 null (calc 内部视为 50 中性) */
  components: MarketSentimentIndexComponents
  /** 9 个权重, 合计 1.0 */
  weights: MarketSentimentIndexWeights
  /** 0-100 合成得分 */
  compositeScore: number | null
  /** 实际有数据的 component 数 (1-9), 9 = 全部 sub-card 都有 */
  componentCount: number
  /** 等级: hot / active / normal / weak / ice */
  level: string
  elapsedMs: number | null
  source: string | null
  fromCache?: boolean
  error?: string
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

export async function fetchMarketSentimentIndex(
  date?: string,
): Promise<MarketSentimentIndexResponse> {
  const q = date ? `?date=${encodeURIComponent(date)}` : ""
  const res = await fetchWithRetry(`${MARKET_SENTIMENT_API_BASE}/index${q}`)
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return {
      ok: false,
      tradeDate: date ?? "",
      components: emptyMsiComponents(),
      weights: emptyMsiWeights(),
      compositeScore: null,
      componentCount: 0,
      level: "normal",
      elapsedMs: null,
      source: null,
      error: `HTTP ${res.status}`,
    }
  }
  return {
    ok: Boolean(data.ok),
    tradeDate: String(data.tradeDate ?? date ?? ""),
    components: parseMsiComponents(data.components),
    weights: parseMsiWeights(data.weights),
    compositeScore: (data.compositeScore as number | null) ?? null,
    componentCount: Number(data.componentCount ?? 0),
    level: String(data.level ?? "normal"),
    elapsedMs: (data.elapsedMs as number) ?? null,
    source: (data.source as string) ?? null,
    fromCache: Boolean(data.fromCache),
    error: (data.error as string) ?? undefined,
  }
}

export async function fetchMarketSentimentIndexHistory(
  start: string,
  end: string,
): Promise<MarketSentimentIndexHistoryResponse> {
  const params = new URLSearchParams({ start, end })
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/index/history?${params.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start, end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: (data.start as string) ?? start,
    end: (data.end as string) ?? end,
    count: (data.count as number) ?? raw.length,
    items: raw.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      compositeScore: (it.compositeScore as number | null) ?? null,
      level: String(it.level ?? "normal"),
      componentCount: Number(it.componentCount ?? 0),
      components: parseMsiComponents(it.components),
      fromCache: Boolean(it.fromCache),
    })),
    error: (data.error as string) ?? undefined,
  }
}

function emptyMsiComponents(): MarketSentimentIndexComponents {
  return {
    vol: null, turnover: null, price_strength: null, risk_appetite: null,
    breadth: null, limit_emotion: null, profit_effect: null,
    sector_breadth: null, style_risk: null,
  }
}

function emptyMsiWeights(): MarketSentimentIndexWeights {
  return {
    vol: 0.15, turnover: 0.15, price_strength: 0.10, risk_appetite: 0.10,
    breadth: 0.15, limit_emotion: 0.15, profit_effect: 0.10,
    sector_breadth: 0.05, style_risk: 0.05,
  }
}

function parseMsiComponents(raw: unknown): MarketSentimentIndexComponents {
  const c = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>
  return {
    vol: (c.vol as number | null) ?? null,
    turnover: (c.turnover as number | null) ?? null,
    price_strength: (c.price_strength as number | null) ?? (c.priceStrength as number | null) ?? null,
    risk_appetite: (c.risk_appetite as number | null) ?? (c.riskAppetite as number | null) ?? null,
    breadth: (c.breadth as number | null) ?? null,
    limit_emotion: (c.limit_emotion as number | null) ?? (c.limitEmotion as number | null) ?? null,
    profit_effect: (c.profit_effect as number | null) ?? (c.profitEffect as number | null) ?? null,
    sector_breadth: (c.sector_breadth as number | null) ?? (c.sectorBreadth as number | null) ?? null,
    style_risk: (c.style_risk as number | null) ?? (c.styleRisk as number | null) ?? null,
  }
}

function parseMsiWeights(raw: unknown): MarketSentimentIndexWeights {
  const w = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>
  return {
    vol: Number(w.vol ?? 0.15),
    turnover: Number(w.turnover ?? 0.15),
    price_strength: Number(w.price_strength ?? w.priceStrength ?? 0.10),
    risk_appetite: Number(w.risk_appetite ?? w.riskAppetite ?? 0.10),
    breadth: Number(w.breadth ?? 0.15),
    limit_emotion: Number(w.limit_emotion ?? w.limitEmotion ?? 0.15),
    profit_effect: Number(w.profit_effect ?? w.profitEffect ?? 0.10),
    sector_breadth: Number(w.sector_breadth ?? w.sectorBreadth ?? 0.05),
    style_risk: Number(w.style_risk ?? w.styleRisk ?? 0.05),
  }
}

// ---------------------------------------------------------------------------
// 三大指数 1m K (Market Pulse 顶部 3 张指数卡联动)
// ---------------------------------------------------------------------------
export interface IndexKlinePoint {
  /** "2026-06-12 09:31:00" */
  time: string
  /** 毫秒时间戳 (前端直接喂给 ChartPanel) */
  timestamp: number
  open: number | null
  high: number | null
  low: number | null
  close: number | null
  /** 手 (1手=100股), klinecharts volumePrecision=0 */
  volume: number | null
  /** 元 (1手=100元) */
  turnover: number | null
}

export interface IndexKlineItem {
  ok: boolean
  /** 6 位纯数字, 例 "000001" (跟现有 target_type=index 约定一致) */
  code: string
  /** 中文名, 例 "上证指数" */
  name: string
  /** 交易日 YYYY-MM-DD */
  date: string
  interval: "1m"
  /** 上一交易日 1d K 的收盘 (用于卡片上"昨收"标签), 可能为 null (拉不到 1d 时) */
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
  error?: string
}

export async function fetchIndexKlineBatch(params: {
  codes?: string[]
  date: string
  interval?: "1m"
}): Promise<IndexKlineBatchResponse> {
  return fetchMarketPulseIndexKlineBatch({
    codes: params.codes ?? ["000001", "399001", "399006"],
    date: params.date,
    interval: params.interval,
  }) as Promise<IndexKlineBatchResponse>
}

export interface IndexDailyItem {
  tradeDate: string
  close: number
  /** 成交额 (元). 后端 duckdb.index_daily_raw.amount 原值, 未做单位换算. */
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

/**
 * 拉单只宽基指数日线历史 (Market Sentiment 顶卡 / POC 叠加用).
 * 数据源: /api/market/sentiment/index/daily -> clickhouse.daily_raw.
 */
export async function fetchIndexDailyHistory(params: {
  code: string
  start: string  // YYYY-MM-DD
  end: string    // YYYY-MM-DD
}): Promise<IndexDailyResponse> {
  const query = new URLSearchParams({
    code: params.code,
    start: params.start,
    end: params.end,
  })
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/index/daily?${query.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    throw new Error((data?.error as string) || `获取指数日线失败: ${res.status}`)
  }
  const rawItems = Array.isArray(data.items)
    ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    code: String(data.code ?? params.code),
    name: String(data.name ?? params.code),
    start: String(data.start ?? params.start),
    end: String(data.end ?? params.end),
    count: typeof data.count === "number" ? data.count : rawItems.length,
    items: rawItems.map((it) => ({
      tradeDate: String(it.tradeDate ?? ""),
      close: typeof it.close === "number" ? it.close : Number(it.close ?? 0),
      amount: typeof it.amount === "number" ? it.amount : Number(it.amount ?? 0),
    })),
    error: typeof data.error === "string" ? (data.error as string) : undefined,
  }
}

export async function triggerMarketOverviewAkshareRefresh(): Promise<{
  ok: boolean
  snapshot?: MarketOverview
  error?: string
}> {
  return triggerMarketOverviewAkshareRefreshFromPulse()
}

/** market_overview_daily 单日条目 (POC 等历史/对齐场景用).
 *  与 fetchMarketOverviewAkshare 同 shape 的核心字段, 加 tradeDate + fromCache.
 *  单位: totalAmount / mainNetInflow / 其余资金流字段一律 "亿元".
 */
export interface MarketOverviewHistoryItem {
  tradeDate: string
  totalAmount: number | null      // 亿
  totalVolume: number | null      // 万手
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  stockCount: number | null
  mainNetInflow: number | null    // 亿 (主力 = 超大 + 大)
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

const _parseNullableNumber = (v: unknown): number | null => {
  if (v == null) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

const _parseNullableInt = (v: unknown): number | null => {
  if (v == null) return null
  const n = Number(v)
  return Number.isFinite(n) ? Math.round(n) : null
}

const _parseDate = (it: Record<string, unknown>): string =>
  String(it.tradeDate ?? it.date ?? "")

/** 拉大盘概况历史区间 (大盘成交额 + 主力净流入 + 涨跌家数等).
 *  数据源: backend.api.stock_chart.stock_chart_market_overview_history → duckdb.market_overview_daily.
 *  返回 items 已是升序, 字段单位统一为 "亿" (资金流/成交额).
 */
export async function fetchMarketOverviewHistory(params: {
  start: string
  end: string
}): Promise<MarketOverviewHistoryResponse> {
  const query = new URLSearchParams({ start: params.start, end: params.end })
  const res = await fetchWithRetry(
    `${MARKET_SENTIMENT_API_BASE}/market-overview/history?${query.toString()}`,
  )
  const data = (await readPayload<Record<string, unknown>>(res, "请求失败").catch(() => null)) as Record<string, unknown> | null
  if (!res.ok || !data) {
    return { ok: false, start: params.start, end: params.end, count: 0, items: [], error: `HTTP ${res.status}` }
  }
  const raw = Array.isArray(data.items) ? (data.items as Array<Record<string, unknown>>) : []
  return {
    ok: Boolean(data.ok),
    start: String(data.start ?? params.start),
    end: String(data.end ?? params.end),
    count: typeof data.count === "number" ? data.count : raw.length,
    items: raw.map((it) => ({
      tradeDate: _parseDate(it),
      totalAmount: _parseNullableNumber(it.totalAmount),
      totalVolume: _parseNullableNumber(it.totalVolume),
      risingCount: _parseNullableInt(it.risingCount),
      fallingCount: _parseNullableInt(it.fallingCount),
      flatCount: _parseNullableInt(it.flatCount),
      limitUpCount: _parseNullableInt(it.limitUpCount),
      limitDownCount: _parseNullableInt(it.limitDownCount),
      stockCount: _parseNullableInt(it.stockCount),
      mainNetInflow: _parseNullableNumber(it.mainNetInflow),
      superLargeNetInflow: _parseNullableNumber(it.superLargeNetInflow),
      largeNetInflow: _parseNullableNumber(it.largeNetInflow),
      mediumNetInflow: _parseNullableNumber(it.mediumNetInflow),
      smallNetInflow: _parseNullableNumber(it.smallNetInflow),
      mainNetInflowRatio: _parseNullableNumber(it.mainNetInflowRatio),
      source: typeof it.source === "string" ? it.source : undefined,
      fromCache: Boolean(it.fromCache),
    })),
    error: typeof data.error === "string" ? (data.error as string) : undefined,
  }
}




