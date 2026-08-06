interface IndustryHistoryCoveragePoint {
  constituentCount?: number | null
  matchedStockCount?: number | null
}

const MIN_INDUSTRY_HISTORY_COVERAGE = 0.5

function finite(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(value)
}

export function isUsableMarginHistoryPoint(point: IndustryHistoryCoveragePoint) {
  if (!finite(point.constituentCount) || !finite(point.matchedStockCount) || point.constituentCount <= 0) return true
  return point.matchedStockCount / point.constituentCount >= MIN_INDUSTRY_HISTORY_COVERAGE
}
