import { Activity, ArrowDown, ArrowUp, Flame, ShieldAlert } from "lucide-react"

import { Skeleton } from "@/components/ui/skeleton"
import type { MarketIndicatorBreadth } from "@/services/market/indicator"

function count(value?: number | null) {
  return value == null ? "-" : value.toLocaleString("zh-CN")
}

function ratio(value: number | null | undefined, total: number) {
  return total > 0 && value != null ? Math.round((value / total) * 100) : 0
}

function signed(value: number) {
  return `${value > 0 ? "+" : ""}${value.toLocaleString("zh-CN")}`
}

function MarketReading({ breadth }: { breadth?: MarketIndicatorBreadth | null }) {
  const advancing = breadth?.advancingCount ?? 0
  const declining = breadth?.decliningCount ?? 0
  const total = breadth?.totalCount ?? 0
  const net = advancing - declining
  const reading = total === 0 ? "等待快照" : net >= Math.max(20, total * 0.12) ? "池内上涨占优" : net <= -Math.max(20, total * 0.12) ? "池内下跌占优" : "池内涨跌分化"
  const tone = reading === "池内上涨占优" ? "text-rose-600 dark:text-rose-400" : reading === "池内下跌占优" ? "text-emerald-600 dark:text-emerald-400" : "text-amber-600 dark:text-amber-400"
  return <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-4 sm:flex-row sm:items-end sm:justify-between"><div><div className="text-xs font-medium text-muted-foreground">池内涨跌结构</div><div className="mt-1 flex items-baseline gap-3"><span className={`text-xl font-semibold ${tone}`}>{reading}</span><span className="font-mono text-xs text-muted-foreground">净强弱 {total ? signed(net) : "-"}</span></div><p className="mt-1 text-xs text-muted-foreground">仅统计近一年涨跌停历史池内的上涨、下跌和横盘家数。</p></div><div className="text-left sm:text-right"><div className="font-mono text-lg font-semibold tabular-nums">{count(total)}</div><div className="text-xs text-muted-foreground">池内股票</div></div></div>
}

function BreadthBar({ label, value, total, tone }: { label: string; value?: number | null; total: number; tone: "up" | "down" | "flat" }) {
  const colors = { up: "bg-rose-500", down: "bg-emerald-500", flat: "bg-slate-300" }
  return <div className="grid grid-cols-[56px_minmax(0,1fr)_64px] items-center gap-3 text-xs"><span className="text-muted-foreground">{label}</span><div className="h-2 overflow-hidden rounded-full bg-muted"><div className={`h-full rounded-full ${colors[tone]}`} style={{ width: `${ratio(value, total)}%` }} /></div><span className="text-right font-mono tabular-nums text-foreground">{count(value)} <span className="text-muted-foreground">{ratio(value, total)}%</span></span></div>
}

function SignalRow({ icon: Icon, label, value, detail, tone }: { icon: typeof Activity; label: string; value?: number | null; detail: string; tone: string }) {
  return <div className="flex items-center gap-3"><span className={`flex size-7 shrink-0 items-center justify-center rounded-md bg-muted/60 ${tone}`}><Icon className="size-3.5" /></span><span className="min-w-0 flex-1 text-xs text-muted-foreground">{label}<span className="ml-2 text-[11px] text-muted-foreground/70">{detail}</span></span><span className="font-mono text-sm font-medium tabular-nums">{count(value)}</span></div>
}

export function BreadthPanel({ breadth, loading }: { breadth?: MarketIndicatorBreadth | null; loading: boolean }) {
  if (loading && !breadth) return <section className="rounded-lg border border-border/70 bg-card p-4" aria-label="Market breadth loading"><Skeleton className="h-5 w-28" /><Skeleton className="mt-3 h-8 w-44" /><div className="mt-5 grid gap-3 sm:grid-cols-3"><Skeleton className="h-5" /><Skeleton className="h-5" /><Skeleton className="h-5" /></div></section>

  const total = breadth?.totalCount ?? 0
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="Pool breadth"><MarketReading breadth={breadth} /><div className="grid divide-y divide-border/60 sm:grid-cols-[minmax(0,1fr)_280px] sm:divide-x sm:divide-y-0"><div className="space-y-3 px-4 py-4"><BreadthBar label="上涨" value={breadth?.advancingCount} total={total} tone="up" /><BreadthBar label="下跌" value={breadth?.decliningCount} total={total} tone="down" /><BreadthBar label="平盘" value={breadth?.flatCount} total={total} tone="flat" /></div><div className="space-y-3 px-4 py-4"><div className="mb-1 text-xs font-medium text-muted-foreground">极端与快速信号</div><SignalRow icon={ArrowUp} label="涨停封板" value={breadth?.limitUpCount} detail={`触板 ${count(breadth?.limitUpTouchCount)}`} tone="text-rose-600 dark:text-rose-400" /><SignalRow icon={ArrowDown} label="跌停封板" value={breadth?.limitDownCount} detail={`触板 ${count(breadth?.limitDownTouchCount)}`} tone="text-emerald-600 dark:text-emerald-400" /><SignalRow icon={Flame} label="快速异动" value={(breadth?.rapidRiseCount ?? 0) + (breadth?.rapidFallCount ?? 0)} detail={`拉升 ${count(breadth?.rapidRiseCount)} · 打压 ${count(breadth?.rapidFallCount)}`} tone="text-amber-600 dark:text-amber-400" /><div className="flex items-center gap-2 pt-1 text-[11px] text-muted-foreground"><ShieldAlert className="size-3.5" />指标仅描述池内结构与采集信号</div></div></div></section>
}
