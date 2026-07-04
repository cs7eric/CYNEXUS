import { AlertTriangle, ArrowDownUp } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
import type { DataSourceFreshnessItem } from "@/services/market/data-source"
import { formatDate, formatDateTime, formatInteger, freshnessHint } from "../lib/format"
import { StatusBadge } from "./status-badge"

export function FreshnessTable({ items }: { items: DataSourceFreshnessItem[] }) {
  if (items.length === 0) {
    return (
      <Card className="border-0 bg-muted/60 shadow-none">
        <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
          <ArrowDownUp className="size-8 text-muted-foreground" />
          <div className="text-sm font-medium text-foreground">没有匹配的数据表</div>
          <p className="max-w-md text-sm leading-6 text-muted-foreground">
            调整数据源、状态或搜索条件后再查看。
          </p>
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
                <TableHead className="min-w-[220px]">Table</TableHead>
                <TableHead className="min-w-[280px]">业务逻辑 / 用途</TableHead>
                <TableHead className="min-w-[240px]">更新 Job / 数据源</TableHead>
                <TableHead className="min-w-[108px]">Source</TableHead>
                <TableHead className="min-w-[120px]">Status</TableHead>
                <TableHead className="min-w-[140px]">Latest Date</TableHead>
                <TableHead className="min-w-[110px] text-right">Lag</TableHead>
                <TableHead className="min-w-[120px] text-right">Rows</TableHead>
                <TableHead className="min-w-[160px]">Date Column</TableHead>
                <TableHead className="min-w-[180px]">Updated At</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={`${item.sourceType}:${item.schemaName}:${item.tableName}`}>
                  <TableCell>
                    <div className="max-w-[280px]">
                      <div className="truncate font-medium text-foreground">{item.tableName}</div>
                      <div className="truncate text-xs text-muted-foreground">
                        {item.schemaName || item.databaseName || "-"}
                      </div>
                    </div>
                    {item.status === "error" ? (
                      <Alert variant="destructive" className="mt-2 py-2">
                        <AlertTriangle className="size-3.5" />
                        <AlertTitle className="text-xs">读取失败</AlertTitle>
                        <AlertDescription className="break-all text-xs">{item.error}</AlertDescription>
                      </Alert>
                    ) : null}
                  </TableCell>
                  <TableCell>
                    <div className="max-w-[320px] text-sm leading-5 text-foreground">
                      {item.businessLogic || "待补充业务说明"}
                    </div>
                    <div className="mt-1 max-w-[320px] text-xs leading-5 text-muted-foreground">
                      用到：{item.usedBy?.length ? item.usedBy.join(" / ") : "-"}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="max-w-[260px] truncate font-mono text-xs text-foreground">
                      {item.updateJob || "未配置自动 Job"}
                    </div>
                    <div className="mt-1 max-w-[260px] truncate text-xs text-muted-foreground">
                      源：{item.dataSource || "-"}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary" className="uppercase">
                      {item.sourceType}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={item.status} />
                  </TableCell>
                  <TableCell className="font-mono text-sm">{formatDate(item.latestEffectiveDate)}</TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {freshnessHint(item)}
                  </TableCell>
                  <TableCell className="text-right font-mono text-sm">{formatInteger(item.rowCount)}</TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">
                    {item.effectiveDateColumn || "-"}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDateTime(item.latestUpdatedAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  )
}
