import { useEffect, useMemo, useState } from "react"

import { notification } from "@/components/ui/notification"
import { StockDetailDialog } from "@/components/stock-detail-dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  fetchIndustryApplicationKline,
  fetchIndustryFundFlow,
  fetchIndustryFundFlowHistory,
  type IndustryApplicationMergedConstituent,
  type IndustryApplicationTdxFundFlowPoint,
  fetchIndustryApplicationTdxFundFlow,
  type IndustryFundFlowRow,
  type IndustryApplicationTdxFundFlowResponse,
} from "@/services/market/industry-application"

import { IndustryTdxFundOverlayChart } from "./industry-tdx-fund-overlay-chart"
import { NotApplicableCard } from "./not-applicable-card"
import { Sparkline } from "@/views/market/market-sentiment/components/sparkline"

type Props = {
  target: {
    target_type: "industry" | "concept" | "index"
    symbol: string
    name: string
  } | null
}

function fmtYi(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  return `${(value / 1e8).toFixed(2)}亿`
}

function fmtSignedYi(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  const sign = value > 0 ? "+" : ""
  return `${sign}${(value / 1e8).toFixed(2)}亿`
}

function fmtNumber(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  return value.toLocaleString("zh-CN", { maximumFractionDigits: 0 })
}

function fmtDate(value: string | null | undefined) {
  return value || "—"
}

function fmtPct(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  const sign = value > 0 ? "+" : ""
  return `${sign}${value.toFixed(2)}%`
}

function matchTypeLabel(value: string | null | undefined) {
  switch (value) {
    case "exact":
      return "精确"
    case "alias":
      return "别名"
    case "one_to_many":
      return "一对多"
    case "weak":
      return "弱映射"
    default:
      return "—"
  }
}

export function IndustryTdxFundPanel({ target }: Props) {
  const [data, setData] = useState<IndustryApplicationTdxFundFlowResponse | null>(null)
  const [klineRows, setKlineRows] = useState<Array<{
    tradeDate: string
    open: number | null
    high: number | null
    low: number | null
    close: number | null
    amount: number | null
  }>>([])
  const [thsPanels, setThsPanels] = useState<Record<string, ThsFundPanelState>>({})
  const [resolvedTargetKey, setResolvedTargetKey] = useState<string | null>(null)
  const [thsChartView, setThsChartView] = useState<"net" | "amount">("net")
  const [thsHoveredDates, setThsHoveredDates] = useState<Record<string, string | null>>({})
  const [selectedStock, setSelectedStock] = useState<{ code: string; name: string } | null>(null)
  const [stockDialogOpen, setStockDialogOpen] = useState(false)
  const targetKey = target ? `${target.target_type}:${target.symbol}` : null

  useEffect(() => {
    if (!target || !targetKey) return
    let cancelled = false
    void Promise.all([
      fetchIndustryApplicationTdxFundFlow({
        targetType: target.target_type,
        symbol: target.symbol,
        days: 30,
      }),
      fetchIndustryApplicationKline(target.target_type, target.symbol, { period: "day", count: 120 }),
    ])
      .then(([fundResult, klineResult]) => {
        if (!cancelled) {
          setData(fundResult)
          setKlineRows(
            (klineResult.kline ?? []).map((item) => ({
              tradeDate: item.time.slice(0, 10),
              open: item.open ?? null,
              high: item.high ?? null,
              low: item.low ?? null,
              close: item.close ?? null,
              amount: item.amount ?? null,
            })),
          )
          setThsPanels({})
          setThsChartView("net")
          setResolvedTargetKey(targetKey)
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setData(null)
          setKlineRows([])
          setThsPanels({})
          setThsChartView("net")
          setThsHoveredDates({})
          setResolvedTargetKey(targetKey)
          notification.danger({ title: "资金数据加载失败", description: error?.message ?? String(error) })
        }
      })
    return () => {
      cancelled = true
    }
  }, [target, targetKey])

  const loading = Boolean(targetKey && resolvedTargetKey !== targetKey)
  const visibleData = targetKey && resolvedTargetKey === targetKey ? data : null

  const history = useMemo(() => visibleData?.history ?? [], [visibleData])
  const thsMappings = useMemo(() => visibleData?.ths_mappings ?? [], [visibleData])
  const netWindow5 = useMemo(() => sumLatestMainNet(history, 5), [history])
  const netWindow10 = useMemo(() => sumLatestMainNet(history, 10), [history])
  const netWindow30 = useMemo(() => sumLatestMainNet(history, 30), [history])
  const pctWindow5 = useMemo(() => calcTrailingReturn(klineRows, 5), [klineRows])
  const pctWindow10 = useMemo(() => calcTrailingReturn(klineRows, 10), [klineRows])
  const pctWindow30 = useMemo(() => calcTrailingReturn(klineRows, 30), [klineRows])
  const constituentLimitStats = useMemo(
    () => calcConstituentLimitStats(visibleData?.constituents ?? []),
    [visibleData?.constituents],
  )
  const constituentSnapshotLabel = useMemo(
    () => formatConstituentSnapshotLabel(visibleData?.constituents ?? []),
    [visibleData?.constituents],
  )
  const defaultThsTab = useMemo(
    () => thsMappings.find((item) => item.is_primary)?.ths_code ?? thsMappings[0]?.ths_code ?? "",
    [thsMappings],
  )
  const visibleThsPanels = visibleData?.ok && thsMappings.length > 0 ? thsPanels : {}

  useEffect(() => {
    if (!visibleData?.ok || thsMappings.length === 0) {
      return
    }
    let cancelled = false
    const mappingCodeSet = new Set(thsMappings.map((item) => item.ths_code).filter((item): item is string => Boolean(item)))
    void Promise.all([
      fetchIndustryFundFlow(),
      fetchIndustryFundFlowHistory({ days: 30 }),
    ])
      .then(([latestResult, historyResult]) => {
        if (cancelled) return
        const latestByCode = new Map(
          latestResult.rows
            .filter((item) => item.code && mappingCodeSet.has(item.code))
            .map((item) => [item.code as string, item]),
        )
        const historyByCode = new Map<string, IndustryFundFlowRow[]>()
        for (const row of historyResult.rows) {
          if (!row.code || !mappingCodeSet.has(row.code)) continue
          const current = historyByCode.get(row.code) ?? []
          current.push(row)
          historyByCode.set(row.code, current)
        }
        const nextPanels = Object.fromEntries(
          thsMappings
            .map((mapping) => {
              const thsCode = mapping.ths_code
              if (!thsCode) return null
              const latestRow = latestByCode.get(thsCode) ?? null
              const thsHistoryRows = (historyByCode.get(thsCode) ?? []).sort((a, b) =>
                String(a.tradeDate ?? "").localeCompare(String(b.tradeDate ?? "")),
              )
              const normalizedHistory = thsHistoryRows.map((item) => normalizeThsFundPoint(item))
              const todayNet = latestRow ? yiNumberToYuan(latestRow["净额(亿)"]) : null
              const todayPct = toFiniteNumber(latestRow?.["行业指数涨跌幅"])
              return [
                thsCode,
                {
                  latestRow,
                  history: normalizedHistory,
                  historyRows: thsHistoryRows,
                  todayNet,
                  todayPct,
                  netWindow5: sumLatestMainNet(normalizedHistory, 5),
                  netWindow10: sumLatestMainNet(normalizedHistory, 10),
                  netWindow30: sumLatestMainNet(normalizedHistory, 30),
                  pctWindow5: calcCompoundedChangePct(thsHistoryRows, 5),
                  pctWindow10: calcCompoundedChangePct(thsHistoryRows, 10),
                  pctWindow30: calcCompoundedChangePct(thsHistoryRows, 30),
                } satisfies ThsFundPanelState,
              ] as const
            })
            .filter((item): item is readonly [string, ThsFundPanelState] => item != null),
        )
        setThsPanels(nextPanels)
        setThsHoveredDates({})
      })
      .catch((error) => {
        if (cancelled) return
        setThsPanels({})
        setThsHoveredDates({})
        notification.danger({ title: "THS 行业资金加载失败", description: error?.message ?? String(error) })
      })
    return () => {
      cancelled = true
    }
  }, [thsMappings, visibleData?.ok])

  if (!target) {
    return <NotApplicableCard title="资金" description="请先选择一个行业标的。" />
  }
  if (loading && !visibleData) {
    return <NotApplicableCard title="资金" description="加载中..." />
  }
  if (!visibleData?.applicable) {
    return (
      <NotApplicableCard
        title="资金"
        description={visibleData?.error || "当前仅支持 TDX 56 行业，指数 / 概念和非 TDX56 行业暂不支持。"}
      />
    )
  }
  if (!visibleData.ok) {
    return <NotApplicableCard title="资金" description={visibleData.error || "暂无 TDX 56 行业资金快照。"} />
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
      <div className="shrink-0 grid gap-3 xl:grid-cols-[minmax(460px,1.9fr)_repeat(3,minmax(150px,0.8fr))]">
        <section className="rounded-none bg-slate-50 px-4 py-3 sm:rounded-2xl">
          <div className="grid gap-3 xl:grid-cols-[minmax(190px,1.15fr)_repeat(3,minmax(120px,0.72fr))] xl:items-end">
            <div className="min-w-0">
              <div className="text-sm font-medium text-slate-500">主力净额</div>
              <div className={metricToneClass(visibleData.main_net, "mt-1 text-3xl font-semibold leading-none")}>
                {fmtYi(visibleData.main_net)}
              </div>
            </div>
            <CompactMetric
              title="大单净额"
              value={fmtYi(visibleData.large_order_net)}
              rawValue={visibleData.large_order_net}
            />
            <CompactMetric
              title="中单净额"
              value={fmtYi(visibleData.medium_order_net)}
              rawValue={visibleData.medium_order_net}
            />
            <CompactMetric
              title="小单净额"
              value={fmtYi(visibleData.small_order_net)}
              rawValue={visibleData.small_order_net}
            />
          </div>
        </section>
        <WindowMetricCard title="近5日" value={fmtYi(netWindow5)} rawValue={netWindow5} pctValue={pctWindow5} />
        <WindowMetricCard title="近10日" value={fmtYi(netWindow10)} rawValue={netWindow10} pctValue={pctWindow10} />
        <WindowMetricCard title="近30日" value={fmtYi(netWindow30)} rawValue={netWindow30} pctValue={pctWindow30} />
      </div>

      <div className="grid min-h-0 flex-1 gap-3 overflow-hidden xl:grid-cols-[minmax(420px,0.44fr)_minmax(520px,0.56fr)]">
        <section className="flex min-h-0 flex-col overflow-hidden rounded-none bg-slate-50 px-4 py-4 sm:rounded-2xl">
          <div className="mb-3 flex shrink-0 items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-slate-900">近 30 日主力净额</div>
              <div className="mt-1 text-xs text-slate-500">红柱流入，绿柱流出。</div>
            </div>
            <div className="text-xs text-slate-400">{target.name}</div>
          </div>
          {history.length === 0 ? (
            <div className="flex h-56 items-center justify-center rounded-xl bg-white text-sm text-slate-500">
              暂无历史数据
            </div>
          ) : (
            <div className="min-h-0 flex-1 rounded-xl bg-white px-3 py-4">
              <IndustryTdxFundOverlayChart history={history} klineRows={klineRows} />
            </div>
          )}
        </section>

        <section className="min-h-0 overflow-hidden rounded-none bg-slate-50 px-4 py-4 sm:rounded-2xl">
          <div className="grid h-full min-h-0 grid-rows-[minmax(0,1fr)_auto] gap-3 text-sm text-slate-700">
            {thsMappings.length > 0 ? (
              <Tabs defaultValue={defaultThsTab} className="flex min-h-0 flex-1 flex-col overflow-hidden">
                <div className="mb-2 text-xs font-medium text-slate-500">THS 行业映射</div>
                <TabsList className="inline-flex h-auto w-fit max-w-full flex-wrap justify-start gap-1 rounded-xl bg-slate-100 p-1">
                  {thsMappings.map((item) => (
                    <TabsTrigger
                      key={item.ths_code || item.ths_name || "na"}
                      value={item.ths_code || "na"}
                      className="h-7 w-[104px] shrink-0 px-3 text-xs !flex-none"
                    >
                      <span className="truncate">{item.ths_name || "—"}</span>
                    </TabsTrigger>
                  ))}
                </TabsList>
                {thsMappings.map((item) => {
                  const thsCode = item.ths_code || "na"
                  const panel = item.ths_code ? visibleThsPanels[item.ths_code] : undefined
                  const hoveredDate = item.ths_code ? thsHoveredDates[item.ths_code] ?? null : null
                  const activeRowIndex = hoveredDate
                    ? findHistoryRowIndexByTradeDate(panel?.historyRows ?? [], hoveredDate)
                    : -1
                  const activeRow = activeRowIndex >= 0 ? panel?.historyRows[activeRowIndex] ?? null : panel?.latestRow ?? null
                  return (
                    <TabsContent key={thsCode} value={thsCode} className="mt-3 flex min-h-0 flex-1 overflow-hidden">
                      <div className="flex h-full min-h-0 flex-1 flex-col gap-3 overflow-hidden">
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <div className="text-xs text-slate-500">
                              {matchTypeLabel(item.match_type)}
                              {item.is_primary ? " · 主映射" : ""}
                              {item.confidence != null ? ` · ${(item.confidence * 100).toFixed(0)}%` : ""}
                            </div>
                          </div>
                          <div className="font-mono text-xs text-slate-400">{item.ths_code || "—"}</div>
                        </div>
                        <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
                          <WindowMetricCard
                            title="今日"
                            value={fmtYi(panel?.todayNet)}
                            rawValue={panel?.todayNet}
                            pctValue={panel?.todayPct}
                            compact
                          />
                          <WindowMetricCard
                            title="近5日"
                            value={fmtYi(panel?.netWindow5)}
                            rawValue={panel?.netWindow5}
                            pctValue={panel?.pctWindow5}
                            compact
                          />
                          <WindowMetricCard
                            title="近10日"
                            value={fmtYi(panel?.netWindow10)}
                            rawValue={panel?.netWindow10}
                            pctValue={panel?.pctWindow10}
                            compact
                          />
                          <WindowMetricCard
                            title="近30日"
                            value={fmtYi(panel?.netWindow30)}
                            rawValue={panel?.netWindow30}
                            pctValue={panel?.pctWindow30}
                            compact
                          />
                        </div>
                        {panel?.historyRows.length ? (
                          <div className="min-h-0 flex-1 overflow-hidden">
                            <div className="grid h-full min-h-0 gap-3 overflow-hidden xl:grid-cols-[minmax(0,6.5fr)_minmax(0,3.5fr)]">
                              <div className="grid h-full min-h-0 grid-rows-[minmax(0,1fr)_minmax(0,1fr)] gap-3">
                                <div className="flex min-h-0 w-full flex-col overflow-hidden rounded-lg bg-slate-100 px-3 py-3">
                                  <div className="mb-2 flex items-center gap-4 text-xs">
                                    <button
                                      type="button"
                                      className={thsChartView === "net" ? "font-semibold text-slate-900" : "text-slate-400"}
                                      onClick={() => setThsChartView("net")}
                                    >
                                      近30日资金净流
                                    </button>
                                    <button
                                      type="button"
                                      className={thsChartView === "amount" ? "font-semibold text-slate-900" : "text-slate-400"}
                                      onClick={() => setThsChartView("amount")}
                                    >
                                      近30日成交额
                                    </button>
                                  </div>
                                  <div className="min-h-0 flex-1">
                                    <Sparkline
                                      data={thsChartView === "net" ? toThsSparkData(panel.historyRows) : toThsAmountSparkData(panel.historyRows)}
                                      height="100%"
                                      formatter={(v) => fmtYi(v)}
                                      dateTickStep={3}
                                      tooltipLabel={thsChartView === "net" ? "资金净流" : "成交额"}
                                      tooltipValueClassName={(point) => yiToneClass(point.value)}
                                      onHoverPointChange={(point) =>
                                        item.ths_code
                                          ? setThsHoveredDates((current) => {
                                              const nextValue = point?.fullDate ?? point?.date ?? null
                                              if (current[item.ths_code as string] === nextValue) {
                                                return current
                                              }
                                              return {
                                                ...current,
                                                [item.ths_code as string]: nextValue,
                                              }
                                            })
                                          : undefined
                                      }
                                      tooltipExtras={(point) => {
                                        const rowIndex = point.fullDate
                                          ? findHistoryRowIndexByTradeDate(panel.historyRows, point.fullDate)
                                          : panel.historyRows.findIndex((item) => String(item.tradeDate).slice(5) === point.date)
                                        const row = rowIndex >= 0 ? panel.historyRows[rowIndex] : null
                                        const prevRow = rowIndex > 0 ? panel.historyRows[rowIndex - 1] : null
                                        if (!row) return []
                                        const netValue = yiNumberToYuan(row["净额(亿)"])
                                        const prevNetValue = prevRow ? yiNumberToYuan(prevRow["净额(亿)"]) : null
                                        const amountValue = thsAmountYuan(row)
                                        const prevAmountValue = prevRow ? thsAmountYuan(prevRow) : null
                                        return thsChartView === "net"
                                          ? [
                                              {
                                                label: "资金净流较昨日",
                                                value: fmtSignedYi(diffNullable(netValue, prevNetValue)),
                                                valueClassName: yiToneClass(diffNullable(netValue, prevNetValue)),
                                              },
                                              { label: "成交额", value: fmtYi(amountValue), valueClassName: yiToneClass(amountValue) },
                                              {
                                                label: "成交额较昨日",
                                                value: fmtSignedYi(diffNullable(amountValue, prevAmountValue)),
                                                valueClassName: yiToneClass(diffNullable(amountValue, prevAmountValue)),
                                              },
                                            ]
                                          : [
                                              {
                                                label: "成交额较昨日",
                                                value: fmtSignedYi(diffNullable(amountValue, prevAmountValue)),
                                                valueClassName: yiToneClass(diffNullable(amountValue, prevAmountValue)),
                                              },
                                              { label: "资金净流", value: fmtYi(netValue), valueClassName: yiToneClass(netValue) },
                                              {
                                                label: "资金净流较昨日",
                                                value: fmtSignedYi(diffNullable(netValue, prevNetValue)),
                                                valueClassName: yiToneClass(diffNullable(netValue, prevNetValue)),
                                              },
                                            ]
                                      }}
                                    />
                                  </div>
                                </div>
                                <div className="flex min-h-0 flex-col overflow-hidden rounded-lg bg-slate-100 px-3 py-3">
                                  <div className="mb-2 flex items-center justify-between gap-3">
                                    <div className="text-xs font-medium text-slate-500">合并成分股</div>
                                    <div className="text-[11px] text-slate-400">
                                      {visibleData.constituents.length} 只{constituentSnapshotLabel ? ` · ${constituentSnapshotLabel}` : ""}
                                    </div>
                                  </div>
                                  <div className="grid grid-cols-[88px_minmax(0,1fr)_96px_72px_72px] gap-2 border-b border-slate-200 pb-2 text-[11px] font-medium text-slate-500">
                                    <div>代码</div>
                                    <div>名称</div>
                                    <div>来源</div>
                                    <div className="text-right">现价</div>
                                    <div className="text-right">涨跌幅</div>
                                  </div>
                                  <div className="mt-2 min-h-0 flex-1 overflow-y-auto pr-1">
                                    {visibleData.constituents.length ? (
                                      <div className="space-y-1.5">
                                        {visibleData.constituents.map((stock) => (
                                          <MergedConstituentRow
                                            key={`${stock.code || "na"}-${stock.name || "na"}`}
                                            item={stock}
                                            onOpenStock={(item) => {
                                              if (!item.code) return
                                              setSelectedStock({ code: item.code, name: item.name || item.code })
                                              setStockDialogOpen(true)
                                            }}
                                          />
                                        ))}
                                      </div>
                                    ) : (
                                      <div className="flex h-full items-center justify-center text-sm text-slate-500">暂无成分股</div>
                                    )}
                                  </div>
                                </div>
                              </div>
                              <div className="flex min-h-0 flex-col overflow-hidden rounded-lg bg-slate-100 px-3 py-3">
                                <div className="mb-3 flex items-center justify-between gap-3">
                                  <div className="text-xs font-medium text-slate-500">THS 行业 info</div>
                                  <div className="text-[11px] text-slate-400">{activeRow?.tradeDate || panel?.latestRow?.tradeDate || "NA"}</div>
                                </div>
                                <div className="min-h-0 flex-1 space-y-2.5 overflow-y-auto pr-1">
                                  <div className="grid grid-cols-2 gap-2">
                                    <InfoStatBlock label="当日排名" value={fmtRank(rankAtIndex(panel?.historyRows ?? [], activeRowIndex))} />
                                    <InfoStatBlock
                                      label="排名趋势"
                                      value={fmtRankTrendAtIndex(panel?.historyRows ?? [], activeRowIndex)}
                                      valueClassName={rankTrendToneClassAtIndex(panel?.historyRows ?? [], activeRowIndex)}
                                    />
                                  </div>
                                  <div className="grid grid-cols-3 gap-2">
                                    <InfoStatBlock label="近5日 rank" value={fmtRank(rankWindowAverageAtIndex(panel?.historyRows ?? [], activeRowIndex, 5))} compact />
                                    <InfoStatBlock label="近10日 rank" value={fmtRank(rankWindowAverageAtIndex(panel?.historyRows ?? [], activeRowIndex, 10))} compact />
                                    <InfoStatBlock label="近30日 rank" value={fmtRank(rankWindowAverageAtIndex(panel?.historyRows ?? [], activeRowIndex, 30))} compact />
                                  </div>
                                  <div className="rounded-md bg-white/80 px-2.5 py-2.5">
                                    <div className="mb-2 text-[11px] font-medium text-slate-500">近7天 rank 趋势</div>
                                    <div className="h-20">
                                      <Sparkline
                                        data={toThsRankSparkData(panel.historyRows)}
                                        color="neutral"
                                        height="100%"
                                        formatter={(v) => fmtRank(Math.abs(v))}
                                        dateTickStep={3}
                                        tooltipLabel="排名"
                                      />
                                    </div>
                                  </div>
                                  <div className="grid grid-cols-2 gap-2">
                                    <InfoStatBlock
                                      label="流入资金(亿)"
                                      value={fmtRawNumber(activeRow?.["流入资金(亿)"])}
                                      valueClassName="text-red-600"
                                      compact
                                    />
                                    <InfoStatBlock
                                      label="流出资金(亿)"
                                      value={fmtRawNumber(activeRow?.["流出资金(亿)"])}
                                      valueClassName="text-emerald-600"
                                      compact
                                    />
                                    <InfoStatBlock label="公司家数" value={fmtRawNumber(activeRow?.["公司家数"])} compact />
                                    <InfoStatBlock label="当前价(元)" value={fmtRawNumber(activeRow?.["当前价(元)"])} compact />
                                    <InfoStatBlock
                                      label="涨停数"
                                      value={fmtNullableInt(constituentLimitStats.limitUpCount)}
                                      valueClassName="text-red-600"
                                      compact
                                    />
                                    <InfoStatBlock
                                      label="跌停数"
                                      value={fmtNullableInt(constituentLimitStats.limitDownCount)}
                                      valueClassName="text-emerald-600"
                                      compact
                                    />
                                    <InfoStatBlock label="领涨股" value={activeRow?.["领涨股"] || "—"} compact />
                                    <InfoStatBlock
                                      label="领涨股涨跌幅"
                                      value={fmtRawPct(activeRow?.["领涨股涨跌幅"])}
                                      valueClassName={rawMetricToneClass(activeRow?.["领涨股涨跌幅"])}
                                      compact
                                    />
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className="flex min-h-[180px] flex-1 items-center justify-center rounded-lg bg-slate-100 text-sm text-slate-500 xl:w-1/2">
                            暂无 THS 历史数据
                          </div>
                        )}
                      </div>
                    </TabsContent>
                  )
                })}
              </Tabs>
            ) : null}
            <div className="rounded-lg bg-slate-100 px-3 py-3">
              <div className="flex flex-wrap gap-2 text-[11px] text-slate-600">
                <CompactInfoChip label="TDX 行业" value={visibleData.tdx_name || target.name} />
                <CompactInfoChip label="流向口径" value={visibleData.flow_sector_name || "—"} />
                <CompactInfoChip label="快照日期" value={fmtDate(visibleData.trade_date)} />
                <CompactInfoChip label="数据日期" value={fmtDate(visibleData.flow_trade_date)} />
                <CompactInfoChip label="状态" value={visibleData.snapshot_status || "—"} />
                <CompactInfoChip label="匹配" value={visibleData.name_match_type || "—"} />
                <CompactInfoChip label="Seed" value={visibleData.seed_code || "—"} mono />
                <CompactInfoChip label="样本条数" value={fmtNumber(visibleData.row_count)} />
                <CompactInfoChip label="来源" value={visibleData.source || "—"} mono />
              </div>
            </div>
          </div>
        </section>
      </div>

      <StockDetailDialog
        open={stockDialogOpen}
        onOpenChange={setStockDialogOpen}
        stockCode={selectedStock?.code ?? null}
        stockName={selectedStock?.name ?? null}
        industryName={visibleData.tdx_name || target.name}
      />
    </div>
  )
}

function MergedConstituentRow({
  item,
  onOpenStock,
}: {
  item: IndustryApplicationMergedConstituent
  onOpenStock: (item: IndustryApplicationMergedConstituent) => void
}) {
  const sources = (item.sources ?? []).map((value) => value.toUpperCase())
  const dateParts = [
    item.ths_snapshot_date ? `THS ${item.ths_snapshot_date}` : null,
    item.tdx_snapshot_date ? `TDX ${item.tdx_snapshot_date}` : null,
    item.quote_trade_date ? `CH ${item.quote_trade_date}` : null,
  ].filter(Boolean)
  return (
    <button
      type="button"
      className="grid w-full cursor-pointer grid-cols-[88px_minmax(0,1fr)_96px_72px_72px] items-center gap-2 rounded-md bg-white/80 px-2 py-2 text-left text-[11px] transition hover:bg-white hover:ring-1 hover:ring-slate-200"
      onClick={() => onOpenStock(item)}
      title={`查看 ${item.name || item.code || "个股"} 详情${dateParts.length ? ` · ${dateParts.join(" / ")}` : ""}`}
    >
      <div className="font-mono text-slate-700">{item.code || "—"}</div>
      <div className="truncate font-medium text-slate-900">{item.name || "—"}</div>
      <div className="flex flex-wrap gap-1">
        {sources.length ? (
          sources.map((source) => (
            <span key={source} className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-600">
              {source}
            </span>
          ))
        ) : (
          <span className="text-slate-400">—</span>
        )}
      </div>
      <div className="text-right text-slate-900">{fmtRawNumber(item.price)}</div>
      <div className={`text-right ${rawMetricToneClass(item.change_pct)}`}>{fmtRawPct(item.change_pct)}</div>
    </button>
  )
}

function WindowMetricCard({
  title,
  value,
  rawValue,
  pctValue,
  compact = false,
}: {
  title: string
  value: string
  rawValue?: number | null
  pctValue?: number | null
  compact?: boolean
}) {
  return (
    <section
      className={
        compact
          ? "flex min-h-[76px] flex-col justify-center rounded-lg bg-white px-3 py-2.5"
          : "flex min-h-[88px] flex-col justify-center rounded-none bg-slate-50 px-4 py-3 sm:rounded-2xl"
      }
    >
      <div className={compact ? "text-xs font-medium text-slate-500" : "text-sm font-medium text-slate-500"}>{title}</div>
      <div className={metricToneClass(rawValue, compact ? "mt-1 text-lg font-semibold leading-none" : "mt-1 text-2xl font-semibold leading-none")}>
        {value}
      </div>
      <div className={metricToneClass(pctValue, compact ? "mt-1.5 text-xs font-semibold leading-none" : "mt-2 text-sm font-semibold leading-none")}>
        {fmtPct(pctValue)}
      </div>
    </section>
  )
}

function CompactMetric({
  title,
  value,
  rawValue,
}: {
  title: string
  value: string
  rawValue?: number | null
}) {
  return (
    <div className="min-w-0">
      <div className="text-xs font-medium text-slate-500">{title}</div>
      <div className={metricToneClass(rawValue, "mt-1 text-xl font-semibold leading-none")}>{value}</div>
    </div>
  )
}

function metricToneClass(rawValue: number | null | undefined, baseClassName: string) {
  const tone =
    rawValue == null || Number.isNaN(rawValue)
      ? "text-slate-900"
      : rawValue >= 0
        ? "text-red-600"
        : "text-emerald-600"
  return `${baseClassName} ${tone}`
}

function sumLatestMainNet(
  history: Array<{ main_net?: number | null }>,
  windowSize: number,
) {
  const items = history.slice(-windowSize)
  if (items.length === 0) return null
  return items.reduce((sum, item) => sum + (item.main_net ?? 0), 0)
}

function calcTrailingReturn(
  rows: Array<{ tradeDate: string; close: number | null }>,
  windowSize: number,
) {
  const ordered = [...rows]
    .filter((item) => item.tradeDate && item.close != null && Number.isFinite(item.close))
    .sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))
  if (ordered.length <= windowSize) return null
  const latest = ordered[ordered.length - 1]?.close ?? null
  const base = ordered[ordered.length - 1 - windowSize]?.close ?? null
  if (latest == null || base == null || base === 0) return null
  return ((latest - base) / base) * 100
}

function CompactInfoChip({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="inline-flex min-w-0 items-center gap-2 rounded-md bg-white px-2.5 py-1.5">
      <span className="shrink-0 text-[11px] text-slate-500">{label}</span>
      <span className={mono ? "min-w-0 truncate font-mono text-[11px] text-slate-900" : "min-w-0 truncate text-[11px] text-slate-900"}>
        {value}
      </span>
    </div>
  )
}

function InfoStatBlock({
  label,
  value,
  valueClassName,
  compact = false,
}: {
  label: string
  value: string
  valueClassName?: string
  compact?: boolean
}) {
  return (
    <div className={`rounded-md bg-white/80 ${compact ? "px-2.5 py-2" : "px-2.5 py-2.5"}`}>
      <div className="text-[10px] text-slate-500">{label}</div>
      <div className={`mt-1 truncate font-semibold leading-none ${compact ? "text-xs" : "text-sm"} ${valueClassName ?? "text-slate-900"}`}>
        {value}
      </div>
    </div>
  )
}

type ThsFundPanelState = {
  latestRow: IndustryFundFlowRow | null
  history: IndustryApplicationTdxFundFlowPoint[]
  historyRows: IndustryFundFlowRow[]
  todayNet: number | null
  todayPct: number | null
  netWindow5: number | null
  netWindow10: number | null
  netWindow30: number | null
  pctWindow5: number | null
  pctWindow10: number | null
  pctWindow30: number | null
}

function normalizeThsFundPoint(row: IndustryFundFlowRow): IndustryApplicationTdxFundFlowPoint {
  return {
    trade_date: row.tradeDate ?? null,
    flow_trade_date: row.tradeDate ?? null,
    snapshot_status: row.source ?? "ths",
    selection_mode: "ths",
    main_net: yiNumberToYuan(row["净额(亿)"]),
    large_order_net: null,
    medium_order_net: null,
    small_order_net: null,
  }
}

function yiNumberToYuan(value: number | string | null | undefined) {
  const num = toFiniteNumber(value)
  if (num == null) return null
  return num * 1e8
}

function toFiniteNumber(value: number | string | null | undefined) {
  if (value == null) return null
  const num = typeof value === "number" ? value : Number(value)
  return Number.isFinite(num) ? num : null
}

function calcCompoundedChangePct(rows: IndustryFundFlowRow[], windowSize: number) {
  const items = rows
    .slice(-windowSize)
    .map((item) => toFiniteNumber(item["行业指数涨跌幅"]))
    .filter((item): item is number => item != null)
  if (items.length === 0) return null
  return (items.reduce((acc, pct) => acc * (1 + pct / 100), 1) - 1) * 100
}

function toThsSparkData(rows: IndustryFundFlowRow[]) {
  return rows
    .filter((row) => row.tradeDate)
    .sort((a, b) => String(a.tradeDate).localeCompare(String(b.tradeDate)))
    .map((row) => ({
      date: String(row.tradeDate).slice(5),
      fullDate: String(row.tradeDate),
      value: yiNumberToYuan(row["净额(亿)"]) ?? 0,
    }))
}

function toThsAmountSparkData(rows: IndustryFundFlowRow[]) {
  return rows
    .filter((row) => row.tradeDate)
    .sort((a, b) => String(a.tradeDate).localeCompare(String(b.tradeDate)))
    .map((row) => ({
      date: String(row.tradeDate).slice(5),
      fullDate: String(row.tradeDate),
      value: thsAmountYuan(row),
    }))
}

function toThsRankSparkData(rows: IndustryFundFlowRow[]) {
  return rows
    .filter((row) => row.tradeDate && toFiniteNumber(row["序号"]) != null)
    .sort((a, b) => String(a.tradeDate).localeCompare(String(b.tradeDate)))
    .slice(-7)
    .map((row) => ({
      date: String(row.tradeDate).slice(5),
      fullDate: String(row.tradeDate),
      value: -(toFiniteNumber(row["序号"]) ?? 0),
    }))
}

function thsAmountYuan(row: IndustryFundFlowRow) {
  const inflowYi = toFiniteNumber(row["流入资金(亿)"]) ?? 0
  const outflowYi = toFiniteNumber(row["流出资金(亿)"]) ?? 0
  return (inflowYi + outflowYi) * 1e8
}

function diffNullable(current: number | null | undefined, previous: number | null | undefined) {
  if (current == null || previous == null) return null
  return current - previous
}

function yiToneClass(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "text-foreground"
  if (value > 0) return "text-red-600"
  if (value < 0) return "text-emerald-600"
  return "text-slate-900"
}

function rawMetricToneClass(value: number | string | null | undefined) {
  const num = toFiniteNumber(value)
  if (num == null) return "text-slate-900"
  if (num > 0) return "text-red-600"
  if (num < 0) return "text-emerald-600"
  return "text-slate-900"
}

function fmtRawNumber(value: number | string | null | undefined) {
  const num = toFiniteNumber(value)
  if (num == null) return "—"
  return num.toLocaleString("zh-CN", { maximumFractionDigits: 2 })
}

function fmtRawPct(value: number | string | null | undefined) {
  const num = toFiniteNumber(value)
  if (num == null) return "—"
  const sign = num > 0 ? "+" : ""
  return `${sign}${num.toFixed(2)}%`
}

function fmtNullableInt(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  return value.toLocaleString("zh-CN", { maximumFractionDigits: 0 })
}

function calcConstituentLimitStats(items: IndustryApplicationMergedConstituent[]) {
  if (!items.length) {
    return { limitUpCount: null, limitDownCount: null }
  }
  let limitUpCount = 0
  let limitDownCount = 0
  for (const item of items) {
    const pct = toFiniteNumber(item.change_pct)
    if (isStStockName(item.name)) continue
    const limitPct = resolveStockLimitPct(item.code)
    if (pct == null) continue
    if (pct >= limitPct - limitTolerance(limitPct)) {
      limitUpCount += 1
    } else if (pct <= -limitPct + limitTolerance(limitPct)) {
      limitDownCount += 1
    }
  }
  return { limitUpCount, limitDownCount }
}

function formatConstituentSnapshotLabel(items: IndustryApplicationMergedConstituent[]) {
  const thsDates = uniqueSortedDates(items.map((item) => item.ths_snapshot_date))
  const tdxDates = uniqueSortedDates(items.map((item) => item.tdx_snapshot_date))
  const quoteDates = uniqueSortedDates(items.map((item) => item.quote_trade_date))
  const parts = [
    thsDates.length ? `THS ${formatDateSet(thsDates)}` : null,
    tdxDates.length ? `TDX ${formatDateSet(tdxDates)}` : null,
    quoteDates.length ? `CH ${formatDateSet(quoteDates)}` : null,
  ].filter(Boolean)
  return parts.join(" / ")
}

function uniqueSortedDates(values: Array<string | null | undefined>) {
  return Array.from(new Set(values.filter((value): value is string => Boolean(value)))).sort()
}

function formatDateSet(values: string[]) {
  if (values.length <= 1) return values[0] ?? ""
  return `${values[0]}~${values[values.length - 1]}`
}

function resolveStockLimitPct(code: string | null | undefined) {
  const normalizedCode = String(code ?? "").replace(/\D/g, "")
  if (
    normalizedCode.startsWith("300") ||
    normalizedCode.startsWith("301") ||
    normalizedCode.startsWith("302") ||
    normalizedCode.startsWith("688") ||
    normalizedCode.startsWith("689")
  ) {
    return 20
  }
  if (
    normalizedCode.startsWith("43") ||
    normalizedCode.startsWith("83") ||
    normalizedCode.startsWith("87") ||
    normalizedCode.startsWith("88") ||
    normalizedCode.startsWith("92")
  ) {
    return 30
  }
  return 10
}

function isStStockName(name: string | null | undefined) {
  return /(^|\*)ST/i.test(String(name ?? "").trim())
}

function limitTolerance(limitPct: number) {
  return limitPct <= 10 ? 0.3 : 0.5
}

function findHistoryRowIndexByTradeDate(rows: IndustryFundFlowRow[], tradeDate: string) {
  return rows.findIndex((row) => row.tradeDate === tradeDate || String(row.tradeDate ?? "").slice(5) === tradeDate)
}

function resolveRankIndex(rows: IndustryFundFlowRow[], activeIndex: number) {
  return activeIndex >= 0 ? activeIndex : rows.length - 1
}

function rankAtIndex(rows: IndustryFundFlowRow[], activeIndex: number) {
  const row = rows[resolveRankIndex(rows, activeIndex)]
  return row ? toFiniteNumber(row["序号"]) : null
}

function rankWindowAverageAtIndex(rows: IndustryFundFlowRow[], activeIndex: number, windowSize: number) {
  const resolvedIndex = resolveRankIndex(rows, activeIndex)
  if (resolvedIndex < 0 || resolvedIndex + 1 < windowSize) return null
  const items = rows.slice(resolvedIndex + 1 - windowSize, resolvedIndex + 1).map((row) => toFiniteNumber(row["序号"]))
  if (items.some((item) => item == null)) return null
  const values = items as number[]
  return values.reduce((sum, item) => sum + item, 0) / values.length
}

function rankTrendDeltaAtIndex(rows: IndustryFundFlowRow[], activeIndex: number) {
  const resolvedIndex = resolveRankIndex(rows, activeIndex)
  if (resolvedIndex <= 0) return null
  const latest = toFiniteNumber(rows[resolvedIndex]?.["序号"])
  const previous = toFiniteNumber(rows[resolvedIndex - 1]?.["序号"])
  if (latest == null || previous == null) return null
  return previous - latest
}

function fmtRank(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "NA"
  return `#${value.toFixed(value % 1 === 0 ? 0 : 1)}`
}

function fmtRankTrendAtIndex(rows: IndustryFundFlowRow[], activeIndex: number) {
  const delta = rankTrendDeltaAtIndex(rows, activeIndex)
  if (delta == null) return "NA"
  if (delta > 0) return `上升 ${delta.toFixed(0)}`
  if (delta < 0) return `下降 ${Math.abs(delta).toFixed(0)}`
  return "持平"
}

function rankTrendToneClassAtIndex(rows: IndustryFundFlowRow[], activeIndex: number) {
  const delta = rankTrendDeltaAtIndex(rows, activeIndex)
  if (delta == null || delta === 0) return "text-slate-900"
  return delta > 0 ? "text-red-600" : "text-emerald-600"
}
