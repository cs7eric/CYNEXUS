import { useCallback, useEffect, useRef, useState } from "react"
import { ChevronLeft, ChevronRight, CircleAlert, Loader2, RefreshCw, Upload } from "lucide-react"
import { Button } from "@/components/ui/button"
import { notification } from "@/components/ui/notification"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { fetchPortfolioImports, uploadPortfolioScreenshot } from "@/services/market/portfolio/imports"
import type { PortfolioImportTask, PortfolioImportTaskPage } from "@/services/market/portfolio/types"
import { ImportHistoryList } from "./import-history-list"

const ACTIVE_STATUSES = new Set(["PENDING_DISPATCH", "QUEUED", "PROCESSING", "RETRY_WAITING"])
const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

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

interface PortfolioImportDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function PortfolioImportDrawer({ open, onOpenChange }: PortfolioImportDrawerProps) {
  const uploadInputRef = useRef<HTMLInputElement>(null)
  const [tasks, setTasks] = useState<PortfolioImportTask[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState<number>(PAGE_SIZE_OPTIONS[0])
  const [pagination, setPagination] = useState<PortfolioImportTaskPage | null>(null)
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [expandedTaskId, setExpandedTaskId] = useState<number | null>(null)
  const [refreshSignal, setRefreshSignal] = useState(0)

  const load = useCallback(async (targetPage = page, showLoading = false, targetPageSize = pageSize) => {
    if (showLoading) setLoading(true)
    try {
      const result = await fetchPortfolioImports(targetPage, targetPageSize)
      setTasks((current) => sameTasks(current, result.items) ? current : result.items)
      setPagination(result)
      setError(null)
      setRefreshSignal((current) => current + 1)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      if (!showLoading) notification.danger({ title: "Import list unavailable", description: message })
    } finally {
      if (showLoading) setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    if (open) void Promise.resolve().then(() => load(undefined, true))
  }, [load, open])

  useEffect(() => {
    if (!open || !tasks.some((task) => ACTIVE_STATUSES.has(task.status))) return
    const timer = window.setInterval(() => { void load() }, 1_500)
    return () => window.clearInterval(timer)
  }, [load, open, tasks])

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
      const result = await fetchPortfolioImports(1, pageSize)
      setTasks((current) => sameTasks(current, result.items) ? current : result.items)
      setPagination(result)
      setPage(1)
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

  return <Sheet open={open} onOpenChange={onOpenChange}>
    <SheetContent side="right" className="w-full gap-0 p-0 md:w-[35vw] md:max-w-none">
      <SheetHeader className="border-b border-border/60 px-5 py-4 pr-12">
        <SheetTitle>Portfolio imports</SheetTitle>
        <SheetDescription>Upload a transaction screenshot and review its processing record here.</SheetDescription>
        <div className="mt-3 flex flex-wrap gap-2"><input ref={uploadInputRef} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={onFileChange} /><Button size="sm" onClick={() => uploadInputRef.current?.click()} disabled={uploading}><Upload className="mr-2 size-4" />{uploading ? "Uploading..." : "Import screenshot"}</Button><Button size="sm" variant="outline" onClick={() => void load(page, true)} disabled={loading || uploading}><RefreshCw className="mr-2 size-4" />Refresh</Button></div>
      </SheetHeader>
      <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
        {error ? <div className="mb-4 flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div className="min-w-0"><p className="font-medium">Import center unavailable</p><p className="mt-1 break-words text-xs">{error}</p></div><Button variant="outline" size="sm" className="ml-auto" onClick={() => void load(page, true)}>Retry</Button></div> : null}
        {!loading && tasks.some((task) => ACTIVE_STATUSES.has(task.status)) ? <div className="mb-4 flex items-center gap-2 rounded-lg border border-border/60 bg-muted/35 px-3 py-2.5 text-xs text-muted-foreground"><Loader2 className="size-4 animate-spin" />Active imports update in place.</div> : null}
        <ImportHistoryList tasks={tasks} loading={loading} onRefresh={() => void load(page, true)} selectedTaskId={expandedTaskId} onSelectedTaskChange={setExpandedTaskId} refreshSignal={refreshSignal} showHeader={false} />
        {pagination ? <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-3"><div className="flex min-w-0 flex-wrap items-center gap-2 text-xs text-muted-foreground"><span className="font-mono tabular-nums">Page {pagination.page} / {pagination.totalPages} · {pagination.total} imports</span><span className="hidden text-border sm:inline">·</span><span className="hidden sm:inline">Rows</span><Select value={String(pageSize)} onValueChange={(value) => { setPageSize(Number(value)); setPage(1) }}><SelectTrigger size="sm" aria-label="Imports per page" className="h-7 w-[104px] border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue /></SelectTrigger><SelectContent side="top">{PAGE_SIZE_OPTIONS.map((option) => <SelectItem key={option} value={String(option)}>{option} / page</SelectItem>)}</SelectContent></Select></div>{pagination.totalPages > 1 ? <div className="flex items-center gap-1"><Button size="icon-sm" variant="outline" aria-label="Previous import page" title="Previous page" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={!pagination.hasPrevious}><ChevronLeft className="size-4" /></Button><Button size="icon-sm" variant="outline" aria-label="Next import page" title="Next page" onClick={() => setPage((current) => current + 1)} disabled={!pagination.hasNext}><ChevronRight className="size-4" /></Button></div> : null}</div> : null}
      </div>
    </SheetContent>
  </Sheet>
}
