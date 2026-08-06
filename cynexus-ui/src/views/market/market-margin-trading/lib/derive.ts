import type { MarginMarketSummary } from "@/services/market/margin"

export interface MarginTrendPoint {
  date: string
  cn: number | null
  sse: number | null
  szse: number | null
  buyCn: number | null
  financingBalanceCn: number | null
  securitiesLendingBalanceAmountCn: number | null
}

export type MarginTrendValueKey = keyof Pick<
  MarginTrendPoint,
  "cn" | "sse" | "szse" | "buyCn" | "financingBalanceCn" | "securitiesLendingBalanceAmountCn"
>

const sum = (...values: Array<number | null | undefined>) => {
  const present = values.filter((value): value is number => value != null && Number.isFinite(value))
  return present.length ? present.reduce((total, value) => total + value, 0) : null
}

function aggregateExchangeSummary(sse: MarginMarketSummary, szse: MarginMarketSummary): MarginMarketSummary {
  const updatedAt = [sse.updatedAt, szse.updatedAt].filter(Boolean).sort().at(-1) ?? null
  return {
    tradeDate: sse.tradeDate,
    exchange: "CN",
    source: "derived.exchange_sum",
    financingBalance: sum(sse.financingBalance, szse.financingBalance),
    financingBuyAmount: sum(sse.financingBuyAmount, szse.financingBuyAmount),
    financingRepayAmount: sum(sse.financingRepayAmount, szse.financingRepayAmount),
    securitiesLendingBalance: sum(sse.securitiesLendingBalance, szse.securitiesLendingBalance),
    securitiesLendingBalanceAmount: sum(sse.securitiesLendingBalanceAmount, szse.securitiesLendingBalanceAmount),
    securitiesLendingSellVolume: sum(sse.securitiesLendingSellVolume, szse.securitiesLendingSellVolume),
    securitiesLendingRepayVolume: sum(sse.securitiesLendingRepayVolume, szse.securitiesLendingRepayVolume),
    marginBalance: sum(sse.marginBalance, szse.marginBalance),
    extra: JSON.stringify({ derived: true, sources: [sse.source, szse.source] }),
    updatedAt,
  }
}

export function withMarketAggregate(items: MarginMarketSummary[]): MarginMarketSummary[] {
  const byDate = new Map<string, MarginMarketSummary[]>()
  for (const item of items) {
    const rows = byDate.get(item.tradeDate) ?? []
    rows.push(item)
    byDate.set(item.tradeDate, rows)
  }

  return [...byDate.values()]
    .flatMap((rows) => {
      const hasCn = rows.some((row) => row.exchange?.toUpperCase() === "CN")
      const sse = rows.find((row) => row.exchange?.toUpperCase() === "SSE")
      const szse = rows.find((row) => row.exchange?.toUpperCase() === "SZSE")
      return !hasCn && sse && szse ? [...rows, aggregateExchangeSummary(sse, szse)] : rows
    })
    .sort((left, right) => left.tradeDate.localeCompare(right.tradeDate) || left.exchange.localeCompare(right.exchange))
}

export function buildTrend(items: MarginMarketSummary[]): MarginTrendPoint[] {
  const byDate = new Map<string, MarginTrendPoint>()
  for (const item of withMarketAggregate(items)) {
    const current =
      byDate.get(item.tradeDate) ??
      ({
        date: item.tradeDate,
        cn: null,
        sse: null,
        szse: null,
        buyCn: null,
        financingBalanceCn: null,
        securitiesLendingBalanceAmountCn: null,
      } satisfies MarginTrendPoint)
    const exchange = item.exchange?.toUpperCase()
    if (exchange === "CN") {
      current.cn = item.marginBalance
      current.buyCn = item.financingBuyAmount
      current.financingBalanceCn = item.financingBalance
      current.securitiesLendingBalanceAmountCn = item.securitiesLendingBalanceAmount
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

export function changeFromPrevious(points: MarginTrendPoint[], key: MarginTrendValueKey) {
  return changeFromDaysAgo(points, key, 1)?.value ?? null
}

export function changeFromDaysAgo(
  points: MarginTrendPoint[],
  key: MarginTrendValueKey,
  daysAgo: number,
) {
  const values = points
    .map((item) => item[key])
    .filter((value): value is number => value != null && Number.isFinite(value))
  if (daysAgo < 1 || values.length <= daysAgo) return null

  const latest = values.at(-1)
  const previous = values.at(-(daysAgo + 1))
  if (latest == null || previous == null) return null
  return {
    value: latest - previous,
    percent: previous === 0 ? null : ((latest - previous) / Math.abs(previous)) * 100,
  }
}
