import type { ComponentType } from "react"
import { CheckCircle2, CircleSlash, HelpCircle, ShieldAlert, XCircle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { DataSourceHealthStatus } from "@/services/core/data-source-health"

const STATUS_CLASS: Record<string, string> = {
  healthy: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  degraded: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  failed: "border-destructive/30 bg-destructive/10 text-destructive",
  skipped: "border-slate-500/20 bg-slate-500/10 text-slate-600 dark:text-slate-300",
}

const STATUS_LABEL: Record<string, string> = {
  healthy: "健康",
  degraded: "降级",
  failed: "失败",
  skipped: "跳过",
}

const STATUS_ICON: Record<string, ComponentType<{ className?: string }>> = {
  healthy: CheckCircle2,
  degraded: ShieldAlert,
  failed: XCircle,
  skipped: CircleSlash,
}

export function HealthStatusBadge({ status }: { status: DataSourceHealthStatus }) {
  const Icon = STATUS_ICON[status] ?? HelpCircle
  return (
    <Badge variant="outline" className={cn("gap-1.5 whitespace-nowrap", STATUS_CLASS[status])}>
      <Icon className="size-3" />
      {STATUS_LABEL[status] ?? status}
    </Badge>
  )
}
