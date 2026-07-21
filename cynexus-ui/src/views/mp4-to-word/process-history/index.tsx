import { useCallback, useEffect, useMemo, useState } from "react"
import { AlertCircle, Check, CheckCircle2, Clock3, Copy, FileAudio, Loader2, RefreshCw, RotateCcw, X } from "lucide-react"
import { useIsMobile } from "@/hooks/use-mobile"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Drawer, DrawerClose, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from "@/components/ui/drawer"
import { Progress } from "@/components/ui/progress"
import { notification } from "@/components/ui/notification"
import { listMediaProcesses, getMediaProcess, retryMediaProcess, type MediaProcessItem } from "@/services/core/mp4-process-history"
import { ProcessHistoryTable } from "./process-history-table"

const phaseLabels: Record<string, string> = {
  queued: "排队中",
  starting: "启动中",
  downloading: "获取视频",
  transcribing: "转写中",
  polishing: "AI 润色",
  summarizing: "生成摘要",
  metadata: "整理元数据",
  persisting: "写入历史",
  completed: "已完成",
  failed: "失败",
}

function formatDate(value?: string | null) {
  if (!value) return "--"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function statusLabel(status: string) {
  if (status === "succeeded") return "完成"
  if (status === "running") return "处理中"
  if (status === "failed") return "失败"
  return "排队中"
}

function statusVariant(status: string): "secondary" | "destructive" | "outline" {
  if (status === "failed") return "destructive"
  if (status === "succeeded") return "secondary"
  return "outline"
}

function statusIcon(status: string) {
  if (status === "succeeded") return <CheckCircle2 className="size-4 text-emerald-600" />
  if (status === "failed") return <AlertCircle className="size-4 text-destructive" />
  if (status === "running") return <Loader2 className="size-4 animate-spin text-primary" />
  return <Clock3 className="size-4 text-muted-foreground" />
}

function MobileProcessCard({ item, onOpen }: { item: MediaProcessItem; onOpen: (item: MediaProcessItem) => void }) {
  return (
    <button
      type="button"
      onClick={() => onOpen(item)}
      className="w-full border-b p-4 text-left transition-colors last:border-b-0 hover:bg-muted/40 focus-visible:bg-muted/60 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-ring"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 gap-3">
          <div className="mt-0.5">{statusIcon(item.status)}</div>
          <div className="min-w-0">
            <div className="truncate font-medium text-foreground">{item.title || item.fileName || "未命名视频"}</div>
            <div className="mt-1 truncate text-xs text-muted-foreground">{item.sourceUrl}</div>
          </div>
        </div>
        <Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge>
      </div>
      <div className="mt-4 flex items-center gap-3">
        <Progress value={item.progress ?? 0} className="h-1.5 flex-1" />
        <span className="w-10 text-right text-xs tabular-nums text-muted-foreground">{item.progress ?? 0}%</span>
      </div>
      <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
        <span>{phaseLabels[item.phase] || item.phase}</span>
        <span>{formatDate(item.createdAt)}</span>
      </div>
    </button>
  )
}

function DetailSection({
  title,
  value,
  copied,
  onCopy,
}: {
  title: string
  value?: string | null
  copied: boolean
  onCopy: () => void
}) {
  if (!value) return null
  return (
    <section className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-xs font-semibold text-muted-foreground">{title}</h3>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="shrink-0 text-muted-foreground hover:text-foreground"
          onClick={onCopy}
          title={copied ? "已复制" : `复制${title}`}
          aria-label={copied ? `${title}已复制` : `复制${title}`}
        >
          {copied ? <Check className="size-4 text-emerald-600" /> : <Copy className="size-4" />}
        </Button>
      </div>
      <div className="select-text whitespace-pre-wrap break-words rounded-md bg-muted/45 p-3 text-sm leading-6 text-foreground" style={{ userSelect: "text" }}>{value}</div>
    </section>
  )
}

function ProcessDetail({ item, onRetry, retrying }: { item: MediaProcessItem; onRetry: () => void; retrying: boolean }) {
  const [errorCopied, setErrorCopied] = useState(false)
  const [copiedSection, setCopiedSection] = useState<string | null>(null)

  const copySection = async (title: string, value?: string | null) => {
    if (!value) return
    try {
      await navigator.clipboard.writeText(value)
      setCopiedSection(title)
      notification.success({ title: "已复制", description: `${title}内容已复制到剪贴板` })
      window.setTimeout(() => setCopiedSection((current) => current === title ? null : current), 1600)
    } catch {
      notification.danger({ title: "复制失败", description: "当前环境不允许访问剪贴板" })
    }
  }

  const copyError = async () => {
    if (!item.errorMessage) return
    try {
      await navigator.clipboard.writeText(item.errorMessage)
      setErrorCopied(true)
      notification.success({ title: "已复制错误信息", description: "错误详情已复制到剪贴板" })
      window.setTimeout(() => setErrorCopied(false), 1600)
    } catch {
      notification.danger({ title: "复制失败", description: "当前环境不允许访问剪贴板" })
    }
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <DrawerHeader className="border-b px-4 py-4 text-left sm:px-6">
        <div className="flex items-start justify-between gap-4 pr-8">
          <div className="min-w-0">
            <DrawerTitle className="truncate text-base">{item.title || item.fileName || "未命名视频"}</DrawerTitle>
            <DrawerDescription className="mt-1 truncate">{phaseLabels[item.phase] || item.phase} · {formatDate(item.updatedAt)}</DrawerDescription>
          </div>
          <Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge>
        </div>
      </DrawerHeader>
      <div className="min-h-0 flex-1 select-text space-y-5 overflow-y-auto px-4 py-5 sm:px-6" style={{ userSelect: "text" }}>
        <div className="rounded-md bg-muted/50 p-4">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium text-foreground">{phaseLabels[item.phase] || item.phase}</span>
            <span className="tabular-nums text-muted-foreground">{item.progress ?? 0}%</span>
          </div>
          <Progress value={item.progress ?? 0} className="mt-3 h-2" />
          <div className="mt-3 grid grid-cols-2 gap-3 text-xs text-muted-foreground">
            <div>创建<br /><span className="text-foreground">{formatDate(item.createdAt)}</span></div>
            <div>完成<br /><span className="text-foreground">{formatDate(item.completedAt)}</span></div>
          </div>
        </div>
        <div className="space-y-3 rounded-md border p-4 text-sm">
          <div><span className="text-muted-foreground">来源</span><div className="mt-1 break-all text-foreground">{item.sourceUrl}</div></div>
          {item.downloadUrl && <div><span className="text-muted-foreground">实际下载地址</span><div className="mt-1 break-all text-xs text-foreground">{item.downloadUrl}</div></div>}
          <div className="grid grid-cols-2 gap-3"><div><span className="text-muted-foreground">平台</span><div className="mt-1 text-foreground">{item.platform || "--"}</div></div><div><span className="text-muted-foreground">尝试次数</span><div className="mt-1 text-foreground">{item.attemptCount ?? 0}</div></div></div>
          {item.historyId && <div><span className="text-muted-foreground">History ID</span><div className="mt-1 font-mono text-xs text-foreground">{item.historyId}</div></div>}
        </div>
        {item.errorMessage && (
          <div className="flex max-w-full items-start gap-3 rounded-md bg-destructive/10 p-3 text-xs leading-5 text-destructive">
            <p className="min-w-0 flex-1 break-all">{item.errorMessage}</p>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              className="-mr-1 -mt-1 shrink-0 text-destructive hover:bg-destructive/10 hover:text-destructive"
              onClick={() => void copyError()}
              title={errorCopied ? "已复制" : "复制错误信息"}
              aria-label={errorCopied ? "已复制错误信息" : "复制错误信息"}
            >
              {errorCopied ? <Check className="size-4" /> : <Copy className="size-4" />}
            </Button>
          </div>
        )}
        <DetailSection title="AI Summary" value={item.summary} copied={copiedSection === "AI Summary"} onCopy={() => void copySection("AI Summary", item.summary)} />
        <DetailSection title="Polished" value={item.polished} copied={copiedSection === "Polished"} onCopy={() => void copySection("Polished", item.polished)} />
        <DetailSection title="Transcript" value={item.transcript} copied={copiedSection === "Transcript"} onCopy={() => void copySection("Transcript", item.transcript)} />
      </div>
      <DrawerFooter className="border-t bg-background px-4 py-3 sm:px-6">
        <div className="flex w-full items-center justify-between gap-3">
          <span className="text-xs text-muted-foreground">{item.historyId ? "已写入 MP4 History" : "处理结果会自动写入 History"}</span>
          {item.status === "failed" && <Button onClick={onRetry} disabled={retrying}><RotateCcw className="size-4" />{retrying ? "重试中" : "重试"}</Button>}
        </div>
      </DrawerFooter>
    </div>
  )
}

export default function Mp4ProcessHistoryPage() {
  const isMobile = useIsMobile()
  const [items, setItems] = useState<MediaProcessItem[]>([])
  const [selected, setSelected] = useState<MediaProcessItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [retrying, setRetrying] = useState(false)
  const [error, setError] = useState("")
  const selectedId = selected?.id

  const load = useCallback(async (background = false) => {
    if (background) setRefreshing(true)
    else setLoading(true)
    setError("")
    try {
      const next = await listMediaProcesses()
      setItems(next)
      if (selectedId) {
        const latest = next.find((item) => item.id === selectedId)
        if (latest) setSelected(latest)
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "加载自动化处理记录失败"
      setError(message)
      if (!background) notification.danger({ title: "加载失败", description: message })
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [selectedId])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  useEffect(() => {
    if (!items.some((item) => item.status === "queued" || item.status === "running")) return
    const timer = window.setInterval(() => { void load(true) }, 3000)
    return () => window.clearInterval(timer)
  }, [items, load])

  const stats = useMemo(() => ({
    active: items.filter((item) => item.status === "queued" || item.status === "running").length,
    succeeded: items.filter((item) => item.status === "succeeded").length,
    failed: items.filter((item) => item.status === "failed").length,
  }), [items])

  const openDetail = useCallback(async (item: MediaProcessItem) => {
    setSelected(item)
    try { setSelected(await getMediaProcess(item.id)) } catch { /* list payload is still usable */ }
  }, [])

  const handleRetry = useCallback(async () => {
    if (!selected) return
    setRetrying(true)
    try {
      const next = await retryMediaProcess(selected.id)
      setSelected(next)
      setItems((current) => current.map((item) => item.id === next.id ? next : item))
      notification.success({ title: "已重新排队", description: "自动化处理将继续执行" })
    } catch (cause) {
      notification.danger({ title: "重试失败", description: cause instanceof Error ? cause.message : "重试失败" })
    } finally { setRetrying(false) }
  }, [selected])

  return (
    <WorkspaceShell sectionLabel="MP4 to Word" pageTitle="Process History" mobilePageTitle="process history">
      <div className="flex w-full flex-col gap-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground"><FileAudio className="size-4" />快捷指令自动化</div>
            <h1 className="text-2xl font-semibold text-foreground">Process History</h1>
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">视频链接进入下载、转写、AI 加工和历史入库后的全过程记录。</p>
          </div>
          <Button variant="outline" onClick={() => void load(true)} disabled={refreshing}>
            <RefreshCw className={refreshing ? "size-4 animate-spin" : "size-4"} />刷新
          </Button>
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          {[
            { label: "处理中", value: stats.active, icon: <Loader2 className="size-4" /> },
            { label: "已完成", value: stats.succeeded, icon: <CheckCircle2 className="size-4" /> },
            { label: "失败", value: stats.failed, icon: <AlertCircle className="size-4" /> },
          ].map((item) => (
            <div key={item.label} className="rounded-md bg-muted/60 px-4 py-3">
              <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">{item.icon}{item.label}</div>
              <div className="mt-2 font-mono text-2xl font-semibold tabular-nums text-foreground">{item.value}</div>
            </div>
          ))}
        </div>

        {error && <Alert variant="destructive"><AlertTitle>自动化处理记录加载失败</AlertTitle><AlertDescription>{error}</AlertDescription></Alert>}

        {loading ? (
          <div className="flex min-h-56 items-center justify-center rounded-md bg-muted/60 text-sm text-muted-foreground"><Loader2 className="mr-2 size-4 animate-spin" />正在读取记录</div>
        ) : (
          <>
            <div className="hidden sm:block"><ProcessHistoryTable items={items} onOpen={openDetail} /></div>
            <div className="overflow-hidden rounded-md bg-muted/50 sm:hidden">
              {items.length ? items.map((item) => <MobileProcessCard key={item.id} item={item} onOpen={openDetail} />) : <div className="flex min-h-56 items-center justify-center text-sm text-muted-foreground">还没有快捷指令处理记录</div>}
            </div>
          </>
        )}
      </div>

      <Drawer open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)} direction={isMobile ? "bottom" : "right"}>
        <DrawerContent
          className="select-text data-[vaul-drawer-direction=bottom]:h-[min(92dvh,760px)] data-[vaul-drawer-direction=bottom]:max-h-[92dvh] data-[vaul-drawer-direction=right]:h-full data-[vaul-drawer-direction=right]:rounded-none data-[vaul-drawer-direction=right]:!max-w-none"
          style={!isMobile ? { width: "33.3333333333vw", minWidth: "33.3333333333vw", maxWidth: "33.3333333333vw", userSelect: "text" } : { userSelect: "text" }}
        >
          {selected && <ProcessDetail item={selected} onRetry={() => void handleRetry()} retrying={retrying} />}
          <DrawerClose className="absolute right-3 top-3 z-10 inline-flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground" aria-label="关闭"><X className="size-4" /></DrawerClose>
        </DrawerContent>
      </Drawer>
    </WorkspaceShell>
  )
}
