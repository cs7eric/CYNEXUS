import { cynexusResult } from "@/services/common/cynexus-result"
import { fetchWithRetry } from "@/services/common/http"
import { SERVICE_PREFIX } from "@/services/common/service-prefix"
import type { FuturesPositionHistory, FuturesPositionInstrument, FuturesPositionMember } from "@/services/market/futures-position"

const FUTURES_POSITION_AI_API = `${SERVICE_PREFIX.ai}/futures-position`

export const FUTURES_POSITION_AI_VARIETIES = ["IF", "IH", "IM", "IC"] as const
export type FuturesPositionAiVariety = (typeof FUTURES_POSITION_AI_VARIETIES)[number]

export interface FuturesPositionAiResult {
  headline?: string
  structure?: string
  evidence?: string[]
  trend?: string
  risk?: string
  watch?: string[]
  data_quality?: string
  disclaimer?: string
}

export interface FuturesPositionAiResponse {
  ok: boolean
  variety_code: FuturesPositionAiVariety
  instrument_code: string
  trade_date: string
  analysis_result: FuturesPositionAiResult
  provider_code?: string | null
  model_code?: string | null
  prompt_version?: string | null
}

export function isFuturesPositionAiVariety(value?: string | null): value is FuturesPositionAiVariety {
  return Boolean(value && FUTURES_POSITION_AI_VARIETIES.includes(value.toUpperCase() as FuturesPositionAiVariety))
}

export async function analyzeFuturesPosition(params: {
  row: FuturesPositionInstrument
  history: FuturesPositionHistory[]
  members: FuturesPositionMember[]
  instrumentName: string
  top: number
  tradeDate?: string | null
}): Promise<FuturesPositionAiResponse> {
  const varietyCode = String(params.row.varietyCode || params.row.instrumentCode.slice(0, 2)).toUpperCase()
  if (!isFuturesPositionAiVariety(varietyCode)) {
    throw new Error("AI 结构分析仅支持 IF、IH、IM、IC")
  }
  const response = await fetchWithRetry(`${FUTURES_POSITION_AI_API}/analysis`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      variety_code: varietyCode,
      instrument_code: params.row.instrumentCode,
      instrument_name: params.instrumentName,
      exchange_code: params.row.exchangeCode,
      trade_date: params.tradeDate || undefined,
      top: params.top,
      current: params.row,
      history: params.history,
      members: params.members,
    }),
  })
  return cynexusResult<FuturesPositionAiResponse>(response)
}
