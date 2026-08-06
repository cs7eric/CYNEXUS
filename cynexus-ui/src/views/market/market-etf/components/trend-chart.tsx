import { useState } from "react"
import { Area, AreaChart, CartesianGrid, Line, XAxis, YAxis } from "recharts"

import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Button } from "@/components/ui/button"
import type { MarketEtfTrend } from "@/services/market/etf"
import { formatDate, formatPct, formatWan } from "../lib/format"

const config = {
  broad: { label: "宽基份额变化", color: "#0f8bb8" },
  sector: { label: "行业份额变化", color: "#16a34a" },
  amount: { label: "成交额", color: "#f59e0b" },
  premium: { label: "平均折溢价", color: "#7c3aed" },
} satisfies ChartConfig

export function EtfTrendChart({ data, loading }: { data: MarketEtfTrend[]; loading: boolean }) {
  const [range, setRange] = useState<5 | 10>(10)
  const visibleData = data.slice(-range)
  return (
    <section className="min-w-0 overflow-hidden rounded-lg bg-muted/35 p-4">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">资金与交易趋势</h2>
          <p className="text-xs text-muted-foreground">最近 {range} 个交易日 · 份额变化为净申购/赎回代理</p>
        </div>
        <div className="flex items-center gap-1"><Button type="button" size="sm" variant={range === 5 ? "secondary" : "ghost"} className="h-7 px-2 text-xs" onClick={() => setRange(5)}>5D</Button><Button type="button" size="sm" variant={range === 10 ? "secondary" : "ghost"} className="h-7 px-2 text-xs" onClick={() => setRange(10)}>10D</Button><span className="ml-1 text-xs text-muted-foreground">{loading ? "加载中" : `${visibleData.length} 个交易日`}</span></div>
      </div>
      <ChartContainer config={config} className="h-[340px] w-full min-w-0">
        <AreaChart data={visibleData} margin={{ left: 0, right: 10, top: 12, bottom: 0 }}>
          <CartesianGrid vertical={false} strokeDasharray="3 3" />
          <XAxis dataKey="tradeDate" tickLine={false} axisLine={false} tickMargin={8} minTickGap={28} tickFormatter={formatDate} />
          <YAxis yAxisId="share" tickLine={false} axisLine={false} width={58} tickFormatter={(value) => formatWan(Number(value), 0)} />
          <YAxis yAxisId="premium" orientation="right" tickLine={false} axisLine={false} width={54} tickFormatter={(value) => `${Number(value).toFixed(1)}%`} />
          <ChartTooltip content={<ChartTooltipContent labelFormatter={(value) => formatDate(String(value))} formatter={(value, name) => <div className="flex min-w-44 items-center justify-between gap-4"><span className="text-muted-foreground">{config[String(name) as keyof typeof config]?.label ?? name}</span><span className="font-mono font-medium tabular-nums">{String(name) === "premium" ? formatPct(Number(value)) : formatWan(Number(value), 2)}</span></div>} />} />
          <defs><linearGradient id="etfBroadFill" x1="0" x2="0" y1="0" y2="1"><stop offset="5%" stopColor="var(--color-broad)" stopOpacity={0.25} /><stop offset="95%" stopColor="var(--color-broad)" stopOpacity={0.02} /></linearGradient></defs>
          <Area yAxisId="share" type="monotone" dataKey="broadShareDelta" name="broad" stroke="var(--color-broad)" fill="url(#etfBroadFill)" strokeWidth={2.2} dot={false} isAnimationActive={false} connectNulls />
          <Line yAxisId="share" type="monotone" dataKey="sectorShareDelta" name="sector" stroke="var(--color-sector)" strokeWidth={1.9} dot={false} isAnimationActive={false} connectNulls />
          <Line yAxisId="share" type="monotone" dataKey="amount" name="amount" stroke="var(--color-amount)" strokeWidth={1.5} strokeDasharray="5 4" dot={false} isAnimationActive={false} connectNulls />
          <Line yAxisId="premium" type="monotone" dataKey="averagePremiumPct" name="premium" stroke="var(--color-premium)" strokeWidth={1.5} dot={false} isAnimationActive={false} connectNulls />
        </AreaChart>
      </ChartContainer>
    </section>
  )
}
