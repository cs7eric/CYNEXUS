import { ArrowDown, ArrowUp, ChevronRight, Database } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { FuturesPositionInstrument } from "@/services/market/futures-position"
import { formatPosition, formatRatio, formatSigned, toneForValue } from "../lib/format"

interface InstrumentTableProps {
  rows: FuturesPositionInstrument[]
  loading: boolean
  scopeType: "CONTRACT" | "VARIETY"
  selectedCode?: string | null
  onSelect: (row: FuturesPositionInstrument) => void
}

function DirectionValue({ value }: { value?: number | null }) {
  const Icon = value != null && value > 0 ? ArrowUp : value != null && value < 0 ? ArrowDown : null
  return <span className={`inline-flex items-center justify-end gap-1 font-mono tabular-nums ${toneForValue(value)}`}>{Icon ? <Icon className="size-3" /> : null}{formatSigned(value)}</span>
}

export function InstrumentTable({ rows, loading, scopeType, selectedCode, onSelect }: InstrumentTableProps) {
  return (
    <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="期货标的排名">
      <div className="flex flex-col gap-2 border-b border-border/60 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-sm font-semibold">{scopeType === "CONTRACT" ? "合约排名" : "品种排名"}</h2>
          <p className="mt-1 text-xs text-muted-foreground">点击标的查看独立的成交量、多头和空头会员排名。</p>
        </div>
        <Badge variant="secondary" className="w-fit gap-1 font-normal"><Database className="size-3" />会员披露快照</Badge>
      </div>
      <Table className="min-w-[760px]">
        <TableHeader><TableRow className="bg-muted/25 hover:bg-muted/25">
          <TableHead className="w-[190px]">标的</TableHead>
          <TableHead className="text-right">成交量</TableHead>
          <TableHead className="text-right text-rose-600 dark:text-rose-400">多头持仓</TableHead>
          <TableHead className="text-right text-emerald-600 dark:text-emerald-400">空头持仓</TableHead>
          <TableHead className="text-right">结构净值</TableHead>
          <TableHead className="text-right">多空比</TableHead>
          <TableHead className="w-[48px]" />
        </TableRow></TableHeader>
        <TableBody>
          {loading ? Array.from({ length: 6 }, (_, index) => <TableRow key={index}><TableCell><Skeleton className="h-8 w-32" /></TableCell>{Array.from({ length: 6 }, (_, cell) => <TableCell key={cell}><Skeleton className="ml-auto h-4 w-20" /></TableCell>)}</TableRow>) : rows.length > 0 ? rows.map((row) => {
            const selected = selectedCode === row.instrumentCode
            return <TableRow key={row.instrumentCode} data-state={selected ? "selected" : undefined} className={selected ? "bg-muted/60" : undefined}>
              <TableCell>
                <button type="button" onClick={() => onSelect(row)} className="group flex min-w-0 items-center gap-2 rounded-md text-left outline-none focus-visible:ring-2 focus-visible:ring-ring">
                  <span className="grid size-7 shrink-0 place-items-center rounded-md bg-muted text-[10px] font-semibold text-muted-foreground">{row.varietyCode?.slice(0, 2) || "期"}</span>
                  <span className="min-w-0"><span className="block truncate font-mono text-sm font-semibold group-hover:text-primary">{row.instrumentCode}</span><span className="block truncate text-[11px] text-muted-foreground">{row.contractCode && row.contractCode !== row.instrumentCode ? row.contractCode : row.varietyCode || "-"}</span></span>
                </button>
              </TableCell>
              <TableCell className="text-right font-mono tabular-nums">{formatPosition(row.volume)}</TableCell>
              <TableCell className="text-right"><div className="font-mono tabular-nums text-rose-600 dark:text-rose-400">{formatPosition(row.longPosition)}</div><div className="text-[11px] text-muted-foreground"><DirectionValue value={row.longChange} /></div></TableCell>
              <TableCell className="text-right"><div className="font-mono tabular-nums text-emerald-600 dark:text-emerald-400">{formatPosition(row.shortPosition)}</div><div className="text-[11px] text-muted-foreground"><DirectionValue value={row.shortChange} /></div></TableCell>
              <TableCell className={`text-right font-mono tabular-nums ${toneForValue(row.netPosition)}`}>{formatSigned(row.netPosition)}</TableCell>
              <TableCell className="text-right font-mono tabular-nums">{formatRatio(row.longShortRatio)}</TableCell>
              <TableCell className="text-right"><ChevronRight className="ml-auto size-4 text-muted-foreground" /></TableCell>
            </TableRow>
          }) : <TableRow><TableCell colSpan={7} className="h-36 text-center"><div className="text-sm text-muted-foreground">当前筛选没有可用排名</div><div className="mt-1 text-xs text-muted-foreground">可尝试切换交易所、范围或交易日。</div></TableCell></TableRow>}
        </TableBody>
      </Table>
    </section>
  )
}
