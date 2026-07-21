import { useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, ExternalLink, FileAudio, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Progress } from "@/components/ui/progress"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import type { MediaProcessItem } from "@/services/core/mp4-process-history"

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

function ProcessStatusIcon({ status }: { status: string }) {
  if (status === "succeeded") return <span className="size-2 rounded-full bg-emerald-500" />
  if (status === "failed") return <span className="size-2 rounded-full bg-destructive" />
  if (status === "running") return <span className="size-2 animate-pulse rounded-full bg-primary" />
  return <span className="size-2 rounded-full bg-muted-foreground/50" />
}

export function ProcessHistoryTable({
  items,
  onOpen,
}: {
  items: MediaProcessItem[]
  onOpen: (item: MediaProcessItem) => void
}) {
  const [query, setQuery] = useState("")
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  const filteredItems = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    if (!keyword) return items
    return items.filter((item) => [
      item.title,
      item.fileName,
      item.sourceUrl,
      item.status,
      item.phase,
      phaseLabels[item.phase],
    ].filter(Boolean).join(" ").toLowerCase().includes(keyword))
  }, [items, query])

  const pageCount = Math.max(1, Math.ceil(filteredItems.length / pageSize))
  const safePageIndex = Math.min(pageIndex, pageCount - 1)
  const visibleItems = filteredItems.slice(safePageIndex * pageSize, (safePageIndex + 1) * pageSize)

  useEffect(() => {
    setPageIndex(0)
  }, [query, pageSize])

  useEffect(() => {
    if (pageIndex !== safePageIndex) setPageIndex(safePageIndex)
  }, [pageIndex, safePageIndex])

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="relative w-full xl:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="搜索标题、文件名、来源地址"
            className="pl-9"
          />
        </div>
        <span className="text-sm text-muted-foreground">共 {filteredItems.length} / {items.length} 条</span>
      </div>

      <div className="max-w-full overflow-hidden rounded-md bg-muted/50 [&_[data-slot=table-container]]:max-h-[66vh] [&_[data-slot=table-container]]:overflow-auto [&_[data-slot=table-container]]:[scrollbar-width:thin]">
        <Table className="min-w-[900px] table-fixed">
          <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
            <TableRow className="border-b hover:bg-transparent">
              <TableHead className="w-[420px] text-xs font-semibold text-muted-foreground">任务</TableHead>
              <TableHead className="w-[210px] text-xs font-semibold text-muted-foreground">阶段</TableHead>
              <TableHead className="w-[130px] text-xs font-semibold text-muted-foreground">状态</TableHead>
              <TableHead className="w-[190px] text-xs font-semibold text-muted-foreground">创建时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visibleItems.length ? visibleItems.map((item) => (
              <TableRow
                key={item.id}
                tabIndex={0}
                className="cursor-pointer focus-visible:bg-muted/60 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-ring"
                onClick={() => onOpen(item)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault()
                    onOpen(item)
                  }
                }}
                aria-label={`查看 ${item.title || item.fileName || "未命名视频"} 处理详情`}
              >
                <TableCell className="whitespace-normal">
                  <div className="flex min-w-0 items-start gap-3">
                    <div className="mt-2 flex size-7 shrink-0 items-center justify-center rounded-md bg-background text-muted-foreground">
                      <FileAudio className="size-4" />
                    </div>
                    <div className="min-w-0 space-y-1">
                      <div className="flex min-w-0 items-center gap-2">
                        <span className="truncate font-medium text-foreground">{item.title || item.fileName || "未命名视频"}</span>
                        <ExternalLink className="size-3 shrink-0 text-muted-foreground" />
                      </div>
                      <div className="truncate text-xs text-muted-foreground" title={item.sourceUrl}>{item.sourceUrl}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-3 text-xs">
                      <span className="text-muted-foreground">{phaseLabels[item.phase] || item.phase}</span>
                      <span className="tabular-nums text-foreground">{item.progress ?? 0}%</span>
                    </div>
                    <Progress value={item.progress ?? 0} className="h-1.5" />
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant={statusVariant(item.status)} className="gap-2 font-normal">
                    <ProcessStatusIcon status={item.status} />
                    {statusLabel(item.status)}
                  </Badge>
                </TableCell>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">{formatDate(item.createdAt)}</TableCell>
              </TableRow>
            )) : (
              <TableRow>
                <TableCell colSpan={4} className="h-28 text-center text-muted-foreground">
                  {query ? "没有匹配的处理记录" : "还没有快捷指令处理记录"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex flex-col items-stretch justify-between gap-3 rounded-md bg-muted/50 px-3 py-3 sm:flex-row sm:items-center">
        <div className="hidden text-sm text-muted-foreground sm:block">{filteredItems.length} record(s)</div>
        <div className="flex items-center justify-between gap-3 sm:justify-start sm:gap-6">
          <div className="flex items-center gap-2">
            <Label htmlFor="process-history-rows-per-page" className="text-sm font-medium">Rows</Label>
            <Select value={`${pageSize}`} onValueChange={(value) => setPageSize(Number(value))}>
              <SelectTrigger id="process-history-rows-per-page" size="sm" className="w-20 rounded-md bg-background">
                <SelectValue />
              </SelectTrigger>
              <SelectContent side="top">
                {[10, 20, 30, 40].map((size) => <SelectItem key={size} value={`${size}`}>{size}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="whitespace-nowrap text-sm font-medium">Page {safePageIndex + 1} of {pageCount}</div>
          <div className="flex shrink-0 items-center gap-2">
            <Button variant="outline" size="icon" className="hidden size-8 rounded-md bg-background lg:flex" onClick={() => setPageIndex(0)} disabled={safePageIndex === 0} aria-label="第一页">
              <ChevronsLeft className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="size-8 rounded-md bg-background" onClick={() => setPageIndex((current) => Math.max(0, current - 1))} disabled={safePageIndex === 0} aria-label="上一页">
              <ChevronLeft className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="size-8 rounded-md bg-background" onClick={() => setPageIndex((current) => Math.min(pageCount - 1, current + 1))} disabled={safePageIndex >= pageCount - 1} aria-label="下一页">
              <ChevronRight className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="hidden size-8 rounded-md bg-background lg:flex" onClick={() => setPageIndex(pageCount - 1)} disabled={safePageIndex >= pageCount - 1} aria-label="最后一页">
              <ChevronsRight className="size-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
