import { Activity, AlertTriangle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { cn } from "@/lib/utils"
import type { DataSourceHealthItem } from "@/services/core/data-source-health"
import { formatDateTime, formatInteger } from "../lib/format"
import { HealthStatusBadge } from "./health-status-badge"

function readableIssue(message?: string | null) {
  if (!message) return null
  if (message.includes("IWENCAI_API_KEY")) return "未配置 IWENCAI_API_KEY"
  if (message.includes("Read timed out") || message.includes("ReadTimeout")) return "上游响应超时"
  if (message.includes("RemoteDisconnected")) return "上游断开连接"
  if (message.includes("Connection aborted")) return "连接被中断"
  return message
}

export function HealthTable({ items }: { items: DataSourceHealthItem[] }) {
  if (items.length === 0) {
    return (
      <Card className="border-0 bg-muted/60 shadow-none">
        <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
          <Activity className="size-8 text-muted-foreground" />
          <div className="text-sm font-medium text-foreground">没有匹配的数据源</div>
          <p className="max-w-md text-sm leading-6 text-muted-foreground">调整来源、状态或搜索条件后再查看。</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="overflow-hidden border-0 bg-muted/60 py-0 shadow-none">
      <CardContent className="p-0">
        <div className="max-h-[62vh] overflow-auto">
          <Table>
            <TableHeader className="sticky top-0 z-10 bg-muted">
              <TableRow>
                <TableHead className="min-w-[220px]">Data Source</TableHead>
                <TableHead className="min-w-[140px]">Status</TableHead>
                <TableHead className="min-w-[120px]">Provider</TableHead>
                <TableHead className="min-w-[130px] text-right">Latency</TableHead>
                <TableHead className="min-w-[130px] text-right">24h Success</TableHead>
                <TableHead className="min-w-[120px] text-right">Samples</TableHead>
                <TableHead className="min-w-[180px]">Authenticity</TableHead>
                <TableHead className="min-w-[260px]">Endpoint</TableHead>
                <TableHead className="min-w-[180px]">Checked At</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={`${item.sourceCode}:${item.checkedAt || ""}`}>
                  <TableCell>
                    <div className="max-w-[260px]">
                      <div className="truncate font-medium text-foreground">{item.sourceName}</div>
                      <div className="truncate font-mono text-xs text-muted-foreground">{item.sourceCode}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <HealthStatusBadge status={item.status} />
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1">
                      <Badge variant="secondary" className="w-fit uppercase">
                        {item.provider}
                      </Badge>
                      <span className="text-xs text-muted-foreground">{item.category}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">
                    {item.latencyMs === null || item.latencyMs === undefined ? "-" : `${formatInteger(item.latencyMs)}ms`}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">
                    {item.successRate24h === null || item.successRate24h === undefined
                      ? "-"
                      : `${Number(item.successRate24h).toFixed(1)}%`}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">{formatInteger(item.sampleCount)}</TableCell>
                  <TableCell>
                    <div className="text-sm text-foreground">
                      {item.connectivity ? "可连通" : "未连通"} · {item.authenticityStatus || "unknown"}
                    </div>
                    <div className="mt-1 text-xs text-muted-foreground">类型：{item.sourceType}</div>
                    {item.errorMessage ? (
                      <div
                        className={cn(
                          "mt-2 inline-flex max-w-[260px] items-center gap-1.5 rounded-md px-2 py-1 text-xs",
                          item.status === "failed"
                            ? "bg-destructive/10 text-destructive"
                            : "bg-muted text-muted-foreground",
                        )}
                        title={item.errorMessage}
                      >
                        <AlertTriangle className="size-3 shrink-0" />
                        <span className="truncate">{readableIssue(item.errorMessage)}</span>
                      </div>
                    ) : null}
                  </TableCell>
                  <TableCell>
                    <div className="max-w-[320px] truncate font-mono text-xs text-muted-foreground">
                      {item.endpoint || "-"}
                    </div>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">{formatDateTime(item.checkedAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  )
}
