import type { FinanceOverview } from "@/services/finance/ledger"
import { formatMoney } from "../lib/format"

export function CategoryBreakdown({ items }: { items: FinanceOverview["categories"] }) {
  const expenses = items.filter((item) => item.direction === "expense").slice(0, 6)
  const max = Math.max(...expenses.map((item) => item.amount), 0)
  return <section className="min-w-0 rounded-xl border border-border/70 bg-card"><div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">支出分布</h2><p className="mt-0.5 text-xs text-muted-foreground">按当前统计周期汇总。</p></div><span className="text-xs text-muted-foreground">{expenses.length} 类</span></div>{expenses.length ? <div className="space-y-4 px-4 py-4">{expenses.map((item) => <div key={`${item.categoryId}-${item.direction}`}><div className="flex items-center justify-between gap-3 text-xs"><span className="truncate font-medium">{item.categoryName}</span><span className="shrink-0 font-mono tabular-nums text-muted-foreground">{formatMoney(item.amount)} <span className="ml-1 text-[11px]">{item.percentage.toFixed(1)}%</span></span></div><div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted"><div className="h-full rounded-full bg-rose-500/75 transition-[width] duration-300" style={{ width: `${max ? Math.max(4, (item.amount / max) * 100) : 0}%` }} /></div></div>)}</div> : <div className="px-4 py-8 text-center text-xs text-muted-foreground">本期暂无支出分类。</div>}</section>
}
