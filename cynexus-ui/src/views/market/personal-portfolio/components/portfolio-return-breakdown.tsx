import { Bar, BarChart, CartesianGrid, Cell, XAxis, YAxis } from "recharts"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import type { PortfolioOverview } from "@/services/market/portfolio/types"

const chartConfig = {
  value: { label: "P&L" },
} satisfies ChartConfig

const COLORS = {
  realized: "#0071e3",
  unrealized: "#059669",
  total: "#252525",
}

interface PortfolioReturnBreakdownProps {
  overview: PortfolioOverview | null
}

function amount(value: number | null | undefined) {
  return value == null ? "--" : value.toFixed(2)
}

export function PortfolioReturnBreakdown({ overview }: PortfolioReturnBreakdownProps) {
  const hasTrades = (overview?.tradeCount ?? 0) > 0
  const data = overview
    ? [
        { key: "realized", label: "Realized", value: overview.realizedPnl ?? 0 },
        { key: "unrealized", label: "Unrealized", value: overview.unrealizedPnl ?? 0 },
        { key: "total", label: "Total", value: overview.totalPnl ?? 0 },
      ]
    : []

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
        <div>
          <h2 className="text-sm font-semibold">Return tracking</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Confirmed trades only. Account-level return rate is intentionally not estimated without cash-flow data.
          </p>
        </div>
        <span className="rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground">
          {hasTrades ? `${overview?.tradeCount ?? 0} trades` : "No ledger data"}
        </span>
      </div>
      {hasTrades ? (
        <div className="grid gap-4 p-4 lg:grid-cols-[minmax(0,1fr)_220px] lg:items-center">
          <ChartContainer config={chartConfig} className="h-[240px] w-full aspect-auto">
            <BarChart data={data} margin={{ top: 12, right: 8, left: -12, bottom: 0 }}>
              <CartesianGrid vertical={false} strokeDasharray="3 3" />
              <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={11} />
              <YAxis tickLine={false} axisLine={false} tickFormatter={(value) => Number(value).toFixed(0)} fontSize={10} />
              <ChartTooltip content={<ChartTooltipContent labelFormatter={(value) => String(value)} />} />
              <Bar dataKey="value" radius={[4, 4, 0, 0]} isAnimationActive={false}>
                {data.map((item) => <Cell key={item.key} fill={COLORS[item.key as keyof typeof COLORS]} />)}
              </Bar>
            </BarChart>
          </ChartContainer>
          <dl className="grid gap-3 rounded-md bg-muted/35 p-3">
            <div><dt className="text-xs text-muted-foreground">Realized P&L</dt><dd className="mt-1 font-mono text-sm tabular-nums">{amount(overview?.realizedPnl)}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Unrealized P&L</dt><dd className="mt-1 font-mono text-sm tabular-nums">{amount(overview?.unrealizedPnl)}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Total P&L</dt><dd className="mt-1 font-mono text-base font-semibold tabular-nums">{amount(overview?.totalPnl)}</dd></div>
          </dl>
        </div>
      ) : (
        <div className="flex min-h-[220px] items-center justify-center px-4 py-10 text-center">
          <div>
            <p className="text-sm font-medium">Your return view will appear here</p>
            <p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">Import a screenshot, review the extracted rows, and confirm them into the ledger. Unconfirmed AI candidates never affect this view.</p>
          </div>
        </div>
      )}
    </section>
  )
}
