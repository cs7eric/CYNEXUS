import { ArrowDown, ArrowUp, LockKeyhole } from "lucide-react"

import type { MarketIndicatorSnapshot } from "@/services/market/indicator"

function price(value?: number | null) { return value == null ? "-" : value.toFixed(2) }
function change(value?: number | null) { return value == null ? "-" : `${value >= 0 ? "+" : ""}${value.toFixed(2)}%` }

function Board({ title, rows, down }: { title: string; rows: MarketIndicatorSnapshot[]; down?: boolean }) {
  const tone = down ? "text-emerald-600 dark:text-emerald-400" : "text-rose-600 dark:text-rose-400"
  return <article className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card"><div className="flex items-center justify-between border-b border-border/60 px-4 py-3"><div className="flex items-center gap-2"><span className={`flex size-7 items-center justify-center rounded-md bg-muted/60 ${tone}`}>{down ? <ArrowDown className="size-4" /> : <ArrowUp className="size-4" />}</span><div><h2 className="text-sm font-semibold">{title}</h2><p className="mt-0.5 text-xs text-muted-foreground">已封板 · 按最新采集排序</p></div></div><span className="font-mono text-xs text-muted-foreground">{rows.length} 只</span></div><div className="max-h-[360px] divide-y divide-border/50 overflow-y-auto">{rows.length === 0 ? <div className="px-4 py-12 text-center text-xs text-muted-foreground">暂无封板数据</div> : rows.map((row) => <div key={row.code} className="flex items-center gap-3 px-4 py-2.5 transition-colors hover:bg-muted/30"><span className={`flex size-6 shrink-0 items-center justify-center rounded-md bg-muted/60 ${tone}`}><LockKeyhole className="size-3" /></span><div className="min-w-0 flex-1"><div className="flex min-w-0 items-center gap-2"><span className="truncate text-sm font-medium">{row.name || row.code}</span><span className="shrink-0 font-mono text-[11px] text-muted-foreground">{row.code}</span></div><div className="mt-0.5 truncate text-xs text-muted-foreground">{row.industry || "未分类"}</div></div><div className="text-right"><div className="font-mono text-sm tabular-nums">{price(row.lastPrice)}</div><div className={`mt-0.5 font-mono text-xs tabular-nums ${tone}`}>{change(row.changePct)}</div></div></div>)}</div></article>
}

export function LimitBoardList({ limitUp, limitDown }: { limitUp: MarketIndicatorSnapshot[]; limitDown: MarketIndicatorSnapshot[] }) {
  return <section className="grid min-w-0 gap-4 sm:grid-cols-2 xl:grid-cols-1" aria-label="Limit board lists"><Board title="涨停封板" rows={limitUp} /><Board title="跌停封板" rows={limitDown} down /></section>
}
