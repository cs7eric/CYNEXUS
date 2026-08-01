import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioOverview, PortfolioTrade } from "./types"

const BASE = `${SERVICE_PREFIX.market}/portfolio`

export async function fetchPortfolioOverview(): Promise<PortfolioOverview> {
  const response = await fetchWithRetry(`${BASE}/overview`, { cache: "no-store" })
  return cynexusResult<PortfolioOverview>(response)
}

export async function fetchPortfolioTrades(symbol?: string): Promise<PortfolioTrade[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : ""
  const response = await fetchWithRetry(`${BASE}/trades${query}`, { cache: "no-store" })
  return cynexusResult<PortfolioTrade[]>(response)
}
