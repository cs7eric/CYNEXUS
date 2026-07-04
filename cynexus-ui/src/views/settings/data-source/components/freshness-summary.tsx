import type { ReactNode } from "react"
import { AlertTriangle, CheckCircle2, Database, HelpCircle, Table2 } from "lucide-react"

import { Card, CardContent } from "@/components/ui/card"
import type { DataSourceFreshnessSummary } from "@/services/market/data-source"
import { formatInteger } from "../lib/format"

interface SummaryItem {
  label: string
  value: number
  hint: string
  icon: ReactNode
}

export function FreshnessSummary({ summary }: { summary: DataSourceFreshnessSummary }) {
  const items: SummaryItem[] = [
    {
      label: "总表数",
      value: summary.total,
      hint: "PG + CH",
      icon: <Table2 className="size-4" />,
    },
    {
      label: "正常",
      value: summary.fresh,
      hint: "3 天内",
      icon: <CheckCircle2 className="size-4 text-emerald-600" />,
    },
    {
      label: "需关注",
      value: summary.lagging + summary.stale + summary.error,
      hint: "滞后 / 过期 / 异常",
      icon: <AlertTriangle className="size-4 text-amber-600" />,
    },
    {
      label: "未追踪",
      value: summary.empty + summary.untracked,
      hint: "空表 / 无业务日期",
      icon: <HelpCircle className="size-4 text-muted-foreground" />,
    },
  ]

  return (
    <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
      {items.map((item) => (
        <Card key={item.label} className="border-0 bg-muted/60 py-0 shadow-none">
          <CardContent className="flex items-center gap-3 px-4 py-3">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-background text-foreground">
              {item.icon}
            </div>
            <div className="min-w-0">
              <div className="text-xl font-semibold leading-none tabular-nums text-foreground">
                {formatInteger(item.value)}
              </div>
              <div className="mt-1 text-xs text-muted-foreground">{item.label}</div>
            </div>
            <div className="ml-auto hidden items-center gap-1 text-right text-xs text-muted-foreground sm:flex">
              <Database className="size-3" />
              {item.hint}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
