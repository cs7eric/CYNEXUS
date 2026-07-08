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
        <div className="[&_[data-slot=table-container]]:max-h-[62vh] [&_[data-slot=table-container]]:overflow-auto">
          <Table className="min-w-[1740px] table-fixed">
            <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
              <TableRow>
                <TableHead className="w-[120px]">Source</TableHead>
                <TableHead className="w-[240px]">Table</TableHead>
                <TableHead className="w-[360px]">业务逻辑 / 用途</TableHead>
                <TableHead className="w-[300px]">更新 Job / 数据源</TableHead>
                <TableHead className="w-[120px]">Status</TableHead>
                <TableHead className="w-[150px]">Latest Date</TableHead>
                <TableHead className="w-[110px] text-right">Lag</TableHead>
                <TableHead className="w-[120px] text-right">Rows</TableHead>
                <TableHead className="w-[160px]">Date Column</TableHead>
                <TableHead className="w-[180px]">Updated At</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={`${item.sourceType}:${item.schemaName}:${item.tableName}`}>
                  <TableCell>
                    <Badge variant="secondary" className="uppercase">
                      {item.sourceType}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="max-w-[220px]">
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
                  <TableCell className="whitespace-normal">
                    <div className="max-w-[330px] truncate text-sm leading-5 text-foreground" title={item.businessLogic || undefined}>
                      {item.businessLogic || "待补充业务说明"}
                    </div>
                    <div
                      className="mt-1 max-w-[330px] truncate text-xs leading-5 text-muted-foreground"
                      title={item.usedBy?.length ? item.usedBy.join(" / ") : undefined}
                    >
                      用到：{item.usedBy?.length ? item.usedBy.join(" / ") : "-"}
                    </div>
                  </TableCell>
                  <TableCell className="whitespace-normal">
                    <div className="max-w-[270px] truncate font-mono text-xs text-foreground" title={item.updateJob || undefined}>
                      {item.updateJob || "未配置自动 Job"}
                    </div>
                    <div className="mt-1 max-w-[270px] truncate text-xs text-muted-foreground" title={item.dataSource || undefined}>
                      源：{item.dataSource || "-"}
                    </div>
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
