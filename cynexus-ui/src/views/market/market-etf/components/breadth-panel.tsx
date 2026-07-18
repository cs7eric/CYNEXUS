import { ArrowDownRight, ArrowUpRight, Gauge, Minus } from "lucide-react"

import type { MarketEtfBreadth, MarketEtfSummary } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatNumber, formatPct } from "../lib/format"

export function EtfBreadthPanel({ breadth, summary }: { breadth: MarketEtfBreadth | null; summary: MarketEtfSummary | null }) {
  const direction = summary?.direction ?? "rotation"
  const DirectionIcon = direction === "risk_on" ? ArrowUpRight : direction === "risk_off" ? ArrowDownRight : Minus
  const tone = direction === "risk_on" ? "text-red-600" : direction === "risk_off" ? "text-emerald-600" : "text-amber-600"
  const breadthWidth = Math.max(0, Math.min(100, breadth?.advancingPct ?? 0))
  return (
    <section className="min-w-0 overflow-hidden rounded-lg bg-muted/35 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">方向与市场广度</h2>
          <p className="mt-1 text-xs text-muted-foreground">ETF 份额趋势与 A 股涨跌结构的交叉观察</p>
        </div>
        <Gauge className="size-4 text-muted-foreground" />
      </div>
      <div className="mt-5 flex items-start gap-3">
        <DirectionIcon className={cn("mt-0.5 size-6", tone)} />
        <div className="min-w-0">
          <div className={cn("text-lg font-semibold", tone)}>{summary?.directionText ?? "等待数据"}</div>
          <div className="mt-1 text-xs text-muted-foreground">宽基与行业份额变化共同决定方向标签</div>
        </div>
      </div>
      <div className="mt-6 space-y-3">
        <div className="flex items-center justify-between text-xs"><span className="text-muted-foreground">上涨家数占比</span><span className="font-mono tabular-nums">{formatPct(breadth?.advancingPct, 1)}</span></div>
        <div className="h-2 overflow-hidden rounded-full bg-background/70"><div className="h-full rounded-full bg-rose-500/75 transition-all" style={{ width: `${breadthWidth}%` }} /></div>
        <div className="grid grid-cols-3 gap-2 pt-1 text-xs"><span className="text-red-600">上涨 {formatNumber(breadth?.risingCount, 0)}</span><span className="text-emerald-600">下跌 {formatNumber(breadth?.fallingCount, 0)}</span><span className="text-muted-foreground">平盘 {formatNumber(breadth?.flatCount, 0)}</span></div>
        <div className="flex flex-wrap gap-x-4 gap-y-1 border-t border-border/50 pt-3 text-xs text-muted-foreground"><span>涨停 {formatNumber(breadth?.limitUpCount, 0)}</span><span>跌停 {formatNumber(breadth?.limitDownCount, 0)}</span><span>样本 {formatNumber(breadth?.stockCount, 0)}</span></div>
      </div>
    </section>
  )
}
