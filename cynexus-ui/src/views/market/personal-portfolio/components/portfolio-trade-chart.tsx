import { ChartPanel } from "@/views/stock-chart/components/chart-panel"
import type { StockKlineBar, StockSignalPoint } from "@/views/stock-chart/lib/types"
import type { PortfolioTrade } from "@/services/market/portfolio/types"

interface PortfolioTradeChartProps {
  symbol: string
  bars: StockKlineBar[]
  trades: PortfolioTrade[]
}

function toSignal(trade: PortfolioTrade): StockSignalPoint {
  return {
    id: `portfolio-trade-${trade.id}`,
    timestamp: new Date(trade.tradedAt).getTime(),
    price: Number(trade.price),
    side: trade.side === "SELL" ? "S" : "B",
    label: trade.side === "SELL" ? "S" : "B",
    reason: "portfolio",
  }
}

export function PortfolioTradeChart({ symbol, bars, trades }: PortfolioTradeChartProps) {
  const signals = trades
    .map(toSignal)
    .filter((signal) => Number.isFinite(signal.timestamp) && Number.isFinite(signal.price))

  return (
    <div className="h-[420px] min-h-0 w-full">
      <ChartPanel
        bars={bars}
        annotations={[]}
        overlayAnnotations={[]}
        bsSignals={signals}
        manualSignalMode={null}
        onManualSignalCreate={() => undefined}
        symbol={symbol}
        period="1d"
        indicators={[]}
        maLines={[]}
        yAxisPosition="right"
      />
    </div>
  )
}
