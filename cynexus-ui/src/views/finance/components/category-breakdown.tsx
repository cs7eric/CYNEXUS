import type { FinanceOverview } from "@/services/finance/ledger"
import { categoryDirectionLabel, formatMoney } from "../lib/format"

export function CategoryBreakdown({ items }: { items: FinanceOverview["categories"] }) {
  const expenses = items.filter((item) => item.direction === "expense").slice(0, 8)
  const max = Math.max(...expenses.map((item) => item.amount), 0)
  return <section className="min-w-0 rounded-lg border border-border/70 bg-card p-4"><div className="mb-3 flex items-center justify-between"><div><h2 className="text-base font-semibold">支出分类</h2><p className="mt-1 text-xs text-muted-foreground">按当前统计周期汇总。</p></div><span className="text-xs text-muted-foreground">{expenses.length} 类</span></div>{expenses.length ? <div className="space-y-3">{expenses.map((item) => <div key={`${item.categoryId}-${item.direction}`}><div className="flex items-center justify-between gap-3 text-xs"><span className="truncate font-medium">{item.categoryName}</span><span className="shrink-0 font-mono tabular-nums text-muted-foreground">{formatMoney(item.amount)}</span></div><div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-muted"><div className="h-full rounded-full bg-rose-400" style={{ width: `${max ? Math.max(3, (item.amount / max) * 100) : 0}%` }} /></div><div className="mt-1 text-[10px] text-muted-foreground">{categoryDirectionLabel(item.direction)} · {item.percentage.toFixed(1)}%</div></div>)}</div> : <div className="border-t border-dashed border-border/60 pt-6 text-center text-xs text-muted-foreground">本期暂无支出分类。</div>}</section>
}
