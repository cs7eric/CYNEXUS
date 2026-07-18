import { useState } from "react"
import { Database, GitBranch, ShieldCheck, Tags } from "lucide-react"

import type { MarketEtfMappingSummary } from "@/services/market/etf"
import { cn } from "@/lib/utils"
import { formatNumber } from "../lib/format"

const GROUP_LABELS: Record<string, string> = {
  universe: "投资范围",
  asset_class: "资产类别",
  region: "区域市场",
  index_scope: "指数范围",
  style: "风格因子",
  theme: "行业主题",
}

const GROUP_ORDER = ["universe", "asset_class", "region", "index_scope", "style", "theme"]

function groupLabel(group: string) {
  return GROUP_LABELS[group] ?? group
}

function MappingStat({ icon: Icon, label, value, detail }: { icon: typeof Tags; label: string; value: string; detail: string }) {
  return (
    <div className="min-w-0 border-b border-border/50 pb-3 last:border-0 sm:border-b-0 sm:border-r sm:pb-0 sm:pr-4 sm:last:border-0 sm:last:pr-0">
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Icon className="size-3.5" />
        <span>{label}</span>
      </div>
      <div className="mt-1 text-lg font-semibold tabular-nums">{value}</div>
      <div className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</div>
    </div>
  )
}

export function EtfMappingPanel({ mapping }: { mapping: MarketEtfMappingSummary | null }) {
  const [activeGroup, setActiveGroup] = useState("theme")

  if (!mapping) {
    return (
      <section className="rounded-lg border border-dashed border-border/70 bg-muted/15 px-4 py-8 text-center text-sm text-muted-foreground">
        暂无 ETF 分类映射数据
      </section>
    )
  }

  const catalogCount = mapping.catalogEtfCount ?? 0
  const mappedCount = mapping.mappedEtfCount ?? 0
  const dimensions = mapping.dimensions ?? []
  const mappingTags = (mapping.tags ?? []).filter((item) => item.group === activeGroup).slice(0, 16)

  return (
    <section className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
      <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <Tags className="size-4 text-cyan-600 dark:text-cyan-400" />
            <h2 className="text-base font-semibold">ETF 分类映射</h2>
          </div>
          <p className="mt-1 max-w-2xl text-xs leading-5 text-muted-foreground">
            同一只 ETF 可以同时归入多个行业、风格和指数范围，用于后续资金轮动和宽基/行业聚合。
          </p>
        </div>
        <div className="shrink-0 text-left text-xs text-muted-foreground sm:text-right">
          <div>映射口径</div>
          <div className="mt-1 font-mono text-foreground">{mapping.source || "—"}</div>
        </div>
      </div>

      <div className="grid gap-4 border-b border-border/60 px-4 py-4 sm:grid-cols-2 lg:grid-cols-4">
        <MappingStat icon={Database} label="目录 ETF" value={formatNumber(mapping.catalogEtfCount, 0)} detail="当前活跃目录" />
        <MappingStat icon={ShieldCheck} label="已覆盖" value={formatNumber(mapping.mappedEtfCount, 0)} detail={catalogCount ? `${((mappedCount / catalogCount) * 100).toFixed(1)}% 覆盖` : "—"} />
        <MappingStat icon={GitBranch} label="关系总数" value={formatNumber(mapping.relationshipCount, 0)} detail="ETF × 分类标签" />
        <MappingStat icon={Tags} label="未覆盖" value={formatNumber(mapping.unmappedEtfCount, 0)} detail="需要补充分类的 ETF" />
      </div>

      <div className="grid min-w-0 gap-6 px-4 py-4 xl:grid-cols-[minmax(250px,0.75fr)_minmax(0,1.25fr)]">
        <div className="min-w-0">
          <div className="mb-3 flex items-center justify-between gap-3">
            <h3 className="text-sm font-semibold">维度覆盖</h3>
            <span className="text-[11px] text-muted-foreground">按活跃目录计算</span>
          </div>
          <div className="space-y-3">
            {dimensions.map((dimension) => {
              const count = dimension.etfCount ?? 0
              const coverage = catalogCount ? Math.min(100, (count / catalogCount) * 100) : 0
              return (
                <div key={dimension.group} className="min-w-0">
                  <div className="flex items-center justify-between gap-3 text-xs">
                    <span className="font-medium">{groupLabel(dimension.group)}</span>
                    <span className="shrink-0 font-mono tabular-nums text-muted-foreground">{formatNumber(count, 0)} 只 · {formatNumber(dimension.relationshipCount, 0)} 条</span>
                  </div>
                  <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-cyan-500/75" style={{ width: `${coverage}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="min-w-0">
          <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h3 className="text-sm font-semibold">分类标签</h3>
            <div className="flex max-w-full gap-1 overflow-x-auto pb-0.5" role="tablist" aria-label="ETF 分类维度">
              {GROUP_ORDER.map((group) => (
                <button
                  key={group}
                  type="button"
                  role="tab"
                  aria-selected={activeGroup === group}
                  onClick={() => setActiveGroup(group)}
                  className={cn(
                    "shrink-0 rounded-md px-2 py-1 text-[11px] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                    activeGroup === group ? "bg-foreground text-background" : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  )}
                >
                  {groupLabel(group)}
                </button>
              ))}
            </div>
          </div>
          {mappingTags.length ? (
            <div className="grid gap-x-5 gap-y-2 sm:grid-cols-2">
              {mappingTags.map((tag) => (
                <div key={`${tag.group}-${tag.code}`} className="flex min-w-0 items-center justify-between gap-3 border-b border-border/40 pb-2 text-xs last:border-0">
                  <span className="truncate font-medium" title={tag.name}>{tag.name}</span>
                  <span className="shrink-0 font-mono tabular-nums text-muted-foreground">{formatNumber(tag.etfCount, 0)} 只</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="rounded-md border border-dashed border-border/60 px-3 py-8 text-center text-xs text-muted-foreground">该维度暂无标签</div>
          )}
        </div>
      </div>
    </section>
  )
}
