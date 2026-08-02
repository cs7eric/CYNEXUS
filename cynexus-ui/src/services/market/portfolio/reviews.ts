import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioReview } from "./types"

const BASE = `${SERVICE_PREFIX.market}/portfolio/reviews`

export async function fetchPortfolioReviews(symbol?: string): Promise<PortfolioReview[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : ""
  const response = await fetchWithRetry(`${BASE}${query}`, { cache: "no-store" })
  return cynexusResult<PortfolioReview[]>(response)
}

export async function fetchPortfolioReview(reviewId: number): Promise<PortfolioReview> {
  const response = await fetchWithRetry(`${BASE}/${reviewId}`, { cache: "no-store" })
  return cynexusResult<PortfolioReview>(response)
}

export async function savePortfolioReview(review: PortfolioReview): Promise<PortfolioReview> {
  const response = await fetchWithRetry(BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(review),
  })
  return cynexusResult<PortfolioReview>(response)
}
