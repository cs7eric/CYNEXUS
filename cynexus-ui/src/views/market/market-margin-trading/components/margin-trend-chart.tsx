import { Area, AreaChart, CartesianGrid, Line, XAxis, YAxis } from "recharts"

import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import type { MarginTrendPoint } from "../lib/derive"
import { formatDate, formatYi } from "../lib/format"

const chartConfig = {
  cn: { label: "全市场余额", color: "#0f8bb8" },
  sse: { label: "上交所", color: "#7c3aed" },
  szse: { label: "深交所", color: "#16a34a" },
  buyCn: { label: "融资买入", color: "#f59e0b" },
} satisfies ChartConfig

interface MarginTrendChartProps {
  data: MarginTrendPoint[]
  loading?: boolean
}

export function MarginTrendChart({ data, loading }: MarginTrendChartProps) {
  return (
    <section className="flex h-full min-w-0 flex-col overflow-hidden rounded-lg bg-muted/35 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">两融余额趋势</h2>
          <p className="text-xs text-muted-foreground">CN 为全市场口径，SSE/SZSE 为交易所口径。</p>
        </div>
        <span className="text-xs text-muted-foreground">{loading ? "加载中" : `${data.length} 个交易日`}</span>
      </div>
      <ChartContainer config={chartConfig} className="h-[360px] min-w-0 w-full xl:aspect-auto xl:min-h-0 xl:flex-1 xl:h-auto">
        <AreaChart data={data} margin={{ left: 0, right: 12, top: 16, bottom: 0 }}>
          <CartesianGrid vertical={false} strokeDasharray="3 3" />
          <XAxis
            dataKey="date"
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            minTickGap={36}
            tickFormatter={formatDate}
          />
          <YAxis
            yAxisId="balance"
            tickLine={false}
            axisLine={false}
            width={68}
            domain={["auto", "auto"]}
            tickFormatter={(value) => formatYi(Number(value), 0)}
          />
          <YAxis
            yAxisId="buy"
            orientation="right"
            tickLine={false}
            axisLine={false}
            width={64}
            domain={["auto", "auto"]}
            tickFormatter={(value) => formatYi(Number(value), 0)}
          />
          <ChartTooltip
            content={
              <ChartTooltipContent
                labelFormatter={(value) => formatDate(String(value))}
                formatter={(value, name) => (
                  <div className="flex min-w-44 items-center justify-between gap-4">
                    <span className="text-muted-foreground">{chartConfig[String(name) as keyof typeof chartConfig]?.label ?? name}</span>
                    <span className="font-mono font-medium tabular-nums">{formatYi(Number(value), 2)}</span>
                  </div>
                )}
              />
            }
          />
          <defs>
            <linearGradient id="cnFill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="5%" stopColor="var(--color-cn)" stopOpacity={0.28} />
              <stop offset="95%" stopColor="var(--color-cn)" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <Area
            yAxisId="balance"
            type="monotone"
            dataKey="cn"
            stroke="var(--color-cn)"
            strokeWidth={2.4}
            fill="url(#cnFill)"
            dot={false}
            isAnimationActive={false}
            connectNulls
          />
          <Line yAxisId="balance" type="monotone" dataKey="sse" stroke="var(--color-sse)" strokeWidth={1.7} dot={false} isAnimationActive={false} connectNulls />
          <Line yAxisId="balance" type="monotone" dataKey="szse" stroke="var(--color-szse)" strokeWidth={1.7} dot={false} isAnimationActive={false} connectNulls />
          <Line yAxisId="buy" type="monotone" dataKey="buyCn" stroke="var(--color-buyCn)" strokeWidth={1.4} strokeDasharray="5 4" dot={false} isAnimationActive={false} connectNulls />
        </AreaChart>
      </ChartContainer>
    </section>
  )
}
