import { CircleHelp } from "lucide-react"
import type { PortfolioPosition } from "@/services/market/portfolio/types"

function money(value: number | null | undefined) {
  if (value == null) return "--"
  return Number(value).toFixed(2)
}

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

export function PortfolioCurrentHoldingsPanel({ positions }: { positions: PortfolioPosition[] }) {
  return <section className="min-w-0 overflow-hidden" aria-labelledby="current-holdings-panel-heading">
    <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3.5">
      <div className="min-w-0">
        <h3 id="current-holdings-panel-heading" className="text-sm font-semibold">当前持仓</h3>
        <p className="mt-1 text-[11px] text-muted-foreground">仅显示仍在持有的标的。</p>
      </div>
      <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{positions.length} 个</span>
    </div>

    {positions.length ? <div className="max-h-[360px] overflow-auto">
      <div className="divide-y divide-border/60">
        {positions.map((position) => <div key={`${position.instrumentType}-${position.symbol}`} className="px-4 py-3 transition-colors hover:bg-muted/30">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{position.securityName ?? "名称待补充"}</p>
              <p className="mt-0.5 truncate font-mono text-[11px] text-muted-foreground">{position.symbol} · {position.instrumentType === "ETF" ? "ETF" : "股票"}</p>
            </div>
            <div className="shrink-0 text-right">
              <p className="font-mono text-sm font-semibold tabular-nums">{money(position.marketValue)}</p>
              <p className={`mt-0.5 font-mono text-[11px] tabular-nums ${tone(position.todayPnl)}`}>今日 {signedMoney(position.todayPnl)}</p>
            </div>
          </div>
          <div className="mt-2 grid grid-cols-3 gap-2 text-[11px]">
            <div><p className="text-muted-foreground">持仓</p><p className="mt-0.5 font-mono tabular-nums">{position.quantity}</p></div>
            <div><p className="text-muted-foreground">成本 / 现价</p><p className="mt-0.5 font-mono tabular-nums">{money(position.averageCost)} / {money(position.latestPrice)}</p></div>
            <div className="text-right"><p className="text-muted-foreground">持仓盈亏</p><p className={`mt-0.5 font-mono font-semibold tabular-nums ${tone(position.unrealizedPnl)}`}>{signedMoney(position.unrealizedPnl)}</p></div>
          </div>
        </div>)}
      </div>
    </div> : <div className="flex min-h-[180px] items-center justify-center px-4 py-8 text-center"><div><CircleHelp className="mx-auto size-4 text-muted-foreground" /><p className="mt-2 text-sm font-medium">暂无当前持仓</p><p className="mt-1 text-xs text-muted-foreground">确认交易后，仍在持有的标的会显示在这里。</p></div></div>}
  </section>
}
