import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioAccount, PortfolioOverview, PortfolioTrade } from "./types"

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

export async function fetchPortfolioAccount(): Promise<PortfolioAccount> {
  const response = await fetchWithRetry(`${BASE}/account`, { cache: "no-store" })
  return cynexusResult<PortfolioAccount>(response)
}

export async function updatePortfolioAccount(availableCash: number): Promise<PortfolioAccount> {
  const response = await fetchWithRetry(`${BASE}/account`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ availableCash }),
  })
  return cynexusResult<PortfolioAccount>(response)
}
