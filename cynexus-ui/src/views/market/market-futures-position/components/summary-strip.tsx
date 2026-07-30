import { ArrowDownRight, ArrowUpRight, BarChart3, Gauge, Minus, MoveHorizontal } from "lucide-react"

import { formatPosition, formatRatio, formatSigned, toneForValue } from "../lib/format"
import type { FuturesPositionAggregate, FuturesPositionLatestResponse } from "@/services/market/futures-position"

interface SummaryStripProps {
  aggregate?: FuturesPositionAggregate | null
  count: number
  top: number
  response?: FuturesPositionLatestResponse | null
}

function Metric({ icon: Icon, label, value, detail, tone = "text-foreground" }: {
  icon: typeof BarChart3
  label: string
  value: string
  detail: string
  tone?: string
}) {
  return (
    <div className="min-w-0 border-b border-border/60 px-4 py-3 last:border-b-0 sm:border-b-0 sm:border-r sm:last:border-r-0">
      <div className="flex items-center gap-2 text-xs text-muted-foreground"><Icon className="size-3.5" />{label}</div>
      <div className={`mt-1 font-mono text-xl font-semibold tabular-nums ${tone}`}>{value}</div>
      <div className="mt-1 truncate text-[11px] text-muted-foreground">{detail}</div>
    </div>
  )
}

export function SummaryStrip({ aggregate, count, top, response }: SummaryStripProps) {
  const net = aggregate?.netPosition
  const coverage = response?.coverage?.items ?? []
  const selected = coverage.filter((item) => item.selected)
  const healthy = selected.filter((item) => item.status === "SUCCEEDED" || item.status === "PARTIAL").length
  const netIcon = net == null ? Minus : net >= 0 ? ArrowUpRight : ArrowDownRight
  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="持仓汇总">
      <div className="grid sm:grid-cols-2 xl:grid-cols-4">
        <Metric icon={BarChart3} label="Top-N 多头持仓" value={formatPosition(aggregate?.longPosition)} detail={`按每个标的前 ${top} 名会员汇总`} tone="text-rose-600 dark:text-rose-400" />
        <Metric icon={MoveHorizontal} label="Top-N 空头持仓" value={formatPosition(aggregate?.shortPosition)} detail={`独立空头排名，不与多头成员配对`} tone="text-emerald-600 dark:text-emerald-400" />
        <Metric icon={netIcon} label="结构净值" value={formatSigned(net)} detail={`多头减空头 · 多头/空头 ${formatRatio(aggregate?.longShortRatio)}`} tone={toneForValue(net)} />
        <Metric icon={Gauge} label="数据覆盖" value={`${healthy}/${selected.length || "-"}`} detail={`${count} 个标的 · ${response?.tradeDate ?? "等待交易日"}`} />
      </div>
    </section>
  )
}
