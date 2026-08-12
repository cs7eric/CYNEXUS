import { Activity, ArrowDownRight, ArrowUpRight, Gauge, ShieldCheck } from "lucide-react"

import type { MarketIndicatorResponse, MarketIndicatorTransmissionResponse } from "@/services/market/indicator"

function pct(value?: number | null, digits = 1) {
  return value == null ? "-" : `${(value * 100).toFixed(digits)}%`
}

export function MarketContextStrip({ overview, transmission }: { overview: MarketIndicatorResponse | null; transmission: MarketIndicatorTransmissionResponse | null }) {
  const breadth = overview?.breadth
  const total = breadth?.totalCount ?? 0
  const advancing = breadth?.advancingCount ?? 0
  const declining = breadth?.decliningCount ?? 0
  const active = Math.max(1, advancing + declining)
  const advanceRatio = advancing / active
  const runtime = transmission?.runtime
  const cadence = runtime?.effectiveCadenceSeconds ?? runtime?.targetCadenceSeconds
  const conclusion = total === 0
    ? "等待全市场环境数据"
    : advanceRatio >= 0.62
      ? "多数股票上涨，板块拉升线索需要继续验证去首动股后的扩散"
      : advanceRatio <= 0.38
        ? "多数股票下跌，优先检查板块压制及反向个股"
        : "市场涨跌分化，单股异动不能直接推导为板块传导"

  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card" aria-label="大盘环境与数据门禁">
      <div className="flex flex-col gap-2 border-b border-border/60 bg-muted/20 px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-sm font-semibold"><Activity className="size-4 text-information" />大盘环境与确认门禁</div>
          <p className="mt-1 text-xs text-muted-foreground">{conclusion}</p>
        </div>
        <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
          <ShieldCheck className="size-3.5" />板块结论必须经过完整成分股覆盖与去首动股验证
        </div>
      </div>
      <div className="grid divide-y divide-border/60 sm:grid-cols-2 sm:divide-x sm:divide-y-0 xl:grid-cols-5">
        <Metric icon={<ArrowUpRight className="size-4 text-red-500" />} label="上涨 / 下跌" value={`${advancing} / ${declining}`} note={`上涨占比 ${total ? ((advancing / total) * 100).toFixed(1) : "-"}%`} />
        <Metric icon={<ArrowDownRight className="size-4 text-emerald-500" />} label="涨停 / 跌停" value={`${breadth?.limitUpCount ?? 0} / ${breadth?.limitDownCount ?? 0}`} note={`触板 ${breadth?.limitUpTouchCount ?? 0} / ${breadth?.limitDownTouchCount ?? 0}`} />
        <Metric icon={<Gauge className="size-4 text-information" />} label="全市场覆盖" value={pct(overview?.coverageRatio)} note={`${overview?.observedCount ?? overview?.snapshotCount ?? 0} / ${(overview?.eligibleCount ?? total) || "-"} 只`} />
        <Metric icon={<Activity className="size-4 text-amber-500" />} label="有效采集节奏" value={cadence != null ? `${cadence}s` : "-"} note={`基准门禁 ${runtime?.benchmarkGate ?? "未知"}`} />
        <Metric icon={<ShieldCheck className="size-4 text-violet-500" />} label="当前证据范围" value={overview?.scope === "priority" ? "优先池线索" : "全市场确认"} note={overview?.fullConfirmationRequired ? "禁止输出高可信传导" : "允许进入确认计算"} />
      </div>
    </section>
  )
}

function Metric({ icon, label, value, note }: { icon: React.ReactNode; label: string; value: string; note: string }) {
  return <div className="px-4 py-3"><div className="flex items-center gap-2 text-[11px] text-muted-foreground">{icon}{label}</div><div className="mt-1.5 font-mono text-lg font-semibold tabular-nums text-foreground">{value}</div><div className="mt-1 text-[11px] text-muted-foreground">{note}</div></div>
}
