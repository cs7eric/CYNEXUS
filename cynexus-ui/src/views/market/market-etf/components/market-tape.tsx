import { ArrowDownRight, ArrowUpRight, Minus } from "lucide-react"

import type { MarketEtfBreadth, MarketEtfSummary } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatPct, formatWan } from "../lib/format"
import { EtfMarketReaction } from "./market-reaction"
import type { MarketEtfRotation } from "@/services/market/etf"

function directionTone(value?: number | null) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-red-600" : "text-emerald-600"
}

function TapeItem({ label, value, detail, tone }: { label: string; value: string; detail: string; tone?: string }) {
  return <div className="min-w-[152px] flex-1 border-r border-border/60 px-3 py-2.5 last:border-r-0"><div className="text-[11px] text-muted-foreground">{label}</div><div className={cn("mt-1 truncate font-mono text-sm font-semibold tabular-nums", tone)}>{value}</div><div className="mt-0.5 truncate text-[10px] text-muted-foreground">{detail}</div></div>
}

export function EtfMarketTape({ summary, breadth, rotations }: { summary: MarketEtfSummary | null; breadth: MarketEtfBreadth | null; rotations: MarketEtfRotation[] }) {
  const direction = summary?.direction ?? "rotation"
  const DirectionIcon = direction === "risk_on" ? ArrowUpRight : direction === "risk_off" ? ArrowDownRight : Minus
  const directionClass = direction === "risk_on" ? "text-red-600" : direction === "risk_off" ? "text-emerald-600" : "text-amber-600"
  return (
    <section aria-label="ETF 市场状态与反应指标" className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
      <div className="flex min-w-0 overflow-x-auto border-b border-border/60"><div className="min-w-[190px] flex-[1.2] border-r border-border/60 px-3 py-2.5"><div className="text-[11px] text-muted-foreground">市场状态</div><div className={cn("mt-1 flex items-center gap-1.5 text-sm font-semibold", directionClass)}><DirectionIcon className="size-4" />{summary?.directionText ?? "等待数据"}</div><div className="mt-0.5 text-[10px] text-muted-foreground">份额方向 × 市场广度</div></div>
      <TapeItem label="宽基份额" value={formatWan(summary?.broadShareDelta)} detail={`申赎代理 ${formatWan(summary?.broadNetSubscriptionProxy)}`} tone={directionTone(summary?.broadShareDelta)} />
      <TapeItem label="行业份额" value={formatWan(summary?.sectorShareDelta)} detail={`申赎代理 ${formatWan(summary?.sectorNetSubscriptionProxy)}`} tone={directionTone(summary?.sectorShareDelta)} />
      <TapeItem label="ETF 成交额" value={formatWan(summary?.totalAmount)} detail={`${summary?.totalEtfs ?? "—"} 只有效样本`} />
      <TapeItem label="平均折溢价" value={formatPct(summary?.averagePremiumPct)} detail="现价相对 IOPV" tone={directionTone(summary?.averagePremiumPct)} />
      <TapeItem label="上涨占比" value={formatPct(breadth?.advancingPct, 1)} detail={`${breadth?.risingCount ?? "—"} 涨 / ${breadth?.fallingCount ?? "—"} 跌`} tone={directionTone((breadth?.advancingPct ?? 50) - 50)} /></div>
      <EtfMarketReaction summary={summary} breadth={breadth} rotations={rotations} />
    </section>
  )
}
