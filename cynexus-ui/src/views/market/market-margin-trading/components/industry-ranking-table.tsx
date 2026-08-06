import type { MarginIndustrySummary, MarginRankingSort, MarginSortDirection } from "@/services/market/margin"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatNumber, formatYuanAsYi } from "../lib/format"
import { RankingPagination } from "./ranking-pagination"
import { SortableTableHead } from "./sortable-table-head"

const SORT_OPTIONS: Array<{ value: MarginRankingSort; label: string }> = [
  { value: "industry_code", label: "行业代码" },
  { value: "margin_balance", label: "两融余额" },
  { value: "financing_balance", label: "融资余额" },
  { value: "financing_buy_amount", label: "融资买入" },
  { value: "securities_lending_balance_amount", label: "融券余额" },
  { value: "matched_stock_count", label: "匹配股票" },
]

interface IndustryRankingTableProps {
  rows: MarginIndustrySummary[]
  count: number
  loading?: boolean
  page: number
  pageSize: number
  sort: MarginRankingSort
  sortDir: MarginSortDirection
  selectedIndustryCode?: string | null
  snapshotDate?: string | null
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  onSortChange: (value: MarginRankingSort, direction?: MarginSortDirection) => void
  onIndustrySelect: (row: MarginIndustrySummary) => void
  onRefresh: () => void
}

export function IndustryRankingTable({
  rows,
  count,
  loading,
  page,
  pageSize,
  sort,
  sortDir,
  selectedIndustryCode,
  snapshotDate,
  onPageChange,
  onPageSizeChange,
  onSortChange,
  onIndustrySelect,
  onRefresh,
}: IndustryRankingTableProps) {
  const totalPages = Math.max(1, Math.ceil(count / pageSize))
  const handleSort = (column: MarginRankingSort) => {
    const nextDir: MarginSortDirection = sort === column && sortDir === "desc" ? "asc" : "desc"
    onSortChange(column, nextDir)
    onPageChange(1)
  }

  return (
    <section className="min-w-0 rounded-lg bg-muted/35 p-4">
      <div className="mb-3 space-y-2">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-base font-semibold">行业两融排名</h2>
          <Select
            value={sort}
            onValueChange={(value) => {
              onSortChange(value as MarginRankingSort, "desc")
              onPageChange(1)
            }}
          >
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
        <p className="text-xs text-muted-foreground">THS 成分聚合，TDX 补名称；成分快照 {snapshotDate ?? "-"}。</p>
      </div>
      <div className="overflow-x-auto">
        <Table className="min-w-[880px]">
          <TableHeader>
            <TableRow>
              <TableHead className="w-12">#</TableHead>
              <TableHead>
                <SortableTableHead label="行业" column="industry_code" activeSort={sort} direction={sortDir} onSort={handleSort} />
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
                <SortableTableHead label="融券余额" column="securities_lending_balance_amount" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
              <TableHead className="text-right">
                <SortableTableHead label="匹配股票" column="matched_stock_count" activeSort={sort} direction={sortDir} align="right" onSort={handleSort} />
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, index) => (
              <TableRow
                key={row.industryCode}
                aria-selected={selectedIndustryCode === row.industryCode}
                className={cn(
                  "cursor-pointer",
                  selectedIndustryCode === row.industryCode && "bg-background hover:bg-background",
                )}
                onClick={() => onIndustrySelect(row)}
              >
                <TableCell className="text-muted-foreground">{(page - 1) * pageSize + index + 1}</TableCell>
                <TableCell>
                  <div className="font-medium">{row.industryName ?? row.tdxName ?? row.industryCode}</div>
                  <div className="font-mono text-xs text-muted-foreground">
                    THS {row.industryCode}{row.tdxCode ? ` · TDX ${row.tdxCode}` : ""}
                  </div>
                </TableCell>
                <TableCell className="text-right font-medium tabular-nums">{formatYuanAsYi(row.marginBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.financingBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.financingBuyAmount, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYuanAsYi(row.securitiesLendingBalanceAmount, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">
                  {formatNumber(row.matchedStockCount)} / {formatNumber(row.constituentCount)}
                </TableCell>
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center text-muted-foreground">
                  {loading ? "加载中" : "暂无行业两融数据"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <RankingPagination
        page={page}
        totalPages={totalPages}
        pageSize={pageSize}
        loading={loading}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
      />
    </section>
  )
}
