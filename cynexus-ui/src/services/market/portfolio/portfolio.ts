import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioAccount, PortfolioOverview, PortfolioPerformance, PortfolioPerformanceRange, PortfolioTrade } from "./types"

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

export interface PortfolioTradeUpdate {
  symbol: string
  securityName?: string
  instrumentType: "STOCK" | "ETF"
  side: "BUY" | "SELL"
  tradedAt: string
  quantity: string
  price: string
  totalFee?: string
}

export async function updatePortfolioTrade(tradeId: number, update: PortfolioTradeUpdate): Promise<PortfolioTrade> {
  const response = await fetchWithRetry(`${BASE}/trades/${tradeId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(update),
  })
  return cynexusResult<PortfolioTrade>(response)
}

export async function fetchPortfolioAccount(): Promise<PortfolioAccount> {
  const response = await fetchWithRetry(`${BASE}/account`, { cache: "no-store" })
  return cynexusResult<PortfolioAccount>(response)
}

export async function updatePortfolioAccount(totalAssets: number): Promise<PortfolioAccount> {
  const response = await fetchWithRetry(`${BASE}/account`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ totalAssets }),
  })
  return cynexusResult<PortfolioAccount>(response)
}

export async function fetchPortfolioPerformance(range: PortfolioPerformanceRange, from?: string, to?: string): Promise<PortfolioPerformance> {
  const params = new URLSearchParams({ range })
  if (from) params.set("from", from)
  if (to) params.set("to", to)
  const response = await fetchWithRetry(`${BASE}/performance?${params.toString()}`, { cache: "no-store" })
  return cynexusResult<PortfolioPerformance>(response)
}
