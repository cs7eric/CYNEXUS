import type { MarginMarketSummary } from "@/services/market/margin"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { formatDateTime, formatNumber, formatYi } from "../lib/format"

interface ExchangeSummaryTableProps {
  rows: MarginMarketSummary[]
}

const EXCHANGE_COPY: Record<string, { label: string; description: string }> = {
  CN: {
    label: "全市场",
    description: "沪深两市合计",
  },
  SSE: {
    label: "上交所",
    description: "上海证券交易所",
  },
  SZSE: {
    label: "深交所",
    description: "深圳证券交易所",
  },
}

const SOURCE_COPY: Record<string, string> = {
  "akshare.stock_margin_account_info": "融资融券账户统计",
  "akshare.stock_margin_sse": "上交所两融汇总",
  "akshare.stock_margin_szse": "深交所两融汇总",
}

function getExchangeCopy(exchange: string) {
  return EXCHANGE_COPY[exchange.toUpperCase()] ?? {
    label: exchange,
    description: "交易所披露口径",
  }
}

function getSourceLabel(source: string) {
  return SOURCE_COPY[source] ?? source
}

export function ExchangeSummaryTable({ rows }: ExchangeSummaryTableProps) {
  return (
    <section className="min-w-0 rounded-lg bg-muted/35 p-4">
      <div className="mb-3">
        <h2 className="text-base font-semibold">汇总范围拆分</h2>
        <p className="text-xs text-muted-foreground">
          CN 是全市场合计，SSE/SZSE 是交易所披露口径；这里用来对比两市贡献和校验数据源差异。
        </p>
      </div>
      <div className="overflow-x-auto">
        <Table className="min-w-[820px]">
          <TableHeader>
            <TableRow>
              <TableHead>范围</TableHead>
              <TableHead>采集接口</TableHead>
              <TableHead className="text-right">两融余额</TableHead>
              <TableHead className="text-right">融资余额</TableHead>
              <TableHead className="text-right">融资买入</TableHead>
              <TableHead className="text-right">融券余量</TableHead>
              <TableHead className="text-right">更新时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={`${row.tradeDate}-${row.exchange}-${row.source}`}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className="bg-background/70">{getExchangeCopy(row.exchange).label}</Badge>
                    <span className="text-xs text-muted-foreground">{row.exchange}</span>
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">{getExchangeCopy(row.exchange).description}</div>
                </TableCell>
                <TableCell className="max-w-[260px]">
                  <div className="truncate">{getSourceLabel(row.source)}</div>
                  <div className="truncate font-mono text-xs text-muted-foreground">{row.source}</div>
                </TableCell>
                <TableCell className="text-right font-medium tabular-nums">{formatYi(row.marginBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYi(row.financingBalance, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatYi(row.financingBuyAmount, 2)}</TableCell>
                <TableCell className="text-right tabular-nums">{formatNumber(row.securitiesLendingBalance)}</TableCell>
                <TableCell className="text-right text-muted-foreground">{formatDateTime(row.updatedAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </section>
  )
}
