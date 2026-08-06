import { useMemo, useState } from "react"
import { Activity, ArrowDownRight, ArrowUpRight, BarChart3, Crosshair, Gauge, Layers3, Minus, Search } from "lucide-react"
import { Area, AreaChart, CartesianGrid, Line, XAxis, YAxis } from "recharts"

import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import type { MarketEtfRotation, MarketEtfRow, MarketEtfTrend } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatDate, formatNumber, formatPct, formatWan } from "../lib/format"

export type InsightTab = "opportunity" | "instrument" | "rotation" | "trend"
type OpportunityMode = "active" | "inflow" | "premium" | "divergence"

const chartConfig = {
  broad: { label: "宽基份额变化", color: "#0284c7" },
  sector: { label: "行业份额变化", color: "#16a34a" },
  premium: { label: "平均折溢价", color: "#d97706" },
} satisfies ChartConfig

function signedTone(value?: number | null) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-red-600" : "text-emerald-600"
}

function opportunityRows(rows: MarketEtfRow[], mode: OpportunityMode) {
  const candidates = mode === "inflow" ? rows.filter((row) => (row.mainNetInflow ?? row.shareDelta ?? 0) > 0)
    : mode === "premium" ? rows.filter((row) => Math.abs(row.premiumPct ?? 0) >= 0.3)
      : mode === "divergence" ? rows.filter((row) => ((row.shareDelta ?? 0) > 0 && (row.changePct ?? 0) < 0) || ((row.shareDelta ?? 0) < 0 && (row.changePct ?? 0) > 0)) : rows
  return [...candidates].sort((a, b) => mode === "active" ? (b.amount ?? 0) - (a.amount ?? 0)
    : mode === "premium" ? Math.abs(b.premiumPct ?? 0) - Math.abs(a.premiumPct ?? 0)
      : mode === "divergence" ? Math.abs(b.shareDelta ?? 0) - Math.abs(a.shareDelta ?? 0)
        : (b.mainNetInflow ?? b.shareDelta ?? 0) - (a.mainNetInflow ?? a.shareDelta ?? 0)).slice(0, 8)
}

function OpportunityView({ rows, onSelect }: { rows: MarketEtfRow[]; onSelect: (row: MarketEtfRow) => void }) {
  const [mode, setMode] = useState<OpportunityMode>("active")
  const ranked = useMemo(() => opportunityRows(rows, mode), [mode, rows])
  const modeOptions: Array<{ value: OpportunityMode; label: string }> = [{ value: "active", label: "活跃" }, { value: "inflow", label: "流入" }, { value: "premium", label: "折溢价" }, { value: "divergence", label: "背离" }]
  return <div className="space-y-3"><div className="flex gap-1" role="tablist" aria-label="机会榜类型">{modeOptions.map((option) => <button key={option.value} type="button" role="tab" aria-selected={mode === option.value} onClick={() => setMode(option.value)} className={cn("h-7 rounded-md px-2.5 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", mode === option.value ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted hover:text-foreground")}>{option.label}</button>)}</div><div className="divide-y divide-border/50">{ranked.map((row, index) => <button key={row.code} type="button" onClick={() => onSelect(row)} className="grid w-full grid-cols-[20px_minmax(0,1fr)_auto] items-center gap-2 py-2.5 text-left transition-colors hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"><span className="font-mono text-[10px] text-muted-foreground">{index + 1}</span><span className="min-w-0"><span className="block truncate text-xs font-medium">{row.name || row.code}</span><span className="mt-0.5 block truncate font-mono text-[10px] text-muted-foreground">{row.code} · {row.theme || row.exchange}</span></span><span className="text-right"><span className={cn("block font-mono text-xs tabular-nums", signedTone(row.changePct))}>{formatPct(row.changePct)}</span><span className="mt-0.5 block font-mono text-[10px] tabular-nums text-muted-foreground">{mode === "active" ? formatWan(row.amount) : mode === "premium" ? formatPct(row.premiumPct) : formatWan(mode === "divergence" ? row.shareDelta : row.mainNetInflow ?? row.shareDelta)}</span></span></button>)}</div>{ranked.length === 0 ? <div className="py-10 text-center text-xs text-muted-foreground">当前没有满足条件的 ETF</div> : null}</div>
}

function FlowRow({ label, value, ratio, max }: { label: string; value: number | null; ratio: number | null; max: number }) {
  const width = Math.min(100, Math.abs(value ?? 0) / max * 100)
  return <div><div className="flex items-center justify-between gap-3 text-xs"><span className="text-muted-foreground">{label}</span><span className={cn("font-mono tabular-nums", signedTone(value))}>{formatWan(value)} <span className="text-[10px] text-muted-foreground">{formatPct(ratio)}</span></span></div><div className="mt-1.5 h-1 overflow-hidden rounded-full bg-muted"><div className={cn("h-full rounded-full", (value ?? 0) >= 0 ? "bg-rose-500/75" : "bg-emerald-500/75")} style={{ width: `${width}%` }} /></div></div>
}

function InstrumentView({ row }: { row: MarketEtfRow | null }) {
  if (!row) return <div className="grid min-h-[360px] place-items-center text-center"><div><Crosshair className="mx-auto size-6 text-muted-foreground" /><div className="mt-3 text-sm font-medium">选择一只 ETF</div><p className="mt-1 text-xs text-muted-foreground">从左侧列表或机会榜打开研究上下文</p></div></div>
  const ChangeIcon = row.changePct == null || row.changePct === 0 ? Minus : row.changePct > 0 ? ArrowUpRight : ArrowDownRight
  const maxFlow = Math.max(Math.abs(row.superLargeNetInflow ?? 0), Math.abs(row.largeNetInflow ?? 0), Math.abs(row.mediumNetInflow ?? 0), Math.abs(row.smallNetInflow ?? 0), 1)
  const metrics = [
    ["成交额", formatWan(row.amount)], ["换手率", formatPct(row.turnoverPct)], ["IOPV", formatNumber(row.iopv)], ["折溢价", formatPct(row.premiumPct)],
    ["当日份额变化", formatWan(row.shareDelta)], ["申赎金额代理", formatWan(row.netSubscriptionProxy)], ["累计份额变化", formatWan(row.cumulativeShareDelta)], ["主力净额", formatWan(row.mainNetInflow)],
  ]
  return <div><div className="border-b border-border/60 pb-4"><div className="font-mono text-[11px] text-muted-foreground">{row.code} · {row.exchange || "ETF"}</div><div className="mt-1 truncate text-base font-semibold">{row.name || "未命名 ETF"}</div><div className="mt-0.5 truncate text-xs text-muted-foreground">{row.theme || "未分类主题"}</div><div className="mt-4 flex items-end justify-between gap-4"><span className="font-mono text-2xl font-semibold tabular-nums">{formatNumber(row.price)}</span><span className={cn("flex items-center gap-1 font-mono text-sm font-medium tabular-nums", signedTone(row.changePct))}><ChangeIcon className="size-4" />{formatPct(row.changePct)}</span></div></div><div className="grid grid-cols-2 border-b border-border/60">{metrics.map(([label, value], index) => <div key={label} className={cn("px-2 py-2.5", index % 2 === 0 && "border-r border-border/50", index < metrics.length - 2 && "border-b border-border/50")}><div className="text-[10px] text-muted-foreground">{label}</div><div className="mt-1 truncate font-mono text-xs font-medium tabular-nums">{value}</div></div>)}</div><div className="pt-4"><div className="mb-3 text-xs font-semibold">分单资金结构</div><div className="space-y-3"><FlowRow label="超大单" value={row.superLargeNetInflow} ratio={row.superLargeNetRatio} max={maxFlow} /><FlowRow label="大单" value={row.largeNetInflow} ratio={row.largeNetRatio} max={maxFlow} /><FlowRow label="中单" value={row.mediumNetInflow} ratio={row.mediumNetRatio} max={maxFlow} /><FlowRow label="小单" value={row.smallNetInflow} ratio={row.smallNetRatio} max={maxFlow} /></div></div></div>
}

function RotationView({ rows, onTheme }: { rows: MarketEtfRotation[]; onTheme: (theme: string) => void }) {
  const maxAmount = Math.max(...rows.map((row) => row.amount ?? 0), 1)
  return <div className="divide-y divide-border/50">{rows.slice(0, 12).map((row) => <button key={row.theme} type="button" onClick={() => onTheme(row.theme)} className="block w-full py-2.5 text-left transition-colors hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"><div className="flex items-center justify-between gap-3"><span className="truncate text-xs font-medium">{row.theme}</span><span className={cn("shrink-0 font-mono text-xs tabular-nums", signedTone(row.averageChangePct))}>{formatPct(row.averageChangePct)}</span></div><div className="mt-1.5 flex items-center gap-2"><span className="h-1 flex-1 overflow-hidden rounded-full bg-muted"><span className="block h-full rounded-full bg-sky-500/70" style={{ width: `${Math.min(100, (row.amount ?? 0) / maxAmount * 100)}%` }} /></span><span className={cn("w-16 text-right font-mono text-[10px] tabular-nums", signedTone(row.shareDelta))}>{formatWan(row.shareDelta)}</span></div><div className="mt-1 flex items-center justify-between text-[10px] text-muted-foreground"><span>{row.etfCount ?? "—"} 只 ETF</span><span className="truncate">{row.leaderName || "—"}</span></div></button>)}</div>
}

function TrendView({ data }: { data: MarketEtfTrend[] }) {
  const [range, setRange] = useState<5 | 10>(10)
  const visible = data.slice(-range)
  return <div><div className="mb-3 flex items-center justify-between"><span className="text-xs text-muted-foreground">份额变化与折溢价</span><div className="flex gap-1"><button type="button" onClick={() => setRange(5)} className={cn("h-7 rounded-md px-2 text-xs", range === 5 ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted")}>5D</button><button type="button" onClick={() => setRange(10)} className={cn("h-7 rounded-md px-2 text-xs", range === 10 ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted")}>10D</button></div></div><ChartContainer config={chartConfig} className="h-[280px] w-full"><AreaChart data={visible} margin={{ left: -18, right: 2, top: 10, bottom: 0 }}><CartesianGrid vertical={false} strokeDasharray="3 3" /><XAxis dataKey="tradeDate" tickLine={false} axisLine={false} tickFormatter={formatDate} fontSize={10} /><YAxis yAxisId="share" tickLine={false} axisLine={false} tickFormatter={(value) => formatWan(Number(value), 0)} fontSize={10} /><YAxis yAxisId="premium" orientation="right" tickLine={false} axisLine={false} hide /><ChartTooltip content={<ChartTooltipContent labelFormatter={(value) => formatDate(String(value))} />} /><Area yAxisId="share" type="monotone" dataKey="broadShareDelta" name="broad" stroke="var(--color-broad)" fill="var(--color-broad)" fillOpacity={0.08} strokeWidth={1.8} dot={false} isAnimationActive={false} /><Line yAxisId="share" type="monotone" dataKey="sectorShareDelta" name="sector" stroke="var(--color-sector)" strokeWidth={1.6} dot={false} isAnimationActive={false} /><Line yAxisId="premium" type="monotone" dataKey="averagePremiumPct" name="premium" stroke="var(--color-premium)" strokeDasharray="4 3" strokeWidth={1.3} dot={false} isAnimationActive={false} /></AreaChart></ChartContainer><div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[10px] text-muted-foreground"><span className="flex items-center gap-1"><span className="size-2 rounded-full bg-sky-600" />宽基份额</span><span className="flex items-center gap-1"><span className="size-2 rounded-full bg-green-600" />行业份额</span><span className="flex items-center gap-1"><span className="size-2 rounded-full bg-amber-600" />平均折溢价</span></div></div>
}

export function EtfInsightRail({ tab, rows, rotations, history, selectedRow, onTabChange, onSelectRow, onTheme }: { tab: InsightTab; rows: MarketEtfRow[]; rotations: MarketEtfRotation[]; history: MarketEtfTrend[]; selectedRow: MarketEtfRow | null; onTabChange: (tab: InsightTab) => void; onSelectRow: (row: MarketEtfRow) => void; onTheme: (theme: string) => void }) {
  const tabs: Array<{ value: InsightTab; label: string; icon: typeof Search }> = [{ value: "opportunity", label: "机会", icon: Gauge }, { value: "instrument", label: "标的", icon: Crosshair }, { value: "rotation", label: "轮动", icon: Layers3 }, { value: "trend", label: "趋势", icon: BarChart3 }]
  return <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-3 py-2"><div className="flex gap-1" role="tablist" aria-label="ETF 研究面板">{tabs.map(({ value, label, icon: Icon }) => <button key={value} type="button" role="tab" aria-selected={tab === value} onClick={() => onTabChange(value)} className={cn("inline-flex h-8 items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", tab === value ? "bg-muted text-foreground" : "text-muted-foreground hover:text-foreground")}><Icon className="size-3.5" />{label}</button>)}</div><Activity className="size-4 text-muted-foreground" /></div><div className="max-h-[620px] overflow-y-auto p-4">{tab === "opportunity" ? <OpportunityView rows={rows} onSelect={onSelectRow} /> : tab === "instrument" ? <InstrumentView row={selectedRow} /> : tab === "rotation" ? <RotationView rows={rotations} onTheme={onTheme} /> : <TrendView data={history} />}</div></section>
}
