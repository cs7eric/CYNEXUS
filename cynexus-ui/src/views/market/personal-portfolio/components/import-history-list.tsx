import { useEffect, useState } from "react"
import { RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchPortfolioImportHistory } from "@/services/market/portfolio/imports"
import type { PortfolioImportProcessHistory, PortfolioImportTask } from "@/services/market/portfolio/types"
import { ImportExecutionRecord } from "./import-execution-record"

interface ImportHistoryListProps {
  tasks: PortfolioImportTask[]
  loading: boolean
  onRefresh: () => void
  selectedTaskId?: number | null
  onSelectedTaskChange?: (taskId: number | null) => void
  refreshSignal?: number
  showHeader?: boolean
}

export function ImportHistoryList({ tasks, loading, onRefresh, selectedTaskId, onSelectedTaskChange, refreshSignal = 0, showHeader = true }: ImportHistoryListProps) {
  const [historyByTask, setHistoryByTask] = useState<Record<number, PortfolioImportProcessHistory>>({})
  const [errorsByTask, setErrorsByTask] = useState<Record<number, string>>({})

  useEffect(() => {
    let cancelled = false
    const activeTaskIds = new Set(tasks.filter((task) => !["READY_FOR_REVIEW", "CONFIRMED", "FAILED", "CANCELLED"].includes(task.status)).map((task) => task.id))
    const tasksToRefresh = refreshSignal === 0
      ? tasks
      : tasks.filter((task) => activeTaskIds.has(task.id) || !historyByTask[task.id])
    for (const task of tasksToRefresh) {
      void fetchPortfolioImportHistory(task.id).then((history) => {
        if (cancelled) return
        setHistoryByTask((current) => ({ ...current, [task.id]: history }))
        setErrorsByTask((current) => {
          if (!current[task.id]) return current
          const next = { ...current }
          delete next[task.id]
          return next
        })
      }).catch((cause) => {
        if (cancelled) return
        setErrorsByTask((current) => ({
          ...current,
          [task.id]: cause instanceof Error ? cause.message : String(cause),
        }))
      })
    }
    return () => { cancelled = true }
  }, [tasks, refreshSignal])

  return <section aria-labelledby={showHeader ? "import-history-heading" : undefined}>
    {showHeader ? <div className="flex flex-wrap items-end justify-between gap-3">
      <div><h2 id="import-history-heading" className="text-base font-semibold">Import history</h2><p className="mt-1 text-xs text-muted-foreground">Every screenshot keeps its own processing record, including retries and review changes.</p></div>
      <Button variant="outline" size="sm" onClick={onRefresh} disabled={loading}><RefreshCw className="mr-2 size-4" />Refresh history</Button>
    </div> : null}
    {loading ? <div className={`${showHeader ? "mt-3" : ""} space-y-3`}><Skeleton className="h-16 rounded-lg" /><Skeleton className="h-16 rounded-lg" /></div> : !tasks.length ? <div className={`${showHeader ? "mt-3" : ""} rounded-lg border border-dashed border-border/70 px-4 py-8 text-center`}><p className="text-sm font-medium">No screenshot imports yet</p><p className="mt-1 text-xs text-muted-foreground">Import a trade screenshot to create the first processing record.</p></div> : <div className={`${showHeader ? "mt-3" : ""} space-y-3`}>{tasks.map((task) => <ImportExecutionRecord key={task.id} task={task} history={historyByTask[task.id] ?? null} candidateCount={null} loading={!historyByTask[task.id] && !errorsByTask[task.id]} error={errorsByTask[task.id] ?? null} open={selectedTaskId === task.id} onOpenChange={(open) => onSelectedTaskChange?.(open ? task.id : null)} reviewHref={`/personal-portfolio/imports/${task.id}/review`} />)}</div>}
  </section>
}
