import { useEffect, useMemo, useState } from "react"
import { ArrowDown, ArrowUp, ArrowUpDown, RotateCcw, Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { MarketEtfResponse, MarketEtfRow } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatNumber, formatPct, formatWan } from "../lib/format"
import { EtfInsightRail, type InsightTab } from "./insight-rail"

type Scope = "all" | "broad" | "sector"
type Preset = "all" | "active" | "inflow" | "premium" | "divergence"
type SortKey = "amount" | "changePct" | "premiumPct" | "turnoverPct" | "shareDelta" | "mainNetInflow" | "volume" | "iopv" | "latestShares" | "netSubscriptionProxy" | "cumulativeShareDelta"
type SortDirection = "asc" | "desc"

const viewStorageKey = "cynexus.market.etf.research-view.v2"

function loadStoredView(): { scope: Scope; preset: Preset; sortKey: SortKey; sortDirection: SortDirection } {
  const fallback = { scope: "all" as Scope, preset: "all" as Preset, sortKey: "amount" as SortKey, sortDirection: "desc" as SortDirection }
  if (typeof window === "undefined") return fallback
  try {
    const stored = JSON.parse(window.localStorage.getItem(viewStorageKey) || "{}")
    return {
      scope: ["all", "broad", "sector"].includes(stored.scope) ? stored.scope : fallback.scope,
      preset: ["all", "active", "inflow", "premium", "divergence"].includes(stored.preset) ? stored.preset : fallback.preset,
      sortKey: ["amount", "changePct", "premiumPct", "turnoverPct", "shareDelta", "mainNetInflow", "volume", "iopv", "latestShares", "netSubscriptionProxy", "cumulativeShareDelta"].includes(stored.sortKey) ? stored.sortKey : fallback.sortKey,
      sortDirection: ["asc", "desc"].includes(stored.sortDirection) ? stored.sortDirection : fallback.sortDirection,
    }
  } catch { return fallback }
}

function signedTone(value?: number | null) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-red-600" : "text-emerald-600"
}

function SortButton({ label, field, activeField, direction, onSort }: { label: string; field: SortKey; activeField: SortKey; direction: SortDirection; onSort: (field: SortKey) => void }) {
  const Icon = activeField !== field ? ArrowUpDown : direction === "desc" ? ArrowDown : ArrowUp
  return <button type="button" onClick={() => onSort(field)} className="ml-auto inline-flex items-center gap-1 whitespace-nowrap font-medium hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"><span>{label}</span><Icon className="size-3" /></button>
}

export function EtfResearchWorkbench({ data }: { data: MarketEtfResponse }) {
  const stored = useMemo(() => loadStoredView(), [])
  const [scope, setScope] = useState<Scope>(stored.scope)
  const [preset, setPreset] = useState<Preset>(stored.preset)
  const [query, setQuery] = useState("")
  const [sortKey, setSortKey] = useState<SortKey>(stored.sortKey)
  const [sortDirection, setSortDirection] = useState<SortDirection>(stored.sortDirection)
  const [selectedCode, setSelectedCode] = useState<string | null>(data.broad[0]?.code ?? data.sectorRows[0]?.code ?? null)
  const [insightTab, setInsightTab] = useState<InsightTab>("opportunity")

  const allRows = useMemo(() => [...data.broad, ...data.sectorRows], [data.broad, data.sectorRows])
  const effectiveSelectedCode = selectedCode && allRows.some((row) => row.code === selectedCode)
    ? selectedCode
    : allRows[0]?.code ?? null
  const selectedRow = allRows.find((row) => row.code === effectiveSelectedCode) ?? null

  useEffect(() => { window.localStorage.setItem(viewStorageKey, JSON.stringify({ scope, preset, sortKey, sortDirection })) }, [preset, scope, sortDirection, sortKey])

  const visibleRows = useMemo(() => {
    const base = scope === "broad" ? data.broad : scope === "sector" ? data.sectorRows : allRows
    const normalized = query.trim().toLowerCase()
    const searched = base.filter((row) => !normalized || [row.code, row.name, row.theme, row.exchange].some((value) => value?.toLowerCase().includes(normalized)))
    const filtered = preset === "inflow" ? searched.filter((row) => (row.mainNetInflow ?? row.shareDelta ?? 0) > 0)
      : preset === "premium" ? searched.filter((row) => Math.abs(row.premiumPct ?? 0) >= 0.3)
        : preset === "divergence" ? searched.filter((row) => ((row.shareDelta ?? 0) > 0 && (row.changePct ?? 0) < 0) || ((row.shareDelta ?? 0) < 0 && (row.changePct ?? 0) > 0)) : searched
    const effectiveSort = preset === "active" ? "amount" : preset === "inflow" ? "mainNetInflow" : preset === "premium" ? "premiumPct" : preset === "divergence" ? "shareDelta" : sortKey
    return [...filtered].sort((a, b) => {
      const left = effectiveSort === "premiumPct" && preset === "premium" ? Math.abs(a[effectiveSort] ?? 0) : a[effectiveSort] ?? 0
      const right = effectiveSort === "premiumPct" && preset === "premium" ? Math.abs(b[effectiveSort] ?? 0) : b[effectiveSort] ?? 0
      return sortDirection === "desc" ? right - left : left - right
    })
  }, [allRows, data.broad, data.sectorRows, preset, query, scope, sortDirection, sortKey])

  const handleSort = (field: SortKey) => { if (field === sortKey) setSortDirection((value) => value === "desc" ? "asc" : "desc"); else { setSortKey(field); setSortDirection("desc") } }
  const handlePreset = (value: Preset) => {
    const presetSort: Record<Preset, SortKey> = { all: "amount", active: "amount", inflow: "mainNetInflow", premium: "premiumPct", divergence: "shareDelta" }
    setPreset(value)
    setSortKey(presetSort[value])
    setSortDirection("desc")
  }
  const handleSelect = (row: MarketEtfRow) => { setSelectedCode(row.code); setInsightTab("instrument") }
  const handleTheme = (theme: string) => { setScope("sector"); setQuery(theme); setPreset("all"); setInsightTab("opportunity") }
  const reset = () => { setScope("all"); setPreset("all"); setQuery(""); setSortKey("amount"); setSortDirection("desc") }
  const hasFilters = scope !== "all" || preset !== "all" || query.length > 0

  const scopes: Array<{ value: Scope; label: string }> = [{ value: "all", label: "全部" }, { value: "broad", label: "宽基" }, { value: "sector", label: "行业" }]
  return <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.65fr)_minmax(360px,0.75fr)]"><section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card"><div className="border-b border-border/60 p-2"><div className="flex min-w-0 flex-col gap-2 lg:flex-row lg:items-center"><div className="flex shrink-0 gap-1" role="tablist" aria-label="ETF 市场范围">{scopes.map((option) => <button key={option.value} type="button" role="tab" aria-selected={scope === option.value} onClick={() => setScope(option.value)} className={cn("h-6 rounded-md px-1.5 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", scope === option.value ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted hover:text-foreground")}>{option.label}</button>)}</div><div className="relative min-w-0 flex-1 lg:max-w-xs"><Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" /><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索代码、名称或主题" aria-label="搜索 ETF" className="h-7 pl-9 text-xs" /></div><Select value={preset} onValueChange={(value) => handlePreset(value as Preset)}><SelectTrigger size="sm" className="h-7 w-full text-xs lg:w-36"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">全部产品</SelectItem><SelectItem value="active">成交活跃</SelectItem><SelectItem value="inflow">净流入</SelectItem><SelectItem value="premium">折溢价异常</SelectItem><SelectItem value="divergence">份额背离</SelectItem></SelectContent></Select><div className="flex items-center justify-between gap-2 lg:ml-auto"><span className="whitespace-nowrap font-mono text-[11px] text-muted-foreground">{visibleRows.length} / {allRows.length}</span><Button type="button" size="icon-sm" variant="ghost" disabled={!hasFilters} onClick={reset} aria-label="重置 ETF 筛选" title="重置筛选"><RotateCcw className="size-3.5" /></Button></div></div></div><div className="max-h-[620px] overflow-auto"><table className="w-full min-w-[1380px] text-sm"><thead className="sticky top-0 z-10 bg-card"><tr className="border-b border-border/70 text-[11px] text-muted-foreground"><th className="sticky left-0 z-20 w-[220px] border-r border-border/50 bg-card px-3 py-2 text-left font-medium">ETF</th><th className="px-2 py-2 text-right"><SortButton label="价格 / 涨跌" field="changePct" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="成交额" field="amount" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="成交量" field="volume" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="IOPV" field="iopv" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="折溢价" field="premiumPct" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="换手" field="turnoverPct" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="份额变化" field="shareDelta" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="份额存量" field="latestShares" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="申赎代理" field="netSubscriptionProxy" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-2 py-2 text-right"><SortButton label="累计份额" field="cumulativeShareDelta" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th><th className="px-3 py-2 text-right"><SortButton label="主力净额" field="mainNetInflow" activeField={sortKey} direction={sortDirection} onSort={handleSort} /></th></tr></thead><tbody>{visibleRows.map((row) => { const selected = effectiveSelectedCode === row.code; return <tr key={row.code} tabIndex={0} aria-selected={selected} onClick={() => handleSelect(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); handleSelect(row) } }} className={cn("group cursor-pointer border-b border-border/40 transition-colors last:border-b-0 hover:bg-muted/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring", selected && "bg-muted/65")}><td className={cn("sticky left-0 z-[5] border-r border-border/40 bg-card px-3 py-2.5 transition-colors group-hover:bg-muted", selected && "bg-muted")}><div className="max-w-[210px] truncate text-xs font-medium">{row.name || "—"}</div><div className="mt-0.5 flex items-center gap-1.5 font-mono text-[10px] text-muted-foreground"><span>{row.code}</span><span>·</span><span className="truncate">{row.theme || row.exchange || "ETF"}</span></div></td><td className="px-2 py-2.5 text-right"><div className="font-mono text-xs tabular-nums">{formatNumber(row.price)}</div><div className={cn("mt-0.5 font-mono text-[10px] tabular-nums", signedTone(row.changePct))}>{formatPct(row.changePct)}</div></td><td className="px-2 py-2.5 text-right font-mono text-xs tabular-nums">{formatWan(row.amount)}</td><td className="px-2 py-2.5 text-right font-mono text-xs tabular-nums">{formatWan(row.volume)}</td><td className="px-2 py-2.5 text-right font-mono text-xs tabular-nums">{formatNumber(row.iopv)}</td><td className={cn("px-2 py-2.5 text-right font-mono text-xs tabular-nums", signedTone(row.premiumPct))}>{formatPct(row.premiumPct)}</td><td className="px-2 py-2.5 text-right font-mono text-xs tabular-nums">{formatPct(row.turnoverPct)}</td><td className={cn("px-2 py-2.5 text-right font-mono text-xs tabular-nums", signedTone(row.shareDelta))}>{formatWan(row.shareDelta)}</td><td className="px-2 py-2.5 text-right font-mono text-xs tabular-nums">{formatWan(row.latestShares)}</td><td className={cn("px-2 py-2.5 text-right font-mono text-xs tabular-nums", signedTone(row.netSubscriptionProxy))}>{formatWan(row.netSubscriptionProxy)}</td><td className={cn("px-2 py-2.5 text-right font-mono text-xs tabular-nums", signedTone(row.cumulativeShareDelta))}>{formatWan(row.cumulativeShareDelta)}</td><td className={cn("px-3 py-2.5 text-right font-mono text-xs tabular-nums", signedTone(row.mainNetInflow))}><div>{formatWan(row.mainNetInflow)}</div><div className="mt-0.5 text-[10px] text-muted-foreground">{formatPct(row.mainNetRatio)}</div></td></tr> })}</tbody></table>{visibleRows.length === 0 ? <div className="grid min-h-[260px] place-items-center text-center"><div><Search className="mx-auto size-5 text-muted-foreground" /><div className="mt-3 text-sm font-medium">没有匹配的 ETF</div><button type="button" onClick={reset} className="mt-2 text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground">清除筛选条件</button></div></div> : null}</div></section><EtfInsightRail tab={insightTab} rows={allRows} rotations={data.rotations} history={data.history} selectedRow={selectedRow} onTabChange={setInsightTab} onSelectRow={handleSelect} onTheme={handleTheme} /></div>
}
