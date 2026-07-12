import { useEffect, useState } from "react"
import { CandlestickChart } from "lucide-react"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ChartPanel, type ChartPanelSelectionItem } from "../../stock-chart/components/chart-panel"
import type { StockKlineBar, StockOverlayAnnotation } from "../../stock-chart/lib/types"

export function ChartCard({
  selectedSymbol,
  bars,
  overlays,
  selectionColors,
  selectedBarTimestamps,
  onSelectionChange,
  onAnalyzeSelection,
  mobileCompact = false,
  cardClassName,
  sidebarPreview = false,
}: {
  selectedSymbol: string
  bars: StockKlineBar[]
  overlays: StockOverlayAnnotation[]
  selectionColors: Record<string, string>
  selectedBarTimestamps: number[]
  onSelectionChange: (items: ChartPanelSelectionItem[]) => void
  onAnalyzeSelection: (item: ChartPanelSelectionItem) => void
  mobileCompact?: boolean
  cardClassName?: string
  sidebarPreview?: boolean
}) {
  const [showMobileHint, setShowMobileHint] = useState(true)

  useEffect(() => {
    if (!showMobileHint) return
    const timer = window.setTimeout(() => setShowMobileHint(false), 3500)
    return () => window.clearTimeout(timer)
  }, [showMobileHint])

  return (
    <Card className={`flex ${sidebarPreview ? "min-h-0 flex-1 self-stretch" : "h-full min-h-0"} flex-col gap-0 rounded-2xl border-0 bg-muted/50 py-0 shadow-none ${cardClassName ?? ""}`}>
      {sidebarPreview ? (
        <CardHeader className="shrink-0 px-3 pb-2 pt-3">
          <CardTitle className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <span className="flex size-7 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
              <CandlestickChart className="size-3.5" />
            </span>
            Chart
          </CardTitle>
        </CardHeader>
      ) : null}
      <CardContent className={`flex min-h-0 flex-1 flex-col overflow-auto ${sidebarPreview ? "p-3 pt-0" : "p-3"}`}>
        <div className={`relative max-w-full overflow-hidden ${mobileCompact ? "h-[50svh] min-h-[300px]" : "h-[54svh] min-h-[340px]"} max-h-[520px] sm:h-[440px] lg:h-full lg:max-h-none lg:min-h-0 lg:flex-1`}>
        {showMobileHint ? (
          <button
            type="button"
            className="absolute left-1/2 top-3 z-10 -translate-x-1/2 rounded-full border border-slate-200 bg-white/95 px-3 py-1 text-[11px] font-medium text-slate-600 shadow-sm backdrop-blur lg:hidden"
            onClick={() => setShowMobileHint(false)}
          >
            可横向拖动 / 点选 K 柱
          </button>
        ) : null}
        <ChartPanel
          bars={bars}
          annotations={[]}
          overlayAnnotations={overlays}
          bsSignals={[]}
          manualSignalMode={null}
          onManualSignalCreate={() => undefined}
          symbol={selectedSymbol}
          period="1d"
          indicators={["MA", "AMOUNT"]}
          maLines={[5, 10, 20, 60]}
          selectionMode="multiple"
          selectionColors={selectionColors}
          selectedBarTimestamps={selectedBarTimestamps}
          onSelectionChange={onSelectionChange}
          onAnalyzeSelection={onAnalyzeSelection}
          syncSelection={!sidebarPreview}
          mobileCompact={mobileCompact}
        />
        </div>
      </CardContent>
    </Card>
  )
}
