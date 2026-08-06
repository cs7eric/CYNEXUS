import { useMemo, useState } from "react"
import { ArrowDownRight, ArrowUpRight, Flame, Gauge, Percent } from "lucide-react"

import type { MarketEtfRow } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatPct, formatWan } from "../lib/format"

type OpportunityMode = "active" | "inflow" | "premium" | "divergence"

const modes: Array<{ value: OpportunityMode; label: string; icon: typeof Flame }> = [
  { value: "active", label: "成交活跃", icon: Flame },
  { value: "inflow", label: "净流入", icon: ArrowUpRight },
  { value: "premium", label: "折溢价异常", icon: Percent },
  { value: "divergence", label: "份额背离", icon: Gauge },
]

function rankRows(rows: MarketEtfRow[], mode: OpportunityMode) {
  const filtered = mode === "inflow"
    ? rows.filter((row) => (row.mainNetInflow ?? row.shareDelta ?? 0) > 0)
    : mode === "premium"
      ? rows.filter((row) => Math.abs(row.premiumPct ?? 0) >= 0.3)
      : mode === "divergence"
        ? rows.filter((row) => ((row.shareDelta ?? 0) > 0 && (row.changePct ?? 0) < 0) || ((row.shareDelta ?? 0) < 0 && (row.changePct ?? 0) > 0))
        : rows
  return [...filtered].sort((a, b) => {
    if (mode === "active") return (b.amount ?? 0) - (a.amount ?? 0)
    if (mode === "premium") return Math.abs(b.premiumPct ?? 0) - Math.abs(a.premiumPct ?? 0)
    if (mode === "divergence") return Math.abs(b.shareDelta ?? 0) - Math.abs(a.shareDelta ?? 0)
    return (b.mainNetInflow ?? b.shareDelta ?? 0) - (a.mainNetInflow ?? a.shareDelta ?? 0)
  }).slice(0, 5)
}

export function EtfOpportunityPanel({ rows, onSelect }: { rows: MarketEtfRow[]; onSelect: (row: MarketEtfRow) => void }) {
  const [mode, setMode] = useState<OpportunityMode>("active")
  const ranked = useMemo(() => rankRows(rows, mode), [mode, rows])
  const maxValue = Math.max(...ranked.map((row) => mode === "active" ? Math.abs(row.amount ?? 0) : mode === "premium" ? Math.abs(row.premiumPct ?? 0) : Math.abs(row.mainNetInflow ?? row.shareDelta ?? 0)), 1)
  return (
    <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div><div className="flex items-center gap-2"><Gauge className="size-4 text-sky-600" /><h2 className="text-base font-semibold">机会筛选</h2></div><p className="mt-1 text-xs text-muted-foreground">把静态行情变成可行动的候选池，点击产品查看详情。</p></div>
        <div className="flex max-w-full gap-1 overflow-x-auto" role="tablist" aria-label="ETF 机会筛选"><span className="sr-only">机会筛选模式</span>{modes.map(({ value, label, icon: Icon }) => <button key={value} type="button" role="tab" aria-selected={mode === value} onClick={() => setMode(value)} className={cn("inline-flex shrink-0 items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", mode === value ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted hover:text-foreground")}><Icon className="size-3.5" />{label}</button>)}</div>
      </div>
      {ranked.length ? <div className="mt-4 grid gap-1.5">{ranked.map((row, index) => {
        const metric = mode === "active" ? row.amount : mode === "premium" ? row.premiumPct : mode === "divergence" ? row.shareDelta : row.mainNetInflow ?? row.shareDelta
        const positive = (metric ?? 0) >= 0
        const width = Math.min(100, Math.abs(metric ?? 0) / maxValue * 100)
        return <button key={row.code} type="button" onClick={() => onSelect(row)} className="group grid min-w-0 grid-cols-[20px_minmax(0,1fr)_auto] items-center gap-2 rounded-md px-2 py-2 text-left transition-colors hover:bg-muted/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"><span className="font-mono text-[11px] text-muted-foreground">{String(index + 1).padStart(2, "0")}</span><span className="min-w-0"><span className="flex min-w-0 items-center justify-between gap-3"><span className="truncate text-xs font-medium">{row.name || row.code}</span><span className="shrink-0 font-mono text-xs tabular-nums text-muted-foreground">{mode === "active" ? formatWan(row.amount) : mode === "premium" ? formatPct(row.premiumPct) : mode === "divergence" ? formatWan(row.shareDelta) : formatWan(row.mainNetInflow ?? row.shareDelta)}</span></span><span className="mt-1 block h-1 overflow-hidden rounded-full bg-muted"><span className={cn("block h-full rounded-full", positive ? "bg-rose-500/75" : "bg-emerald-500/75")} style={{ width: `${width}%` }} /></span></span><span className={cn("flex items-center gap-1 font-mono text-xs tabular-nums", (row.changePct ?? 0) > 0 ? "text-red-600" : (row.changePct ?? 0) < 0 ? "text-emerald-600" : "text-muted-foreground")}>{(row.changePct ?? 0) > 0 ? <ArrowUpRight className="size-3" /> : (row.changePct ?? 0) < 0 ? <ArrowDownRight className="size-3" /> : null}{formatPct(row.changePct)}</span></button>
      })}</div> : <div className="mt-4 rounded-md border border-dashed border-border/70 px-3 py-7 text-center text-xs text-muted-foreground">当前筛选没有满足条件的 ETF</div>}
    </section>
  )
}
