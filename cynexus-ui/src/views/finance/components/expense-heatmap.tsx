import { useEffect, useMemo, useRef } from "react"
import * as echarts from "echarts/core"
import { TreemapChart } from "echarts/charts"
import { TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"

import type { FinanceOverview } from "@/services/finance/ledger"
import { formatMoney } from "../lib/format"

echarts.use([TreemapChart, TooltipComponent, CanvasRenderer])

export function ExpenseHeatmap({ overview }: { overview: FinanceOverview }) {
  const chartRef = useRef<HTMLDivElement | null>(null)
  const expenses = useMemo(() => overview.categories.filter((item) => item.direction === "expense" && item.amount > 0).sort((a, b) => b.amount - a.amount), [overview.categories])
  const total = expenses.reduce((sum, item) => sum + item.amount, 0)

  useEffect(() => {
    if (!chartRef.current || !expenses.length) return
    const chart = echarts.init(chartRef.current, undefined, { renderer: "canvas" })
    const maxShare = Math.max(...expenses.map((item) => item.amount / Math.max(total, 1)), 0)
    const data = expenses.map((item) => {
      const share = item.amount / Math.max(total, 1)
      const colors = expenseColor(share, maxShare)
      return {
        name: item.categoryName,
        value: item.amount,
        itemStyle: { color: colors.background, borderColor: "#ffffff", borderWidth: 2, gapWidth: 4 },
        label: { show: true, color: colors.foreground, fontSize: 13, fontWeight: 700, lineHeight: 18, formatter: () => item.categoryName + "\n" + formatMoney(item.amount) },
        _amount: item.amount,
        _share: item.percentage,
      }
    })

    chart.setOption({
      backgroundColor: "transparent",
      animation: false,
      tooltip: {
        trigger: "item",
        formatter: (params: { data?: { name?: string; _amount?: number; _share?: number } }) => {
          const item = params.data
          if (!item) return ""
          return "<div style=\"font-weight:700;margin-bottom:6px\">" + (item.name ?? "") + "</div><div>支出金额：" + formatMoney(item._amount) + "</div><div>占比：" + Number(item._share ?? 0).toFixed(1) + "%</div>"
        },
      },
      series: [{
        type: "treemap",
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
        roam: false,
        nodeClick: false,
        breadcrumb: { show: false },
        nodeGap: 4,
        label: { show: true, color: "#ffffff", fontSize: 12, lineHeight: 16 },
        labelLayout: { hideOverlap: true },
        data,
      }],
    })

    const observer = new ResizeObserver(() => chart.resize())
    observer.observe(chartRef.current)
    return () => { observer.disconnect(); chart.dispose() }
  }, [expenses, total])

  return <section className="min-w-0 overflow-hidden rounded-xl border border-border/70 bg-card">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">支出结构</h2><p className="mt-0.5 text-xs text-muted-foreground">面积代表金额，颜色越深表示支出越集中。</p></div>{expenses.length ? <div className="text-right"><div className="font-mono text-sm font-semibold tabular-nums">{formatMoney(total)}</div><div className="text-[10px] text-muted-foreground">{expenses.length} 个分类</div></div> : null}</div>
    {expenses.length ? <div ref={chartRef} aria-label="支出分类热力图" className="h-[230px] w-full px-1 pb-1" /> : <div className="flex min-h-32 items-center justify-center px-4 text-center text-xs text-muted-foreground">这个周期还没有支出分类。</div>}
  </section>
}

function expenseColor(share: number, maxShare: number) {
  const ratio = share / Math.max(maxShare, 0.01)
  if (ratio >= 0.75) return { background: "#be123c", foreground: "#ffffff" }
  if (ratio >= 0.5) return { background: "#e11d48", foreground: "#ffffff" }
  if (ratio >= 0.25) return { background: "#fb7185", foreground: "#4c0519" }
  return { background: "#fecdd3", foreground: "#881337" }
}
