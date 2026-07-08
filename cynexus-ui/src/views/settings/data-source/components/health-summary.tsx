import type { ReactNode } from "react"
import { AlertTriangle, CheckCircle2, Gauge, Network, TimerReset } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"
import type { DataSourceHealthSummary as DataSourceHealthSummaryType } from "@/services/core/data-source-health"
import { formatInteger } from "../lib/format"

interface SummaryItem {
  label: string
  value: string
  hint: string
  icon: ReactNode
}

export function HealthSummary({ summary }: { summary: DataSourceHealthSummaryType }) {
  const items: SummaryItem[] = [
    {
      label: "数据源",
      value: formatInteger(summary.total),
      hint: "Python provider checks",
      icon: <Network className="size-4" />,
    },
    {
      label: "健康",
      value: formatInteger(summary.healthy),
      hint: "连通且样本有效",
      icon: <CheckCircle2 className="size-4 text-emerald-600" />,
    },
    {
      label: "需关注",
      value: formatInteger(summary.degraded + summary.failed),
      hint: "降级 / 失败",
      icon: <AlertTriangle className="size-4 text-amber-600" />,
    },
    {
      label: "成功率",
      value: summary.successRate === null || summary.successRate === undefined ? "-" : `${summary.successRate.toFixed(1)}%`,
      hint: "本次检查",
      icon: <Gauge className="size-4" />,
    },
    {
      label: "平均耗时",
      value: summary.avgLatencyMs === null || summary.avgLatencyMs === undefined ? "-" : `${Math.round(summary.avgLatencyMs)}ms`,
      hint: "可用检查项",
      icon: <TimerReset className="size-4" />,
    },
  ]

  return (
    <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-5">
      {items.map((item) => (
        <Card key={item.label} className="border-0 bg-muted/60 py-0 shadow-none">
          <CardContent className="flex items-center gap-3 px-4 py-3">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-background text-foreground">
              {item.icon}
            </div>
            <div className="min-w-0">
              <div className="text-xl font-semibold leading-none tabular-nums text-foreground">{item.value}</div>
              <div className="mt-1 text-xs text-muted-foreground">{item.label}</div>
            </div>
            <div className="ml-auto hidden text-right text-xs text-muted-foreground sm:block">{item.hint}</div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
