import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioReviewAnchor } from "./types"

const BASE = `${SERVICE_PREFIX.market}/portfolio/review-anchors`

export async function fetchPortfolioReviewAnchors(symbol: string, tradeDate?: string): Promise<PortfolioReviewAnchor[]> {
  const query = new URLSearchParams({ symbol })
  if (tradeDate) query.set("trade_date", tradeDate)
  return cynexusResult<PortfolioReviewAnchor[]>(await fetchWithRetry(`${BASE}?${query.toString()}`, { cache: "no-store" }))
}

export async function savePortfolioReviewAnchor(anchor: PortfolioReviewAnchor): Promise<PortfolioReviewAnchor> {
  return cynexusResult<PortfolioReviewAnchor>(await fetchWithRetry(BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(anchor),
  }))
}

export async function deletePortfolioReviewAnchor(id: number): Promise<void> {
  await cynexusResult(await fetchWithRetry(`${BASE}/${encodeURIComponent(String(id))}`, { method: "DELETE" }))
}
