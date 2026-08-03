import { ArrowDownRight, ArrowUpRight, CircleHelp, Equal, ReceiptText } from "lucide-react"
import { Bar, BarChart, CartesianGrid, Cell, XAxis, YAxis } from "recharts"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import type { PortfolioOverview } from "@/services/market/portfolio/types"

interface PortfolioReturnBreakdownProps {
  overview: PortfolioOverview | null
}

const chartConfig = { pnl: { label: "持仓盈亏", color: "var(--destructive)" } } satisfies ChartConfig

function amount(value: number | null | undefined) {
  if (value == null) return "--"
  const numericValue = Number(value)
  return `${numericValue > 0 ? "+" : ""}${numericValue.toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  // A-share convention: red indicates a gain, green indicates a loss.
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function DirectionIcon({ value }: { value: number | null | undefined }) {
  if (value == null || value === 0) return <Equal className="size-3.5" aria-hidden="true" />
  return value > 0 ? <ArrowUpRight className="size-3.5" aria-hidden="true" /> : <ArrowDownRight className="size-3.5" aria-hidden="true" />
}

export function PortfolioReturnBreakdown({ overview }: PortfolioReturnBreakdownProps) {
  const hasTrades = (overview?.tradeCount ?? 0) > 0
  const total = overview?.totalPnl ?? 0
  const realized = overview?.realizedPnl ?? 0
  const unrealized = overview?.unrealizedPnl ?? 0
  const positions = (overview?.positions ?? []).filter((position) => Number(position.quantity) > 0)
  const chartData = positions
    .map((position) => ({ position, label: position.securityName ? `${position.symbol} ${position.securityName}` : position.symbol, pnl: Number(position.unrealizedPnl ?? 0) }))
    .sort((left, right) => Math.abs(right.pnl) - Math.abs(left.pnl))
    .slice(0, 8)

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="return-breakdown-heading">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
        <div>
          <div className="flex items-center gap-2"><ReceiptText className="size-4 text-muted-foreground" aria-hidden="true" /><h2 id="return-breakdown-heading" className="text-base font-semibold">收益表现</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">按当前持仓拆解浮动盈亏贡献，避免单个负柱吞掉整体信息。</p>
        </div>
        <span className="rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">{hasTrades ? `${overview?.tradeCount ?? 0} 笔已确认交易` : "暂无交易记录"}</span>
      </div>

      {hasTrades ? <div className="p-4">
        <div className="flex flex-wrap items-end justify-between gap-4 border-b border-border/60 pb-4">
          <div><p className="text-xs text-muted-foreground">当前总盈亏</p><p className={`mt-1 font-mono text-3xl font-semibold tracking-tight tabular-nums ${tone(total)}`}>{amount(overview?.totalPnl)}</p></div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground"><span className="inline-flex size-6 items-center justify-center rounded-full border border-border/70 bg-muted/35" aria-hidden="true"><DirectionIcon value={total} /></span><span>已实现盈亏 + 当前持仓盈亏</span></div>
        </div>

        {chartData.length ? <div className="mt-4">
          <div className="mb-2 flex items-center justify-between gap-3"><p className="text-xs font-medium text-muted-foreground">当前持仓收益贡献</p><p className="text-[11px] text-muted-foreground">按绝对金额排序 · 展示前 8 个标的</p></div>
          <ChartContainer config={chartConfig} className="h-[260px] w-full aspect-auto">
            <BarChart data={chartData} layout="vertical" margin={{ top: 4, right: 12, left: 4, bottom: 0 }}>
              <CartesianGrid horizontal={false} strokeDasharray="3 3" />
              <XAxis type="number" tickLine={false} axisLine={false} tickFormatter={(value) => Number(value).toFixed(0)} fontSize={10} />
              <YAxis type="category" dataKey="label" width={120} tickLine={false} axisLine={false} fontSize={10} tickFormatter={(value) => String(value).slice(0, 16)} />
              <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent labelFormatter={(value) => String(value)} formatter={(value) => [amount(Number(value)), "持仓盈亏"]} />} />
              <Bar dataKey="pnl" fill="var(--destructive)" radius={[0, 4, 4, 0]} barSize={18} isAnimationActive={false}>
                {chartData.map((item) => <Cell key={item.position.symbol} className={item.pnl >= 0 ? "fill-rose-600" : "fill-emerald-600"} />)}
              </Bar>
            </BarChart>
          </ChartContainer>
        </div> : <div className="flex min-h-[150px] items-center justify-center px-4 text-center text-xs text-muted-foreground">当前没有仍在持有的仓位，收益主要来自已实现盈亏。</div>}

        <dl className="mt-3 grid gap-2 border-t border-border/60 pt-3 sm:grid-cols-3">
          <Metric label="当前持仓盈亏" value={unrealized} description="市值 − 持仓成本" />
          <Metric label="已实现盈亏" value={realized} description="已卖出部分" />
          <Metric label="持仓成本" value={overview?.costBasis ?? null} description="买入成本 + 手续费" />
        </dl>
        <div className="mt-3 flex items-start gap-2 text-xs leading-5 text-muted-foreground"><CircleHelp className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" /><p>收益图展示当前持仓的浮动盈亏贡献；总盈亏仍按已实现盈亏 + 当前持仓盈亏计算。</p></div>
      </div> : <div className="flex min-h-[210px] items-center justify-center px-4 py-10 text-center"><div><p className="text-sm font-medium">确认交易后，这里会显示收益表现</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">导入截图并确认识别结果后，系统会按成交价格、手续费和最新行情计算收益。</p></div></div>}
    </section>
  )
}

function Metric({ label, value, description }: { label: string; value: number | null; description: string }) {
  return <div><dt className="text-xs text-muted-foreground">{label}</dt><dd className={`mt-1 inline-flex items-center gap-1 font-mono text-sm font-semibold tabular-nums ${tone(value)}`}><DirectionIcon value={value} />{amount(value)}</dd><p className="mt-0.5 text-[11px] text-muted-foreground">{description}</p></div>
}
