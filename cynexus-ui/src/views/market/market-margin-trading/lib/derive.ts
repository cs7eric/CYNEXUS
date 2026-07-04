import type { MarginMarketSummary } from "@/services/market/margin"

export interface MarginTrendPoint {
  date: string
  cn: number | null
  sse: number | null
  szse: number | null
  buyCn: number | null
}

export function buildTrend(items: MarginMarketSummary[]): MarginTrendPoint[] {
  const byDate = new Map<string, MarginTrendPoint>()
  for (const item of items) {
    const current =
      byDate.get(item.tradeDate) ??
      ({
        date: item.tradeDate,
        cn: null,
        sse: null,
        szse: null,
        buyCn: null,
      } satisfies MarginTrendPoint)
    const exchange = item.exchange?.toUpperCase()
    if (exchange === "CN") {
      current.cn = item.marginBalance
      current.buyCn = item.financingBuyAmount
    } else if (exchange === "SSE") {
      current.sse = item.marginBalance
    } else if (exchange === "SZSE") {
      current.szse = item.marginBalance
    }
    byDate.set(item.tradeDate, current)
  }
  return [...byDate.values()].sort((left, right) => left.date.localeCompare(right.date))
}

export function summaryByExchange(items: MarginMarketSummary[]) {
  return new Map(items.map((item) => [item.exchange?.toUpperCase(), item]))
}

export function latestNonNull<T>(items: T[], value: (item: T) => number | null | undefined) {
  for (let index = items.length - 1; index >= 0; index -= 1) {
    const next = value(items[index])
    if (next != null && Number.isFinite(next)) return next
  }
  return null
}

export function changeFromPrevious(points: MarginTrendPoint[], key: keyof Pick<MarginTrendPoint, "cn" | "sse" | "szse">) {
  const values = points.map((item) => item[key]).filter((item): item is number => item != null && Number.isFinite(item))
  if (values.length < 2) return null
  return values[values.length - 1] - values[values.length - 2]
}
