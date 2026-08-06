import { useCallback, useEffect, useState } from "react"
import { Image, RefreshCw, MessageSquareText, ChevronLeft, ChevronRight } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { cn } from "@/lib/utils"
import { fetchWechatEventHistory, type WechatEventHistory } from "@/services/wechat/history"

const PAGE_SIZE = 25

const MESSAGE_TYPE_LABELS: Record<string, string> = {
  image: "图片",
  text: "文本",
  event: "事件",
  voice: "语音",
  video: "视频",
  location: "位置",
  link: "链接",
}

const STATUS_LABELS: Record<string, string> = {
  RECEIVED: "已接收",
  ROUTING: "分类中",
  ROUTED: "已分类",
  ROUTING_FAILED: "分类失败",
  FAILED: "处理失败",
  COMPLETED: "已完成",
}

function label(value: string | null | undefined, labels: Record<string, string>): string {
  if (!value) return "—"
  return labels[value] || value
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false })
}

function formatConfidence(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") return "—"
  const number = Number(value)
  return Number.isFinite(number) ? `${(number * 100).toFixed(1)}%` : String(value)
}

function statusClass(status: string | null | undefined): string {
  if (status === "ROUTING_FAILED" || status === "FAILED") return "border-destructive/40 text-destructive"
  if (status === "ROUTED" || status === "COMPLETED") return "border-emerald-500/40 text-emerald-700 dark:text-emerald-300"
  return ""
}

function EventDetails({ item }: { item: WechatEventHistory }) {
  const details = [
    ["用户", item.fromUserName],
    ["事件", item.eventType],
    ["原始内容", item.content],
    ["回复", item.replyContent],
    ["原因", item.routeReason],
    ["错误", item.errorMessage],
  ].filter(([, value]) => value)

  if (!details.length && !item.routeSignals?.length && !item.routeWarnings?.length) return null

  return (
    <details className="mt-1 max-w-[620px] text-xs text-muted-foreground">
      <summary className="cursor-pointer select-none hover:text-foreground">查看详情</summary>
      <div className="mt-2 space-y-1 rounded-md bg-muted/50 p-2 leading-5">
        {details.map(([key, value]) => (
          <div key={key} className="grid grid-cols-[72px_minmax(0,1fr)] gap-2">
            <span className="text-muted-foreground">{key}</span>
            <span className="break-words whitespace-pre-wrap text-foreground">{value}</span>
          </div>
        ))}
        {item.routeSignals?.length ? <div>信号：{item.routeSignals.join("、")}</div> : null}
        {item.routeWarnings?.length ? <div className="text-amber-700 dark:text-amber-300">警告：{item.routeWarnings.join("、")}</div> : null}
      </div>
    </details>
  )
}

function HistoryTable({ items }: { items: WechatEventHistory[] }) {
  return (
    <div className="overflow-hidden rounded-md bg-muted/40 [&_[data-slot=table-container]]:max-h-[68vh] [&_[data-slot=table-container]]:overflow-auto">
      <Table className="min-w-[1050px]">
        <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-10 [&_th]:bg-muted">
          <TableRow>
            <TableHead className="w-[170px]">时间</TableHead>
            <TableHead className="w-[90px]">类型</TableHead>
            <TableHead className="w-[140px]">处理状态</TableHead>
            <TableHead className="w-[220px]">图片分类</TableHead>
            <TableHead className="w-[100px]">置信度</TableHead>
            <TableHead>事件</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.id}>
              <TableCell className="align-top font-mono text-xs text-muted-foreground">
                {formatDateTime(item.occurredAt)}
              </TableCell>
              <TableCell className="align-top">
                <div className="flex items-center gap-1.5 text-sm">
                  {item.messageType === "image" ? <Image className="size-3.5 text-primary" /> : <MessageSquareText className="size-3.5 text-muted-foreground" />}
                  {label(item.messageType, MESSAGE_TYPE_LABELS)}
                </div>
              </TableCell>
              <TableCell className="align-top">
                <Badge variant="outline" className={cn("whitespace-nowrap", statusClass(item.processingStatus))}>
                  {label(item.processingStatus, STATUS_LABELS)}
                </Badge>
              </TableCell>
              <TableCell className="align-top whitespace-normal">
                {item.routeCode ? (
                  <div>
                    <div className="font-mono text-xs font-medium text-foreground">{item.routeCode}</div>
                    <div className="mt-1 text-xs text-muted-foreground line-clamp-2" title={item.routeReason || undefined}>
                      {item.routeReason || "无分类原因"}
                    </div>
                  </div>
                ) : <span className="text-sm text-muted-foreground">—</span>}
              </TableCell>
              <TableCell className="align-top font-mono text-xs">{formatConfidence(item.routeConfidence)}</TableCell>
              <TableCell className="align-top whitespace-normal">
                <div className="max-w-[620px]">
                  <div className="line-clamp-2 text-sm text-foreground">{item.content || item.eventType || "微信事件"}</div>
                  <EventDetails item={item} />
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export default function WechatHistoryPage() {
  const [items, setItems] = useState<WechatEventHistory[]>([])
  const [total, setTotal] = useState(0)
  const [offset, setOffset] = useState(0)
  const [messageType, setMessageType] = useState("all")
  const [processingStatus, setProcessingStatus] = useState("all")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchWechatEventHistory({
        limit: PAGE_SIZE,
        offset,
        messageType: messageType === "all" ? undefined : messageType,
        processingStatus: processingStatus === "all" ? undefined : processingStatus,
      })
      setItems(result.items || [])
      setTotal(result.total || 0)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setLoading(false)
    }
  }, [messageType, offset, processingStatus])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const page = Math.floor(offset / PAGE_SIZE) + 1
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  function changeMessageType(value: string) {
    setMessageType(value)
    setOffset(0)
  }

  function changeStatus(value: string) {
    setProcessingStatus(value)
    setOffset(0)
  }

  return (
    <WorkspaceShell sectionLabel="Settings" pageTitle="微信历史事件">
      <div className="flex w-full flex-col gap-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <MessageSquareText className="size-4" />
              WeChat · Event history
            </div>
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
              查看公众号收到的消息、图片分类结果和后续处理状态。用户标识由后端脱敏，图片分类与业务 QC 保持独立。
            </p>
          </div>
          <Button variant="outline" onClick={() => void load()} disabled={loading}>
            <RefreshCw className={cn("size-4", loading && "animate-spin")} />
            刷新
          </Button>
        </div>

        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-border/70 bg-muted/25 px-3 py-3 sm:px-4">
          <span className="text-xs font-medium text-muted-foreground">筛选</span>
          <Select value={messageType} onValueChange={changeMessageType}>
            <SelectTrigger size="sm" className="w-[130px] bg-background"><SelectValue placeholder="消息类型" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部类型</SelectItem>
              <SelectItem value="image">图片</SelectItem>
              <SelectItem value="text">文本</SelectItem>
              <SelectItem value="event">事件</SelectItem>
            </SelectContent>
          </Select>
          <Select value={processingStatus} onValueChange={changeStatus}>
            <SelectTrigger size="sm" className="w-[140px] bg-background"><SelectValue placeholder="处理状态" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部状态</SelectItem>
              <SelectItem value="RECEIVED">已接收</SelectItem>
              <SelectItem value="ROUTING">分类中</SelectItem>
              <SelectItem value="ROUTED">已分类</SelectItem>
              <SelectItem value="ROUTING_FAILED">分类失败</SelectItem>
              <SelectItem value="COMPLETED">已完成</SelectItem>
            </SelectContent>
          </Select>
          <span className="ml-auto text-xs text-muted-foreground">共 {total} 条</span>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertTitle>微信历史事件加载失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        {loading ? (
          <Skeleton className="h-[360px] w-full" />
        ) : items.length ? (
          <HistoryTable items={items} />
        ) : (
          <div className="rounded-lg border border-dashed border-border px-4 py-12 text-center text-sm text-muted-foreground">
            暂无匹配的微信历史事件
          </div>
        )}

        <div className="flex items-center justify-between border-t border-border/60 pt-3 text-xs text-muted-foreground">
          <span>第 {page} / {totalPages} 页</span>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="sm" onClick={() => setOffset(Math.max(0, offset - PAGE_SIZE))} disabled={loading || offset === 0}>
              <ChevronLeft className="size-4" /> 上一页
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setOffset(offset + PAGE_SIZE)} disabled={loading || page >= totalPages}>
              下一页 <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      </div>
    </WorkspaceShell>
  )
}
