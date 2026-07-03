import { LEGACY_API_BASE, fetchWithRetry, readLegacyJson } from "@/services/legacy/shared"

export async function fetchMarketOverview(): Promise<Record<string, unknown>> {
  const res = await fetchWithRetry(`${LEGACY_API_BASE}/stock-chart/market-overview`)
  return readLegacyJson<Record<string, unknown>>(res, "获取市场概览失败")
}
