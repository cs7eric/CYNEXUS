import { useCallback, useEffect, useRef, useState } from "react"
import { CircleAlert, Loader2, RefreshCw, Upload, Workflow } from "lucide-react"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { notification } from "@/components/ui/notification"
import { fetchPortfolioImports, uploadPortfolioScreenshot } from "@/services/market/portfolio/imports"
import type { PortfolioImportTask } from "@/services/market/portfolio/types"
import { ImportHistoryList } from "./components/import-history-list"

const ACTIVE_STATUSES = new Set(["PENDING_DISPATCH", "QUEUED", "PROCESSING", "RETRY_WAITING"])

function sameTasks(left: PortfolioImportTask[], right: PortfolioImportTask[]) {
  return left.length === right.length && left.every((task, index) => {
    const next = right[index]
    return task.id === next?.id
      && task.status === next.status
      && task.version === next.version
      && task.attemptCount === next.attemptCount
      && task.startedAt === next.startedAt
      && task.completedAt === next.completedAt
      && task.lastErrorCode === next.lastErrorCode
      && task.lastErrorMessage === next.lastErrorMessage
  })
}

export default function PersonalPortfolioImportsCenterPage() {
  const uploadInputRef = useRef<HTMLInputElement>(null)
  const [tasks, setTasks] = useState<PortfolioImportTask[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [expandedTaskId, setExpandedTaskId] = useState<number | null>(null)
  const [refreshSignal, setRefreshSignal] = useState(0)

  const load = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true)
    try {
      const nextTasks = await fetchPortfolioImports()
      setTasks((current) => sameTasks(current, nextTasks) ? current : nextTasks)
      setError(null)
      setRefreshSignal((current) => current + 1)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      if (!showLoading) notification.danger({ title: "Import list unavailable", description: message })
    } finally {
      if (showLoading) setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(true) }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  useEffect(() => {
    if (!tasks.some((task) => ACTIVE_STATUSES.has(task.status))) return
    const timer = window.setInterval(() => { void load() }, 1_500)
    return () => window.clearInterval(timer)
  }, [load, tasks])

  async function onFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""
    if (!file) return
    if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
      notification.warn({ title: "Unsupported screenshot", description: "Use PNG, JPEG, or WebP." })
      return
    }
    setUploading(true)
    try {
      const task = await uploadPortfolioScreenshot(file, `ui-${crypto.randomUUID()}`)
      const nextTasks = await fetchPortfolioImports()
      setTasks((current) => sameTasks(current, nextTasks) ? current : nextTasks)
      setExpandedTaskId(task.id)
      setRefreshSignal((current) => current + 1)
      notification.success({ title: "Screenshot queued", description: "The processing chain is expanded below and updates while it runs." })
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "Upload failed", description: message })
    } finally {
      setUploading(false)
    }
  }

  return <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Import Center">
    <div className="flex w-full min-w-0 flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><Workflow className="size-4" />Personal portfolio</div>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">Screenshot import center</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">Upload a transaction screenshot, then follow its processing record without leaving the list.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <input ref={uploadInputRef} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={onFileChange} />
          <Button onClick={() => uploadInputRef.current?.click()} disabled={uploading}><Upload className="mr-2 size-4" />{uploading ? "Uploading..." : "Import screenshot"}</Button>
          <Button variant="outline" onClick={() => void load(true)} disabled={loading || uploading}><RefreshCw className="mr-2 size-4" />Refresh</Button>
        </div>
      </header>

      {error ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">Import center unavailable</p><p className="mt-1 break-words text-xs">{error}</p></div><Button variant="outline" size="sm" className="ml-auto" onClick={() => void load(true)}>Retry</Button></div> : null}

      {!loading && tasks.some((task) => ACTIVE_STATUSES.has(task.status)) ? <div className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/35 px-4 py-3 text-xs text-muted-foreground"><Loader2 className="size-4 animate-spin" />Active imports update in place. Existing records remain open while new events arrive.</div> : null}

      <ImportHistoryList
        tasks={tasks}
        loading={loading}
        onRefresh={() => void load(true)}
        expandedTaskId={expandedTaskId}
        onExpandedTaskChange={setExpandedTaskId}
        refreshSignal={refreshSignal}
      />
    </div>
  </WorkspaceShell>
}
