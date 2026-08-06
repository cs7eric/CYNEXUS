import { Activity, ArrowDownRight, ArrowUpRight, Layers3, Minus, Waves } from "lucide-react"

import type { MarketEtfBreadth, MarketEtfRotation, MarketEtfSummary } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatPct, formatWan } from "../lib/format"

type SignalTone = "up" | "down" | "neutral" | "warning"

function toneClass(tone: SignalTone) {
  return tone === "up" ? "text-red-600" : tone === "down" ? "text-emerald-600" : tone === "warning" ? "text-amber-600" : "text-foreground"
}

function Indicator({ label, value, detail, signal, tone, icon: Icon }: { label: string; value: string; detail: string; signal: string; tone: SignalTone; icon: typeof Activity }) {
  return <div className="min-w-0 px-3 py-2.5 xl:py-2"><div className="flex items-center gap-1.5 text-[11px] text-muted-foreground"><Icon className="size-3.5" />{label}</div><div className={cn("mt-1 flex items-center gap-1 font-mono text-sm font-semibold tabular-nums", toneClass(tone))}>{tone === "up" ? <ArrowUpRight className="size-3.5" /> : tone === "down" ? <ArrowDownRight className="size-3.5" /> : tone === "warning" ? <Waves className="size-3.5" /> : <Minus className="size-3.5" />}{value}</div><div className="mt-0.5 truncate text-[10px] text-muted-foreground">{detail}</div><div className={cn("mt-1 truncate text-[10px] font-medium", toneClass(tone))}>{signal}</div></div>
}

export function EtfMarketReaction({ summary, breadth, rotations }: { summary: MarketEtfSummary | null; breadth: MarketEtfBreadth | null; rotations: MarketEtfRotation[] }) {
  const advancingPct = breadth?.advancingPct ?? null
  const breadthTone: SignalTone = advancingPct == null ? "neutral" : advancingPct >= 60 ? "up" : advancingPct <= 40 ? "down" : "neutral"
  const breadthSignal = advancingPct == null ? "等待市场广度" : advancingPct >= 60 ? "上涨扩散" : advancingPct <= 40 ? "下跌扩散" : "涨跌均衡"

  const broadShare = summary?.broadShareDelta ?? null
  const sectorShare = summary?.sectorShareDelta ?? null
  const combinedShare = broadShare == null && sectorShare == null ? null : (broadShare ?? 0) + (sectorShare ?? 0)
  const sameDirection = broadShare != null && sectorShare != null && broadShare * sectorShare > 0
  const flowTone: SignalTone = combinedShare == null || combinedShare === 0 ? "neutral" : combinedShare > 0 ? "up" : "down"
  const flowSignal = sameDirection ? combinedShare! > 0 ? "宽基与行业共振流入" : "宽基与行业共振流出" : broadShare == null || sectorShare == null ? "资金方向不完整" : "宽基与行业方向分化"

  const averagePremium = summary?.averagePremiumPct ?? null
  const premiumTone: SignalTone = averagePremium == null || Math.abs(averagePremium) < 0.3 ? "neutral" : averagePremium > 0 ? "warning" : "down"
  const premiumSignal = averagePremium == null ? "暂无 IOPV 对比" : Math.abs(averagePremium) < 0.3 ? "价格贴近 IOPV" : averagePremium > 0 ? "场内买盘溢价" : "场内折价交易"

  const leadingRotation = [...rotations].sort((left, right) => (right.averageChangePct ?? -Infinity) - (left.averageChangePct ?? -Infinity))[0]
  const rotationTone: SignalTone = leadingRotation?.averageChangePct == null || leadingRotation.averageChangePct === 0 ? "neutral" : leadingRotation.averageChangePct > 0 ? "up" : "down"
  const rotationValue = leadingRotation?.theme || "暂无主题"
  const rotationDetail = leadingRotation ? `${formatPct(leadingRotation.averageChangePct)} · 份额 ${formatWan(leadingRotation.shareDelta)}` : "等待主题轮动数据"

  return <div className="grid min-w-0 grid-cols-2 divide-x divide-y divide-border/50 xl:grid-cols-5 xl:divide-y-0"><Indicator label="市场广度" value={formatPct(advancingPct, 1)} detail={`${breadth?.risingCount ?? "—"} 涨 · ${breadth?.fallingCount ?? "—"} 跌 · ${breadth?.flatCount ?? "—"} 平`} signal={`${breadthSignal}${breadth?.limitUpCount == null ? "" : ` · 涨停 ${breadth.limitUpCount} / 跌停 ${breadth.limitDownCount ?? "—"}`}`} tone={breadthTone} icon={Activity} /><Indicator label="资金方向" value={formatWan(combinedShare)} detail={`宽基 ${formatWan(broadShare)} · 行业 ${formatWan(sectorShare)}`} signal={flowSignal} tone={flowTone} icon={ArrowUpRight} /><Indicator label="价格压力" value={formatPct(averagePremium)} detail="现价相对 IOPV" signal={premiumSignal} tone={premiumTone} icon={Waves} /><Indicator label="交易活跃" value={formatWan(summary?.totalAmount)} detail={`${summary?.totalEtfs ?? "—"} 只有效 ETF 样本`} signal={summary?.activeVolume == null ? "成交额快照" : `活跃成交 ${formatWan(summary.activeVolume)}`} tone="neutral" icon={Activity} /><Indicator label="主题集中" value={rotationValue} detail={rotationDetail} signal={leadingRotation ? "当日涨幅领先主题" : "暂无主题数据"} tone={rotationTone} icon={Layers3} /></div>
}
