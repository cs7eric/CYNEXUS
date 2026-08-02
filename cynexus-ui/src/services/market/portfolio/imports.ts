import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { PortfolioCandidate, PortfolioImportProcessHistory, PortfolioImportReview, PortfolioImportTask } from "./types"

const BASE = `${SERVICE_PREFIX.market}/portfolio/imports`

export async function uploadPortfolioScreenshot(file: File, idempotencyKey: string): Promise<PortfolioImportTask> {
  const body = new FormData()
  body.append("file", file)
  const response = await fetchWithRetry(BASE, { method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body })
  return cynexusResult<PortfolioImportTask>(response)
}

export async function fetchPortfolioImport(taskId: number): Promise<PortfolioImportTask> {
  const response = await fetchWithRetry(`${BASE}/${taskId}`, { cache: "no-store" })
  return cynexusResult<PortfolioImportTask>(response)
}

export async function fetchPortfolioImportReview(taskId: number): Promise<PortfolioImportReview> {
  const response = await fetchWithRetry(`${BASE}/${taskId}/review`, { cache: "no-store" })
  return cynexusResult<PortfolioImportReview>(response)
}

export async function fetchPortfolioImportHistory(taskId: number): Promise<PortfolioImportProcessHistory> {
  const response = await fetchWithRetry(`${BASE}/${taskId}/history`, { cache: "no-store" })
  return cynexusResult<PortfolioImportProcessHistory>(response)
}

export interface PortfolioCandidateEdit {
  version: number
  symbol: string
  securityName?: string
  side: "BUY" | "SELL"
  tradedAt: string
  quantity: string
  price: string
  totalFee?: string
  externalTradeNo?: string
}

export async function updatePortfolioCandidate(taskId: number, candidateId: number, edit: PortfolioCandidateEdit): Promise<PortfolioCandidate> {
  const response = await fetchWithRetry(`${BASE}/${taskId}/candidates/${candidateId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(edit),
  })
  return cynexusResult<PortfolioCandidate>(response)
}

export async function confirmPortfolioImport(taskId: number, version: number, candidateIds: number[]): Promise<PortfolioImportTask> {
  const response = await fetchWithRetry(`${BASE}/${taskId}/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ version, candidateIds }),
  })
  return cynexusResult<PortfolioImportTask>(response)
}
