import { type ReactNode, useMemo, useState } from "react"
import { Activity, Clock3, Database, Landmark } from "lucide-react"

import { Badge } from "@/components/ui/badge"

import type { IndustryApplicationTargetCode } from "../lib/types"
import { KlineTargetPicker } from "./kline-target-picker"
import { NotApplicableCard } from "./not-applicable-card"
import { IndustryTdxFundPanel } from "./industry-tdx-fund-panel"

const QUICK_CODES = ["sh880471", "sh880472", "sh880400", "sh880493", "sh880482", "sh880390"] as const

type Props = {
  items: IndustryApplicationTargetCode[]
  preferredTarget?: {
    symbol: string
    name: string
    target_type: "industry" | "concept" | "index"
  } | null
}

export function IndustryTdxFundWorkspace({ items, preferredTarget = null }: Props) {
  const preferredCode =
    preferredTarget?.target_type === "industry" && items.some((item) => item.code === preferredTarget.symbol)
      ? preferredTarget.symbol
      : null

  const [manualSelectedCode, setManualSelectedCode] = useState<string | null>(null)
  const defaultCode = useMemo(
    () => QUICK_CODES.find((code) => items.some((item) => item.code === code)) ?? items[0]?.code ?? null,
    [items],
  )

  const selectedCode =
    manualSelectedCode && items.some((item) => item.code === manualSelectedCode)
      ? manualSelectedCode
      : preferredCode ?? defaultCode

  const selectedTarget = useMemo(
    () => items.find((item) => item.code === selectedCode) ?? null,
    [items, selectedCode],
  )

  const quickTargets = useMemo(() => {
    const ordered = QUICK_CODES
      .map((code) => items.find((item) => item.code === code))
      .filter((item): item is IndustryApplicationTargetCode => Boolean(item))
    const fallback = items.filter((item) => !ordered.some((picked) => picked.code === item.code)).slice(0, 6)
    return [...ordered, ...fallback].slice(0, 8)
  }, [items])

  if (items.length === 0) {
    return <NotApplicableCard title="资金" description="当前没有可用的 TDX 56 行业资金标的。" />
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <section className="shrink-0 rounded-none bg-slate-50 px-4 py-4 sm:rounded-2xl">
        <div className="grid gap-4 xl:grid-cols-[minmax(320px,420px)_minmax(0,1fr)]">
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Landmark className="size-4 text-slate-500" />
              TDX56 行业选择
            </div>
            <KlineTargetPicker
              items={items}
              value={
                selectedTarget
                  ? {
                      code: selectedTarget.code,
                      name: selectedTarget.name,
                      target_type: "industry",
                    }
                  : null
              }
              onSelect={(item) => setManualSelectedCode(item.code)}
              className="sm:w-full"
            />
            <div className="text-xs leading-5 text-slate-500">
              这里只展示支持 `TDX 56` 资金快照的行业，概念、指数和无快照行业不会出现在这里。
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_300px]">
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                <Activity className="size-4 text-slate-500" />
                快照口径
              </div>
              <div className="grid gap-3 md:grid-cols-3">
                <DataBlock
                  label="当前行业"
                  value={selectedTarget?.name ?? "未选择"}
                  extra={
                    <div className="mt-2 flex items-center gap-2">
                      <Badge variant="outline" className="rounded-full border-blue-200 bg-blue-50 text-blue-700">
                        行业
                      </Badge>
                      <span className="text-xs text-slate-500">{selectedTarget?.code ?? "—"}</span>
                    </div>
                  }
                />
                <DataBlock
                  label={
                    <span className="inline-flex items-center gap-1.5">
                      <Clock3 className="size-3.5" />
                      刷新节奏
                    </span>
                  }
                  value="交易日 17:00"
                  description="日终任务写入 PostgreSQL，页面读取当日快照。"
                />
                <DataBlock
                  label={
                    <span className="inline-flex items-center gap-1.5">
                      <Database className="size-3.5" />
                      数据来源
                    </span>
                  }
                  value="TDX 56 资金"
                  description="UI → Java → Python → PG，返回已落库快照。"
                />
              </div>
            </div>

            <div className="space-y-3">
              <div className="text-sm font-semibold text-slate-900">快捷切换</div>
              <div className="flex flex-wrap gap-2">
                {quickTargets.map((item) => {
                  const active = selectedTarget?.code === item.code
                  return (
                    <button
                      key={item.code}
                      type="button"
                      onClick={() => setManualSelectedCode(item.code)}
                      className={
                        active
                          ? "inline-flex h-8 items-center rounded-lg bg-slate-900 px-2.5 text-xs font-medium text-white"
                          : "inline-flex h-8 items-center rounded-lg bg-white px-2.5 text-xs font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
                      }
                    >
                      {item.name}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="min-h-0 flex-1 overflow-hidden">
        <IndustryTdxFundPanel
          target={
            selectedTarget
              ? {
                  target_type: "industry",
                  symbol: selectedTarget.code,
                  name: selectedTarget.name,
                }
              : null
          }
        />
      </div>
    </div>
  )
}

function DataBlock({
  label,
  value,
  description,
  extra,
}: {
  label: ReactNode
  value: string
  description?: string
  extra?: ReactNode
}) {
  return (
    <div className="rounded-xl bg-white px-4 py-3">
      <div className="text-[11px] font-medium uppercase tracking-[0.08em] text-slate-400">{label}</div>
      <div className="mt-2 text-sm font-semibold text-slate-900">{value}</div>
      {description ? <div className="mt-2 text-xs leading-5 text-slate-500">{description}</div> : null}
      {extra}
    </div>
  )
}
