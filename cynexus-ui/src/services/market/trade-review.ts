import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"

export interface LocalMinuteLoadResult {
  accepted?: boolean
  status?: string
  runId?: string
  message?: string
}

export interface TradeReviewMaterializationStatus {
  tradeDate?: string
  dataStatus?: "ready" | "partial" | "unavailable" | string
  manifest?: Record<string, unknown>
  missingReasons?: string[]
  source?: string
}

export async function triggerLocalTdxMinuteLoad(): Promise<LocalMinuteLoadResult> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/scheduler/data-jobs/sync_kline_minute/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      triggeredBy: "trade-review-local-1m",
      dryRun: false,
      params: {
        providers: ["tdx"],
        // The executor mounts the complete local TDX installation at this path.
        // Loading is incremental, so it only imports dates newer than effective coverage.
        tdx_root: "/app/tdx",
        create_table: true,
        incremental: true,
      },
    }),
  })
  return cynexusResult<LocalMinuteLoadResult>(response)
}

export async function triggerTradeReviewTimeshareLoad(params: {
  tradeDate: string
  symbol?: string
  instrumentType?: "STOCK" | "ETF"
}): Promise<LocalMinuteLoadResult> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/scheduler/data-jobs/sync_trade_review_timeshare/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      triggeredBy: "trade-review-timeshare-reload",
      dryRun: false,
      params: {
        trade_date: params.tradeDate,
        codes: params.instrumentType === "ETF" ? undefined : params.symbol,
        etf_codes: params.instrumentType === "ETF" ? params.symbol : undefined,
        redis_cache: true,
        ignore_errors: true,
      },
    }),
  })
  return cynexusResult<LocalMinuteLoadResult>(response)
}

/** Lightweight manifest read; intentionally does not fetch the replay bundle. */
export async function fetchTradeReviewMaterializationStatus(tradeDate: string): Promise<TradeReviewMaterializationStatus> {
  const query = new URLSearchParams({ tradeDate })
  const response = await fetchWithRetry(`${SERVICE_PREFIX.market}/trade-review/market-state/status?${query.toString()}`, {
    cache: "no-store",
  })
  return cynexusResult<TradeReviewMaterializationStatus>(response)
}

export async function triggerTradeReviewMarketStateMaterialization(tradeDate: string): Promise<LocalMinuteLoadResult> {
  const response = await fetchWithRetry(`${SERVICE_PREFIX.core}/scheduler/data-jobs/build_trade_review_market_state/trigger`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      triggeredBy: "trade-review-manual-materialization",
      dryRun: false,
      params: {
        trade_date: tradeDate,
        window_days: 60,
        create_table: true,
        redis_cache: true,
        redis_ttl_seconds: 604800,
      },
    }),
  })
  return cynexusResult<LocalMinuteLoadResult>(response)
}
