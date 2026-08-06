import type { MarketEtfRow } from "@/services/market/etf"
import { ChevronRight } from "lucide-react"
import { formatNumber, formatPct, formatWan } from "../lib/format"

function FlowCell({ value, ratio }: { value: number | null; ratio: number | null }) {
  const tone = value == null || value === 0 ? "text-muted-foreground" : value > 0 ? "text-red-600" : "text-emerald-600"
  return (
    <div className="text-right font-mono tabular-nums">
      <div className={tone}>{formatWan(value)}</div>
      <div className="text-[10px] text-muted-foreground">{formatPct(ratio, 2)}</div>
    </div>
  )
}

export function EtfTable({
  title,
  description,
  rows,
  loading,
  onSelect,
}: {
  title: string
  description: string
  rows: MarketEtfRow[]
  loading: boolean
  onSelect?: (row: MarketEtfRow) => void
}) {
  return (
    <section className="min-w-0 overflow-hidden rounded-lg bg-muted/35 p-4">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">{title}</h2>
          <p className="text-xs text-muted-foreground">{description}</p>
        </div>
        <span className="text-xs text-muted-foreground">{loading ? "加载中" : `${rows.length} 只`}</span>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[980px] text-sm">
          <thead>
            <tr className="border-b border-border/60 text-left text-xs text-muted-foreground">
              <th className="pb-2 font-medium">ETF</th>
              <th className="pb-2 text-right font-medium">价格</th>
              <th className="pb-2 text-right font-medium">涨跌</th>
              <th className="pb-2 text-right font-medium">成交额</th>
              <th className="pb-2 text-right font-medium">折溢价</th>
              <th className="pb-2 text-right font-medium">换手</th>
              <th className="pb-2 text-right font-medium">份额变化</th>
              <th className="pb-2 text-right font-medium">主力净额/占比</th>
              <th className="pb-2 text-right font-medium">详情</th>
            </tr>
          </thead>
          <tbody>
            {rows.slice(0, 30).map((row) => (
              <tr key={row.code} aria-label={`查看 ${row.name || row.code} ETF 详情`} className="cursor-pointer border-b border-border/35 last:border-0 hover:bg-background/70 focus-within:bg-background/70" onClick={() => onSelect?.(row)} tabIndex={onSelect ? 0 : undefined} onKeyDown={(event) => { if (onSelect && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); onSelect(row) } }}>
                <td className="py-3">
                  <div className="max-w-[220px] truncate font-medium">{row.name || "—"}</div>
                  <div className="text-xs text-muted-foreground">{row.code} · {row.theme || row.exchange || "ETF"}</div>
                </td>
                <td className="py-3 text-right font-mono tabular-nums">{formatNumber(row.price)}</td>
                <td className={`py-3 text-right font-mono tabular-nums ${row.changePct == null || row.changePct === 0 ? "text-muted-foreground" : row.changePct > 0 ? "text-red-600" : "text-emerald-600"}`}>{formatPct(row.changePct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatWan(row.amount)}</td>
                <td className={`py-3 text-right font-mono tabular-nums ${row.premiumPct == null || row.premiumPct === 0 ? "text-muted-foreground" : row.premiumPct > 0 ? "text-red-600" : "text-emerald-600"}`}>{formatPct(row.premiumPct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatPct(row.turnoverPct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatWan(row.shareDelta)}</td>
                <td className="py-3"><FlowCell value={row.mainNetInflow} ratio={row.mainNetRatio} /></td>
                <td className="py-3 text-right"><ChevronRight className="ml-auto size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" /></td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? <div className="py-8 text-center text-sm text-muted-foreground">暂无数据</div> : null}
      </div>
    </section>
  )
}
