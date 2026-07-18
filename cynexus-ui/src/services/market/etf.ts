import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

const ETF_API_BASE = `${SERVICE_PREFIX.market}/etf`

export interface MarketEtfSummary {
  totalEtfs: number | null
  broadCount: number | null
  sectorCount: number | null
  totalAmount: number | null
  activeVolume: number | null
  broadShareDelta: number | null
  sectorShareDelta: number | null
  broadNetSubscriptionProxy: number | null
  sectorNetSubscriptionProxy: number | null
  averagePremiumPct: number | null
  classificationAuthorityCount: number | null
  classificationFallbackCount: number | null
  classificationCoveragePct: number | null
  direction: string | null
  directionText: string | null
}

export interface MarketEtfBreadth {
  tradeDate: string | null
  stockCount: number | null
  risingCount: number | null
  fallingCount: number | null
  flatCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  advancingPct: number | null
  source: string | null
}

export interface MarketEtfTrend {
  tradeDate: string
  broadShareDelta: number | null
  sectorShareDelta: number | null
  broadNetSubscriptionProxy: number | null
  sectorNetSubscriptionProxy: number | null
  amount: number | null
  averagePremiumPct: number | null
  advancingPct: number | null
}

export interface MarketEtfRow {
  code: string
  exchange: string | null
  name: string | null
  theme: string | null
  price: number | null
  changePct: number | null
  amount: number | null
  volume: number | null
  iopv: number | null
  premiumPct: number | null
  turnoverPct: number | null
  latestShares: number | null
  shareDelta: number | null
  netSubscriptionProxy: number | null
  cumulativeShareDelta: number | null
  cumulativeNetSubscriptionProxy: number | null
  insideVolume: number | null
  outsideVolume: number | null
  superLargeNetInflow: number | null
  superLargeNetRatio: number | null
  mainNetInflow: number | null
  mainNetRatio: number | null
  largeNetInflow: number | null
  largeNetRatio: number | null
  mediumNetInflow: number | null
  mediumNetRatio: number | null
  smallNetInflow: number | null
  smallNetRatio: number | null
}

export interface MarketEtfRotation {
  theme: string
  etfCount: number | null
  amount: number | null
  shareDelta: number | null
  netSubscriptionProxy: number | null
  averageChangePct: number | null
  leaderCode: string | null
  leaderName: string | null
}

export interface MarketEtfMappingDimension {
  group: string
  etfCount: number | null
  relationshipCount: number | null
}

export interface MarketEtfMappingTag {
  group: string
  code: string
  name: string
  etfCount: number | null
  relationshipCount: number | null
}

export interface MarketEtfMappingSummary {
  catalogEtfCount: number | null
  mappedEtfCount: number | null
  unmappedEtfCount: number | null
  relationshipCount: number | null
  source: string | null
  dimensions: MarketEtfMappingDimension[]
  tags: MarketEtfMappingTag[]
}

export interface MarketEtfResponse {
  ok: boolean
  error?: string | null
  tradeDate: string | null
  fetchedAt: string | null
  source: string | null
  summary: MarketEtfSummary | null
  breadth: MarketEtfBreadth | null
  history: MarketEtfTrend[]
  broad: MarketEtfRow[]
  sectorRows: MarketEtfRow[]
  rotations: MarketEtfRotation[]
  mapping: MarketEtfMappingSummary | null
}

export async function fetchMarketEtf(params: { historyDays?: number; rowLimit?: number } = {}) {
  const query = new URLSearchParams({
    historyDays: String(params.historyDays ?? 10),
    rowLimit: String(params.rowLimit ?? 180),
  })
  const response = await fetchWithRetry(`${ETF_API_BASE}?${query.toString()}`, { cache: "no-store" })
  return cynexusResult<MarketEtfResponse>(response)
}
