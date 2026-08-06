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

export interface PortfolioImportTaskPage {
  items: PortfolioImportTask[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
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

export interface PortfolioImportProcessHistoryEvent {
  id: number
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

export interface PortfolioImportProcessHistory {
  id: number
  taskId: number
  createdAt?: string | null
  updatedAt?: string | null
  events: PortfolioImportProcessHistoryEvent[]
}

export interface PortfolioPosition {
  symbol: string
  securityName?: string | null
  instrumentType: "STOCK" | "ETF"
  quantity: number
  averageCost: number
  costBasis: number
  realizedPnl: number
  latestPrice?: number | null
  marketValue: number
  unrealizedPnl: number
  todayPnl?: number | null
  unrealizedReturnRate: number
}

export interface PortfolioOverview {
  positions: PortfolioPosition[]
  dataQualityWarnings?: PortfolioLedgerWarning[]
  costBasis?: number | null
  marketValue?: number | null
  availableCash?: number | null
  totalAssets?: number | null
  accountUpdatedAt?: string | null
  realizedPnl: number
  unrealizedPnl: number
  totalPnl: number
  todayPnl?: number | null
  tradeCount: number
  valuationStatus: string
}

export type PortfolioPerformanceRange = "daily" | "weekly" | "monthly" | "yearly" | "custom"

export interface PortfolioPerformancePoint {
  tradeDate: string
  marketValue: number
  totalAssets: number
  costBasis: number
  totalPnl: number
  periodPnl: number
  returnRate: number
}

export interface PortfolioPerformanceContribution {
  symbol: string
  securityName?: string | null
  instrumentType: "STOCK" | "ETF"
  periodPnl: number
}

export interface PortfolioPerformance {
  range: PortfolioPerformanceRange
  from: string
  to: string
  periodPnl: number
  todayPnl?: number | null
  periodReturnRate: number
  startAssets: number
  endAssets: number
  bestPeriodPnl: number
  worstPeriodPnl: number
  tradingDays: number
  points: PortfolioPerformancePoint[]
  contributions: PortfolioPerformanceContribution[]
}

export interface PortfolioAccount {
  totalAssets: number
  availableCash: number
  updatedAt?: string | null
}

export interface PortfolioLedgerWarning {
  code: string
  symbol: string
  unmatchedQuantity?: number | null
  message: string
}

export interface PortfolioTrade {
  id: number
  symbol: string
  securityName?: string | null
  instrumentType: "STOCK" | "ETF"
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
