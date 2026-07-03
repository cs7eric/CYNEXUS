import { useEffect, useState } from "react"
import { RefreshCw } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { fetchStockFundFlow, type StockFundFlowResponse } from "@/services/market/stock-chart"
import type { StockTargetType } from "../../stock-chart/lib/types"

export interface FundFlowTabProps {
  targetType: StockTargetType
  symbol: string
  name: string
}

function formatWan(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "-"
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(2)} 亿`
  return `${value.toFixed(0)} 万`
}

function formatPct(value: number | null | undefined) {
  return value == null || !Number.isFinite(value) ? "-" : `${value.toFixed(2)}%`
}

function tone(value: number | null | undefined) {
  if (value == null) return "text-slate-500"
  return value >= 0 ? "text-red-600" : "text-emerald-600"
}

export function FundFlowTab({ symbol, name }: FundFlowTabProps) {
  const [data, setData] = useState<StockFundFlowResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const refresh = async () => {
    try {
      setLoading(true)
      setError("")
      setData(await fetchStockFundFlow(symbol))
    } catch (err) {
      setData(null)
      setError(err instanceof Error ? err.message : "加载资金流失败")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol])

  return (
    <Card className="min-w-0 border-white/70 bg-white/80 shadow-[0_20px_70px_rgba(15,23,42,0.08)]">
      <CardHeader className="gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <CardTitle className="break-words text-base">资金 · {name} · {symbol}</CardTitle>
          <CardDescription>
            {data?.trade_date ? `交易日 ${data.trade_date}` : "读取 Postgres stock_fund_flow_snapshot 最新快照"}
          </CardDescription>
        </div>
        <Button size="sm" variant="outline" onClick={() => void refresh()} disabled={loading} className="w-full gap-2 sm:w-auto">
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          刷新
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        {error || data?.ok === false ? (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
            {error || data?.error || "暂无资金流快照"}
          </div>
        ) : null}

        <div className="grid gap-3 md:grid-cols-4">
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <div className="text-xs text-slate-500">主力净额</div>
            <div className={`mt-1 text-lg font-semibold tabular-nums ${tone(data?.activeNetAmountWan)}`}>
              {formatWan(data?.activeNetAmountWan)}
            </div>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <div className="text-xs text-slate-500">主动买入占比</div>
            <div className="mt-1 text-lg font-semibold tabular-nums text-slate-900">{formatPct(data?.activeBuyRatio)}</div>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <div className="text-xs text-slate-500">成交额</div>
            <div className="mt-1 text-lg font-semibold tabular-nums text-slate-900">{formatWan(data?.amountWan)}</div>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
            <div className="text-xs text-slate-500">换手率</div>
            <div className="mt-1 text-lg font-semibold tabular-nums text-slate-900">{formatPct(data?.turnoverRate)}</div>
          </div>
        </div>

        <div className="overflow-hidden rounded-xl border border-slate-200">
          <div className="grid grid-cols-5 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-500">
            <div>日期</div>
            <div className="text-right">主力净额</div>
            <div className="text-right">成交额</div>
            <div className="text-right">涨跌幅</div>
            <div className="text-right">买/卖占比</div>
          </div>
          {(data?.history ?? []).slice(0, 12).map((item) => (
            <div key={item.trade_date} className="grid grid-cols-5 border-t border-slate-100 px-3 py-2 text-xs text-slate-700">
              <div>{item.trade_date}</div>
              <div className={`text-right tabular-nums ${tone(item.activeNetAmountWan)}`}>{formatWan(item.activeNetAmountWan)}</div>
              <div className="text-right tabular-nums">{formatWan(item.amountWan)}</div>
              <div className={`text-right tabular-nums ${tone(item.changePct)}`}>{formatPct(item.changePct)}</div>
              <div className="text-right tabular-nums">{formatPct(item.activeBuyRatio)} / {formatPct(item.activeSellRatio)}</div>
            </div>
          ))}
          {data?.history?.length ? null : (
            <div className="border-t border-slate-100 px-3 py-6 text-center text-sm text-slate-500">
              暂无历史资金流记录
            </div>
          )}
        </div>

        {data?.source ? <Badge variant="outline">source: {data.source}</Badge> : null}
      </CardContent>
    </Card>
  )
}
