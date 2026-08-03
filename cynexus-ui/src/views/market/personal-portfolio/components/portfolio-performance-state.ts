import { useCallback, useEffect, useState } from "react"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

export interface PortfolioPerformanceState {
  range: PortfolioPerformanceRange
  setRange: (range: PortfolioPerformanceRange) => void
  performance: PortfolioPerformance | null
  loading: boolean
  error: string | null
}

export function usePortfolioPerformance(): PortfolioPerformanceState {
  const [range, setRange] = useState<PortfolioPerformanceRange>("daily")
  const [performance, setPerformance] = useState<PortfolioPerformance | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPerformance = useCallback(async (cancelledRef: { current: boolean }) => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchPortfolioPerformance(range)
      if (!cancelledRef.current) setPerformance(result)
    } catch (cause) {
      if (!cancelledRef.current) setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      if (!cancelledRef.current) setLoading(false)
    }
  }, [range])

  useEffect(() => {
    const cancelledRef = { current: false }
    const timer = window.setTimeout(() => { void loadPerformance(cancelledRef) }, 0)
    return () => { cancelledRef.current = true; window.clearTimeout(timer) }
  }, [loadPerformance])

  return { range, setRange, performance, loading, error }
}
