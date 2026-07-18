import type { MarketEtfRow } from "@/services/market/etf"
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
}: {
  title: string
  description: string
  rows: MarketEtfRow[]
  loading: boolean
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
        <table className="w-full min-w-[1500px] text-sm">
          <thead>
            <tr className="border-b border-border/60 text-left text-xs text-muted-foreground">
              <th className="pb-2 font-medium">ETF</th>
              <th className="pb-2 text-right font-medium">价格</th>
              <th className="pb-2 text-right font-medium">涨跌</th>
              <th className="pb-2 text-right font-medium">成交额</th>
              <th className="pb-2 text-right font-medium">折溢价</th>
              <th className="pb-2 text-right font-medium">换手</th>
              <th className="pb-2 text-right font-medium">累计份额变化</th>
              <th className="pb-2 text-right font-medium">累计申赎代理</th>
              <th className="pb-2 text-right font-medium">主力净额/占比</th>
              <th className="pb-2 text-right font-medium">超大单</th>
              <th className="pb-2 text-right font-medium">大单</th>
              <th className="pb-2 text-right font-medium">中单</th>
              <th className="pb-2 text-right font-medium">小单</th>
              <th className="pb-2 text-right font-medium">内盘/外盘</th>
            </tr>
          </thead>
          <tbody>
            {rows.slice(0, 30).map((row) => (
              <tr key={row.code} className="border-b border-border/35 last:border-0">
                <td className="py-3">
                  <div className="max-w-[220px] truncate font-medium">{row.name || "—"}</div>
                  <div className="text-xs text-muted-foreground">{row.code} · {row.theme || row.exchange || "ETF"}</div>
                </td>
                <td className="py-3 text-right font-mono tabular-nums">{formatNumber(row.price)}</td>
                <td className={`py-3 text-right font-mono tabular-nums ${row.changePct == null || row.changePct === 0 ? "text-muted-foreground" : row.changePct > 0 ? "text-red-600" : "text-emerald-600"}`}>{formatPct(row.changePct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatWan(row.amount)}</td>
                <td className={`py-3 text-right font-mono tabular-nums ${row.premiumPct == null || row.premiumPct === 0 ? "text-muted-foreground" : row.premiumPct > 0 ? "text-red-600" : "text-emerald-600"}`}>{formatPct(row.premiumPct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatPct(row.turnoverPct)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatWan(row.cumulativeShareDelta)}</td>
                <td className="py-3 text-right font-mono tabular-nums">{formatWan(row.cumulativeNetSubscriptionProxy)}</td>
                <td className="py-3"><FlowCell value={row.mainNetInflow} ratio={row.mainNetRatio} /></td>
                <td className="py-3"><FlowCell value={row.superLargeNetInflow} ratio={row.superLargeNetRatio} /></td>
                <td className="py-3"><FlowCell value={row.largeNetInflow} ratio={row.largeNetRatio} /></td>
                <td className="py-3"><FlowCell value={row.mediumNetInflow} ratio={row.mediumNetRatio} /></td>
                <td className="py-3"><FlowCell value={row.smallNetInflow} ratio={row.smallNetRatio} /></td>
                <td className="py-3 text-right font-mono tabular-nums text-muted-foreground">{formatNumber(row.insideVolume, 0)} / {formatNumber(row.outsideVolume, 0)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? <div className="py-8 text-center text-sm text-muted-foreground">暂无数据</div> : null}
      </div>
    </section>
  )
}
