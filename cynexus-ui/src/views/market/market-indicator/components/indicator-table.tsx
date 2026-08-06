import { ArrowDown, ArrowUp, LockKeyhole, UnlockKeyhole } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { MarketIndicatorSnapshot } from "@/services/market/indicator"
import type { IndicatorView } from "../index"

function number(value?: number | null, digits = 2) {
  return value == null || Number.isNaN(value) ? "-" : value.toFixed(digits)
}

function time(value?: string | null) {
  return value ? new Date(value).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" }) : "-"
}

function statusLabel(status?: string | null) {
  return { limit_up_touch: "触及涨停", limit_up_sealed: "涨停封板", limit_down_touch: "触及跌停", limit_down_sealed: "跌停封板" }[status ?? ""] ?? "正常"
}

function statusClass(status?: string | null) {
  return status?.startsWith("limit_up")
    ? "border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-900/60 dark:bg-rose-950/40 dark:text-rose-300"
    : status?.startsWith("limit_down")
      ? "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/40 dark:text-emerald-300"
      : "border-border bg-muted/40 text-muted-foreground"
}

function viewLabel(view: IndicatorView) {
  return { focus: "重点异动", all: "涨跌停池", limit_up: "涨停", limit_down: "跌停", rapid: "快速变化" }[view]
}

export function IndicatorTable({ rows, loading, view }: { rows: MarketIndicatorSnapshot[]; loading: boolean; view: IndicatorView }) {
  return <div className="overflow-x-auto"><Table className="min-w-[780px]"><TableHeader><TableRow className="bg-muted/25 hover:bg-muted/25"><TableHead className="w-[24%]">股票</TableHead><TableHead className="text-right">最新</TableHead><TableHead className="text-right">涨跌幅</TableHead><TableHead className="text-right">日内区间</TableHead><TableHead className="text-right">换手率</TableHead><TableHead>信号</TableHead><TableHead className="text-right">采集</TableHead></TableRow></TableHeader><TableBody>{loading ? Array.from({ length: 6 }, (_, index) => <TableRow key={`skeleton-${index}`}><TableCell><Skeleton className="h-4 w-28" /><Skeleton className="mt-1 h-3 w-20" /></TableCell><TableCell><Skeleton className="ml-auto h-4 w-14" /></TableCell><TableCell><Skeleton className="ml-auto h-4 w-16" /></TableCell><TableCell><Skeleton className="ml-auto h-4 w-24" /></TableCell><TableCell><Skeleton className="ml-auto h-4 w-14" /></TableCell><TableCell><Skeleton className="h-5 w-16" /></TableCell><TableCell><Skeleton className="ml-auto h-4 w-16" /></TableCell></TableRow>) : null}{!loading && rows.length === 0 ? <TableRow><TableCell colSpan={7} className="h-36 text-center"><div className="text-sm font-medium text-foreground">没有匹配的{viewLabel(view)}股票</div><div className="mt-1 text-xs text-muted-foreground">尝试切换信号范围或清空搜索条件。</div></TableCell></TableRow> : null}{!loading && rows.map((row) => { const positive = (row.changePct ?? 0) >= 0; const Icon = row.limitStatus?.includes("sealed") ? LockKeyhole : row.limitStatus?.includes("touch") ? UnlockKeyhole : positive ? ArrowUp : ArrowDown; return <TableRow key={row.code}><TableCell><div className="min-w-0"><div className="truncate font-medium">{row.name || row.code}</div><div className="mt-0.5 truncate font-mono text-[11px] text-muted-foreground">{row.code} · {row.industry || "未分类"}</div></div></TableCell><TableCell className="text-right font-mono tabular-nums">{number(row.lastPrice)}</TableCell><TableCell className={`text-right font-mono tabular-nums ${positive ? "text-rose-600 dark:text-rose-400" : "text-emerald-600 dark:text-emerald-400"}`}><span className="inline-flex items-center gap-1"><Icon className="size-3" />{number(row.changePct)}%</span></TableCell><TableCell className="text-right font-mono text-xs tabular-nums text-muted-foreground">{number(row.lowPrice)} - {number(row.highPrice)}</TableCell><TableCell className="text-right font-mono tabular-nums">{number(row.turnoverRate)}%</TableCell><TableCell><Badge variant="outline" className={statusClass(row.limitStatus)}>{statusLabel(row.limitStatus)}</Badge></TableCell><TableCell className="whitespace-nowrap text-right font-mono text-[11px] text-muted-foreground">{time(row.capturedAt)}</TableCell></TableRow> })}</TableBody></Table></div>
}
