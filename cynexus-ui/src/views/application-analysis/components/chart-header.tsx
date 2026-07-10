import { ChartNoAxesCombined, Clock, RefreshCw } from "lucide-react"

import type { ApplicationAnalysisTarget } from "@/services/ai/application-analysis"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useIsMobile } from "@/hooks/use-mobile"
import type { StockAdjust } from "../../stock-chart/lib/types"

export function ChartHeader({
  target,
  selectedLabel,
  adjust,
  onAdjustChange,
  running,
  canRun,
  onTrigger,
  onManualRun,
}: {
  target?: ApplicationAnalysisTarget | null
  selectedLabel: string
  adjust: StockAdjust
  onAdjustChange: (value: StockAdjust) => void
  running: boolean
  canRun: boolean
  onTrigger: () => void
  onManualRun: () => void
}) {
  const isMobile = useIsMobile()
  const title = target ? `${target.name} · ${target.symbol}` : selectedLabel || "请选择左侧目标"
  return (
    <Card className="rounded-none border-0 bg-muted py-0 shadow-none sm:rounded-xl">
      <CardHeader className="px-3 py-3 sm:px-4 sm:py-3 2xl:px-5">
        <div className="flex items-center justify-between gap-4">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            {!isMobile ? (
              <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-background text-muted-foreground ring-1 ring-border">
                <ChartNoAxesCombined className="size-4" />
              </div>
            ) : null}
            <div className="min-w-0">
              <CardTitle className="truncate text-[15px] font-semibold text-slate-950 sm:text-base">
                {title}
              </CardTitle>
              {target ? (
                <div className="mt-1 hidden items-center gap-x-1.5 text-[11px] text-slate-500 sm:flex">
                  <span className="inline-flex items-center gap-1">
                    <Clock className="size-3" />
                    <span>每 {target.interval_minutes} 分钟</span>
                  </span>
                  <span className="hidden text-slate-300 sm:inline">·</span>
                  {target.enabled ? (
                    <Badge
                      className="rounded-md border-emerald-200 bg-emerald-50 px-1.5 py-0 text-[10px] text-emerald-700"
                      variant="outline"
                    >
                      启用
                    </Badge>
                  ) : (
                    <Badge
                      className="rounded-md border-slate-200 bg-slate-100 px-1.5 py-0 text-[10px] text-slate-500"
                      variant="outline"
                    >
                      停用
                    </Badge>
                  )}
                  {target.last_updated_at ? (
                    <>
                      <span className="hidden text-slate-300 sm:inline">·</span>
                      <span className="hidden sm:inline">最近 {new Date(target.last_updated_at).toLocaleString()}</span>
                    </>
                  ) : null}
                  <span className="hidden text-slate-300 sm:inline">·</span>
                  <Badge
                    className="rounded-md border-slate-200 bg-background px-1.5 py-0 text-[10px] text-slate-600"
                    variant="outline"
                  >
                    {target.target_type}
                  </Badge>
                </div>
              ) : null}
            </div>
          </div>
          <div className="grid shrink-0 grid-cols-2 items-stretch gap-1.5 sm:flex sm:items-center sm:justify-end">
            <Select value={adjust} onValueChange={(value) => onAdjustChange(value as StockAdjust)}>
              <SelectTrigger size="sm" className="hidden h-7 w-24 justify-center rounded-md border-0 bg-secondary px-2.5 text-center text-xs text-secondary-foreground shadow-none ring-0 hover:bg-secondary/80 [&_svg]:size-3 sm:flex">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="qfq">Forward</SelectItem>
                <SelectItem value="none">Raw</SelectItem>
                <SelectItem value="hfq">Backward</SelectItem>
              </SelectContent>
            </Select>
            <Button
              className="h-7 w-full justify-center rounded-md px-2 text-[10px] text-muted-foreground hover:bg-muted hover:text-foreground sm:w-auto sm:px-3 sm:text-xs"
              onClick={onTrigger}
              disabled={!canRun || running}
              variant="ghost"
            >
              <RefreshCw className={`size-3 ${running ? "animate-spin" : ""}`} />
              <span className="max-lg:hidden">120D / 4S Refresh</span>
              <span className="lg:hidden">Refresh</span>
            </Button>
            <Button
              className="h-7 w-full justify-center rounded-md px-2 text-[10px] shadow-none sm:w-auto sm:px-3 sm:text-xs"
              variant="secondary"
              onClick={onManualRun}
              disabled={!canRun || running}
            >
              <span className="max-lg:hidden">Manual Run</span>
              <span className="lg:hidden">Once</span>
            </Button>
          </div>
        </div>
      </CardHeader>
    </Card>
  )
}
