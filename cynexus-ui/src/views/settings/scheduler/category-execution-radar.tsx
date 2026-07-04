import { PolarAngleAxis, PolarGrid, Radar, RadarChart } from "recharts"

import { Card, CardContent } from "@/components/ui/card"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { cn } from "@/lib/utils"

export type SchedulerCategoryExecutionRadarPoint = {
  label: string
  scheduledJobs: number
  totalJobs: number
}

const chartConfig = {
  scheduledJobs: {
    label: "今日需跑",
    color: "oklch(0.646 0.222 41.116)",
  },
  totalJobs: {
    label: "分类总数",
    color: "oklch(0.6 0.118 184.704)",
  },
} satisfies ChartConfig

export function SchedulerCategoryExecutionRadar({
  data,
  className,
}: {
  data: SchedulerCategoryExecutionRadarPoint[]
  className?: string
}) {
  const chartData = data

  return (
    <Card className={cn("border-0 bg-background py-0 shadow-none", className)}>
      <CardContent className="px-1 py-0">
        {chartData.length > 0 ? (
          <ChartContainer
            config={chartConfig}
            className="mx-auto aspect-square h-[250px] max-h-[250px]"
          >
            <RadarChart
              data={chartData}
              margin={{
                top: 26,
                right: 18,
                bottom: 26,
                left: 18,
              }}
            >
              <ChartTooltip
                cursor={false}
                content={<ChartTooltipContent indicator="line" />}
              />
              <PolarAngleAxis
                dataKey="label"
                tick={({ x, y, textAnchor, index, ...props }) => {
                  const item = chartData[index]
                  const yValue = typeof y === "number" ? y : 0

                  return (
                    <text
                      x={x}
                      y={yValue + (index === 0 ? -8 : 0)}
                      textAnchor={textAnchor}
                      fontSize={11}
                      fontWeight={500}
                      {...props}
                    >
                      <tspan>{item.scheduledJobs}</tspan>
                      <tspan className="fill-muted-foreground">/</tspan>
                      <tspan>{item.totalJobs}</tspan>
                      <tspan
                        x={x}
                        dy="0.9rem"
                        fontSize={10}
                        className="fill-muted-foreground"
                      >
                        {item.label}
                      </tspan>
                    </text>
                  )
                }}
              />
              <PolarGrid />
              <Radar
                dataKey="totalJobs"
                fill="var(--color-totalJobs)"
                fillOpacity={0.2}
                stroke="var(--color-totalJobs)"
              />
              <Radar
                dataKey="scheduledJobs"
                fill="var(--color-scheduledJobs)"
                fillOpacity={0.55}
                stroke="var(--color-scheduledJobs)"
              />
            </RadarChart>
          </ChartContainer>
        ) : (
          <div className="grid h-[250px] place-items-center text-xs text-muted-foreground">
            暂无分类执行数据
          </div>
        )}
      </CardContent>
    </Card>
  )
}
