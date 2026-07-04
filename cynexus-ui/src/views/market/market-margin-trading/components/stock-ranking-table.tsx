import { Fragment } from "react"
import { X } from "lucide-react"

import type { MarginExchange, MarginRankingSort, MarginSortDirection, MarginStockDetail } from "@/services/market/margin"
import { Button } from "@/components/ui/button"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatNumber, formatYuanAsYi } from "../lib/format"
import { SortableTableHead } from "./sortable-table-head"

const SORT_OPTIONS: Array<{ value: MarginRankingSort; label: string }> = [
  { value: "stock_code", label: "股票代码" },
  { value: "exchange", label: "交易所" },
  { value: "margin_balance", label: "两融余额" },
  { value: "financing_balance", label: "融资余额" },
  { value: "financing_buy_amount", label: "融资买入" },
  { value: "securities_lending_balance", label: "融券余量" },
  { value: "securities_lending_balance_amount", label: "融券余额" },
]

interface StockRankingTableProps {
  rows: MarginStockDetail[]
  count: number
  loading?: boolean
  page: number
  pageSize: number
  exchange: "ALL" | MarginExchange
  selectedIndustryCode?: string | null
  selectedIndustryName?: string | null
  sort: MarginRankingSort
  sortDir: MarginSortDirection
  onPageChange: (page: number) => void
  onExchangeChange: (value: "ALL" | MarginExchange) => void
  onSortChange: (value: MarginRankingSort, direction?: MarginSortDirection) => void
  onIndustryClear: () => void
  onRefresh: () => void
}

function getPageItems(page: number, totalPages: number) {
  const items = new Set([1, totalPages, page - 1, page, page + 1].filter((item) => item >= 1 && item <= totalPages))
  return [...items].sort((left, right) => left - right)
}

export function StockRankingTable({
  rows,
  count,
  loading,
  page,
  pageSize,
  exchange,
  selectedIndustryCode,
  selectedIndustryName,
  sort,
  sortDir,
  onPageChange,
  onExchangeChange,
  onSortChange,
  onIndustryClear,
  onRefresh,
}: StockRankingTableProps) {
  const totalPages = Math.max(1, Math.ceil(count / pageSize))
  const pageItems = getPageItems(page, totalPages)
  const handleSort = (column: MarginRankingSort) => {
    const nextDir: MarginSortDirection = sort === column && sortDir === "desc" ? "asc" : "desc"
    onSortChange(column, nextDir)
    onPageChange(1)
  }

  return (
    <section className="min-w-0 rounded-lg bg-muted/35 p-4">
      <div className="mb-3 space-y-2">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-base font-semibold">个股两融排名</h2>
          {selectedIndustryCode ? (
            <Button
              variant="secondary"
              size="sm"
              className="bg-destructive/10 text-destructive hover:bg-destructive/15"
              onClick={onIndustryClear}
            >
              <X className="size-3.5" />
              {selectedIndustryName ?? selectedIndustryCode} · 清除行业
            </Button>
          ) : null}
          <Select value={exchange} onValueChange={(value) => onExchangeChange(value as "ALL" | MarginExchange)}>
            <SelectTrigger size="sm" className="w-[116px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">全部</SelectItem>
              <SelectItem value="SSE">上交所</SelectItem>
              <SelectItem value="SZSE">深交所</SelectItem>
            </SelectContent>
          </Select>
          <Select value={sort} onValueChange={(value) => onSortChange(value as MarginRankingSort, "desc")}>
            <SelectTrigger size="sm" className="w-[132px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SORT_OPTIONS.map((item) => (
                <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button variant="secondary" size="sm" onClick={onRefresh} disabled={loading}>
            {loading ? "loading" : "reload"}
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">
          {selectedIndustryCode ? `THS ${selectedIndustryCode} 成分股；` : ""}
          共 {count.toLocaleString("zh-CN")} 条，当前第 {page} / {totalPages} 页。
        </p>
      </div>
      <div className="overflow-x-auto">
        <Table className="min-w-[820px]">
          <TableHeader>
            <TableRow>
              <TableHead className="w-12">#</TableHead>
              <TableHead>
                <SortableTableHead label="股票" column="stock_code" activeSort={sort} direction={sortDir} onSort={handleSort} />
              </TableHead>
              <TableHead>
                <SortableTableHead label="交易所" column="exchange" activeSort={sort} direction={sortDir} onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="两融余额" column="margin_balance" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="融资余额" column="financing_balance" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="融资买入" column="financing_buy_amount" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="融券余量" column="securities_lending_balance" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="融券余额" column="securities_lending_balance_amount" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, index) => (
              <TableRow key={`${row.exchange}-${row.stockCode}`}>
                <TableCell className="text-muted-foreground">{(page - 1) * pageSize + index + 1}</TableCell>
                <TableCell>
                  <div className="font-medium">{row.stockName ?? "-"}</div>
                  <div className="font-mono text-xs text-muted-foreground">{row.stockCode}</div>
                </TableCell>
                <TableCell>{row.exchange}</TableCell>
                <TableCell className="text-right font-medium tabular-nums">{formatYuanAsYi(row.marginBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.financingBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.financingBuyAmount, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatNumber(row.securitiesLendingBalance)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.securitiesLendingBalanceAmount, 2)}</TableCell>
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={8} className="h-24 text-center text-muted-foreground">
                  {loading ? "加载中" : "暂无个股两融数据"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <Pagination className="mt-3 justify-end">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious disabled={page <= 1 || loading} onClick={() => onPageChange(Math.max(1, page - 1))} />
          </PaginationItem>
          {pageItems.map((item, index) => {
            const previous = pageItems[index - 1]
            return (
              <Fragment key={item}>
                {previous != null && item - previous > 1 ? (
                  <PaginationItem>
                    <PaginationEllipsis />
                  </PaginationItem>
                ) : null}
                <PaginationItem>
                  <PaginationLink disabled={loading} isActive={item === page} onClick={() => onPageChange(item)}>
                    {item}
                  </PaginationLink>
                </PaginationItem>
              </Fragment>
            )
          })}
          <PaginationItem>
            <PaginationNext disabled={page >= totalPages || loading} onClick={() => onPageChange(Math.min(totalPages, page + 1))} />
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </section>
  )
}
