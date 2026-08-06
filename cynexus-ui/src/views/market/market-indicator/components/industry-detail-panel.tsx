import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, GitBranch, Link2 } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { MarketIndicatorIndustry, MarketIndicatorIndustryDetail, MarketIndicatorIndustryRelation, MarketIndicatorSnapshot } from "@/services/market/indicator"

function number(value?: number | null, digits = 2) {
  return value == null || Number.isNaN(value) ? "-" : value.toFixed(digits)
}

function count(value?: number | null) {
  return value == null ? "-" : value.toLocaleString("zh-CN")
}

function signed(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "-"
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}%`
}

function time(value?: string | null) {
  return value ? new Date(value).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }) : "-"
}

function directionTone(value?: number | null) {
  return (value ?? 0) >= 0 ? "text-rose-600 dark:text-rose-400" : "text-emerald-600 dark:text-emerald-400"
}

function statusLabel(status?: string | null) {
  return { limit_up_touch: "触板", limit_up_sealed: "涨停", limit_down_touch: "触板", limit_down_sealed: "跌停" }[status ?? ""] ?? "正常"
}

function constituentRow(row: MarketIndicatorSnapshot) {
  const rising = (row.changePct ?? 0) >= 0
  return <TableRow key={row.code}>
    <TableCell>
      <div className="min-w-0">
        <div className="truncate font-medium">{row.name || row.code}</div>
        <div className="mt-0.5 truncate font-mono text-[11px] text-muted-foreground">{row.code} · {row.industry || "未分类"}</div>
      </div>
    </TableCell>
    <TableCell className="text-right font-mono tabular-nums">{number(row.lastPrice)}</TableCell>
    <TableCell className={`text-right font-mono tabular-nums ${directionTone(row.changePct)}`}>
      <span className="inline-flex items-center gap-1">{rising ? <ArrowUp className="size-3" /> : <ArrowDown className="size-3" />}{signed(row.changePct)}</span>
    </TableCell>
    <TableCell className="text-right font-mono text-xs tabular-nums text-muted-foreground">{number(row.lowPrice)} - {number(row.highPrice)}</TableCell>
    <TableCell className="text-right font-mono text-xs tabular-nums">{number(row.turnoverRate)}%</TableCell>
    <TableCell className="text-right"><Badge variant="outline" className="text-[10px]">{statusLabel(row.limitStatus)}</Badge></TableCell>
  </TableRow>
}

function relationRow(relation: MarketIndicatorIndustryRelation, onSelect?: (relation: MarketIndicatorIndustryRelation) => void) {
  const stateTone = relation.relationState === "同向联动" ? "text-rose-600 dark:text-rose-400" : relation.relationState === "反向分化" ? "text-amber-600 dark:text-amber-400" : "text-muted-foreground"
  return <button key={relation.industryCode} type="button" onClick={() => onSelect?.(relation)} className="group flex w-full items-start gap-3 border-b border-border/50 px-4 py-3 text-left transition-colors last:border-0 hover:bg-muted/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset">
    <span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-md bg-muted/60 text-information"><Link2 className="size-3.5" /></span>
    <span className="min-w-0 flex-1">
      <span className="flex items-center justify-between gap-2"><span className="truncate text-sm font-medium">{relation.industryName}</span><span className="shrink-0 font-mono text-xs tabular-nums text-foreground">{number(relation.relationScore, 1)}</span></span>
      <span className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-muted-foreground"><span className={stateTone}>{relation.relationState || "方向未明"}</span><span>映射与方向线索</span><span>目标池内 {count(relation.targetStockCount)} 只</span></span>
      <span className="mt-1 block text-[11px] text-muted-foreground">板块平均 <span className={`font-mono ${directionTone(relation.averageChangePct)}`}>{signed(relation.averageChangePct)}</span></span>
    </span>
  </button>
}

export function IndustryDetailPanel({
  industry,
  detail,
  loading,
  onPageChange,
  onSelectRelation,
}: {
  industry?: MarketIndicatorIndustry | null
  detail?: MarketIndicatorIndustryDetail | null
  loading: boolean
  onPageChange: (page: number) => void
  onSelectRelation?: (relation: MarketIndicatorIndustryRelation) => void
}) {
  const name = detail?.industryName || industry?.industryName || "选择一个行业"
  const total = detail?.totalCount ?? industry?.stockCount ?? 0
  const page = detail?.page ?? 0
  const pageSize = detail?.pageSize ?? 50
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const average = detail?.averageChangePct ?? null

  return <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="行业成分与关联板块">
    <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-3 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0">
        <div className="flex items-center gap-2 text-xs text-muted-foreground"><GitBranch className="size-3.5 text-information" /><span>行业下钻</span><span className="text-border">/</span><span>关联线索</span></div>
        <div className="mt-1 flex items-center gap-2"><h2 className="truncate text-sm font-semibold">{name}</h2>{detail?.categoryLabel ? <Badge variant="secondary" className="shrink-0 text-[10px]">{detail.categoryLabel}</Badge> : null}</div>
        <p className="mt-1 text-xs text-muted-foreground">按 TDX56-THS90 映射与池内涨跌方向计算关联线索；同向联动不代表确定因果，也不表示两行业共享成分股。</p>
      </div>
      <div className="flex shrink-0 items-center gap-4 text-right">
        <div><div className="font-mono text-base font-semibold tabular-nums">{count(total)}</div><div className="text-[11px] text-muted-foreground">池内成分股</div></div>
        <div><div className={`font-mono text-base font-semibold tabular-nums ${directionTone(average)}`}>{signed(average)}</div><div className="text-[11px] text-muted-foreground">平均涨跌</div></div>
      </div>
    </div>
    {loading && !detail ? <div className="grid gap-3 p-4 sm:grid-cols-3"><Skeleton className="h-8" /><Skeleton className="h-8" /><Skeleton className="h-8" /><Skeleton className="h-40 sm:col-span-2" /><Skeleton className="h-40" /></div> : !detail ? <div className="px-4 py-14 text-center text-xs text-muted-foreground">选择行业后查看成分股与关联板块。</div> : <div className="grid min-w-0 lg:grid-cols-[minmax(0,1fr)_300px] lg:divide-x lg:divide-border/60">
      <div className="min-w-0">
        <div className="flex items-center justify-between border-b border-border/50 px-4 py-2.5"><div><h3 className="text-xs font-semibold">池内成分股</h3><p className="mt-0.5 text-[11px] text-muted-foreground">按绝对涨跌幅排序 · 快照 {time(detail.capturedAt)}</p></div><span className="font-mono text-[11px] text-muted-foreground">{count(detail.constituents.length)} / {count(total)}</span></div>
        <div className="max-h-[430px] overflow-auto"><Table className="min-w-[700px]"><TableHeader><TableRow className="bg-muted/20 hover:bg-muted/20"><TableHead>股票</TableHead><TableHead className="text-right">最新</TableHead><TableHead className="text-right">涨跌幅</TableHead><TableHead className="text-right">日内区间</TableHead><TableHead className="text-right">换手</TableHead><TableHead className="text-right">状态</TableHead></TableRow></TableHeader><TableBody>{detail.constituents.length ? detail.constituents.map(constituentRow) : <TableRow><TableCell colSpan={6} className="h-32 text-center text-xs text-muted-foreground">当前行业没有可用成分股快照。</TableCell></TableRow>}</TableBody></Table></div>
        <div className="flex items-center justify-between border-t border-border/50 px-4 py-2"><span className="text-[11px] text-muted-foreground">第 {page + 1} / {totalPages} 页</span><div className="flex items-center gap-1"><Button type="button" variant="ghost" size="icon-sm" onClick={() => onPageChange(page - 1)} disabled={page <= 0 || loading} aria-label="上一页" title="上一页"><ChevronLeft /></Button><Button type="button" variant="ghost" size="icon-sm" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages || loading} aria-label="下一页" title="下一页"><ChevronRight /></Button></div></div>
      </div>
      <aside className="min-w-0"><div className="flex items-center justify-between border-b border-border/50 px-4 py-2.5"><div><h3 className="text-xs font-semibold">关联板块</h3><p className="mt-0.5 text-[11px] text-muted-foreground">按 TDX56 共同映射与涨跌方向排序</p></div><span className="font-mono text-[11px] text-muted-foreground">关联度 / 100</span></div><div className="max-h-[500px] overflow-auto">{detail.relations.length ? detail.relations.map((relation) => relationRow(relation, onSelectRelation)) : <div className="px-4 py-12 text-center text-xs text-muted-foreground">暂未发现可用的行业映射关系。</div>}</div></aside>
    </div>}
  </section>
}
