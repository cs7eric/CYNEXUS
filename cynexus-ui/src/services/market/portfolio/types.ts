export interface PortfolioImportTask {
  id: number
  status: string
  version: number
  attemptCount: number
  assetId: number
  queuedAt?: string | null
  startedAt?: string | null
  completedAt?: string | null
  confirmedAt?: string | null
  lastErrorCode?: string | null
  lastErrorMessage?: string | null
}

export interface PortfolioCandidate {
  id: number
  rowNo: number
  normalizedPayload: string
  reviewStatus: string
  version: number
  confidence?: number | null
  warnings?: string[]
}

export interface PortfolioImportReview {
  task: PortfolioImportTask
  candidates: PortfolioCandidate[]
}

export interface PortfolioImportProcessHistory {
  id: number
  taskId: number
  attemptNo: number
  eventType: string
  fromStatus?: string | null
  toStatus?: string | null
  messageId?: string | null
  correlationId?: string | null
  providerCode?: string | null
  modelCode?: string | null
  errorCode?: string | null
  errorMessage?: string | null
  details?: string | null
  occurredAt: string
  durationMs?: number | null
}

export interface PortfolioPosition {
  symbol: string
  quantity: number
  averageCost: number
  costBasis: number
  realizedPnl: number
  latestPrice?: number | null
  marketValue: number
  unrealizedPnl: number
  unrealizedReturnRate: number
}

export interface PortfolioOverview {
  positions: PortfolioPosition[]
  realizedPnl: number
  unrealizedPnl: number
  totalPnl: number
  tradeCount: number
  valuationStatus: string
}

export interface PortfolioTrade {
  id: number
  symbol: string
  securityName?: string | null
  side: "BUY" | "SELL"
  tradedAt: string
  quantity: number
  price: number
  totalFee: number
  status: string
}

export interface PortfolioReview {
  id?: number
  symbol: string
  periodStart: string
  periodEnd: string
  thesis?: string | null
  plan?: string | null
  execution?: string | null
  lessons?: string | null
  nextAction?: string | null
  tags?: string | null
  rating?: number | null
  metricsSnapshot?: string | null
  status?: string | null
}
