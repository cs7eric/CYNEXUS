/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/market-pulse.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

/**
 * Stock Overview · 行情 (Market Pulse) 页面.
 *
 * 数据链路总览:
 *
 * 1. 页面接口
 *    - 强势板块 / 行业主力净流入 / 行业轮动(日 Top N):
 *      fetchMarketPulse() -> Java service composition under src/services/market/pulse.ts
 *      -> GET /api/market/industry-application/fund-flow
 *      -> GET /api/market/industry-application/fund-flow/history
 *    - 行业轮动历史趋势:
 *      fetchMarketPulseRotationTrend() -> GET /api/market/industry-application/fund-flow/history
 *    - 调度状态 / 手动触发:
 *      fetchMarketPulseSchedulerStatus() -> GET /api/core/scheduler/management/jobs
 *      triggerMarketPulseSnapshot() -> POST /api/core/scheduler/management/jobs/market_pulse_close/trigger
 *
 * 2. 后端 service
 *    - 行业资金流:
 *      IndustryApplicationController -> IndustryApplicationAppService -> IndustryApplicationRepository
 *    - 调度:
 *      Core SchedulerManagementController -> SchedulerManagementAppService
 *
 * 3. 原始数据源
 *    - 当前主来源: Java 持久化后的 sector_fund_flow_daily_snapshots
 *    - 字段: 行业涨跌幅 / 流入资金 / 流出资金 / 净额 / 公司家数 / 领涨股 / 领涨股涨跌幅 / 当前价
 *
 * 4. 持久化方式
 *    - 每个交易日的行业资金流快照存入 sector_fund_flow_daily_snapshots
 *    - MyBatis XML 负责最新日/历史日查询; 前端 service 只做 DTO 字段归一化
 *
 * 5. 行业轮动/历史趋势怎么来的
 *    - 行业轮动(日 Top N):
 *      读取某交易日 Postgres 快照 -> 取前 N 名 -> 返回 rows[]
 *    - 行业轮动历史趋势:
 *      读取最近 N 个交易日快照 -> 每日取 Top N -> 按行业名聚合 ->
 *      产出 appearances / ranks / changePcts / latestRank / bestRank / avgRank
 *
 * 6. 调度与落盘
 *    - Core scheduler jobs:
 *      market_pulse_inside / market_pulse_close / market_pulse_constituents
 *
 * 7. 历史补数/冷启动
 *    - 支撑数据缺失时先补 Java 表和采集链路，不在页面里兜旧 Python API
 */
import { useCallback, useEffect, useMemo, useState } from "react"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  fetchMarketPulse,
  fetchIndustryFundFlowIndustryList,
  fetchMarketPulseIndustryCompare,
  fetchMarketPulseRotationTrend,
  fetchMarketPulseSchedulerStatus,
  triggerMarketPulseSnapshot,
} from "@/services/market/pulse"

import { CapitalFlow } from "./components/CapitalFlow"
import { IndustryComparePanel } from "./components/IndustryComparePanel"
import { IndustryDetailDrawer } from "./components/IndustryDetailDrawer"
import { IndustryRotation } from "./components/IndustryRotation"
import { PageHeader, SchedulerStatusBar, SummaryStrip } from "./components/PageHeader"
import { RotationTrend } from "./components/RotationTrend"
import { StrongSectors } from "./components/StrongSectors"
import { INSIDE_REFRESH_MS } from "./lib/format"
import type {
  IndustryCompareResponse,
  IndustryFundFlowIndustryOption,
  MarketPulse,
  RotationTrendData,
  SchedulerStatus,
} from "./lib/types"

const ROTATION_TREND_ALL_DAYS = 365
const ROTATION_TREND_TOP_N = 10
const INDUSTRY_COMPARE_ALL_DAYS = 365
const INDUSTRY_COMPARE_DEFAULT_COUNT = 10

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

export default function StockOverviewMarketPulsePage() {
  const [data, setData] = useState<MarketPulse | null>(null)
  const [trend, setTrend] = useState<RotationTrendData | null>(null)
  const [industryCompare, setIndustryCompare] = useState<IndustryCompareResponse | null>(null)
  const [industryCompareLoading, setIndustryCompareLoading] = useState(false)
  const [industryOptions, setIndustryOptions] = useState<IndustryFundFlowIndustryOption[]>([])
  const [selectedIndustries, setSelectedIndustries] = useState<string[]>([])
  const [scheduler, setScheduler] = useState<SchedulerStatus | null>(null)
  const [loading, setLoading] = useState(false)
  const [picked, setPicked] = useState<string | null>(null)

  const compareOptions = useMemo(() => {
    const fromDb = industryOptions.map((item) => item.industry)
    const fromCurrentSnapshot = [
      ...(data?.flow?.inflow ?? []).map((item) => item.name),
      ...((data?.rotation?.rows ?? [])[0]?.items ?? []).map((item) => item.name),
      ...(data?.strong?.top ?? []).map((item) => item.name),
      ...(data?.flow?.outflow ?? []).map((item) => item.name),
    ]
    return [...new Set([...fromDb, ...fromCurrentSnapshot])].filter((item): item is string => Boolean(item))
  }, [data, industryOptions])
  const defaultCompareIndustries = useMemo(() => {
    const compositeTop = [...(trend?.industries ?? [])]
      .filter((item) => item.compositeRank != null && compareOptions.includes(item.name))
      .sort((a, b) => {
        const rankDiff = (a.compositeRank ?? Number.MAX_SAFE_INTEGER) - (b.compositeRank ?? Number.MAX_SAFE_INTEGER)
        if (rankDiff !== 0) return rankDiff
        return (b.compositeScore ?? -Infinity) - (a.compositeScore ?? -Infinity)
      })
      .map((item) => item.name)
      .slice(0, INDUSTRY_COMPARE_DEFAULT_COUNT)
    if (compositeTop.length) return compositeTop

    return [
      ...new Set([
        ...(data?.flow?.inflow ?? []).map((item) => item.name),
        ...((data?.rotation?.rows ?? [])[0]?.items ?? []).map((item) => item.name),
        ...(data?.strong?.top ?? []).map((item) => item.name),
      ]),
    ]
      .filter((item): item is string => Boolean(item) && compareOptions.includes(item))
      .slice(0, INDUSTRY_COMPARE_DEFAULT_COUNT)
  }, [compareOptions, data, trend?.industries])
  const effectiveSelectedIndustries = useMemo(() => {
    const kept = selectedIndustries.filter((item) => compareOptions.includes(item))
    return kept.length ? kept : defaultCompareIndustries
  }, [compareOptions, defaultCompareIndustries, selectedIndustries])
  const trendIndustryMeta = useMemo(
    () =>
      new Map(
        (trend?.industries ?? []).map((item) => [item.name, { compositeRank: item.compositeRank, compositeScore: item.compositeScore }]),
      ),
    [trend?.industries],
  )
  const effectiveIndustryCompare = useMemo<IndustryCompareResponse | null>(() => {
    if (!industryCompare) return null
    return {
      ...industryCompare,
      industries: industryCompare.industries.map((item) => ({
        ...item,
        ...trendIndustryMeta.get(item.name),
      })),
    }
  }, [industryCompare, trendIndustryMeta])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [market, trendData, schedulerStatus, industryList] = await Promise.all([
        fetchMarketPulse(),
        fetchMarketPulseRotationTrend(ROTATION_TREND_ALL_DAYS, ROTATION_TREND_TOP_N).catch(() => null),
        fetchMarketPulseSchedulerStatus().catch(() => null),
        fetchIndustryFundFlowIndustryList(INDUSTRY_COMPARE_ALL_DAYS).catch(() => null),
      ])
      setData(market as MarketPulse)
      if (trendData) setTrend(trendData as RotationTrendData)
      if (schedulerStatus) setScheduler(schedulerStatus as SchedulerStatus)
      if (industryList?.ok) setIndustryOptions(industryList.items)
    } catch (error: unknown) {
      notification.danger({ title: "行情数据加载失败", description: getErrorMessage(error) })
    } finally {
      setLoading(false)
    }
  }, [])

  const refreshSnapshot = useCallback(async () => {
    setLoading(true)
    try {
      const market = await fetchMarketPulse({ refreshRotation: true })
      setData(market as MarketPulse)
      notification.success({ title: "今日 Top 快照已落盘" })
      const trendData = await fetchMarketPulseRotationTrend(ROTATION_TREND_ALL_DAYS, ROTATION_TREND_TOP_N).catch(() => null)
      if (trendData) setTrend(trendData as RotationTrendData)
    } catch (error: unknown) {
      notification.danger({ title: "刷新快照失败", description: getErrorMessage(error) })
    } finally {
      setLoading(false)
    }
  }, [])

  const triggerScheduler = useCallback(async () => {
    setLoading(true)
    try {
      await triggerMarketPulseSnapshot()
      notification.success({ title: "已手动触发今日 snapshot" })
      await load()
    } catch (error: unknown) {
      notification.danger({ title: "手动 snapshot 失败", description: getErrorMessage(error) })
    } finally {
      setLoading(false)
    }
  }, [load])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  useEffect(() => {
    if (!scheduler?.isTradeTime) return
    const timer = window.setInterval(() => {
      void load()
    }, INSIDE_REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [scheduler?.isTradeTime, load])

  useEffect(() => {
    if (!effectiveSelectedIndustries.length) return
    let cancelled = false
    const timer = window.setTimeout(() => {
      if (!cancelled) setIndustryCompareLoading(true)
    }, 0)
    fetchMarketPulseIndustryCompare(effectiveSelectedIndustries, INDUSTRY_COMPARE_ALL_DAYS)
      .then((result) => {
        if (!cancelled) setIndustryCompare(result)
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          notification.danger({ title: "行业对比数据加载失败", description: getErrorMessage(error) })
        }
      })
      .finally(() => {
        if (!cancelled) setIndustryCompareLoading(false)
      })
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [effectiveSelectedIndustries])

  const handleAddIndustries = useCallback(
    (names: string[]) => {
      const validNames = names.filter((item) => compareOptions.includes(item))
      if (!validNames.length) return
      setSelectedIndustries((prev) => {
        const current = (prev.length ? prev : defaultCompareIndustries).filter((item) => compareOptions.includes(item))
        return [...new Set([...current, ...validNames])]
      })
    },
    [compareOptions, defaultCompareIndustries],
  )

  const handleRemoveIndustry = useCallback(
    (name: string) => {
      setSelectedIndustries((prev) => {
        const current = (prev.length ? prev : defaultCompareIndustries).filter((item) => compareOptions.includes(item))
        return current.filter((item) => item !== name)
      })
    },
    [compareOptions, defaultCompareIndustries],
  )

  const handleResetCompareIndustries = useCallback(() => {
    setSelectedIndustries(defaultCompareIndustries)
  }, [defaultCompareIndustries])

  return (
    <WorkspaceShell sectionLabel="Stock Overview" sectionUrl="/stock-overview" pageTitle="market pulse">
      <div className="h-[calc(100svh-4rem)] max-w-full space-y-4 overflow-y-auto overflow-x-hidden p-2 sm:p-4">
        <div className="hidden space-y-4 md:block">
          <PageHeader
            onRefresh={load}
            loading={loading}
            fetchedAt={data?.strong?.fetchedAt}
            flowElapsedMs={data?.flow?.elapsedMs}
            scheduler={scheduler}
            market={data}
          />
          <SchedulerStatusBar status={scheduler} onTrigger={triggerScheduler} />
          {data ? <SummaryStrip data={data} /> : null}
          <div className="grid gap-4 xl:grid-cols-2">
            <StrongSectors data={data?.strong} onPick={setPicked} />
            <CapitalFlow data={data?.flow} onPick={setPicked} />
          </div>
          <IndustryRotation data={data?.rotation} onRefreshSnapshot={refreshSnapshot} onPick={setPicked} />
          <RotationTrend data={trend} onPick={setPicked} />
          <IndustryComparePanel
            options={compareOptions}
            selected={effectiveSelectedIndustries}
            defaultCount={INDUSTRY_COMPARE_DEFAULT_COUNT}
            loading={industryCompareLoading}
            data={effectiveSelectedIndustries.length ? effectiveIndustryCompare : null}
            onAdd={handleAddIndustries}
            onRemove={handleRemoveIndustry}
            onResetDefault={handleResetCompareIndustries}
          />
        </div>

        <div className="space-y-3 md:hidden">
          <Tabs defaultValue="overview" className="w-full">
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="overview" className="text-xs">概览</TabsTrigger>
              <TabsTrigger value="sector" className="text-xs">板块</TabsTrigger>
              <TabsTrigger value="rotation" className="text-xs">轮动</TabsTrigger>
              <TabsTrigger value="compare" className="text-xs">对比</TabsTrigger>
            </TabsList>
            <TabsContent value="overview" className="mt-3 space-y-3">
              <PageHeader
                onRefresh={load}
                loading={loading}
                fetchedAt={data?.strong?.fetchedAt}
                flowElapsedMs={data?.flow?.elapsedMs}
                scheduler={scheduler}
                market={data}
              />
              <SchedulerStatusBar status={scheduler} onTrigger={triggerScheduler} />
              {data ? <SummaryStrip data={data} /> : null}
            </TabsContent>
            <TabsContent value="sector" className="mt-3 space-y-3">
              <StrongSectors data={data?.strong} onPick={setPicked} />
              <CapitalFlow data={data?.flow} onPick={setPicked} />
            </TabsContent>
            <TabsContent value="rotation" className="mt-3 space-y-3">
              <IndustryRotation data={data?.rotation} onRefreshSnapshot={refreshSnapshot} onPick={setPicked} />
              <RotationTrend data={trend} onPick={setPicked} />
            </TabsContent>
            <TabsContent value="compare" className="mt-3 space-y-3">
              <IndustryComparePanel
                options={compareOptions}
                selected={effectiveSelectedIndustries}
                defaultCount={INDUSTRY_COMPARE_DEFAULT_COUNT}
                loading={industryCompareLoading}
                data={effectiveSelectedIndustries.length ? effectiveIndustryCompare : null}
                onAdd={handleAddIndustries}
                onRemove={handleRemoveIndustry}
                onResetDefault={handleResetCompareIndustries}
              />
            </TabsContent>
          </Tabs>
        </div>
        <IndustryDetailDrawer name={picked} onClose={() => setPicked(null)} />
      </div>
    </WorkspaceShell>
  )
}
