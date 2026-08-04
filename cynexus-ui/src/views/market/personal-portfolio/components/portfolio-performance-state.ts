import { useCallback, useEffect, useMemo, useState } from "react"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

export interface PortfolioPerformanceState {
  range: PortfolioPerformanceRange
  setRange: (range: PortfolioPerformanceRange) => void
  periodLabel: string | null
  periodFrom: string | null
  periodTo: string | null
  canGoPrevious: boolean
  canGoNext: boolean
  goPrevious: () => void
  goNext: () => void
  refresh: () => void
  performance: PortfolioPerformance | null
  loading: boolean
  error: string | null
}

export function usePortfolioPerformance(): PortfolioPerformanceState {
  const [range, setRange] = useState<PortfolioPerformanceRange>("daily")
  const [periodOffset, setPeriodOffset] = useState(0)
  const [performance, setPerformance] = useState<PortfolioPerformance | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshToken, setRefreshToken] = useState(0)
  const refresh = useCallback(() => setRefreshToken((value) => value + 1), [])

  const loadPerformance = useCallback(async (cancelledRef: { current: boolean }) => {
    setLoading(true)
    setError(null)
    try {
      const window = buildPeriodWindow(range, periodOffset)
      const result = await fetchPortfolioPerformance(range, window?.from, window?.to)
      if (!cancelledRef.current) setPerformance(result)
    } catch (cause) {
      if (!cancelledRef.current) setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      if (!cancelledRef.current) setLoading(false)
    }
  }, [periodOffset, range])

  useEffect(() => {
    const cancelledRef = { current: false }
    const timer = window.setTimeout(() => { void loadPerformance(cancelledRef) }, 0)
    return () => { cancelledRef.current = true; window.clearTimeout(timer) }
  }, [loadPerformance, refreshToken])

  const periodWindow = useMemo(() => buildPeriodWindow(range, periodOffset), [periodOffset, range])
  const updateRange = useCallback((nextRange: PortfolioPerformanceRange) => {
    setPeriodOffset(0)
    setRange(nextRange)
  }, [])

  return {
    range,
    setRange: updateRange,
    periodLabel: periodWindow?.label ?? null,
    periodFrom: periodWindow?.from ?? null,
    periodTo: periodWindow?.to ?? null,
    canGoPrevious: range !== "daily" && (!periodWindow || !performance || performance.from <= periodWindow.from),
    canGoNext: range !== "daily" && periodOffset < 0,
    goPrevious: () => setPeriodOffset((value) => value - 1),
    goNext: () => setPeriodOffset((value) => Math.min(value + 1, 0)),
    refresh,
    performance,
    loading,
    error,
  }
}

function buildPeriodWindow(range: PortfolioPerformanceRange, offset: number) {
  if (range === "daily" || range === "custom") return null
  const today = new Date()
  if (range === "weekly") {
    const monthStart = shiftMonths(new Date(today.getFullYear(), today.getMonth(), 1), offset)
    const monthEnd = new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0)
    return {
      from: toDateString(monthStart),
      to: toDateString(monthEnd),
      label: `${monthStart.getFullYear()}年${String(monthStart.getMonth() + 1).padStart(2, "0")}月`,
    }
  }
  if (range === "monthly") {
    const year = today.getFullYear() + offset
    return {
      from: `${year}-01-01`,
      to: `${year}-12-31`,
      label: `${year}年`,
    }
  }
  const centerYear = today.getFullYear() + offset
  return {
    from: `${centerYear - 2}-01-01`,
    to: `${centerYear + 2}-12-31`,
    label: `${centerYear - 2}-${centerYear + 2}`,
  }
}

function shiftMonths(value: Date, amount: number) {
  return new Date(value.getFullYear(), value.getMonth() + amount, 1)
}

function toDateString(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
}
