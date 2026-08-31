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
  suggestedSymbol?: string | null
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
  strategy?: string | null
  tags?: string | null
  industryCode?: string | null
  industryName?: string | null
}
export interface PortfolioCashFlow { id?: number; occurredAt: string; amount: number; flowType: "DEPOSIT" | "WITHDRAWAL"; note?: string | null }
export interface PortfolioAccountSnapshot { id?: number; snapshotDate: string; totalAssets: number; availableCash: number; marketValue: number; netDeposit: number; netWithdrawal: number; dailyPnl?: number | null; dailyReturn?: number | null }

export interface PortfolioTradeRound {
  roundId: number
  symbol: string
  securityName?: string | null
  instrumentType: "STOCK" | "ETF"
  openedAt: string
  closedAt?: string | null
  entryQuantity: number
  exitQuantity: number
  averageEntryPrice: number
  averageExitPrice?: number | null
  grossPnl: number
  totalFee: number
  netPnl: number
  returnRate: number
  holdingDays: number
  status: "OPEN" | "CLOSED"
  unmatchedQuantity: number
  sourceTradeIds: number[]
}

export interface PortfolioTradeAnalyticsWarning {
  code: string
  symbol: string
  quantity?: number | null
  message: string
}

export interface PortfolioTradeAnalytics {
  from: string
  to: string
  completedCount: number
  openCount: number
  winCount: number
  lossCount: number
  breakevenCount: number
  winRate: number
  averageWin: number
  averageLoss: number
  payoffRatio: number
  profitFactor: number
  expectancy: number
  netPnl: number
  maxWin: number
  maxLoss: number
  maxConsecutiveWins: number
  maxConsecutiveLosses: number
  averageHoldingDays: number
  medianHoldingDays: number
  feeRatio: number
  maxDrawdown: number
  rounds: PortfolioTradeRound[]
  equityCurve: Array<{ date: string; cumulativePnl: number; drawdown: number }>
  dataQualityWarnings: PortfolioTradeAnalyticsWarning[]
  timeWeightedReturn?: number | null
  moneyWeightedReturn?: number | null
  accountMaxDrawdown?: number | null
  accountMaxDrawdownRate?: number | null
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

export interface PortfolioReviewAnchor {
  id?: number
  symbol: string
  instrumentType: "STOCK" | "ETF"
  tradeId?: number | null
  reviewId?: number | null
  anchorType: "TRADE" | "OBSERVATION"
  side: "BUY" | "SELL" | "OBSERVATION" | "EVENT"
  occurredAt: string
  price: number
  note?: string | null
  tags?: string | null
}
