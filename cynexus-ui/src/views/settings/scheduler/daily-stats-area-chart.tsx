import { useMemo, useState } from "react"
import { Area, AreaChart, CartesianGrid, XAxis } from "recharts"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export type SchedulerDailyStatsAreaPoint = {
  date: string
  success: number
  failed: number
  skipped: number
}

const chartConfig = {
  success: {
    label: "成功",
    color: "#10b981",
  },
  failed: {
    label: "失败",
    color: "#ef4444",
  },
  skipped: {
    label: "跳过",
    color: "#f59e0b",
  },
} satisfies ChartConfig

type TimeRange = "14d" | "7d" | "all"

export function SchedulerDailyStatsAreaChart({
  data,
  average,
  delta,
  successRate,
  className,
}: {
  data: SchedulerDailyStatsAreaPoint[]
  average: number
  delta: number
  successRate: string
  className?: string
}) {
  const [timeRange, setTimeRange] = useState<TimeRange>("14d")
  const filteredData = useMemo(() => {
    if (timeRange === "all") return data
    const count = timeRange === "7d" ? 7 : 14
    return data.slice(-count)
  }, [data, timeRange])

  return (
    <Card className={className}>
      <CardHeader className="flex items-center gap-2 space-y-0 border-b py-4 sm:flex-row">
        <div className="grid flex-1 gap-1">
          <CardTitle className="text-sm font-medium">历史趋势</CardTitle>
          <CardDescription>
            7日均值 {average.toFixed(1)} 次 / 日
            <span className={delta >= 0 ? "ml-2 text-emerald-600" : "ml-2 text-red-600"}>
              {delta >= 0 ? "+" : ""}{delta.toFixed(1)}
            </span>
            <span className="ml-2">区间成功率 {successRate}</span>
          </CardDescription>
        </div>
        <Select value={timeRange} onValueChange={(value) => setTimeRange(value as TimeRange)}>
          <SelectTrigger
            className="hidden w-[132px] rounded-lg sm:ml-auto sm:flex"
            aria-label="选择统计范围"
          >
            <SelectValue placeholder="最近 14 天" />
          </SelectTrigger>
          <SelectContent className="rounded-xl">
            <SelectItem value="14d" className="rounded-lg">最近 14 天</SelectItem>
            <SelectItem value="7d" className="rounded-lg">最近 7 天</SelectItem>
            <SelectItem value="all" className="rounded-lg">全部</SelectItem>
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent className="px-2 pt-4 sm:px-5 sm:pt-5">
        <ChartContainer config={chartConfig} className="aspect-auto h-[246px] w-full">
          <AreaChart data={filteredData}>
            <defs>
              <linearGradient id="fillSuccess" x1="0" x2="0" y1="0" y2="1">
                <stop offset="5%" stopColor="var(--color-success)" stopOpacity={0.72} />
                <stop offset="95%" stopColor="var(--color-success)" stopOpacity={0.08} />
              </linearGradient>
              <linearGradient id="fillSkipped" x1="0" x2="0" y1="0" y2="1">
                <stop offset="5%" stopColor="var(--color-skipped)" stopOpacity={0.64} />
                <stop offset="95%" stopColor="var(--color-skipped)" stopOpacity={0.07} />
              </linearGradient>
              <linearGradient id="fillFailed" x1="0" x2="0" y1="0" y2="1">
                <stop offset="5%" stopColor="var(--color-failed)" stopOpacity={0.7} />
                <stop offset="95%" stopColor="var(--color-failed)" stopOpacity={0.08} />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="date"
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              minTickGap={32}
              tickFormatter={(value) => {
                const date = new Date(value)
                if (Number.isNaN(date.getTime())) return String(value).slice(5)
                return date.toLocaleDateString("zh-CN", {
                  month: "numeric",
                  day: "numeric",
                })
              }}
            />
            <ChartTooltip
              cursor={false}
              content={
                <ChartTooltipContent
                  labelFormatter={(value) => {
                    const date = new Date(String(value))
                    if (Number.isNaN(date.getTime())) return String(value)
                    return date.toLocaleDateString("zh-CN", {
                      month: "long",
                      day: "numeric",
                    })
                  }}
                  indicator="dot"
                />
              }
            />
            <Area
              dataKey="failed"
              type="natural"
              fill="url(#fillFailed)"
              stroke="var(--color-failed)"
              stackId="runs"
            />
            <Area
              dataKey="skipped"
              type="natural"
              fill="url(#fillSkipped)"
              stroke="var(--color-skipped)"
              stackId="runs"
            />
            <Area
              dataKey="success"
              type="natural"
              fill="url(#fillSuccess)"
              stroke="var(--color-success)"
              stackId="runs"
            />
            <ChartLegend content={<ChartLegendContent />} />
          </AreaChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
