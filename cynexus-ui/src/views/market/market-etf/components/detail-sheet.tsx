import { useState } from "react"
import { ArrowDownRight, ArrowUpRight, Bookmark, Check, Droplets, ExternalLink, Minus, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import type { MarketEtfRow } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatNumber, formatPct, formatWan } from "../lib/format"

function DetailMetric({ label, value, hint, tone }: { label: string; value: string; hint?: string; tone?: string }) {
  return <div className="rounded-md bg-muted/45 px-3 py-2.5"><div className="text-[11px] text-muted-foreground">{label}</div><div className={cn("mt-1 font-mono text-sm font-semibold tabular-nums", tone)}>{value}</div>{hint ? <div className="mt-0.5 text-[10px] text-muted-foreground">{hint}</div> : null}</div>
}

function FlowBar({ label, value, max }: { label: string; value: number | null; max: number }) {
  const width = max ? Math.min(100, Math.abs(value ?? 0) / max * 100) : 0
  const positive = (value ?? 0) >= 0
  return <div className="space-y-1.5"><div className="flex items-center justify-between text-xs"><span className="text-muted-foreground">{label}</span><span className={cn("font-mono tabular-nums", positive ? "text-red-600" : "text-emerald-600")}>{formatWan(value)}</span></div><div className="h-1.5 overflow-hidden rounded-full bg-muted"><div className={cn("h-full rounded-full", positive ? "bg-rose-500/75" : "bg-emerald-500/75")} style={{ width: `${width}%` }} /></div></div>
}

export function EtfDetailSheet({ row, onClose }: { row: MarketEtfRow | null; onClose: () => void }) {
  const [savedCode, setSavedCode] = useState<string | null>(null)
  const saved = savedCode != null && savedCode === row?.code
  const changeTone = row?.changePct == null || row.changePct === 0 ? "text-muted-foreground" : row.changePct > 0 ? "text-red-600" : "text-emerald-600"
  const ChangeIcon = row?.changePct == null || row.changePct === 0 ? Minus : row.changePct > 0 ? ArrowUpRight : ArrowDownRight
  const maxFlow = Math.max(Math.abs(row?.superLargeNetInflow ?? 0), Math.abs(row?.largeNetInflow ?? 0), Math.abs(row?.mediumNetInflow ?? 0), Math.abs(row?.smallNetInflow ?? 0), 1)
  return (
    <Sheet open={Boolean(row)} onOpenChange={(open) => { if (!open) onClose() }}>
      <SheetContent side="right" showCloseButton={false} className="w-full overflow-y-auto sm:max-w-md">
        {row ? <>
          <SheetHeader className="border-b border-border/60 px-5 py-5 pr-14">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0"><div className="font-mono text-xs text-muted-foreground">{row.code} · {row.exchange || "ETF"}</div><SheetTitle className="mt-1 truncate text-lg">{row.name || "未命名 ETF"}</SheetTitle><SheetDescription className="mt-1 truncate">{row.theme || "未分类主题"}</SheetDescription></div>
              <SheetClose asChild><Button variant="ghost" size="icon" className="-mr-2 -mt-2 shrink-0" aria-label="关闭 ETF 详情"><X className="size-4" /></Button></SheetClose>
            </div>
            <div className="mt-4 flex items-end justify-between gap-4"><div><div className="font-mono text-3xl font-semibold tabular-nums">{formatNumber(row.price)}</div><div className={cn("mt-1 flex items-center gap-1 text-sm font-medium", changeTone)}><ChangeIcon className="size-4" />{formatPct(row.changePct)}</div></div><Button variant={saved ? "secondary" : "outline"} size="sm" className="gap-1.5 text-xs" onClick={() => setSavedCode((current) => current === row.code ? null : row.code)}>{saved ? <Check className="size-3.5" /> : <Bookmark className="size-3.5" />}{saved ? "已加入观察" : "加入观察"}</Button></div>
          </SheetHeader>
          <div className="space-y-5 px-5 py-5">
            <div><div className="mb-2 flex items-center gap-2 text-sm font-semibold"><Droplets className="size-4 text-sky-600" />交易与估值</div><div className="grid grid-cols-2 gap-2"><DetailMetric label="成交额" value={formatWan(row.amount)} hint="当日" /><DetailMetric label="换手率" value={formatPct(row.turnoverPct)} /><DetailMetric label="IOPV" value={formatNumber(row.iopv)} /><DetailMetric label="折溢价" value={formatPct(row.premiumPct)} tone={row.premiumPct && row.premiumPct > 0 ? "text-red-600" : row.premiumPct && row.premiumPct < 0 ? "text-emerald-600" : undefined} /></div></div>
            <div><div className="mb-2 text-sm font-semibold">份额与申赎代理</div><div className="grid grid-cols-2 gap-2"><DetailMetric label="最新份额" value={formatWan(row.latestShares)} /><DetailMetric label="当日变化" value={formatWan(row.shareDelta)} tone={row.shareDelta && row.shareDelta > 0 ? "text-red-600" : row.shareDelta && row.shareDelta < 0 ? "text-emerald-600" : undefined} /><DetailMetric label="累计变化" value={formatWan(row.cumulativeShareDelta)} /><DetailMetric label="申赎金额代理" value={formatWan(row.netSubscriptionProxy)} /></div></div>
            <div><div className="mb-3 text-sm font-semibold">分单资金结构</div><div className="space-y-3"><FlowBar label="超大单净额" value={row.superLargeNetInflow} max={maxFlow} /><FlowBar label="大单净额" value={row.largeNetInflow} max={maxFlow} /><FlowBar label="中单净额" value={row.mediumNetInflow} max={maxFlow} /><FlowBar label="小单净额" value={row.smallNetInflow} max={maxFlow} /></div></div>
            <div className="border-t border-border/60 pt-4 text-xs leading-5 text-muted-foreground">净流入为二级市场分类资金参考；免费行情源不提供完整申购/赎回毛数据。<button type="button" className="ml-1 inline-flex items-center gap-1 text-foreground underline underline-offset-2"><ExternalLink className="size-3" />查看口径</button></div>
          </div>
        </> : null}
      </SheetContent>
    </Sheet>
  )
}
