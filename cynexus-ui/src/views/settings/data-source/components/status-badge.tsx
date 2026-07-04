import type { ComponentType } from "react"
import { AlertTriangle, CheckCircle2, Clock3, Database, HelpCircle, XCircle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { DataSourceStatus } from "@/services/market/data-source"
import { STATUS_LABELS } from "../lib/format"

const STATUS_CLASS: Record<DataSourceStatus, string> = {
  fresh: "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  lagging: "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  stale: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-400",
  empty: "border-slate-500/20 bg-slate-500/10 text-slate-600 dark:text-slate-300",
  untracked: "border-blue-500/25 bg-blue-500/10 text-blue-700 dark:text-blue-300",
  error: "border-destructive/30 bg-destructive/10 text-destructive",
}

const STATUS_ICON = {
  fresh: CheckCircle2,
  lagging: Clock3,
  stale: AlertTriangle,
  empty: Database,
  untracked: HelpCircle,
  error: XCircle,
} satisfies Record<DataSourceStatus, ComponentType<{ className?: string }>>

export function StatusBadge({ status }: { status: DataSourceStatus }) {
  const Icon = STATUS_ICON[status] ?? HelpCircle
  return (
    <Badge variant="outline" className={cn("gap-1.5 whitespace-nowrap", STATUS_CLASS[status])}>
      <Icon className="size-3" />
      {STATUS_LABELS[status] ?? status}
    </Badge>
  )
}
