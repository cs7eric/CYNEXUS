import { ChevronRight, CircleAlert } from "lucide-react"
import { Link } from "react-router-dom"
import type { PortfolioImportTask } from "@/services/market/portfolio/types"

const STATUS_LABEL: Record<string, string> = {
  PENDING_DISPATCH: "Waiting to queue",
  QUEUED: "Queued",
  PROCESSING: "Processing",
  RETRY_WAITING: "Retry scheduled",
  READY_FOR_REVIEW: "Ready for review",
  CONFIRMED: "Confirmed",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
}

function statusClass(status: string) {
  if (status === "FAILED" || status === "CANCELLED") return "bg-rose-50 text-rose-700"
  if (status === "READY_FOR_REVIEW" || status === "CONFIRMED") return "bg-emerald-50 text-emerald-700"
  if (status === "RETRY_WAITING") return "bg-amber-50 text-amber-700"
  return "bg-muted text-muted-foreground"
}

function taskTime(task: PortfolioImportTask) {
  const value = task.completedAt ?? task.startedAt ?? task.queuedAt
  return value ? new Date(value).toLocaleString() : "Awaiting worker"
}

interface RecentImportListProps {
  currentTaskId: number
  tasks: PortfolioImportTask[]
}

export function RecentImportList({ currentTaskId, tasks }: RecentImportListProps) {
  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="recent-imports-heading">
    <div className="border-b border-border/60 px-4 py-3">
      <h2 id="recent-imports-heading" className="text-sm font-semibold">Recent screenshot imports</h2>
      <p className="mt-1 text-xs text-muted-foreground">Each screenshot is a separate record. Open a record to inspect its full processing chain.</p>
    </div>
    <div className="divide-y divide-border/60">
      {tasks.map((task) => {
        const current = task.id === currentTaskId
        return <Link key={task.id} to={`/personal-portfolio/imports/${task.id}`} aria-current={current ? "page" : undefined}
          className={`flex min-w-0 items-center gap-3 px-4 py-3 transition-colors hover:bg-muted/35 ${current ? "bg-muted/35" : ""}`}>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1"><span className="font-mono text-sm font-medium">Import #{task.id}</span><span className={`rounded-full px-2 py-0.5 text-[11px] ${statusClass(task.status)}`}>{STATUS_LABEL[task.status] ?? task.status}</span><span className="text-[11px] text-muted-foreground">{taskTime(task)}</span></div>
            {task.lastErrorMessage ? <p className="mt-1 flex min-w-0 items-center gap-1.5 truncate text-xs text-rose-700"><CircleAlert className="size-3.5 shrink-0" /><span className="truncate"><span className="font-mono">{task.lastErrorCode}</span>{task.lastErrorCode ? " · " : ""}{task.lastErrorMessage}</span></p> : null}
          </div>
          <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
        </Link>
      })}
    </div>
  </section>
}
